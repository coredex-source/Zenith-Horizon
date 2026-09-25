package io.canvasmc.horizon.inject.fabricapi;

import net.fabricmc.fabric.impl.networking.server.ServerNetworkingImpl;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.jspecify.annotations.NonNull;

public final class FabricNetworking {
    private FabricNetworking() {
    }

    public static boolean handleConfigurationPayload(@NonNull ServerConfigurationPacketListenerImpl listener, @NonNull ServerboundCustomPayloadPacket packet) {
        try {
            return ServerNetworkingImpl.getAddon(listener).handle(packet.payload());
        } catch (RunningOnDifferentThreadException exception) {
            MinecraftServer.getServer().packetProcessor().scheduleIfPossible(listener, packet);
            return true;
        }
    }
}
