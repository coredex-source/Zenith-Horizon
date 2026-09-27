package io.canvasmc.horizon.inject.fabricapi.mixin;

import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import io.canvasmc.horizon.fabric.FabricApiModule;
import io.canvasmc.horizon.inject.fabricapi.FabricEvents;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NewChunkHolder.class)
public abstract class NewChunkHolderMixin {

    @Shadow
    @Final
    public ServerLevel world;

    @Shadow
    private ChunkAccess currentChunk;

    @Shadow
    private FullChunkStatus currentFullChunkStatus;

    @Inject(method = "updateCurrentState", at = @At("HEAD"))
    private void horizon$fabricFullChunkStatusChange(FullChunkStatus to, CallbackInfo ci) {
        if (FabricApiModule.LIFECYCLE_EVENTS.isLoaded() && this.currentChunk instanceof LevelChunk chunk && this.currentFullChunkStatus != to) {
            FabricEvents.fullChunkStatusChange(this.world, chunk, this.currentFullChunkStatus, to);
        }
    }
}
