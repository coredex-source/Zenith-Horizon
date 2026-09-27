package io.canvasmc.testmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("horizon-testmod");
    public static final Identifier TEST_SOUND = Identifier.fromNamespaceAndPath("horizon-testmod", "test_sound");
    public static volatile boolean worldgenAttachments;

    @Override
    public void onInitialize() {
        LOGGER.info("Hello from test mod! main entrypoint (class), game instance: {}", FabricLoader.getInstance().getGameInstance());
        FabricLoader.getInstance().getEntrypoints("horizon-testmod:custom", Runnable.class).forEach(Runnable::run);
        if (FabricLoader.getInstance().isModLoaded("fabric-lifecycle-events-v1") && FabricLoader.getInstance().isModLoaded("fabric-command-api-v2")) {
            TestFabricApi.register();
        }
        if (FabricLoader.getInstance().isModLoaded("fabric-entity-events-v1") && FabricLoader.getInstance().isModLoaded("fabric-data-attachment-api-v1")
            && FabricLoader.getInstance().isModLoaded("fabric-item-api-v1") && FabricLoader.getInstance().isModLoaded("fabric-command-api-v2")) {
            TestEntityEvents.register();
        }
        if (FabricLoader.getInstance().isModLoaded("fabric-data-attachment-api-v1") && FabricLoader.getInstance().isModLoaded("fabric-lifecycle-events-v1")) {
            TestChunkAttachments.register();
        }
        if (FabricLoader.getInstance().isModLoaded("fabric-events-interaction-v0")) {
            TestInteraction.register();
        }
        if (FabricLoader.getInstance().isModLoaded("fabric-message-api-v1")) {
            TestChat.register();
        }
        if (FabricLoader.getInstance().isModLoaded("fabric-networking-api-v1")) {
            TestNetworking.register();
        }
        if (FabricLoader.getInstance().isModLoaded("fabric-permission-api-v1") && FabricLoader.getInstance().isModLoaded("fabric-command-api-v2")) {
            TestPermissions.register();
        }
        Registry.register(BuiltInRegistries.SOUND_EVENT, TEST_SOUND, SoundEvent.createVariableRangeEvent(TEST_SOUND));
        LOGGER.info("registered {} in main", TEST_SOUND);
    }
}
