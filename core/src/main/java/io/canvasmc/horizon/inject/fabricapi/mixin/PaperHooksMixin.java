package io.canvasmc.horizon.inject.fabricapi.mixin;

import ca.spottedleaf.moonrise.paper.PaperHooks;
import io.canvasmc.horizon.fabric.FabricApiModule;
import io.canvasmc.horizon.inject.fabricapi.FabricEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PaperHooks.class)
public abstract class PaperHooksMixin {

    @Inject(method = "screenEntity", at = @At("HEAD"), cancellable = true)
    private void horizon$fabricAllowLoad(ServerLevel level, Entity entity, boolean fromDisk, boolean event, CallbackInfoReturnable<Boolean> cir) {
        if (FabricApiModule.LIFECYCLE_EVENTS.isLoaded() && !FabricEvents.allowEntityLoad(level, entity, fromDisk)) {
            cir.setReturnValue(false);
        }
    }
}
