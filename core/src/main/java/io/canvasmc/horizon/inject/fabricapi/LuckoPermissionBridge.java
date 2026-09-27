package io.canvasmc.horizon.inject.fabricapi;

import me.lucko.fabric.api.permissions.v0.OfflineOptionRequestEvent;
import me.lucko.fabric.api.permissions.v0.OfflinePermissionCheckEvent;
import me.lucko.fabric.api.permissions.v0.OptionRequestEvent;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.Bukkit;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

final class LuckoPermissionBridge {
    private LuckoPermissionBridge() {
    }

    static void register() {
        PermissionCheckEvent.EVENT.register((source, permission) -> source instanceof CommandSourceStack stack
            ? state(BukkitPermissions.value(BukkitPermissions.permissible(stack, null, null), permission))
            : TriState.DEFAULT);
        OfflinePermissionCheckEvent.EVENT.register((player, permission) -> {
            LuckPermsLink luckPerms = LuckPermsLink.get();
            if (Bukkit.getPlayer(player) != null || luckPerms == null) {
                return CompletableFuture.completedFuture(state(BukkitPermissions.value(BukkitPermissions.permissible(null, null, player), permission)));
            }
            return luckPerms.loadUser(player).thenApply((user) -> state(luckPerms.permission(user, permission)));
        });
        OptionRequestEvent.EVENT.register((source, key) -> {
            LuckPermsLink luckPerms = LuckPermsLink.get();
            if (luckPerms == null || !(source instanceof CommandSourceStack stack) || stack.getEntity() == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(luckPerms.meta(luckPerms.user(stack.getEntity().getUUID()), key));
        });
        OfflineOptionRequestEvent.EVENT.register((player, key) -> {
            LuckPermsLink luckPerms = LuckPermsLink.get();
            if (luckPerms == null) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return luckPerms.loadUser(player).thenApply((user) -> Optional.ofNullable(luckPerms.meta(user, key)));
        });
    }

    private static TriState state(@Nullable Boolean value) {
        return value == null ? TriState.DEFAULT : TriState.of(value);
    }
}
