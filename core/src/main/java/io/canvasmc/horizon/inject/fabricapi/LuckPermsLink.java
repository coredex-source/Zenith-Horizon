package io.canvasmc.horizon.inject.fabricapi;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class LuckPermsLink {
    private static volatile @Nullable LuckPermsLink link;

    private final Object userManager;
    private final Method getUser;
    private final Method loadUser;
    private final Method cachedData;
    private final Method permissionData;
    private final Method checkPermission;
    private final Method metaData;
    private final Method metaValue;

    private LuckPermsLink(@NonNull ClassLoader loader) throws ReflectiveOperationException {
        Class<?> users = loader.loadClass("net.luckperms.api.model.user.UserManager");
        Class<?> data = loader.loadClass("net.luckperms.api.cacheddata.CachedDataManager");
        Object luckPerms = loader.loadClass("net.luckperms.api.LuckPermsProvider").getMethod("get").invoke(null);
        this.userManager = loader.loadClass("net.luckperms.api.LuckPerms").getMethod("getUserManager").invoke(luckPerms);
        this.getUser = users.getMethod("getUser", UUID.class);
        this.loadUser = users.getMethod("loadUser", UUID.class);
        this.cachedData = loader.loadClass("net.luckperms.api.model.user.User").getMethod("getCachedData");
        this.permissionData = data.getMethod("getPermissionData");
        this.checkPermission = loader.loadClass("net.luckperms.api.cacheddata.CachedPermissionData").getMethod("checkPermission", String.class);
        this.metaData = data.getMethod("getMetaData");
        this.metaValue = loader.loadClass("net.luckperms.api.cacheddata.CachedMetaData").getMethod("getMetaValue", String.class);
    }

    public static @Nullable LuckPermsLink get() {
        LuckPermsLink current = link;
        if (current != null) {
            return current;
        }

        Plugin plugin = Bukkit.getServer() == null ? null : Bukkit.getPluginManager().getPlugin("LuckPerms");
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }
        try {
            current = new LuckPermsLink(plugin.getClass().getClassLoader());
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
        link = current;
        return current;
    }

    public @Nullable Object user(@NonNull UUID uuid) {
        return call(this.getUser, this.userManager, uuid);
    }

    public @NonNull CompletableFuture<?> loadUser(@NonNull UUID uuid) {
        return (CompletableFuture<?>) call(this.loadUser, this.userManager, uuid);
    }

    public @Nullable Boolean permission(@Nullable Object user, @NonNull String node) {
        if (user == null) {
            return null;
        }
        Object result = call(this.checkPermission, call(this.permissionData, call(this.cachedData, user)), node);
        return switch (((Enum<?>) result).name()) {
            case "TRUE" -> true;
            case "FALSE" -> false;
            default -> null;
        };
    }

    public @Nullable String meta(@Nullable Object user, @NonNull String key) {
        return user == null ? null : (String) call(this.metaValue, call(this.metaData, call(this.cachedData, user)), key);
    }

    private static Object call(@NonNull Method method, @NonNull Object target, Object... arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        } catch (InvocationTargetException exception) {
            throw exception.getCause() instanceof RuntimeException runtime ? runtime : new IllegalStateException(exception.getCause());
        }
    }
}
