package io.canvasmc.testmod.mixin;

import io.canvasmc.testmod.ServerExtension;
import io.canvasmc.testmod.TestMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
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

        Identifier late = Identifier.fromNamespaceAndPath("horizon-testmod", "late_sound");
        boolean frozen;
        try {
            Registry.register(BuiltInRegistries.SOUND_EVENT, late, SoundEvent.createVariableRangeEvent(late));
            frozen = false;
        } catch (IllegalStateException exception) {
            frozen = true;
        }
        TestMod.LOGGER.info("registry: {} present={}, frozen afterwards={}", TestMod.TEST_SOUND, BuiltInRegistries.SOUND_EVENT.containsKey(TestMod.TEST_SOUND), frozen);
    }

    @Environment(EnvType.CLIENT)
    @Inject(method = "testmod$clientOnlyTarget", at = @At("HEAD"))
    private void testmod$clientOnly(CallbackInfo ci) {
    }
}
