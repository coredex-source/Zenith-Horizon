package io.canvasmc.horizon.inject.fabricapi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.canvasmc.horizon.fabric.FabricApiModule;
import io.canvasmc.horizon.inject.fabricapi.FabricEvents;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow
    @Final
    public LevelStorageSource.LevelStorageAccess storageSource;

    @Shadow
    public abstract Iterable<ServerLevel> getAllLevels();

    @Shadow
    public abstract LayeredRegistryAccess<RegistryLayer> registries();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void horizon$fabricRegistryState(CallbackInfo ci) {
        if (FabricApiModule.REGISTRY_SYNC.isLoaded()) {
            FabricEvents.writeRegistryState(this.storageSource, this.registries().compositeAccess());
        }
    }

    @Inject(method = "removeLevel", at = @At("HEAD"))
    private void horizon$fabricLevelUnload(ServerLevel level, CallbackInfo ci) {
        if (FabricApiModule.LIFECYCLE_EVENTS.isLoaded()) {
            FabricEvents.levelUnload((MinecraftServer) (Object) this, level);
        }
    }

    @WrapOperation(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;saveAllChunks(ZZZZ)Z"))
    private boolean horizon$fabricSaveAtShutdown(MinecraftServer server, boolean silent, boolean flush, boolean force, boolean close, Operation<Boolean> original) {
        if (!FabricApiModule.LIFECYCLE_EVENTS.isLoaded()) {
            return original.call(server, silent, flush, force, close);
        }
        FabricEvents.beforeSave(server, flush, force);
        boolean saved = original.call(server, silent, flush, force, close);
        FabricEvents.afterSave(server, flush, force);
        return saved;
    }

    @Inject(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;saveAllChunks(ZZZZ)Z", shift = At.Shift.AFTER))
    private void horizon$fabricLevelUnloadAtShutdown(CallbackInfo ci) {
        if (FabricApiModule.LIFECYCLE_EVENTS.isLoaded()) {
            for (ServerLevel level : this.getAllLevels()) {
                FabricEvents.levelUnload((MinecraftServer) (Object) this, level);
            }
        }
    }
}
