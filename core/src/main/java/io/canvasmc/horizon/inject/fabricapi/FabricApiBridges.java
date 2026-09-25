package io.canvasmc.horizon.inject.fabricapi;

import net.fabricmc.loader.api.FabricLoader;

public final class FabricApiBridges {
    private FabricApiBridges() {
    }

    public static void register() {
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("fabric-networking-api-v1")) {
            PluginChannelBridge.register();
        }
        if (loader.isModLoaded("fabric-permission-api-v1")) {
            FabricPermissionBridge.register();
        }
        if (loader.isModLoaded("fabric-permissions-api-v0")) {
            LuckoPermissionBridge.register();
        }
    }
}
