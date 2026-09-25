package io.canvasmc.horizon.fabric.mixin;

import com.fasterxml.jackson.databind.JsonNode;
import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MixinPatches {
    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "mixin_patches");
    private static final String CALLBACK_INFO = "org/spongepowered/asm/mixin/injection/callback/CallbackInfo";
    private static final String CALLBACK_INFO_RETURNABLE = "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";

    private static final Map<String, List<Patch>> PATCHES = new ConcurrentHashMap<>();
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private MixinPatches() {
    }

    public static void load(@NonNull JsonNode patches) {
        patches.properties().forEach((mod) -> {
            Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(mod.getKey());
            if (container.isEmpty()) return;

            mod.getValue().properties().forEach((entry) -> {
                int split = entry.getKey().indexOf('#');
                if (split < 0 || !entry.getValue().isObject()) {
                    LOGGER.warn("Ignoring mixin patch {} for {}, expected \"mixin.Class#handler\": {{...}}", entry.getKey(), mod.getKey());
                    return;
                }

                JsonNode patch = entry.getValue();
                if (!matches(container.get(), patch.path("versions").asText(null))
                    || !matches(FabricLoader.getInstance().getModContainer("minecraft").orElseThrow(), patch.path("minecraft").asText(null))) {
                    return;
                }

                String mixin = entry.getKey().substring(0, split).replace('.', '/');
                PATCHES.computeIfAbsent(mixin, (key) -> new ArrayList<>())
                    .add(new Patch(mod.getKey(), mixin, entry.getKey().substring(split + 1), patch));
            });
        });
    }

    public static void apply(@NonNull ClassNode mixin) {
        List<Patch> patches = PATCHES.get(mixin.name);
        if (patches == null) {
            return;
        }

        for (Patch patch : patches) {
            List<MethodNode> handlers = mixin.methods.stream().filter((method) -> method.name.equals(patch.handler())).toList();
            if (handlers.isEmpty()) {
                if (REPORTED.add(patch.key())) {
                    LOGGER.warn("Mixin patch {} from the Paper overrides doesn't match anything in {}", patch.key(), patch.mod());
                }
                continue;
            }

            for (MethodNode handler : handlers) {
                apply(mixin, handler, patch);
            }
            if (REPORTED.add(patch.key())) {
                LOGGER.info("Patched mixin {} from {} ({})", patch.key(), patch.mod(), describe(patch.data()));
            }
        }
    }

    private static void apply(@NonNull ClassNode mixin, @NonNull MethodNode handler, @NonNull Patch patch) {
        JsonNode data = patch.data();
        if (data.path("disable").asBoolean(false)) {
            disable(mixin, handler);
            return;
        }

        AnnotationNode injector = MixinPreflight.injector(handler);
        if (injector == null) {
            return;
        }

        if (data.has("require")) {
            set(injector, "require", data.get("require").asInt());
        }
        if (data.has("method")) {
            set(injector, "method", new ArrayList<>(List.of(data.get("method").asText())));
        }
        if (data.has("at")) {
            retargetAt(injector, data.get("at"));
        }
        if (data.has("append")) {
            List<Type> types = new ArrayList<>();
            data.get("append").forEach((type) -> types.add(Type.getType(type.asText())));
            append(handler, types);
        }
    }

    private static void disable(@NonNull ClassNode mixin, @NonNull MethodNode handler) {
        boolean referenced = mixin.methods.stream().anyMatch((method) -> {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call && call.owner.equals(mixin.name) && call.name.equals(handler.name)) return true;
            }
            return false;
        });

        if (!referenced) {
            mixin.methods.remove(handler);
            return;
        }
        if (handler.visibleAnnotations != null) handler.visibleAnnotations.removeIf(MixinPreflight::isInjector);
        if (handler.invisibleAnnotations != null) handler.invisibleAnnotations.removeIf(MixinPreflight::isInjector);
    }

    private static void retargetAt(@NonNull AnnotationNode injector, @NonNull JsonNode at) {
        Object value = MixinPreflight.value(injector, "at");
        AnnotationNode point = value instanceof AnnotationNode single ? single
            : value instanceof List<?> list && !list.isEmpty() && list.get(at.path("index").asInt(0)) instanceof AnnotationNode indexed ? indexed
            : null;
        if (point == null) {
            return;
        }

        if (at.has("value")) set(point, "value", at.get("value").asText());
        if (at.has("target")) set(point, "target", at.get("target").asText());
        if (at.has("ordinal")) set(point, "ordinal", at.get("ordinal").asInt());
    }

    static void append(@NonNull MethodNode handler, @NonNull List<Type> types) {
        Type[] arguments = Type.getArgumentTypes(handler.desc);
        int callback = callbackIndex(arguments);
        List<Type> prefix = new ArrayList<>(Arrays.asList(arguments).subList(0, callback));
        int[] positions = new int[callback];
        for (int i = 0; i < callback; i++) positions[i] = i;
        prefix.addAll(types);
        reshape(handler, prefix, positions);
    }

    static void reshape(@NonNull MethodNode handler, @NonNull List<Type> prefix, int @NonNull [] positions) {
        Type[] arguments = Type.getArgumentTypes(handler.desc);
        int callback = callbackIndex(arguments);
        int base = (handler.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;

        int[] oldSlots = new int[callback];
        int slot = base;
        for (int i = 0; i < callback; i++) {
            oldSlots[i] = slot;
            slot += arguments[i].getSize();
        }
        int oldEnd = slot;

        int[] newSlots = new int[prefix.size()];
        slot = base;
        for (int i = 0; i < prefix.size(); i++) {
            newSlots[i] = slot;
            slot += prefix.get(i).getSize();
        }
        int delta = slot - oldEnd;

        int[] remap = new int[Math.max(oldEnd, 1)];
        for (int i = 0; i < remap.length; i++) remap[i] = i;
        for (int i = 0; i < callback; i++) remap[oldSlots[i]] = newSlots[positions[i]];

        List<Type> adapted = new ArrayList<>(prefix);
        adapted.addAll(Arrays.asList(arguments).subList(callback, arguments.length));
        handler.desc = Type.getMethodDescriptor(Type.getReturnType(handler.desc), adapted.toArray(Type[]::new));
        handler.signature = null;

        for (AbstractInsnNode insn : handler.instructions.toArray()) {
            if (insn instanceof VarInsnNode variable) variable.var = slot(variable.var, oldEnd, delta, remap);
            if (insn instanceof IincInsnNode increment) increment.var = slot(increment.var, oldEnd, delta, remap);
            if (insn instanceof FrameNode) handler.instructions.remove(insn);
        }
        if (handler.localVariables != null) {
            for (LocalVariableNode local : handler.localVariables) local.index = slot(local.index, oldEnd, delta, remap);
        }
        handler.maxLocals += Math.max(delta, 0);
        handler.visibleParameterAnnotations = moveParameters(handler.visibleParameterAnnotations, callback, prefix.size(), positions);
        handler.invisibleParameterAnnotations = moveParameters(handler.invisibleParameterAnnotations, callback, prefix.size(), positions);
        handler.visibleAnnotableParameterCount = 0;
        handler.invisibleAnnotableParameterCount = 0;
        handler.parameters = null;
    }

    static void upgradeCallback(@NonNull MethodNode handler) {
        Type[] arguments = Type.getArgumentTypes(handler.desc);
        int callback = callbackIndex(arguments);
        if (callback >= arguments.length || !arguments[callback].getInternalName().equals(CALLBACK_INFO)) {
            return;
        }

        arguments[callback] = Type.getObjectType(CALLBACK_INFO_RETURNABLE);
        handler.desc = Type.getMethodDescriptor(Type.getReturnType(handler.desc), arguments);
        handler.signature = null;
        if (handler.localVariables != null) {
            for (LocalVariableNode local : handler.localVariables) {
                if (local.desc.equals("L" + CALLBACK_INFO + ";")) {
                    local.desc = "L" + CALLBACK_INFO_RETURNABLE + ";";
                    local.signature = null;
                }
            }
        }
    }

    static int callbackIndex(Type @NonNull [] arguments) {
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].getSort() == Type.OBJECT
                && (arguments[i].getInternalName().equals(CALLBACK_INFO) || arguments[i].getInternalName().equals(CALLBACK_INFO_RETURNABLE))) {
                return i;
            }
        }
        return arguments.length;
    }

    private static int slot(int slot, int oldEnd, int delta, int @NonNull [] remap) {
        if (slot >= oldEnd) return slot + delta;
        return remap[slot];
    }

    @SuppressWarnings("unchecked")
    private static List<AnnotationNode> @Nullable [] moveParameters(List<AnnotationNode> @Nullable [] annotations, int callback, int prefix, int @NonNull [] positions) {
        if (annotations == null) {
            return null;
        }

        List<AnnotationNode>[] result = new List[annotations.length - callback + prefix];
        for (int i = 0; i < annotations.length; i++) {
            result[i < callback ? positions[i] : i - callback + prefix] = annotations[i];
        }
        return result;
    }

    static void set(@NonNull AnnotationNode annotation, @NonNull String key, @NonNull Object value) {
        if (annotation.values == null) annotation.values = new ArrayList<>();
        for (int i = 0; i < annotation.values.size() - 1; i += 2) {
            if (key.equals(annotation.values.get(i))) {
                annotation.values.set(i + 1, value);
                return;
            }
        }
        annotation.values.add(key);
        annotation.values.add(value);
    }

    private static boolean matches(@NonNull ModContainer mod, @Nullable String predicate) {
        if (predicate == null) {
            return true;
        }

        try {
            return VersionPredicate.parse(predicate).test(mod.getMetadata().getVersion());
        } catch (VersionParsingException exception) {
            LOGGER.warn("Ignoring invalid version range {} in a mixin patch for {}", predicate, mod.getMetadata().getId());
            return false;
        }
    }

    private static @NonNull String describe(@NonNull JsonNode data) {
        List<String> parts = new ArrayList<>();
        data.properties().forEach((field) -> {
            if (!field.getKey().equals("versions") && !field.getKey().equals("minecraft")) parts.add(field.getKey() + "=" + field.getValue());
        });
        return String.join(", ", parts);
    }

    private record Patch(String mod, String mixin, String handler, JsonNode data) {
        String key() {
            return mixin.replace('/', '.') + "#" + handler;
        }
    }
}
