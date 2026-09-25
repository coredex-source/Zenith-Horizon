package io.canvasmc.horizon.inject.fabricapi;

import me.lucko.fabric.api.permissions.v0.OfflinePermissionCheckEvent;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.commands.CommandSourceStack;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

final class LuckoPermissionBridge {
    private LuckoPermissionBridge() {
    }

    static void register() {
        PermissionCheckEvent.EVENT.register((source, permission) -> source instanceof CommandSourceStack stack
            ? state(BukkitPermissions.value(BukkitPermissions.permissible(stack, null, null), permission))
            : TriState.DEFAULT);
        OfflinePermissionCheckEvent.EVENT.register((player, permission) ->
            CompletableFuture.completedFuture(state(BukkitPermissions.value(BukkitPermissions.permissible(null, null, player), permission))));
    }

    private static TriState state(@Nullable Boolean value) {
        return value == null ? TriState.DEFAULT : TriState.of(value);
    }
}
