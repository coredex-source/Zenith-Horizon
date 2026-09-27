package io.canvasmc.testmod;

import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class TestChat {
    private static final MethodType VANILLA_DECORATE = MethodType.methodType(Component.class, ServerPlayer.class, Component.class);

    private TestChat() {
    }

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            boolean allowed = !message.signedContent().contains("fabric-deny");
            TestMod.LOGGER.info("chat: fabric allow '{}' from {}: {}", message.signedContent(), sender.getPlainTextName(), allowed);
            return allowed;
        });
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) ->
            TestMod.LOGGER.info("chat: fabric sent '{}' from {}", message.decoratedContent().getString(), sender.getPlainTextName()));
        ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.CONTENT_PHASE, vanillaDecorator());
    }

    private static @NonNull ChatDecorator vanillaDecorator() {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            return (ChatDecorator) LambdaMetafactory.metafactory(lookup, "decorate", MethodType.methodType(ChatDecorator.class), VANILLA_DECORATE,
                lookup.findStatic(TestChat.class, "decorate", VANILLA_DECORATE), VANILLA_DECORATE).getTarget().invoke();
        } catch (Throwable thrown) {
            throw new IllegalStateException(thrown);
        }
    }

    private static @NonNull Component decorate(@Nullable ServerPlayer sender, @NonNull Component message) {
        String text = message.getString();
        return text.contains("fabric-decorate") ? Component.literal(text.replace("fabric-decorate", "fabric-decorated")) : message;
    }
}
