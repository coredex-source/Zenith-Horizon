package io.canvasmc.horizon.inject.fabricapi;

import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class FabricChat {
    private static final MethodHandle VANILLA_DECORATE = vanillaDecorate();

    private FabricChat() {
    }

    public static @NonNull Component decorate(@Nullable ServerPlayer sender, @NonNull Component message) {
        try {
            return (Component) VANILLA_DECORATE.invokeExact(ServerMessageDecoratorEvent.EVENT.invoker(), sender, message);
        } catch (RuntimeException | Error exception) {
            throw exception;
        } catch (Throwable thrown) {
            throw new IllegalStateException(thrown);
        }
    }

    public static boolean allowChatMessage(@NonNull PlayerChatMessage message, @NonNull ServerPlayer sender) {
        return ServerMessageEvents.ALLOW_CHAT_MESSAGE.invoker().allowChatMessage(message, sender, ChatType.bind(ChatType.CHAT, sender));
    }

    public static void chatMessage(@NonNull PlayerChatMessage message, @NonNull ServerPlayer sender) {
        ServerMessageEvents.CHAT_MESSAGE.invoker().onChatMessage(message, sender, ChatType.bind(ChatType.CHAT, sender));
    }

    private static @NonNull MethodHandle vanillaDecorate() {
        try {
            return MethodHandles.publicLookup().findVirtual(ChatDecorator.class, "decorate", MethodType.methodType(Component.class, ServerPlayer.class, Component.class));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("ChatDecorator is missing vanilla's decorate method", exception);
        }
    }
}
