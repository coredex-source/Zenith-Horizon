package io.canvasmc.testmod.mixin;

import io.canvasmc.testmod.TestMod;
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
    }
}
