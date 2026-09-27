package io.canvasmc.horizon.inject.mixin.fabric;

import io.canvasmc.horizon.fabric.HorizonFabric;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.timers.TimerQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow
    public abstract ServerLevel overworld();

    @Inject(method = "getGameRules", at = @At("HEAD"), cancellable = true)
    private void horizon$overworldGameRules(CallbackInfoReturnable<GameRules> cir) {
        ServerLevel overworld = HorizonFabric.isLoaded() ? this.overworld() : null;
        if (overworld != null) {
            cir.setReturnValue(overworld.getGameRules());
        }
    }

    @Inject(method = "getWeatherData", at = @At("HEAD"), cancellable = true)
    private void horizon$overworldWeather(CallbackInfoReturnable<WeatherData> cir) {
        ServerLevel overworld = HorizonFabric.isLoaded() ? this.overworld() : null;
        if (overworld != null) {
            cir.setReturnValue(overworld.getWeatherData());
        }
    }

    @Inject(method = "getScheduledEvents", at = @At("HEAD"), cancellable = true)
    private void horizon$overworldScheduledEvents(CallbackInfoReturnable<TimerQueue<MinecraftServer>> cir) {
        ServerLevel overworld = HorizonFabric.isLoaded() ? this.overworld() : null;
        if (overworld != null) {
            cir.setReturnValue(overworld.scheduledEvents);
        }
    }
}
