package io.canvasmc.horizon.inject.fabricapi;

import net.fabricmc.fabric.api.permission.v1.PermissionContext;
import net.fabricmc.fabric.api.permission.v1.PermissionEvents;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class FabricPermissionBridge {
    private FabricPermissionBridge() {
    }

    static void register() {
        PermissionEvents.ON_REQUEST.register(new PermissionEvents.OnRequest() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public <T> @Nullable T handlePermissionRequest(@NonNull PermissionContext context, @NonNull PermissionNode<T> permission) {
                Object source = context.get((PermissionContext.Key) PermissionContext.COMMAND_SOURCE_STACK);
                Boolean value = BukkitPermissions.value(BukkitPermissions.permissible(
                    source instanceof CommandSourceStack stack ? stack : null,
                    context.get(PermissionContext.ENTITY),
                    Util.NIL_UUID.equals(context.uuid()) ? null : context.uuid()
                ), BukkitPermissions.node(permission.key()));
                return value == null ? null : permission.cast(value);
            }
        });
    }
}
