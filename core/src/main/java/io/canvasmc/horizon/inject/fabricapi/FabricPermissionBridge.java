package io.canvasmc.horizon.inject.fabricapi;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.permission.v1.PermissionContext;
import net.fabricmc.fabric.api.permission.v1.PermissionEvents;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class FabricPermissionBridge {
    private static final PermissionContext.Key<Object> LUCKPERMS_USER = PermissionContext.key(Identifier.fromNamespaceAndPath("horizon", "luckperms_user"));

    private FabricPermissionBridge() {
    }

    static void register() {
        PermissionEvents.ON_REQUEST.register(new PermissionEvents.OnRequest() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public <T> @Nullable T handlePermissionRequest(@NonNull PermissionContext context, @NonNull PermissionNode<T> permission) {
                String node = BukkitPermissions.node(permission.key());
                UUID player = Util.NIL_UUID.equals(context.uuid()) ? null : context.uuid();
                if (permission.codec() == Codec.BOOL) {
                    Object source = context.get((PermissionContext.Key) PermissionContext.COMMAND_SOURCE_STACK);
                    Boolean value = BukkitPermissions.value(BukkitPermissions.permissible(
                        source instanceof CommandSourceStack stack ? stack : null,
                        context.get(PermissionContext.ENTITY),
                        player
                    ), node);
                    if (value == null && player != null && Bukkit.getPlayer(player) == null) {
                        LuckPermsLink luckPerms = LuckPermsLink.get();
                        value = luckPerms == null ? null : luckPerms.permission(user(luckPerms, context, player), node);
                    }
                    return value == null ? null : permission.cast(value);
                }

                LuckPermsLink luckPerms = player == null ? null : LuckPermsLink.get();
                String raw = luckPerms == null ? null : luckPerms.meta(user(luckPerms, context, player), node);
                return raw == null ? null : decode(permission, raw);
            }
        });
        PermissionEvents.PREPARE_OFFLINE_PLAYER.register((context, server) -> {
            LuckPermsLink luckPerms = LuckPermsLink.get();
            if (luckPerms == null) {
                return CompletableFuture.completedFuture(null);
            }
            return luckPerms.loadUser(context.uuid()).thenApply((user) -> (mutable) -> mutable.set(LUCKPERMS_USER, user));
        });
    }

    private static @Nullable Object user(@NonNull LuckPermsLink luckPerms, @NonNull PermissionContext context, @NonNull UUID player) {
        Object prepared = context.get(LUCKPERMS_USER);
        return prepared != null ? prepared : luckPerms.user(player);
    }

    private static <T> @Nullable T decode(@NonNull PermissionNode<T> permission, @NonNull String raw) {
        JsonElement json;
        try {
            json = JsonParser.parseString(raw);
        } catch (JsonParseException exception) {
            json = new JsonPrimitive(raw);
        }

        Optional<T> value = permission.codec().parse(JsonOps.INSTANCE, json).result();
        if (value.isEmpty()) {
            value = permission.codec().parse(JsonOps.INSTANCE, new JsonPrimitive(raw)).result();
        }
        return value.map(permission::cast).orElse(null);
    }
}
