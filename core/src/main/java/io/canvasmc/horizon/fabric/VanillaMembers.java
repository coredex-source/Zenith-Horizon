package io.canvasmc.horizon.fabric;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import org.jspecify.annotations.NonNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class VanillaMembers {
    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "vanilla_members");
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    private static final List<Member> MEMBERS = List.of(
        new Member("net/minecraft/server/network/CommonListenerCookie", Opcodes.ACC_PUBLIC, "<init>",
            "(Lcom/mojang/authlib/GameProfile;ILnet/minecraft/server/level/ClientInformation;Z)V", 9, 5, (code) -> {
            code.add(new VarInsnNode(Opcodes.ALOAD, 0));
            code.add(new VarInsnNode(Opcodes.ALOAD, 1));
            code.add(new VarInsnNode(Opcodes.ILOAD, 2));
            code.add(new VarInsnNode(Opcodes.ALOAD, 3));
            code.add(new VarInsnNode(Opcodes.ILOAD, 4));
            code.add(new InsnNode(Opcodes.ACONST_NULL));
            code.add(new TypeInsnNode(Opcodes.NEW, "java/util/HashSet"));
            code.add(new InsnNode(Opcodes.DUP));
            code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/HashSet", "<init>", "()V", false));
            code.add(new TypeInsnNode(Opcodes.NEW, "io/papermc/paper/util/KeepAlive"));
            code.add(new InsnNode(Opcodes.DUP));
            code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "io/papermc/paper/util/KeepAlive", "<init>", "()V", false));
            code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/minecraft/server/network/CommonListenerCookie", "<init>",
                "(Lcom/mojang/authlib/GameProfile;ILnet/minecraft/server/level/ClientInformation;ZLjava/lang/String;Ljava/util/Set;Lio/papermc/paper/util/KeepAlive;)V", false));
            code.add(new InsnNode(Opcodes.RETURN));
        }),
        new Member("net/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity", Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "getTotalCookTime",
            "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;)I", 5, 2, (code) -> {
            code.add(new VarInsnNode(Opcodes.ALOAD, 0));
            code.add(new VarInsnNode(Opcodes.ALOAD, 1));
            code.add(new VarInsnNode(Opcodes.ALOAD, 1));
            code.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity", "recipeType",
                "Lnet/minecraft/world/item/crafting/RecipeType;"));
            code.add(new VarInsnNode(Opcodes.ALOAD, 1));
            code.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity", "cookSpeedMultiplier", "D"));
            code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity", "getTotalCookTime",
                "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;Lnet/minecraft/world/item/crafting/RecipeType;D)I", false));
            code.add(new InsnNode(Opcodes.IRETURN));
        })
    );

    private VanillaMembers() {
    }

    public static boolean adds(@NonNull String owner) {
        for (Member member : MEMBERS) {
            if (member.owner().equals(owner)) return true;
        }
        return false;
    }

    public static void add(@NonNull ClassNode node) {
        for (Member member : MEMBERS) {
            if (!member.owner().equals(node.name) || node.methods.stream().anyMatch((method) -> method.name.equals(member.name()) && method.desc.equals(member.desc()))) {
                continue;
            }

            MethodNode method = new MethodNode(member.access(), member.name(), member.desc(), null, null);
            member.body().accept(method.instructions);
            method.maxStack = member.maxStack();
            method.maxLocals = member.maxLocals();
            node.methods.add(method);
            if (LOGGED.add(node.name + member.name() + member.desc())) {
                LOGGER.info("Added vanilla {}{} to {}", member.name(), member.desc(), node.name.replace('/', '.'));
            }
        }
    }

    private record Member(String owner, int access, String name, String desc, int maxStack, int maxLocals, Consumer<InsnList> body) {
    }
}
