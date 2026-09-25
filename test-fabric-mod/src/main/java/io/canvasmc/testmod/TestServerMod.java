package io.canvasmc.testmod;

import net.fabricmc.api.DedicatedServerModInitializer;

public class TestServerMod implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        TestMod.LOGGER.info("server entrypoint (class)");
    }
}
