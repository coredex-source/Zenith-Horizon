package io.canvasmc.horizon.inject.fabricapi;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permissible;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class BukkitPermissions {
    private BukkitPermissions() {
    }

    public static @NonNull String node(@NonNull Identifier permission) {
        return permission.getNamespace() + "." + permission.getPath();
    }

    public static @Nullable Permissible permissible(@Nullable CommandSourceStack source, @Nullable Entity entity, @Nullable UUID player) {
        if (source != null) {
            return source.getBukkitSender();
        }
        if (entity != null) {
            return entity.getBukkitEntity();
        }
        if (player != null) {
            return Bukkit.getPlayer(player);
        }
        return null;
    }

    public static @Nullable Boolean value(@Nullable Permissible permissible, @NonNull String node) {
        if (permissible == null) {
            return null;
        }
        if (permissible.isPermissionSet(node) || Bukkit.getPluginManager().getPermission(node) != null) {
            return permissible.hasPermission(node);
        }
        return null;
    }
}
