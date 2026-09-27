package io.canvasmc.horizon.fabric;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BukkitViews {
    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "bukkit_views");
    private static final String MENU = "net/minecraft/world/inventory/AbstractContainerMenu";
    private static final String NAME = "getBukkitView";
    private static final String DESC = "()Lorg/bukkit/inventory/InventoryView;";
    private static final List<String> SERVER_PACKAGES = List.of(
        "net/minecraft/", "com/mojang/", "org/bukkit/", "io/papermc/", "com/destroystokyo/", "org/spigotmc/", "ca/spottedleaf/", "io/canvasmc/horizon/"
    );
    private static final Map<String, Boolean> MISSING = new ConcurrentHashMap<>();

    private BukkitViews() {
    }

    public static boolean needs(@NonNull ClassNode node) {
        if ((node.access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ANNOTATION)) != 0 || node.superName == null) return false;
        for (String prefix : SERVER_PACKAGES) {
            if (node.name.startsWith(prefix)) return false;
        }
        if (declares(node) || mixin(node)) return false;
        return missing(node.superName);
    }

    public static void add(@NonNull ClassNode node) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, NAME, DESC, null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/canvasmc/horizon/inject/ModMenus", "bukkitView",
            "(L" + MENU + ";)Lorg/bukkit/inventory/InventoryView;", false));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        method.maxStack = 1;
        method.maxLocals = 1;
        node.methods.add(method);
        LOGGER.info("Added a Bukkit inventory view to the menu {}", node.name.replace('/', '.'));
    }

    private static boolean mixin(@NonNull ClassNode node) {
        for (List<AnnotationNode> annotations : Arrays.asList(node.invisibleAnnotations, node.visibleAnnotations)) {
            if (annotations != null && annotations.stream().anyMatch((annotation) -> annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;"))) return true;
        }
        return false;
    }

    private static boolean declares(@NonNull ClassNode node) {
        return node.methods.stream().anyMatch((method) -> method.name.equals(NAME) && method.desc.equals(DESC) && (method.access & Opcodes.ACC_ABSTRACT) == 0);
    }

    private static boolean missing(@NonNull String name) {
        if (name.equals(MENU)) return true;
        if (name.equals("java/lang/Object")) return false;

        Boolean cached = MISSING.get(name);
        if (cached != null) return cached;

        ClassNode node = read(name);
        boolean result = node != null && !declares(node) && node.superName != null && missing(node.superName);
        MISSING.put(name, result);
        return result;
    }

    private static @Nullable ClassNode read(@NonNull String name) {
        byte[] bytes;
        try {
            bytes = FabricLauncherBase.getLauncher().getClassByteArray(name.replace('/', '.'), false);
        } catch (Exception exception) {
            return null;
        }
        if (bytes == null) return null;

        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }
}
