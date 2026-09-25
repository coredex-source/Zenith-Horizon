package io.canvasmc.testmod;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

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
    }
}
