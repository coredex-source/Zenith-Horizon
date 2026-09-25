package io.canvasmc.testmod.mixin.quarantine;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class ConstantMinecraftServerMixin {
    @Inject(method = "tickServer", at = @At(value = "CONSTANT", args = "stringValue=testmod$missingConstant"))
    private void testmod$missingConstant(BooleanSupplier haveTime, CallbackInfo ci) {
    }
}
