package io.canvasmc.testmod;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestFabricApi {
    private TestFabricApi() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register((server) -> TestMod.LOGGER.info("lifecycle: server starting"));
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> TestMod.LOGGER.info("lifecycle: server started"));
        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> TestMod.LOGGER.info("lifecycle: server stopping"));
        ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
            TestMod.LOGGER.info("lifecycle: server stopped");
            try {
                Files.writeString(Path.of("horizon-testmod-stopped.txt"), "server stopped\n");
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        });
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, resources) -> TestMod.LOGGER.info("lifecycle: data pack reload start"));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> TestMod.LOGGER.info("lifecycle: data pack reload end, success={}", success));
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> TestMod.LOGGER.info("lifecycle: data pack sync to {}, joined={}", player.getName().getString(), joined));
        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> TestMod.LOGGER.info("lifecycle: before save"));
        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> TestMod.LOGGER.info("lifecycle: after save"));
        ServerLevelEvents.LOAD.register((server, level) -> TestMod.LOGGER.info("lifecycle: level load {}", level.dimension()));
        ServerLevelEvents.UNLOAD.register((server, level) -> TestMod.LOGGER.info("lifecycle: level unload {}", level.dimension()));

        AtomicBoolean ticked = new AtomicBoolean();
        ServerTickEvents.START_SERVER_TICK.register((server) -> {
            if (ticked.compareAndSet(false, true)) TestMod.LOGGER.info("lifecycle: first server tick");
        });

        AtomicInteger loaded = new AtomicInteger();
        AtomicInteger unloaded = new AtomicInteger();
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
            if (loaded.incrementAndGet() == 1) TestMod.LOGGER.info("lifecycle: first chunk load {}", chunk.getPos());
        });
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            if (unloaded.incrementAndGet() == 1) TestMod.LOGGER.info("lifecycle: first chunk unload {}", chunk.getPos());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(Commands.literal("horizontest")
            .executes((command) -> {
                command.getSource().sendSuccess(() -> Component.literal("horizon test command works"), false);
                return 1;
            })));
    }
}
