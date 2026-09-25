package io.canvasmc.horizon.inject.fabricapi;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.NonNull;

public final class FabricEvents {
    private FabricEvents() {
    }

    public static void chunkLoad(@NonNull ServerLevel level, @NonNull LevelChunk chunk, boolean generated) {
        if (generated) {
            ServerChunkEvents.CHUNK_GENERATE.invoker().onChunkGenerate(level, chunk);
        }
        ServerChunkEvents.CHUNK_LOAD.invoker().onChunkLoad(level, chunk, generated);
    }

    public static void chunkUnload(@NonNull ServerLevel level, @NonNull LevelChunk chunk) {
        ServerChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(level, chunk);
    }

    public static void levelUnload(@NonNull MinecraftServer server, @NonNull ServerLevel level) {
        ServerLevelEvents.UNLOAD.invoker().onLevelUnload(server, level);
    }
}
