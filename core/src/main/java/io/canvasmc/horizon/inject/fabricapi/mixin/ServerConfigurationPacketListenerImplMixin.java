package io.canvasmc.horizon.inject.fabricapi.mixin;

import io.canvasmc.horizon.fabric.HorizonFabric;
import io.canvasmc.horizon.inject.fabricapi.FabricNetworking;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class ServerConfigurationPacketListenerImplMixin {

    @Shadow
    public abstract void startConfiguration();

    @Inject(method = "startConfiguration", at = @At("HEAD"), cancellable = true)
    private void horizon$configureOnMainThread(CallbackInfo ci) {
        MinecraftServer server = MinecraftServer.getServer();
        if (!server.isSameThread()) {
            server.execute(this::startConfiguration);
            ci.cancel();
        }
    }

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void horizon$fabricConfigurationPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (HorizonFabric.hasNetworking() && FabricNetworking.handleConfigurationPayload((ServerConfigurationPacketListenerImpl) (Object) this, packet)) {
            ci.cancel();
        }
    }
}
