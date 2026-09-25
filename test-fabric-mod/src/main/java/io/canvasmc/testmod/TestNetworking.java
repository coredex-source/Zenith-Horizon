package io.canvasmc.testmod;

import net.fabricmc.fabric.api.networking.v1.FabricServerConfigurationPacketListenerImpl;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ConfigurationTask;
import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class TestNetworking {
    private TestNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TextPayload.ECHO, TextPayload.codec(TextPayload.ECHO));
        PayloadTypeRegistry.clientboundPlay().register(TextPayload.ECHO, TextPayload.codec(TextPayload.ECHO));
        PayloadTypeRegistry.serverboundConfiguration().register(TextPayload.CONFIG, TextPayload.codec(TextPayload.CONFIG));
        PayloadTypeRegistry.clientboundConfiguration().register(TextPayload.CONFIG, TextPayload.codec(TextPayload.CONFIG));

        ServerPlayNetworking.registerGlobalReceiver(TextPayload.ECHO, (payload, context) -> {
            TestMod.LOGGER.info("networking: play payload '{}' from {} on {}", payload.text(), context.player().getName().getString(), Thread.currentThread().getName());
            context.responseSender().sendPacket(new TextPayload(TextPayload.ECHO, "echo " + payload.text()));
        });

        ServerConfigurationConnectionEvents.CONFIGURE.register((listener, server) -> {
            if (ServerConfigurationNetworking.canSend(listener, TextPayload.CONFIG)) {
                ((FabricServerConfigurationPacketListenerImpl) listener).addTask(new ConfigTask());
            }
        });
        ServerConfigurationNetworking.registerGlobalReceiver(TextPayload.CONFIG, (payload, context) -> {
            TestMod.LOGGER.info("networking: configuration payload '{}' on {}", payload.text(), Thread.currentThread().getName());
            ((FabricServerConfigurationPacketListenerImpl) context.packetListener()).completeTask(ConfigTask.TYPE);
        });

        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> TestMod.LOGGER.info(
            "networking: {} joined, can send echo={}", listener.player.getName().getString(), ServerPlayNetworking.canSend(listener, TextPayload.ECHO)));
    }

    public record TextPayload(Type<TextPayload> type, String text) implements CustomPacketPayload {
        static final Type<TextPayload> ECHO = new Type<>(Identifier.fromNamespaceAndPath("horizon-testmod", "echo"));
        static final Type<TextPayload> CONFIG = new Type<>(Identifier.fromNamespaceAndPath("horizon-testmod", "config"));

        static @NonNull StreamCodec<FriendlyByteBuf, TextPayload> codec(@NonNull Type<TextPayload> type) {
            return StreamCodec.of((buf, payload) -> buf.writeBytes(payload.text().getBytes(StandardCharsets.UTF_8)), (buf) -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new TextPayload(type, new String(bytes, StandardCharsets.UTF_8));
            });
        }
    }

    private record ConfigTask() implements ConfigurationTask {
        static final Type TYPE = new Type("horizon-testmod:config");

        @Override
        public void start(@NonNull Consumer<Packet<?>> sender) {
            sender.accept(ServerConfigurationNetworking.createClientboundPacket(new TextPayload(TextPayload.CONFIG, "ping")));
        }

        @Override
        public @NonNull Type type() {
            return TYPE;
        }
    }
}
