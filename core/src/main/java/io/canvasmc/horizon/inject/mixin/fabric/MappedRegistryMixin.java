package io.canvasmc.horizon.inject.mixin.fabric;

import io.canvasmc.horizon.fabric.HorizonFabric;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin<T> {

    @Inject(method = "register", at = @At("RETURN"))
    private void horizon$bindValue(ResourceKey<T> key, T value, RegistrationInfo info, CallbackInfoReturnable<Holder.Reference<T>> cir) {
        if (HorizonFabric.registriesOpen()) {
            ((HolderReferenceAccessor) cir.getReturnValue()).horizon$bindValue(value);
        }
    }
}
