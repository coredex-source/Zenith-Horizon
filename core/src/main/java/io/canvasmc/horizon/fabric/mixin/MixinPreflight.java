package io.canvasmc.horizon.fabric.mixin;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import io.canvasmc.horizon.util.Util;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.service.MixinService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MixinPreflight {
    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "mixin_preflight");

    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String PSEUDO = "Lorg/spongepowered/asm/mixin/Pseudo;";
    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String OVERWRITE = "Lorg/spongepowered/asm/mixin/Overwrite;";
    private static final String ACCESSOR = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String MODIFY_VARIABLE = "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;";
    private static final String GROUP = "Lorg/spongepowered/asm/mixin/injection/Group;";
    private static final String COERCE = "Lorg/spongepowered/asm/mixin/injection/Coerce;";
    private static final String SURROGATE = "Lorg/spongepowered/asm/mixin/injection/Surrogate;";
    private static final String CALLBACK_INFO = "org/spongepowered/asm/mixin/injection/callback/CallbackInfo";
    private static final String CALLBACK_INFO_RETURNABLE = "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    private static final Set<String> INJECTORS = Set.of(
        INJECT,
        "Lorg/spongepowered/asm/mixin/injection/Redirect;",
        "Lorg/spongepowered/asm/mixin/injection/ModifyArg;",
        "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;",
        MODIFY_VARIABLE,
        "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;",
        "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;",
        "Lcom/llamalad7/mixinextras/injector/ModifyReceiver;",
        "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;",
        "Lcom/llamalad7/mixinextras/injector/WrapWithCondition;",
        "Lcom/llamalad7/mixinextras/injector/v2/WrapWithCondition;",
        "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;",
        "Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;"
    );
    private static final Set<String> MEMBER_POINTS = Set.of("INVOKE", "INVOKE_ASSIGN", "INVOKE_STRING", "FIELD", "NEW");
    private static final Pattern ACCESSOR_NAME = Pattern.compile("^(get|is|set|call|invoke|new|create)(([A-Z])(.*?))(_\\$md.*)?$");

    private static final Map<String, Optional<ClassNode>> CLASSES = new HashMap<>();
    private static final Map<String, List<Member>> MERGED_FIELDS = new HashMap<>();
    private static final Map<String, List<Member>> MERGED_METHODS = new HashMap<>();
    private static final Map<String, Declared> MIXINS = new HashMap<>();
    private static final Map<String, Set<String>> BROKEN = new TreeMap<>();
    private static final Map<String, Adaptations> ADAPTATIONS = new HashMap<>();
    private static final Map<String, Set<String>> STRIPS = new HashMap<>();
    private static final Map<String, IMixinConfigPlugin> PLUGINS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> STRIPPED = new TreeMap<>();
    private static boolean indexed;
    private static boolean indexing;

    private MixinPreflight() {
    }

    public static synchronized boolean check(FabricMixinConfigs.@NonNull Entry entry, @NonNull String targetClassName, @NonNull String mixinClassName) {
        ClassNode mixin = classNode(mixinClassName);
        if (mixin == null) {
            return true;
        }

        index();
        String target = targetClassName.replace('.', '/');
        PreflightPolicy policy = HorizonLoader.getInstance().getProperties().mixinPreflight();
        Findings findings = inspect(entry, mixin, target);
        List<String> problems = findings.problems();
        if (problems.isEmpty() && policy != PreflightPolicy.WARN) {
            String parent = brokenParent(mixin);
            if (parent != null) problems.add("it extends the mixin " + parent + ", which doesn't match this server");
        }
        if (problems.isEmpty() || (policy == PreflightPolicy.DISABLE_MIXIN && findings.strippable())) {
            return true;
        }

        String mixinName = mixinClassName.startsWith(entry.mixinPackage() + ".")
            ? mixinClassName.substring(entry.mixinPackage().length() + 1)
            : mixinClassName;
        BROKEN.computeIfAbsent(entry.modId(), (key) -> new TreeSet<>()).add(mixinName);

        String targetName = target.replace('/', '.');
        if (policy == PreflightPolicy.FAIL) {
            LOGGER.error("Mixin {} from {} ({}) doesn't match {}:", mixinName, entry.modId(), entry.name(), targetName);
            problems.forEach((problem) -> LOGGER.error("  - {}", problem));
            return false;
        }

        String header = policy == PreflightPolicy.DISABLE_MIXIN ? "Disabling mixin {} from {} ({}) for {}:" : "Mixin {} from {} ({}) doesn't match {}:";
        LOGGER.warn(header, mixinName, entry.modId(), entry.name(), targetName);
        problems.forEach((problem) -> LOGGER.warn("  - {}", problem));
        return policy == PreflightPolicy.WARN;
    }

    public static void adapt(@NonNull ClassNode mixin) {
        FabricMixinConfigs.Entry entry = FabricMixinConfigs.ofMixin(mixin.name);
        if (entry != null) {
            adapt(entry, mixin);
        }
    }

    private static synchronized void adapt(FabricMixinConfigs.@NonNull Entry entry, @NonNull ClassNode mixin) {
        Adaptations adaptations = ADAPTATIONS.get(mixin.name);
        if (adaptations == null) {
            adaptations = new Adaptations(adaptations(entry, mixin), retypes(entry, mixin));
            ADAPTATIONS.put(mixin.name, adaptations);
        }

        for (Retype retype : adaptations.fields()) {
            for (FieldNode field : mixin.fields) {
                if (field.name.equals(retype.field()) && field.desc.equals(retype.from())) {
                    field.desc = retype.to();
                    field.signature = null;
                }
            }
            for (MethodNode method : mixin.methods) {
                for (AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof FieldInsnNode access && access.owner.equals(mixin.name) && access.name.equals(retype.field()) && access.desc.equals(retype.from())) {
                        access.desc = retype.to();
                    }
                }
            }
        }

        for (Adaptation adaptation : adaptations.handlers()) {
            for (MethodNode handler : mixin.methods) {
                if (!handler.name.equals(adaptation.handler()) || !handler.desc.equals(adaptation.desc())) continue;

                AnnotationNode injector = injector(handler);
                if (injector != null && adaptation.method() != null) {
                    MixinPatches.set(injector, "method", new ArrayList<>(List.of(adaptation.method())));
                }
                if (adaptation.returnable()) {
                    MixinPatches.upgradeCallback(handler);
                }
                if (adaptation.positions() != null) {
                    MixinPatches.reshape(handler, adaptation.parameters(), adaptation.positions());
                }
                break;
            }
        }

        if (indexing || HorizonLoader.getInstance().getProperties().mixinPreflight() != PreflightPolicy.DISABLE_MIXIN) {
            return;
        }
        Set<String> strip = STRIPS.get(mixin.name);
        if (strip == null) {
            strip = strips(entry, mixin);
            STRIPS.put(mixin.name, strip);
        }
        for (MethodNode handler : mixin.methods) {
            if (strip.contains(handler.name + handler.desc)) {
                if (handler.visibleAnnotations != null) handler.visibleAnnotations.removeIf((annotation) -> INJECTORS.contains(annotation.desc));
                if (handler.invisibleAnnotations != null) handler.invisibleAnnotations.removeIf((annotation) -> INJECTORS.contains(annotation.desc));
            }
        }
    }

    static void plugin(@NonNull String config, @NonNull IMixinConfigPlugin plugin) {
        PLUGINS.put(config, plugin);
    }

    private static @NonNull Set<String> strips(FabricMixinConfigs.@NonNull Entry entry, @NonNull ClassNode mixin) {
        AnnotationNode annotation = annotation(mixin.visibleAnnotations, mixin.invisibleAnnotations, MIXIN);
        if (annotation == null) {
            return Set.of();
        }

        index();
        Set<String> handlers = new LinkedHashSet<>();
        List<String> problems = new ArrayList<>();
        IMixinConfigPlugin plugin = PLUGINS.get(entry.name());
        for (String target : targets(annotation)) {
            if (plugin != null && !plugin.shouldApplyMixin(target.replace('/', '.'), mixin.name.replace('/', '.'))) continue;

            Findings findings = inspect(entry, mixin, target);
            if (findings.problems().isEmpty()) continue;
            if (!findings.strippable()) return Set.of();

            handlers.addAll(findings.injectors());
            problems.addAll(findings.problems());
        }

        if (!handlers.isEmpty()) {
            String name = mixin.name.replace('/', '.');
            String mixinName = name.startsWith(entry.mixinPackage() + ".") ? name.substring(entry.mixinPackage().length() + 1) : name;
            LOGGER.warn("Disabling {} injector(s) in mixin {} from {} ({}), the rest of the mixin still applies:", handlers.size(), mixinName, entry.modId(), entry.name());
            problems.forEach((problem) -> LOGGER.warn("  - {}", problem));
            STRIPPED.merge(entry.modId(), handlers.size(), Integer::sum);
        }
        return handlers;
    }

    private static @NonNull List<Adaptation> adaptations(FabricMixinConfigs.@NonNull Entry entry, @NonNull ClassNode mixin) {
        AnnotationNode annotation = annotation(mixin.visibleAnnotations, mixin.invisibleAnnotations, MIXIN);
        List<String> targets = annotation != null ? targets(annotation) : List.of();
        ClassNode target = targets.size() == 1 ? classNode(targets.getFirst()) : null;
        if (target == null) {
            return List.of();
        }

        List<Adaptation> adaptations = new ArrayList<>();
        for (MethodNode handler : mixin.methods) {
            AnnotationNode injector = injector(handler);
            if (injector == null || injector.desc.equals(MODIFY_VARIABLE)) continue;
            AnnotationNode inject = injector.desc.equals(INJECT) ? injector : null;
            List<String> selectors = value(injector, "target") == null ? strings(value(injector, "method")) : List.of();
            Selector selector = selectors.size() == 1 ? Selector.parse(selectors.getFirst()) : null;
            if (selector == null || selector.all() || selector.name() == null) continue;

            List<MethodNode> selected = selector.select(target, (handler.access & Opcodes.ACC_STATIC) != 0);
            if (selected.isEmpty()) continue;

            MethodNode method = selected.getFirst();
            String retarget = null;
            List<AnnotationNode> points = points(injector);
            if (selector.desc() == null && points != null && evaluable(points) && !found(points, List.of(method))) {
                MethodNode original = method;
                List<MethodNode> candidates = target.methods.stream()
                    .filter((candidate) -> candidate != original && candidate.name.equals(original.name) && found(points, List.of(candidate)))
                    .toList();
                if (candidates.size() != 1) continue;

                method = candidates.getFirst();
                retarget = method.name + method.desc;
            }
            if (inject == null) {
                if (retarget != null) {
                    adaptations.add(new Adaptation(handler.name, handler.desc, retarget, false, List.of(), null));
                    LOGGER.info("Adapting {} {} in {} from {} to {}::{}{}", simpleName(injector.desc), handler.name, mixin.name.replace('/', '.'), entry.modId(),
                        target.name.replace('/', '.'), method.name, method.desc);
                }
                continue;
            }
            if (retarget == null && selector.desc() == null) {
                MethodNode delegate = delegate(target, method);
                if (delegate != null) {
                    method = delegate;
                    retarget = method.name + method.desc;
                }
            }

            Type[] arguments = Type.getArgumentTypes(handler.desc);
            int callback = MixinPatches.callbackIndex(arguments);
            if (callback == arguments.length) continue;

            String expected = Type.getReturnType(method.desc).getSort() == Type.VOID ? CALLBACK_INFO : CALLBACK_INFO_RETURNABLE;
            boolean returnable = false;
            if (!arguments[callback].getInternalName().equals(expected)) {
                if (!expected.equals(CALLBACK_INFO_RETURNABLE) || Boolean.TRUE.equals(value(inject, "cancellable"))) continue;
                returnable = true;
            }

            Type[] parameters = Type.getArgumentTypes(method.desc);
            int[] positions = callback == 0 ? null : align(handler, Arrays.copyOf(arguments, callback), parameters);
            if (callback > 0 && positions == null) continue;
            if (positions != null && callback == parameters.length) positions = null;
            if (retarget == null && !returnable && positions == null) continue;

            adaptations.add(new Adaptation(handler.name, handler.desc, retarget, returnable, List.of(parameters), positions));
            LOGGER.info("Adapting @Inject {} in {} from {} to {}::{}{}", handler.name, mixin.name.replace('/', '.'), entry.modId(),
                target.name.replace('/', '.'), method.name, method.desc);
        }
        return adaptations;
    }

    private static @NonNull List<Retype> retypes(FabricMixinConfigs.@NonNull Entry entry, @NonNull ClassNode mixin) {
        AnnotationNode annotation = annotation(mixin.visibleAnnotations, mixin.invisibleAnnotations, MIXIN);
        List<String> targets = annotation != null ? targets(annotation) : List.of();
        ClassNode target = targets.size() == 1 ? classNode(targets.getFirst()) : null;
        if (target == null) {
            return List.of();
        }

        List<Retype> retypes = new ArrayList<>();
        for (FieldNode field : mixin.fields) {
            AnnotationNode shadow = annotation(field.visibleAnnotations, field.invisibleAnnotations, SHADOW);
            if (shadow == null || Type.getType(field.desc).getSort() != Type.OBJECT) continue;

            List<String> names = aliases(field.name, shadow);
            if (target.fields.stream().anyMatch((candidate) -> names.contains(candidate.name) && candidate.desc.equals(field.desc))) continue;

            FieldNode narrowed = target.fields.stream()
                .filter((candidate) -> names.contains(candidate.name) && Type.getType(candidate.desc).getSort() == Type.OBJECT)
                .findFirst()
                .orElse(null);
            if (narrowed == null || writes(mixin, field)
                || !subtype(Type.getType(narrowed.desc).getInternalName(), Type.getType(field.desc).getInternalName())) {
                continue;
            }

            retypes.add(new Retype(field.name, field.desc, narrowed.desc));
            LOGGER.info("Adapting @Shadow field {} in {} from {} to {}", field.name, mixin.name.replace('/', '.'), entry.modId(), Type.getType(narrowed.desc).getClassName());
        }
        return retypes;
    }

    private static boolean writes(@NonNull ClassNode mixin, @NonNull FieldNode field) {
        for (MethodNode method : mixin.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof FieldInsnNode access && access.owner.equals(mixin.name) && access.name.equals(field.name)
                    && (access.getOpcode() == Opcodes.PUTFIELD || access.getOpcode() == Opcodes.PUTSTATIC)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean subtype(@NonNull String type, @NonNull String parent) {
        for (String current = type; current != null; ) {
            if (current.equals(parent)) return true;
            ClassNode node = classNode(current);
            if (node == null) return false;
            if (node.interfaces.contains(parent)) return true;
            current = node.superName;
        }
        return false;
    }

    private static @Nullable MethodNode delegate(@NonNull ClassNode target, @NonNull MethodNode method) {
        MethodInsnNode call = null;
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof InvokeDynamicInsnNode) return null;
            if (!(insn instanceof MethodInsnNode invoke)) continue;
            if (call != null) return null;
            call = invoke;
        }

        if (call == null || !call.owner.equals(target.name) || !call.name.equals(method.name) || call.desc.equals(method.desc)
            || !Type.getReturnType(call.desc).equals(Type.getReturnType(method.desc))) {
            return null;
        }

        MethodInsnNode delegate = call;
        return target.methods.stream()
            .filter((candidate) -> candidate.name.equals(delegate.name) && candidate.desc.equals(delegate.desc)
                && (candidate.access & Opcodes.ACC_STATIC) == (method.access & Opcodes.ACC_STATIC))
            .findFirst()
            .orElse(null);
    }

    private static int @Nullable [] align(@NonNull MethodNode handler, Type @NonNull [] parameters, Type @NonNull [] target) {
        for (int i = 0; i < parameters.length; i++) {
            if (coerced(handler, i)) return null;
        }

        int[] leftmost = new int[parameters.length];
        int position = 0;
        for (int i = 0; i < parameters.length; i++) {
            while (position < target.length && !target[position].equals(parameters[i])) position++;
            if (position == target.length) return null;
            leftmost[i] = position++;
        }

        int[] rightmost = new int[parameters.length];
        position = target.length - 1;
        for (int i = parameters.length - 1; i >= 0; i--) {
            while (position >= 0 && !target[position].equals(parameters[i])) position--;
            if (position < 0) return null;
            rightmost[i] = position--;
        }
        return Arrays.equals(leftmost, rightmost) ? leftmost : null;
    }

    public static synchronized void finish() {
        CLASSES.clear();
        MIXINS.clear();
        MERGED_FIELDS.clear();
        MERGED_METHODS.clear();
        indexed = false;

        if (!STRIPPED.isEmpty()) {
            LOGGER.warn("Disabled {} injector(s) that don't match this server: {}", STRIPPED.values().stream().mapToInt(Integer::intValue).sum(),
                STRIPPED.entrySet().stream().map((entry) -> entry.getKey() + " (" + entry.getValue() + ")").collect(Collectors.joining(", ")));
            STRIPPED.clear();
        }
        if (BROKEN.isEmpty()) {
            return;
        }

        int total = BROKEN.values().stream().mapToInt(Set::size).sum();
        String mods = BROKEN.entrySet().stream()
            .map((entry) -> entry.getKey() + " (" + entry.getValue().size() + ")")
            .collect(Collectors.joining(", "));
        BROKEN.clear();

        switch (HorizonLoader.getInstance().getProperties().mixinPreflight()) {
            case FAIL -> throw Util.kill("Found " + total + " mixin(s) that don't match this server in " + mods
                + ". Set mixinPreflight to disable-mixin in horizon.yml to disable them instead", null);
            case DISABLE_MIXIN -> LOGGER.warn("Disabled {} mixin(s) that don't match this server: {}", total, mods);
            case WARN -> LOGGER.warn("Found {} mixin(s) that don't match this server, applying them anyway: {}", total, mods);
        }
    }

    private static @NonNull Findings inspect(FabricMixinConfigs.@NonNull Entry entry, @NonNull ClassNode mixin, @NonNull String target) {
        List<String> problems = new ArrayList<>();
        Set<String> injectors = new LinkedHashSet<>();
        ClassNode node = classNode(target);
        if (node == null) {
            if (annotation(mixin.visibleAnnotations, mixin.invisibleAnnotations, PSEUDO) == null && missingTargetIsFatal(entry)) {
                problems.add("target class " + target.replace('/', '.') + " doesn't exist");
            }
            return new Findings(problems, injectors, 0);
        }

        for (FieldNode field : mixin.fields) {
            AnnotationNode shadow = annotation(field.visibleAnnotations, field.invisibleAnnotations, SHADOW);
            if (shadow == null) continue;

            List<String> names = aliases(field.name, shadow);
            boolean found = node.fields.stream().anyMatch((candidate) -> names.contains(candidate.name) && candidate.desc.equals(field.desc))
                || merged(MERGED_FIELDS, target, mixin).stream().anyMatch((member) -> names.contains(member.name()) && member.desc().equals(field.desc));
            if (!found) problems.add("@Shadow field " + field.name + " " + field.desc + " doesn't exist");
        }

        int injectorProblems = 0;
        for (MethodNode method : mixin.methods) {
            for (AnnotationNode annotation : annotations(method.visibleAnnotations, method.invisibleAnnotations)) {
                switch (annotation.desc) {
                    case SHADOW, OVERWRITE -> checkMethod(problems, mixin, method, annotation, node);
                    case ACCESSOR -> checkAccessor(problems, mixin, method, annotation, node);
                    case INVOKER -> checkInvoker(problems, mixin, method, annotation, node);
                    default -> {
                        if (INJECTORS.contains(annotation.desc)) {
                            int before = problems.size();
                            checkInjector(problems, entry, mixin, method, annotation, node);
                            if (problems.size() > before) {
                                injectors.add(method.name + method.desc);
                                injectorProblems += problems.size() - before;
                            }
                        }
                    }
                }
            }
        }
        return new Findings(problems, injectors, injectorProblems);
    }

    private static @Nullable String brokenParent(@NonNull ClassNode mixin) {
        Declared parent = MIXINS.get(mixin.superName);
        ClassNode node = parent != null ? classNode(mixin.superName) : null;
        if (node == null) return null;

        for (String target : parent.targets()) {
            if (!inspect(parent.entry(), node, target).problems().isEmpty()) return node.name.replace('/', '.');
        }
        return brokenParent(node);
    }

    private static boolean missingTargetIsFatal(FabricMixinConfigs.@NonNull Entry entry) {
        if (MixinEnvironment.getCurrentEnvironment().getOption(MixinEnvironment.Option.DEBUG_TARGETS)) return true;

        IMixinConfig config = FabricMixinConfigs.config(entry.name());
        if (config == null) return entry.required();
        return config.isRequired() && FabricUtil.getCompatibility(config) >= FabricUtil.COMPATIBILITY_0_17_4;
    }

    private static void checkMethod(@NonNull List<String> problems, @NonNull ClassNode mixin, @NonNull MethodNode method, @NonNull AnnotationNode annotation, @NonNull ClassNode target) {
        String name = method.name;
        if (annotation.desc.equals(SHADOW)) {
            Object prefix = value(annotation, "prefix");
            String shadowPrefix = prefix instanceof String value ? value : "shadow$";
            if (name.startsWith(shadowPrefix)) name = name.substring(shadowPrefix.length());
        }

        List<String> names = aliases(name, annotation);
        boolean found = target.methods.stream().anyMatch((candidate) -> names.contains(candidate.name) && candidate.desc.equals(method.desc))
            || merged(MERGED_METHODS, target.name, mixin).stream().anyMatch((member) -> names.contains(member.name()) && member.desc().equals(method.desc));
        if (!found) problems.add(simpleName(annotation.desc) + " method " + name + method.desc + " doesn't exist");
    }

    private static void checkAccessor(@NonNull List<String> problems, @NonNull ClassNode mixin, @NonNull MethodNode method, @NonNull AnnotationNode annotation, @NonNull ClassNode target) {
        String name = accessorTarget(method, annotation);
        if (name == null) return;

        boolean found = target.fields.stream().anyMatch((field) -> field.name.equals(name))
            || merged(MERGED_FIELDS, target.name, mixin).stream().anyMatch((member) -> member.name().equals(name));
        if (!found) problems.add("@Accessor " + method.name + " needs the field " + name + ", which doesn't exist");
    }

    private static void checkInvoker(@NonNull List<String> problems, @NonNull ClassNode mixin, @NonNull MethodNode method, @NonNull AnnotationNode annotation, @NonNull ClassNode target) {
        String name = accessorTarget(method, annotation);
        boolean factory = !(value(annotation, "value") instanceof String value && !value.isEmpty())
            && (method.name.startsWith("new") || method.name.startsWith("create"));
        if (name == null || factory || name.equals("<init>")) return;

        boolean found = target.methods.stream().anyMatch((candidate) -> candidate.name.equals(name))
            || merged(MERGED_METHODS, target.name, mixin).stream().anyMatch((member) -> member.name().equals(name));
        if (!found) problems.add("@Invoker " + method.name + " needs the method " + name + ", which doesn't exist");
    }

    private static void checkInjector(@NonNull List<String> problems, FabricMixinConfigs.@NonNull Entry entry, @NonNull ClassNode mixin, @NonNull MethodNode handler, @NonNull AnnotationNode injector, @NonNull ClassNode target) {
        List<String> selectors = strings(value(injector, "method"));
        if (selectors.isEmpty() || value(injector, "target") != null) return;

        String kind = simpleName(injector.desc);
        int require = require(entry, handler, injector);
        Set<MethodNode> methods = new LinkedHashSet<>();
        for (String raw : selectors) {
            Selector selector = Selector.parse(raw);
            if (selector == null) return;

            List<MethodNode> selected = selector.select(target, (handler.access & Opcodes.ACC_STATIC) != 0);
            if (selected.isEmpty() && merged(MERGED_METHODS, target.name, null).stream().anyMatch((member) -> selector.matches(target.name, member.name(), member.desc(), false))) {
                return;
            }
            methods.addAll(selected);
        }

        if (methods.isEmpty()) {
            if (require > 0) problems.add(kind + " " + handler.name + " targets " + String.join(", ", selectors) + ", which doesn't exist");
            return;
        }

        List<AnnotationNode> points = points(injector);
        boolean evaluable = points != null && evaluable(points);
        if (evaluable && require > 0 && !found(points, methods)) {
            problems.add(kind + " " + handler.name + " in " + describe(methods) + " can't find " + describePoints(points));
            return;
        }

        if (injector.desc.equals(INJECT)) {
            checkCallback(problems, mixin, handler, methods, evaluable ? points : null, require);
        }
    }

    private static void checkCallback(@NonNull List<String> problems, @NonNull ClassNode mixin, @NonNull MethodNode handler, @NonNull Collection<MethodNode> methods, @Nullable List<AnnotationNode> points, int require) {
        Type[] arguments = Type.getArgumentTypes(handler.desc);
        int callback = -1;
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].getSort() == Type.OBJECT
                && (arguments[i].getInternalName().equals(CALLBACK_INFO) || arguments[i].getInternalName().equals(CALLBACK_INFO_RETURNABLE))) {
                callback = i;
                break;
            }
        }

        if (callback < 0 || hasSurrogate(mixin, handler.name)) return;

        List<String> mismatches = new ArrayList<>();
        boolean matched = false;
        for (MethodNode method : methods) {
            if (points != null && !found(points, List.of(method))) continue;

            String problem = callbackProblem(handler, arguments, callback, method);
            if (problem == null) matched = true;
            else mismatches.add(problem);
        }

        if (!mismatches.isEmpty() && (methods.size() == 1 || (!matched && require > 0))) {
            problems.add(mismatches.getFirst());
        }
    }

    private static @Nullable String callbackProblem(@NonNull MethodNode handler, Type @NonNull [] arguments, int callback, @NonNull MethodNode method) {
        String expected = Type.getReturnType(method.desc).getSort() == Type.VOID ? CALLBACK_INFO : CALLBACK_INFO_RETURNABLE;
        if (!arguments[callback].getInternalName().equals(expected) && !coerced(handler, callback)) {
            return "@Inject " + handler.name + " needs " + expected.substring(expected.lastIndexOf('/') + 1) + " for " + method.name + method.desc;
        }

        if (callback > 0 && !parametersMatch(handler, arguments, callback, Type.getArgumentTypes(method.desc))) {
            return "@Inject " + handler.name + handler.desc + " doesn't match the parameters of " + method.name + method.desc;
        }
        return null;
    }

    private static boolean parametersMatch(@NonNull MethodNode handler, Type @NonNull [] arguments, int count, Type @NonNull [] target) {
        if (count != target.length) return false;

        for (int i = 0; i < count; i++) {
            if (arguments[i].equals(target[i])) continue;
            if (arguments[i].getSort() == Type.ARRAY || !coerced(handler, i)) return false;
        }
        return true;
    }

    private static boolean coerced(@NonNull MethodNode handler, int parameter) {
        return hasParameterAnnotation(handler.invisibleParameterAnnotations, parameter)
            || hasParameterAnnotation(handler.visibleParameterAnnotations, parameter);
    }

    private static boolean hasParameterAnnotation(List<AnnotationNode> @Nullable [] annotations, int parameter) {
        if (annotations == null || parameter >= annotations.length || annotations[parameter] == null) return false;
        return annotations[parameter].stream().anyMatch((annotation) -> annotation.desc.equals(COERCE));
    }

    private static boolean hasSurrogate(@NonNull ClassNode mixin, @NonNull String name) {
        return mixin.methods.stream().anyMatch((method) -> method.name.equals(name)
            && annotation(method.visibleAnnotations, method.invisibleAnnotations, SURROGATE) != null);
    }

    private static int require(FabricMixinConfigs.@NonNull Entry entry, @NonNull MethodNode handler, @NonNull AnnotationNode injector) {
        if (value(injector, "require") instanceof Integer require && require > -1) return require;
        if (annotation(handler.visibleAnnotations, handler.invisibleAnnotations, GROUP) != null) return 0;
        return entry.defaultRequire();
    }

    private static @Nullable List<AnnotationNode> points(@NonNull AnnotationNode injector) {
        Object at = value(injector, "at");
        if (at instanceof AnnotationNode point) return List.of(point);
        if (!(at instanceof List<?> list) || list.isEmpty()) return null;

        List<AnnotationNode> points = new ArrayList<>();
        for (Object element : list) {
            if (!(element instanceof AnnotationNode point)) return null;
            points.add(point);
        }
        return points;
    }

    private static boolean evaluable(@NonNull List<AnnotationNode> points) {
        for (AnnotationNode point : points) {
            if (!(value(point, "value") instanceof String kind)) return false;
            if (kind.equals("RETURN")) {
                if (value(point, "target") != null || value(point, "desc") != null) return false;
                continue;
            }
            if (!MEMBER_POINTS.contains(kind)) return false;
            if (!(value(point, "target") instanceof String target) || target.isBlank() || value(point, "desc") != null) return false;
            if (!kind.equals("NEW") && Selector.parse(target) == null) return false;
        }
        return true;
    }

    private static boolean found(@NonNull List<AnnotationNode> points, @NonNull Collection<MethodNode> methods) {
        for (AnnotationNode point : points) {
            String kind = (String) value(point, "value");
            String target = (String) value(point, "target");
            int ordinal = value(point, "ordinal") instanceof Integer value ? value : -1;
            for (MethodNode method : methods) {
                int matches = 0;
                for (AbstractInsnNode insn : method.instructions) {
                    if (matches(insn, kind, target) && ++matches > ordinal) return true;
                }
            }
        }
        return false;
    }

    private static boolean matches(@NonNull AbstractInsnNode insn, @NonNull String kind, @Nullable String target) {
        if (kind.equals("RETURN")) {
            return insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN;
        }
        if (target == null) return false;
        if (kind.equals("NEW")) {
            return insn instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW && type.desc.equals(newType(target));
        }

        Selector selector = Selector.parse(target);
        if (selector == null) return true;
        if (kind.equals("FIELD")) {
            return insn instanceof FieldInsnNode field && selector.matches(field.owner, field.name, field.desc, true);
        }
        if (insn instanceof MethodInsnNode call) {
            return selector.matches(call.owner, call.name, call.desc, true);
        }
        if (insn instanceof InvokeDynamicInsnNode dynamic) {
            for (Object argument : dynamic.bsmArgs) {
                if (argument instanceof Handle handle && selector.matches(handle.getOwner(), handle.getName(), handle.getDesc(), true)) return true;
            }
        }
        return false;
    }

    private static @NonNull String newType(@NonNull String target) {
        String type = target.replaceAll("\\s", "");
        if (type.startsWith("(")) return Type.getReturnType(type).getInternalName();

        int semicolon = type.indexOf(';');
        if (type.startsWith("L") && semicolon > -1) return type.substring(1, semicolon);
        return type.replace('.', '/');
    }

    private static @Nullable String accessorTarget(@NonNull MethodNode method, @NonNull AnnotationNode annotation) {
        if (value(annotation, "value") instanceof String value && !value.isEmpty()) {
            Selector selector = Selector.parse(value);
            return selector != null ? selector.name() : null;
        }

        Matcher matcher = ACCESSOR_NAME.matcher(method.name);
        if (!matcher.matches()) return null;

        String name = matcher.group(2);
        String first = matcher.group(3);
        boolean upperCase = name.toUpperCase(Locale.ROOT).equals(name);
        return (upperCase ? first : first.toLowerCase(Locale.ROOT)) + matcher.group(4);
    }

    private static @NonNull String describe(@NonNull Collection<MethodNode> methods) {
        return methods.stream().map((method) -> method.name + method.desc).distinct().collect(Collectors.joining(", "));
    }

    private static @NonNull String describePoints(@NonNull List<AnnotationNode> points) {
        return points.stream()
            .map((point) -> value(point, "value") + (value(point, "target") != null ? " " + value(point, "target") : "")
                + (value(point, "ordinal") != null ? " ordinal " + value(point, "ordinal") : ""))
            .collect(Collectors.joining(" or "));
    }

    private static @NonNull String simpleName(@NonNull String descriptor) {
        return "@" + descriptor.substring(descriptor.lastIndexOf('/') + 1, descriptor.length() - 1);
    }

    private static @NonNull List<String> aliases(@NonNull String name, @NonNull AnnotationNode annotation) {
        List<String> names = new ArrayList<>();
        names.add(name);
        names.addAll(strings(value(annotation, "aliases")));
        return names;
    }

    private static @NonNull List<Member> merged(@NonNull Map<String, List<Member>> members, @NonNull String target, @Nullable ClassNode exclude) {
        List<Member> merged = members.getOrDefault(target, List.of());
        if (exclude == null) return merged;
        return merged.stream().filter((member) -> !member.mixin().equals(exclude.name)).toList();
    }

    private static void index() {
        if (indexed) return;
        indexed = true;
        indexing = true;
        try {
            indexMixins();
        } finally {
            indexing = false;
        }
    }

    private static void indexMixins() {
        for (FabricMixinConfigs.Entry entry : FabricMixinConfigs.entries()) {
            for (String name : entry.mixins()) {
                ClassNode mixin = classNode(name);
                AnnotationNode annotation = mixin != null ? annotation(mixin.visibleAnnotations, mixin.invisibleAnnotations, MIXIN) : null;
                if (annotation == null) continue;

                List<String> targets = targets(annotation);
                MIXINS.put(mixin.name, new Declared(entry, targets));

                for (FieldNode field : mixin.fields) {
                    if (annotation(field.visibleAnnotations, field.invisibleAnnotations, SHADOW) != null) continue;
                    targets.forEach((target) -> MERGED_FIELDS.computeIfAbsent(target, (key) -> new ArrayList<>()).add(new Member(field.name, field.desc, mixin.name)));
                }

                for (MethodNode method : mixin.methods) {
                    if (method.name.startsWith("<") || !mergesInto(method)) continue;
                    targets.forEach((target) -> MERGED_METHODS.computeIfAbsent(target, (key) -> new ArrayList<>()).add(new Member(method.name, method.desc, mixin.name)));
                }
            }
        }
    }

    private static @NonNull List<String> targets(@NonNull AnnotationNode annotation) {
        List<String> targets = new ArrayList<>();
        if (value(annotation, "value") instanceof List<?> types) {
            types.forEach((type) -> targets.add(((Type) type).getInternalName()));
        }
        strings(value(annotation, "targets")).forEach((type) -> targets.add(type.replace('.', '/')));
        return targets;
    }

    private static boolean mergesInto(@NonNull MethodNode method) {
        for (AnnotationNode annotation : annotations(method.visibleAnnotations, method.invisibleAnnotations)) {
            if (annotation.desc.equals(SHADOW) || annotation.desc.equals(OVERWRITE) || INJECTORS.contains(annotation.desc)) return false;
        }
        return true;
    }

    private static @Nullable ClassNode classNode(@NonNull String name) {
        String key = name.replace('.', '/');
        Optional<ClassNode> cached = CLASSES.get(key);
        if (cached == null) {
            try {
                cached = Optional.of(MixinService.getService().getBytecodeProvider().getClassNode(key, true, ClassReader.SKIP_FRAMES));
            } catch (Exception exception) {
                cached = Optional.empty();
            }
            CLASSES.put(key, cached);
        }
        return cached.orElse(null);
    }

    private static @Nullable AnnotationNode annotation(@Nullable List<AnnotationNode> visible, @Nullable List<AnnotationNode> invisible, @NonNull String descriptor) {
        for (AnnotationNode annotation : annotations(visible, invisible)) {
            if (annotation.desc.equals(descriptor)) return annotation;
        }
        return null;
    }

    private static @NonNull List<AnnotationNode> annotations(@Nullable List<AnnotationNode> visible, @Nullable List<AnnotationNode> invisible) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (visible != null) annotations.addAll(visible);
        if (invisible != null) annotations.addAll(invisible);
        return annotations;
    }

    static @Nullable AnnotationNode injector(@NonNull MethodNode method) {
        for (AnnotationNode annotation : annotations(method.visibleAnnotations, method.invisibleAnnotations)) {
            if (isInjector(annotation)) return annotation;
        }
        return null;
    }

    static boolean isInjector(@NonNull AnnotationNode annotation) {
        return INJECTORS.contains(annotation.desc);
    }

    static @Nullable Object value(@NonNull AnnotationNode annotation, @NonNull String key) {
        if (annotation.values == null) return null;

        for (int i = 0; i < annotation.values.size() - 1; i += 2) {
            if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        }
        return null;
    }

    private static @NonNull List<String> strings(@Nullable Object value) {
        if (value instanceof String string) return List.of(string);
        if (!(value instanceof List<?> list)) return List.of();

        List<String> strings = new ArrayList<>();
        for (Object element : list) {
            if (element instanceof String string) strings.add(string);
        }
        return strings;
    }

    private record Member(String name, String desc, String mixin) {
    }

    private record Declared(FabricMixinConfigs.Entry entry, List<String> targets) {
    }

    private record Findings(List<String> problems, Set<String> injectors, int injectorProblems) {
        boolean strippable() {
            return !this.problems.isEmpty() && this.injectorProblems == this.problems.size();
        }
    }

    private record Adaptations(List<Adaptation> handlers, List<Retype> fields) {
    }

    private record Retype(String field, String from, String to) {
    }

    private record Adaptation(String handler, String desc, @Nullable String method, boolean returnable, List<Type> parameters, int @Nullable [] positions) {
    }

    private record Selector(@Nullable String owner, @Nullable String name, @Nullable String desc, boolean all) {
        static @Nullable Selector parse(@NonNull String input) {
            String name = input.replaceAll("\\s", "");
            if (name.isEmpty() || name.startsWith("/") || name.startsWith("@") || name.contains("->") || name.contains("{")) return null;

            String owner = null;
            String desc = null;
            int paren = name.indexOf('(');
            int colon = name.indexOf(':');
            if (paren > -1) {
                desc = name.substring(paren);
                name = name.substring(0, paren);
            } else if (colon > -1) {
                desc = name.substring(colon + 1);
                name = name.substring(0, colon);
            }

            int dot = name.lastIndexOf('.');
            int semicolon = name.indexOf(';');
            if (dot > -1) {
                owner = name.substring(0, dot).replace('.', '/');
                name = name.substring(dot + 1);
            } else if (semicolon > -1 && name.startsWith("L")) {
                owner = name.substring(1, semicolon);
                name = name.substring(semicolon + 1);
            }

            if (owner == null && name.indexOf('/') > -1) {
                owner = name;
                name = "";
            }

            boolean all = name.endsWith("*") || name.endsWith("+");
            if (all) name = name.substring(0, name.length() - 1);
            return new Selector(owner, name.isEmpty() ? null : name, desc, all);
        }

        boolean matches(@NonNull String owner, @NonNull String name, @NonNull String desc, boolean ignoreCase) {
            return (this.owner == null || this.owner.equals(owner))
                && (this.name == null || (ignoreCase ? this.name.equalsIgnoreCase(name) : this.name.equals(name)))
                && (this.desc == null || this.desc.equals(desc));
        }

        @NonNull List<MethodNode> select(@NonNull ClassNode target, boolean staticHandler) {
            List<MethodNode> methods = new ArrayList<>();
            for (MethodNode method : target.methods) {
                if (!matches(target.name, method.name, method.desc, false)) continue;
                if (!all) {
                    methods.add(method);
                    break;
                }
                if (staticHandler || (method.access & Opcodes.ACC_STATIC) == 0) methods.add(method);
            }
            return methods;
        }
    }
}
