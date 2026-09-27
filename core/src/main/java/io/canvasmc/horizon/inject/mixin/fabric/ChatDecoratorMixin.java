package io.canvasmc.horizon.inject.mixin.fabric;

import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChatDecorator.class)
public interface ChatDecoratorMixin {

    @SuppressWarnings("deprecation")
    default Component decorate(@Nullable ServerPlayer sender, Component message) {
        return ((ChatDecorator) this).decorate(sender, message).join();
    }
}
