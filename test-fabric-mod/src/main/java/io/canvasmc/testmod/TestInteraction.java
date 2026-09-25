package io.canvasmc.testmod;

import net.fabricmc.fabric.api.event.player.PlayerPickItemEvents;

public final class TestInteraction {
    private TestInteraction() {
    }

    public static void register() {
        PlayerPickItemEvents.BLOCK.register((player, pos, state, includeData) -> {
            TestMod.LOGGER.info("interaction: {} picked {} at {}", player.getName().getString(), state.getBlock(), pos.toShortString());
            return null;
        });
    }
}
