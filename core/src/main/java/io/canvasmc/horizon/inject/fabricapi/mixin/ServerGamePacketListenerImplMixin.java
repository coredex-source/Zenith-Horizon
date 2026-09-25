package io.canvasmc.horizon.inject.fabricapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.canvasmc.horizon.fabric.HorizonFabric;
import io.canvasmc.horizon.inject.fabricapi.FabricEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Shadow
    protected abstract void tryPickItem(ItemStack itemStack, @Nullable BlockPos blockPos, @Nullable Entity entity, boolean includeData);

    @WrapOperation(method = "handlePickItemFromBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getCloneItemStack(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack horizon$fabricPickItemFromBlock(BlockState state, LevelReader level, BlockPos pos, boolean includeData, Operation<ItemStack> original, @Local(argsOnly = true) ServerboundPickItemFromBlockPacket packet) {
        if (HorizonFabric.hasInteractionEvents()) {
            ItemStack stack = FabricEvents.pickItemFromBlock(this.player, pos, state, packet.includeData());
            if (stack != null) {
                if (!stack.isEmpty()) {
                    this.tryPickItem(stack, pos, null, packet.includeData());
                }
                return ItemStack.EMPTY;
            }
        }
        return original.call(state, level, pos, includeData);
    }

    @WrapOperation(method = "handlePickItemFromEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getPickResult()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack horizon$fabricPickItemFromEntity(Entity entity, Operation<ItemStack> original, @Local(argsOnly = true) ServerboundPickItemFromEntityPacket packet) {
        if (HorizonFabric.hasInteractionEvents()) {
            ItemStack stack = FabricEvents.pickItemFromEntity(this.player, entity, packet.includeData());
            if (stack != null) {
                if (!stack.isEmpty()) {
                    this.tryPickItem(stack, null, entity, packet.includeData());
                }
                return ItemStack.EMPTY;
            }
        }
        return original.call(entity);
    }
}
