package io.canvasmc.horizon.fabric;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.jspecify.annotations.NonNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class VanillaFields {
    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "vanilla_fields");
    private static final String HOLDER_MAP = "Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;";
    private static final String CHUNK_HOLDERS = "(Lnet/minecraft/server/level/ChunkMap;)" + HOLDER_MAP;
    private static final List<Field> FIELDS = List.of(
        new Field("net/minecraft/server/level/ChunkMap", "visibleChunkMap", HOLDER_MAP, "chunkHolders", CHUNK_HOLDERS),
        new Field("net/minecraft/server/level/ChunkMap", "updatingChunkMap", HOLDER_MAP, "chunkHolders", CHUNK_HOLDERS)
    );
    private static final List<String> SERVER_PACKAGES = List.of(
        "net/minecraft/", "com/mojang/", "org/bukkit/", "io/papermc/", "com/destroystokyo/", "org/spigotmc/", "ca/spottedleaf/", "io/canvasmc/horizon/"
    );
    private static final String VIEWS = "io/canvasmc/horizon/inject/VanillaViews";
    private static final Map<String, Boolean> MISSING = new ConcurrentHashMap<>();
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private VanillaFields() {
    }

    public static boolean reads(@NonNull ClassNode node) {
        for (String prefix : SERVER_PACKAGES) {
            if (node.name.startsWith(prefix)) return false;
        }

        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof FieldInsnNode access && field(access) != null) return true;
            }
        }
        return false;
    }

    public static void rewrite(@NonNull ClassNode node) {
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (!(insn instanceof FieldInsnNode access)) continue;

                Field field = field(access);
                if (field == null) continue;

                method.instructions.set(access, new MethodInsnNode(Opcodes.INVOKESTATIC, VIEWS, field.view(), field.viewDesc(), false));
                if (LOGGED.add(node.name + " " + field.name())) {
                    LOGGER.info("{} reads {}.{}, which Paper replaced; it gets a snapshot from Moonrise instead",
                        node.name.replace('/', '.'), field.owner().replace('/', '.'), field.name());
                }
            }
        }
    }

    private static Field field(@NonNull FieldInsnNode access) {
        if (access.getOpcode() != Opcodes.GETFIELD) return null;

        for (Field field : FIELDS) {
            if (field.owner().equals(access.owner) && field.name().equals(access.name) && field.desc().equals(access.desc) && missing(field)) {
                return field;
            }
        }
        return null;
    }

    private static boolean missing(@NonNull Field field) {
        return MISSING.computeIfAbsent(field.owner() + "." + field.name(), (key) -> {
            byte[] bytes;
            try {
                bytes = FabricLauncherBase.getLauncher().getClassByteArray(field.owner().replace('/', '.'), false);
            } catch (Exception exception) {
                return false;
            }
            if (bytes == null) return false;

            ClassNode owner = new ClassNode();
            new ClassReader(bytes).accept(owner, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return owner.fields.stream().noneMatch((candidate) -> candidate.name.equals(field.name()) && candidate.desc.equals(field.desc()));
        });
    }

    private record Field(String owner, String name, String desc, String view, String viewDesc) {
    }
}
