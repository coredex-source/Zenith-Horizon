package io.canvasmc.horizon.inject.mixin.fabric;

import io.canvasmc.horizon.fabric.HorizonFabric;
import joptsimple.OptionSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Main;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MainMixin {

    @Inject(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;startTimerHackThread()V"))
    private static void horizon$fabricServerEntrypoints(OptionSet optionSet, CallbackInfo ci) {
        HorizonFabric.startServer();
        if (HorizonFabric.registryFreezePending()) {
            BuiltInRegistries.bootStrap();
            CreativeModeTabs.validate();
            LootContextParamSets.validate();
        }
    }
}
