package io.canvasmc.horizon.inject.fabricapi;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.logger.Logger;
import io.papermc.paper.connection.PluginMessageBridgeImpl;
import net.fabricmc.fabric.api.networking.v1.ClientboundConfigurationChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ClientboundPlayChannelEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.NonNull;

import java.util.List;

final class PluginChannelBridge {
    private static final Logger LOGGER = Logger.fork(HorizonLoader.LOGGER, "plugin_channels");

    private PluginChannelBridge() {
    }

    static void register() {
        ClientboundConfigurationChannelEvents.REGISTER.register((listener, sender, server, channels) -> update(server, listener.paperConnection, channels, true));
        ClientboundConfigurationChannelEvents.UNREGISTER.register((listener, sender, server, channels) -> update(server, listener.paperConnection, channels, false));
        ClientboundPlayChannelEvents.REGISTER.register((listener, sender, server, channels) -> update(server, listener.player.getBukkitEntity(), channels, true));
        ClientboundPlayChannelEvents.UNREGISTER.register((listener, sender, server, channels) -> update(server, listener.player.getBukkitEntity(), channels, false));
    }

    private static void update(@NonNull MinecraftServer server, @NonNull PluginMessageBridgeImpl bridge, @NonNull List<Identifier> channels, boolean register) {
        List<String> names = channels.stream().map(Identifier::toString).toList();
        server.execute(() -> {
            for (String channel : names) {
                try {
                    if (register) {
                        bridge.addChannel(channel);
                    } else {
                        bridge.removeChannel(channel);
                    }
                } catch (IllegalStateException exception) {
                    LOGGER.warn("Couldn't give plugins the channel {}: {} (start with -Dpaper.disableChannelLimit to lift Paper's limit)", channel, exception.getMessage());
                    return;
                }
            }
        });
    }
}
