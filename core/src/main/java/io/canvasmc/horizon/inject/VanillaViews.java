package io.canvasmc.horizon.inject;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class VanillaViews {

    private VanillaViews() {
    }

    public static @NonNull Long2ObjectLinkedOpenHashMap<ChunkHolder> chunkHolders(@NonNull ChunkMap chunkMap) {
        List<ChunkHolder> holders = chunkMap.level.moonrise$getChunkTaskScheduler().chunkHolderManager.getOldChunkHolders();
        Long2ObjectLinkedOpenHashMap<ChunkHolder> view = new Long2ObjectLinkedOpenHashMap<>(holders.size());
        for (ChunkHolder holder : holders) {
            view.put(holder.getPos().pack(), holder);
        }
        return view;
    }
}
