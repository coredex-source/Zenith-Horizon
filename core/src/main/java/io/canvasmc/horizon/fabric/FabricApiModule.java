package io.canvasmc.horizon.fabric;

import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.NonNull;

public enum FabricApiModule {
    BLOCK_API("fabric-block-api-v1"),
    ENTITY_EVENTS("fabric-entity-events-v1"),
    INTERACTION_EVENTS("fabric-events-interaction-v0"),
    ITEM_API("fabric-item-api-v1"),
    LIFECYCLE_EVENTS("fabric-lifecycle-events-v1"),
    MENU_API("fabric-menu-api-v1"),
    MESSAGE_API("fabric-message-api-v1"),
    NETWORKING("fabric-networking-api-v1"),
    REGISTRY_SYNC("fabric-registry-sync-v0"),
    TAG_API("fabric-tag-api-v1");

    private final String id;
    private boolean loaded;

    FabricApiModule(String id) {
        this.id = id;
    }

    public boolean isLoaded() {
        return loaded;
    }

    static void detect(@NonNull FabricLoader loader) {
        for (FabricApiModule module : values()) {
            module.loaded = loader.isModLoaded(module.id);
        }
    }
}
