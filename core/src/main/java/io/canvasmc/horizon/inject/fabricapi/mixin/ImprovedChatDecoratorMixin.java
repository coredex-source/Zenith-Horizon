package io.canvasmc.horizon.inject.fabricapi.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.canvasmc.horizon.fabric.FabricApiModule;
import io.canvasmc.horizon.inject.fabricapi.FabricChat;
import io.papermc.paper.adventure.ImprovedChatDecorator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.CompletableFuture;

@Mixin(ImprovedChatDecorator.class)
public abstract class ImprovedChatDecoratorMixin {

    @WrapMethod(method = "decorate(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/network/chat/Component;)Ljava/util/concurrent/CompletableFuture;")
    private static CompletableFuture<Component> horizon$fabricDecorators(MinecraftServer server, @Nullable ServerPlayer player, @Nullable CommandSourceStack source, Component message, Operation<CompletableFuture<Component>> original) {
        if (!FabricApiModule.MESSAGE_API.isLoaded()) {
            return original.call(server, player, source, message);
        }
        return CompletableFuture.supplyAsync(() -> FabricChat.decorate(player, message), server.chatExecutor)
            .thenCompose(decorated -> original.call(server, player, source, decorated));
    }
}
