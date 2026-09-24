package io.canvasmc.testmod;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMod implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("horizon-testmod");

    @Override
    public void onInitialize() {
        LOGGER.info("Hello from test mod!");
    }
}
