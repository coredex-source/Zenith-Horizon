package io.canvasmc.testmod.mixin;

import io.canvasmc.testmod.ServerExtension;
import io.canvasmc.testmod.TestMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "runServer", at = @At("HEAD"))
    private void testmod$logGameInstance(CallbackInfo ci) {
        TestMod.LOGGER.info("mixin on runServer, game instance: {}", FabricLoader.getInstance().getGameInstance().getClass().getName());
        TestMod.LOGGER.info("accessor tickCount={}, injected interface says {}", ((MinecraftServerAccessor) this).testmod$tickCount(), ((ServerExtension) this).testmod$injected());
    }

    @Environment(EnvType.CLIENT)
    @Inject(method = "testmod$clientOnlyTarget", at = @At("HEAD"))
    private void testmod$clientOnly(CallbackInfo ci) {
    }
}
