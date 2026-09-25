package io.canvasmc.horizon.inject.mixin.fabric;

import io.canvasmc.horizon.fabric.HorizonFabric;
import io.canvasmc.horizon.inject.fabric.FabricEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow
    public abstract Iterable<ServerLevel> getAllLevels();

    @Inject(method = "removeLevel", at = @At("HEAD"))
    private void horizon$fabricLevelUnload(ServerLevel level, CallbackInfo ci) {
        if (HorizonFabric.hasLifecycleEvents()) {
            FabricEvents.levelUnload((MinecraftServer) (Object) this, level);
        }
    }

    @Inject(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;saveAllChunks(ZZZZ)Z", shift = At.Shift.AFTER))
    private void horizon$fabricLevelUnloadAtShutdown(CallbackInfo ci) {
        if (HorizonFabric.hasLifecycleEvents()) {
            for (ServerLevel level : this.getAllLevels()) {
                FabricEvents.levelUnload((MinecraftServer) (Object) this, level);
            }
        }
    }
}
