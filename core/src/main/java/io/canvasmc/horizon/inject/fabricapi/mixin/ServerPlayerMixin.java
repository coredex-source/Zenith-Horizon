package io.canvasmc.horizon.inject.fabricapi.mixin;

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import io.canvasmc.horizon.fabric.FabricApiModule;
import io.canvasmc.horizon.inject.fabricapi.FabricEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @WrapOperation(method = "getBedResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setRespawnPosition(Lnet/minecraft/server/level/ServerPlayer$RespawnConfig;ZLcom/destroystokyo/paper/event/player/PlayerSetSpawnEvent$Cause;)Z"))
    private boolean horizon$fabricAllowSettingSpawn(ServerPlayer player, ServerPlayer.RespawnConfig config, boolean sendMessage, PlayerSetSpawnEvent.Cause cause, Operation<Boolean> original) {
        if (FabricApiModule.ENTITY_EVENTS.isLoaded() && !FabricEvents.allowSettingSpawn(player, config.respawnData().pos())) {
            return false;
        }
        return original.call(player, config, sendMessage, cause);
    }

    @WrapOperation(method = "getBedResult", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
    private boolean horizon$fabricAllowNearbyMonsters(List<?> monsters, Operation<Boolean> original, @Local(argsOnly = true) BlockPos pos) {
        boolean noMonsters = original.call(monsters);
        return FabricApiModule.ENTITY_EVENTS.isLoaded() ? FabricEvents.allowNearbyMonsters((ServerPlayer) (Object) this, pos, noMonsters) : noMonsters;
    }

    @WrapOperation(method = "openMenu", at = @At(value = "INVOKE", target = "Lorg/bukkit/craftbukkit/event/CraftEventFactory;callInventoryOpenEventWithTitle(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/inventory/AbstractContainerMenu;Z)Lcom/mojang/datafixers/util/Pair;"))
    private Pair<?, ?> horizon$fabricCloseCurrentScreen(ServerPlayer player, AbstractContainerMenu menu, boolean cancelled, Operation<Pair<?, ?>> original, @Local(argsOnly = true) MenuProvider provider) {
        if (FabricApiModule.MENU_API.isLoaded() && player.containerMenu != player.inventoryMenu && FabricEvents.shouldCloseCurrentScreen(provider)) {
            player.connection.send(new ClientboundContainerClosePacket(player.containerMenu.containerId));
        }
        return original.call(player, menu, cancelled);
    }
}
