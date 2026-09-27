package io.canvasmc.testmod;

import me.lucko.fabric.api.permissions.v0.Options;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class TestLuckoPermissions {
    private TestLuckoPermissions() {
    }

    static String check(CommandSourceStack source, String node) {
        return Permissions.getPermissionValue(source, node).toString();
    }

    static String option(CommandSourceStack source, String key) {
        return Options.get(source, key).orElse("none");
    }

    static CompletableFuture<String> offline(UUID player, String node) {
        return Permissions.getPermissionValue(player, node).thenCombine(Options.get(player, node), (value, option) -> value + "/" + option.orElse("none"));
    }
}
