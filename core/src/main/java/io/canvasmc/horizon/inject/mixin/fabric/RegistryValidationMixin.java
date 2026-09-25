package io.canvasmc.horizon.inject.mixin.fabric;

import io.canvasmc.horizon.fabric.HorizonFabric;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CreativeModeTabs.class, LootContextParamSets.class})
public class RegistryValidationMixin {

    @Inject(method = "validate", at = @At("HEAD"), cancellable = true)
    private static void horizon$deferValidation(CallbackInfo ci) {
        if (HorizonFabric.registryFreezePending()) {
            ci.cancel();
        }
    }
}
