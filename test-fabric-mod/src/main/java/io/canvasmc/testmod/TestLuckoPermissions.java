package io.canvasmc.testmod;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;

final class TestLuckoPermissions {
    private TestLuckoPermissions() {
    }

    static String check(CommandSourceStack source, String node) {
        return Permissions.getPermissionValue(source, node).toString();
    }
}
