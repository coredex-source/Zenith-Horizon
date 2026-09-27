package io.canvasmc.horizon.inject.fabricapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.canvasmc.horizon.fabric.FabricApiModule;
import io.canvasmc.horizon.inject.fabricapi.FabricEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @WrapOperation(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;Z)V"))
    private void horizon$fabricCustomDamage(ItemStack stack, int amount, ServerLevel level, LivingEntity player, Consumer<ItemStack> onBreak, boolean force, Operation<Void> original, @Local(argsOnly = true) LivingEntity owner, @Local(argsOnly = true) EquipmentSlot slot) {
        if (FabricApiModule.ITEM_API.isLoaded() && !owner.hasInfiniteMaterials()) {
            boolean[] broken = {false};
            Integer damage = FabricEvents.customDamage(stack, amount, owner, slot, () -> {
                broken[0] = true;
                ItemStack copy = stack.copy();
                stack.shrink(1);
                onBreak.accept(copy);
            });
            if (broken[0]) {
                return;
            }
            if (damage != null) {
                amount = damage;
            }
        }
        original.call(stack, amount, level, player, onBreak, force);
    }
}
