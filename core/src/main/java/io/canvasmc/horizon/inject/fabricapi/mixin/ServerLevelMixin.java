package io.canvasmc.horizon.inject.fabricapi.mixin;

import io.canvasmc.horizon.fabric.FabricApiModule;
import io.canvasmc.horizon.inject.fabricapi.FabricEvents;
import io.canvasmc.horizon.inject.fabricapi.PendingConversion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "addFreshEntity(Lnet/minecraft/world/entity/Entity;Lorg/bukkit/event/entity/CreatureSpawnEvent$SpawnReason;)Z", at = @At("HEAD"))
    private void horizon$fabricMobConversion(Entity entity, CreatureSpawnEvent.SpawnReason reason, CallbackInfoReturnable<Boolean> cir) {
        if (FabricApiModule.ENTITY_EVENTS.isLoaded() && entity instanceof PendingConversion pending && entity instanceof Mob converted) {
            Mob previous = pending.horizon$previous();
            ConversionParams params = pending.horizon$takeParams();
            if (previous != null && params != null) {
                FabricEvents.mobConversion(previous, converted, params);
            }
        }
    }
}
