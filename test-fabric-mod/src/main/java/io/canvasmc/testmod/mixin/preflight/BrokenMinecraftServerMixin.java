package io.canvasmc.testmod.mixin.preflight;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class BrokenMinecraftServerMixin {
    @Shadow
    private int testmod$missingField;

    @Shadow
    protected abstract void testmod$missingMethod();

    @Inject(method = "testmod$missingTarget", at = @At("HEAD"))
    private void testmod$missingTarget(CallbackInfo ci) {
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;testmod$missingCall()V"))
    private void testmod$missingCall(BooleanSupplier haveTime, CallbackInfo ci) {
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void testmod$wrongParameters(int wrong, CallbackInfo ci) {
    }

    @Inject(method = "isStopped", at = @At("HEAD"))
    private void testmod$wrongCallback(CallbackInfo ci) {
    }
}
