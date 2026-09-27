package io.canvasmc.testmod;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.permission.v1.PermissionContext;
import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TestPermissions {
    private TestPermissions() {
    }

    public static void register() {
        boolean lucko = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(Commands.literal("horizonperm")
            .then(Commands.argument("node", StringArgumentType.greedyString()).executes((command) -> {
                String node = StringArgumentType.getString(command, "node");
                CommandSourceStack source = command.getSource();
                Identifier identifier = Identifier.parse(node.replaceFirst("\\.", ":"));
                String result = "permission " + node + " for " + source.getTextName() + ": v1=" + ((PermissionContextOwner) source).checkPermission(identifier)
                    + (lucko ? " v0=" + TestLuckoPermissions.check(source, node) : "");
                TestMod.LOGGER.info(result);
                source.sendSuccess(() -> Component.literal(result), false);
                return 1;
            }))));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(Commands.literal("horizonpermint")
            .then(Commands.argument("node", StringArgumentType.greedyString()).executes((command) -> {
                String node = StringArgumentType.getString(command, "node");
                CommandSourceStack source = command.getSource();
                PermissionNode<Integer> permission = PermissionNode.ofInteger(Identifier.parse(node.replaceFirst("\\.", ":")));
                String result = "integer permission " + node + " for " + source.getTextName() + ": v1=" + ((PermissionContextOwner) source).checkPermission(permission)
                    + (lucko ? " v0=" + TestLuckoPermissions.option(source, node) : "");
                TestMod.LOGGER.info(result);
                source.sendSuccess(() -> Component.literal(result), false);
                return 1;
            }))));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(Commands.literal("horizonpermoffline")
            .then(Commands.argument("player", UuidArgument.uuid()).then(Commands.argument("node", StringArgumentType.greedyString()).executes((command) -> {
                UUID player = UuidArgument.getUuid(command, "player");
                String node = StringArgumentType.getString(command, "node");
                CommandSourceStack source = command.getSource();
                Identifier identifier = Identifier.parse(node.replaceFirst("\\.", ":"));
                PermissionContext.offlinePlayer(player, source.getServer())
                    .thenCombine(lucko ? TestLuckoPermissions.offline(player, node) : CompletableFuture.completedFuture("-"), (offline, v0) ->
                        "offline permission " + node + " for " + player + ": v1=" + offline.checkPermission(identifier) + "/" + offline.checkPermission(PermissionNode.ofInteger(identifier))
                            + (lucko ? " v0=" + v0 : ""))
                    .thenAccept((result) -> source.getServer().execute(() -> {
                        TestMod.LOGGER.info(result);
                        source.sendSuccess(() -> Component.literal(result), false);
                    }));
                return 1;
            })))));
    }
}
