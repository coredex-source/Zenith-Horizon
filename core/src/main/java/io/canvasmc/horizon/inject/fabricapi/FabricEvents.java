package io.canvasmc.horizon.inject.fabricapi;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.player.PlayerPickItemEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

    public static @Nullable ItemStack pickItemFromBlock(@NonNull ServerPlayer player, @NonNull BlockPos pos, @NonNull BlockState state, boolean includeData) {
        return PlayerPickItemEvents.BLOCK.invoker().onPickItemFromBlock(player, pos, state, includeData);
    }

    public static @Nullable ItemStack pickItemFromEntity(@NonNull ServerPlayer player, @NonNull Entity entity, boolean includeData) {
        return PlayerPickItemEvents.ENTITY.invoker().onPickItemFromEntity(player, entity, includeData);
    }
}
