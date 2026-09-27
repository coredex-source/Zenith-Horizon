package io.canvasmc.horizon.fabric;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.fabric.mixin.FabricMixinConfigs;
import io.canvasmc.horizon.logger.Logger;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class VanillaCallSites {
    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "vanilla_call_sites");
    private static final List<CallSite> SITES = List.of(
        new CallSite(
            "net/minecraft/server/network/ServerGamePacketListenerImpl", "lambda$handleChat$",
            "net/minecraft/network/chat/ChatDecorator", "decorate",
            "(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/network/chat/Component;)Ljava/util/concurrent/CompletableFuture;",
            "Lnet/minecraft/network/chat/ChatDecorator;decorate(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;",
            (call) -> {
                InsnList vanilla = new InsnList();
                vanilla.add(new InsnNode(Opcodes.SWAP));
                vanilla.add(new InsnNode(Opcodes.POP));
                vanilla.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, call.owner, call.name,
                    "(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;", true));
                vanilla.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/concurrent/CompletableFuture", "completedFuture",
                    "(Ljava/lang/Object;)Ljava/util/concurrent/CompletableFuture;", false));
                return vanilla;
            }
        )
    );

    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    private static volatile @Nullable Set<String> targets;

    private VanillaCallSites() {
    }

    public static boolean restores(@NonNull String owner) {
        for (CallSite site : SITES) {
            if (site.owner().equals(owner) && demanded(site)) return true;
        }
        return false;
    }

    public static void restore(@NonNull ClassNode node) {
        for (CallSite site : SITES) {
            if (!site.owner().equals(node.name) || !demanded(site)) continue;

            for (MethodNode method : node.methods) {
                if (!method.name.startsWith(site.methodPrefix())) continue;

                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (insn instanceof MethodInsnNode call && call.owner.equals(site.callOwner()) && call.name.equals(site.callName()) && call.desc.equals(site.callDesc())) {
                        method.instructions.insertBefore(call, site.rewrite().apply(call));
                        method.instructions.remove(call);
                        if (LOGGED.add(node.name + "." + method.name + " " + site.vanillaTarget())) {
                            LOGGER.info("Restored vanilla call {} in {}::{} for a mod mixin that targets it", site.vanillaTarget(), node.name.replace('/', '.'), method.name);
                        }
                    }
                }
            }
        }
    }

    private static boolean demanded(@NonNull CallSite site) {
        Set<String> found = targets;
        if (found == null) {
            found = scan();
            targets = found;
        }
        return found.contains(site.vanillaTarget());
    }

    private static @NonNull Set<String> scan() {
        Set<String> found = new HashSet<>();
        for (FabricMixinConfigs.Entry entry : FabricMixinConfigs.entries()) {
            for (String mixin : entry.mixins()) {
                byte[] bytes;
                try {
                    bytes = FabricLauncherBase.getLauncher().getClassByteArray(mixin, false);
                } catch (Exception exception) {
                    continue;
                }
                if (bytes == null) continue;

                ClassNode node = new ClassNode();
                new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                for (MethodNode method : node.methods) {
                    collect(method.visibleAnnotations, found);
                    collect(method.invisibleAnnotations, found);
                }
            }
        }
        return found;
    }

    private static void collect(@Nullable List<?> values, @NonNull Set<String> found) {
        if (values == null) return;

        for (Object value : values) {
            if (value instanceof AnnotationNode annotation) {
                if (annotation.values == null) continue;
                for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                    Object inner = annotation.values.get(i + 1);
                    if ("target".equals(annotation.values.get(i)) && inner instanceof String target) found.add(target);
                    if (inner instanceof AnnotationNode nested) collect(List.of(nested), found);
                    if (inner instanceof List<?> list) collect(list, found);
                }
            }
        }
    }

    private record CallSite(String owner, String methodPrefix, String callOwner, String callName, String callDesc, String vanillaTarget,
                            Function<MethodInsnNode, InsnList> rewrite) {
    }
}
