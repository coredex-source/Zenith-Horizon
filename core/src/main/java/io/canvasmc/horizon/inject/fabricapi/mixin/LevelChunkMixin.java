package io.canvasmc.horizon.inject.fabricapi.mixin;

import io.canvasmc.horizon.fabric.HorizonFabric;
import io.canvasmc.horizon.inject.fabricapi.FabricEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    public boolean needsDecoration;

    @Inject(method = "loadCallback", at = @At("HEAD"))
    private void horizon$fabricChunkLoad(CallbackInfo ci) {
        if (HorizonFabric.hasLifecycleEvents()) {
            FabricEvents.chunkLoad(this.level, (LevelChunk) (Object) this, this.needsDecoration);
        }
    }

    @Inject(method = "unloadCallback", at = @At("HEAD"))
    private void horizon$fabricChunkUnload(CallbackInfo ci) {
        if (HorizonFabric.hasLifecycleEvents()) {
            FabricEvents.chunkUnload(this.level, (LevelChunk) (Object) this);
        }
    }
}
