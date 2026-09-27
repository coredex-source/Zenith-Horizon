package io.canvasmc.coverage;

import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

final class DecoratorListener {
    private static final MethodType VANILLA_DECORATE = MethodType.methodType(Component.class, ServerPlayer.class, Component.class);

    private DecoratorListener() {
    }

    static @NonNull ChatDecorator create(@NonNull Recorder recorder, @NonNull String event) throws Throwable {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle target = lookup.findStatic(DecoratorListener.class, "decorate", VANILLA_DECORATE.insertParameterTypes(0, Recorder.class, String.class));
        MethodType factory = MethodType.methodType(ChatDecorator.class, Recorder.class, String.class);
        return (ChatDecorator) LambdaMetafactory.metafactory(lookup, "decorate", factory, VANILLA_DECORATE, target, VANILLA_DECORATE).getTarget().invoke(recorder, event);
    }

    private static @NonNull Component decorate(@NonNull Recorder recorder, @NonNull String event, @Nullable ServerPlayer sender, @NonNull Component message) {
        recorder.record(event, new Object[]{sender, message});
        return message;
    }
}
