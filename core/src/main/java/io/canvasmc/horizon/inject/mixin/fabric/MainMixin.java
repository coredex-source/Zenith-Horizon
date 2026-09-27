package io.canvasmc.horizon.inject.mixin.fabric;

import io.canvasmc.horizon.fabric.HorizonFabric;
import io.canvasmc.horizon.inject.fabricapi.FabricApiBridges;
import joptsimple.OptionSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Main;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Mixin(Main.class)
public class MainMixin {

    @Inject(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;startTimerHackThread()V"))
    private static void horizon$fabricServerEntrypoints(OptionSet optionSet, CallbackInfo ci) {
        HorizonFabric.startServer();
        if (HorizonFabric.isLoaded()) {
            FabricApiBridges.register();
        }
        if (HorizonFabric.registryFreezePending()) {
            BuiltInRegistries.bootStrap();
            horizon$validate(CreativeModeTabs.class);
            horizon$validate(LootContextParamSets.class);
        }
    }

    private static void horizon$validate(Class<?> holder) {
        Method validate;
        try {
            validate = holder.getMethod("validate");
        } catch (NoSuchMethodException exception) {
            return;
        }
        try {
            validate.invoke(null);
        } catch (InvocationTargetException exception) {
            throw exception.getCause() instanceof RuntimeException runtime ? runtime : new IllegalStateException(exception.getCause());
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
