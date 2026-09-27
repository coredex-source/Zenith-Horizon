package io.canvasmc.horizon.inject.fabricapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.canvasmc.horizon.fabric.FabricApiModule;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

@Mixin(LevelChunkSection.class)
public abstract class LevelChunkSectionMixin {

    @WrapOperation(method = "recalcBlockCounts", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;maybeHas(Ljava/util/function/Predicate;)Z"))
    private boolean horizon$fabricAirLikeBlocks(LevelChunkSection section, Predicate<BlockState> predicate, Operation<Boolean> original) {
        return original.call(section, FabricApiModule.BLOCK_API.isLoaded() ? (Predicate<BlockState>) (state) -> !horizon$air(state) : predicate);
    }

    @WrapOperation(method = "recalcBlockCounts", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"))
    private boolean horizon$fabricAirCount(BlockState state, Operation<Boolean> original) {
        return FabricApiModule.BLOCK_API.isLoaded() ? horizon$air(state) : original.call(state);
    }

    private static boolean horizon$air(@NonNull BlockState state) {
        return state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR) || state.is(Blocks.VOID_AIR);
    }
}
