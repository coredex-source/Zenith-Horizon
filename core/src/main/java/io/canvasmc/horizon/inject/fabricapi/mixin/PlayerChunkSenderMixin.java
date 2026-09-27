package io.canvasmc.horizon.inject.fabricapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.canvasmc.horizon.fabric.FabricApiModule;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.BitSet;

@Mixin(PlayerChunkSender.class)
public abstract class PlayerChunkSenderMixin {

    @WrapOperation(method = "sendChunk", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/lighting/LevelLightEngine;Ljava/util/BitSet;Ljava/util/BitSet;Z)Lnet/minecraft/network/protocol/game/ClientboundLevelChunkWithLightPacket;"))
    private static ClientboundLevelChunkWithLightPacket horizon$fabricChunkPacketContext(LevelChunk chunk, LevelLightEngine light, BitSet skyChanges, BitSet blockChanges, boolean modifyBlocks,
                                                                                         Operation<ClientboundLevelChunkWithLightPacket> original, ServerGamePacketListenerImpl listener) {
        if (!FabricApiModule.NETWORKING.isLoaded()) {
            return original.call(chunk, light, skyChanges, blockChanges, modifyBlocks);
        }
        return PacketContext.supplyWithContext((PacketContextProvider) (Object) listener, () -> original.call(chunk, light, skyChanges, blockChanges, modifyBlocks));
    }
}
