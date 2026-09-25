package io.canvasmc.testmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("horizon-testmod");

    @Override
    public void onInitialize() {
        LOGGER.info("Hello from test mod! main entrypoint (class), game instance: {}", FabricLoader.getInstance().getGameInstance());
        FabricLoader.getInstance().getEntrypoints("horizon-testmod:custom", Runnable.class).forEach(Runnable::run);
    }
}
