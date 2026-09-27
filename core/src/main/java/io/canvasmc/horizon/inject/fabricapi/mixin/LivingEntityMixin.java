package io.canvasmc.horizon.inject.fabricapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.canvasmc.horizon.fabric.FabricApiModule;
import io.canvasmc.horizon.inject.fabricapi.FabricEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.potion.CraftPotionUtil;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @WrapOperation(method = "removeAllEffects(Lorg/bukkit/event/entity/EntityPotionEffectEvent$Cause;)Z", at = @At(value = "INVOKE", target = "Lorg/bukkit/craftbukkit/event/CraftEventFactory;callEntityPotionEffectChangeEvent(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/effect/MobEffectInstance;Lorg/bukkit/event/entity/EntityPotionEffectEvent$Cause;Lorg/bukkit/event/entity/EntityPotionEffectEvent$Action;)Lorg/bukkit/event/entity/EntityPotionEffectEvent;"))
    private EntityPotionEffectEvent horizon$fabricRemoveAllEffects(LivingEntity entity, MobEffectInstance effect, MobEffectInstance newEffect, EntityPotionEffectEvent.Cause cause, EntityPotionEffectEvent.Action action, Operation<EntityPotionEffectEvent> original) {
        if (!FabricApiModule.ENTITY_EVENTS.isLoaded()) {
            return original.call(entity, effect, newEffect, cause, action);
        }
        if (!FabricEvents.allowEarlyRemove(effect, entity)) {
            EntityPotionEffectEvent kept = new EntityPotionEffectEvent(entity.getBukkitEntity(), CraftPotionUtil.toBukkit(effect), null, null, cause, action, false);
            kept.setCancelled(true);
            return kept;
        }

        EntityPotionEffectEvent event = original.call(entity, effect, newEffect, cause, action);
        if (!event.isCancelled()) {
            FabricEvents.beforeRemove(effect, entity);
        }
        return event;
    }
}
