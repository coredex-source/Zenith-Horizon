package io.canvasmc.horizon.inject.mixin.fabric;

import io.canvasmc.horizon.fabric.HorizonFabric;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltInRegistries.class)
public abstract class BuiltInRegistriesMixin {

    @Shadow
    @Final
    public static Registry<? extends Registry<?>> REGISTRY;

    @Shadow
    private static void createContents() {
        throw new AssertionError();
    }

    @Shadow
    private static void freeze() {
        throw new AssertionError();
    }

    @Shadow
    private static <T extends Registry<?>> void validate(Registry<T> registry) {
        throw new AssertionError();
    }

    @Inject(method = "bootStrap(Ljava/lang/Runnable;)V", at = @At("HEAD"), cancellable = true)
    private static void horizon$deferFreeze(Runnable runnable, CallbackInfo ci) {
        switch (HorizonFabric.registryBootstrap()) {
            case DEFER -> {
                REGISTRY.freeze();
                createContents();
                runnable.run();
                ci.cancel();
            }
            case FREEZE -> {
                runnable.run();
                freeze();
                validate(REGISTRY);
                ci.cancel();
            }
            case SKIP -> ci.cancel();
            case RUN -> {
            }
        }
    }
}
