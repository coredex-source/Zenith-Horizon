package io.canvasmc.testmod.mixin;

import io.canvasmc.testmod.TestChunkAttachments;
import io.canvasmc.testmod.TestMod;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMixin {

    @Inject(method = "applyBiomeDecoration(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/StructureManager;)V", at = @At("HEAD"))
    private void testmod$markWorldgen(WorldGenLevel level, ChunkAccess chunk, StructureManager structures, CallbackInfo ci) {
        if (TestMod.worldgenAttachments) {
            TestChunkAttachments.mark(chunk);
        }
    }
}
