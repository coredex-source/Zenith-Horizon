package io.canvasmc.testmod;

import net.fabricmc.api.DedicatedServerModInitializer;

public final class TestEntrypoints {
    public static final DedicatedServerModInitializer SERVER = () -> TestMod.LOGGER.info("server entrypoint (static field)");

    private TestEntrypoints() {
    }

    public static void onMain() {
        TestMod.LOGGER.info("main entrypoint (static method)");
    }

    public static void onCustom() {
        TestMod.LOGGER.info("custom entrypoint (static method)");
    }
}
