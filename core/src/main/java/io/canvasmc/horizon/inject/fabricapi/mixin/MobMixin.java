package io.canvasmc.horizon.inject.fabricapi.mixin;

import io.canvasmc.horizon.fabric.FabricApiModule;
import io.canvasmc.horizon.inject.fabricapi.PendingConversion;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin implements PendingConversion {

    @Unique
    private @Nullable Mob horizon$previous;

    @Unique
    private @Nullable ConversionParams horizon$params;

    @Inject(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$CancellingAfterConversion;Lorg/bukkit/event/entity/EntityTransformEvent$TransformReason;Lorg/bukkit/event/entity/CreatureSpawnEvent$SpawnReason;)Lnet/minecraft/world/entity/Mob;", at = @At("RETURN"))
    private void horizon$rememberConversion(EntityType<?> type, ConversionParams params, EntitySpawnReason spawnReason, ConversionParams.CancellingAfterConversion<?> afterConversion, EntityTransformEvent.@Nullable TransformReason transformReason, CreatureSpawnEvent.@Nullable SpawnReason creatureSpawnReason, CallbackInfoReturnable<Mob> cir) {
        if (FabricApiModule.ENTITY_EVENTS.isLoaded() && transformReason == null && cir.getReturnValue() instanceof PendingConversion converted) {
            converted.horizon$convertedFrom((Mob) (Object) this, params);
        }
    }

    @Override
    public void horizon$convertedFrom(Mob previous, ConversionParams params) {
        this.horizon$previous = previous;
        this.horizon$params = params;
    }

    @Override
    public @Nullable Mob horizon$previous() {
        return this.horizon$previous;
    }

    @Override
    public @Nullable ConversionParams horizon$takeParams() {
        ConversionParams params = this.horizon$params;
        this.horizon$params = null;
        this.horizon$previous = null;
        return params;
    }
}
