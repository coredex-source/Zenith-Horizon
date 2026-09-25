package io.canvasmc.testmod;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class TestPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        TestMod.LOGGER.info("preLaunch entrypoint (class)");
    }
}
