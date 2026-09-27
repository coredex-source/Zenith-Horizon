package io.canvasmc.horizon.inject.fabricapi.mixin;

import io.canvasmc.horizon.fabric.FabricApiModule;
import io.canvasmc.horizon.inject.fabricapi.FabricChat;
import io.papermc.paper.adventure.ChatProcessor;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.event.player.AbstractChatEvent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatProcessor.class)
public abstract class ChatProcessorMixin {
    @Shadow
    @Final
    ServerPlayer player;
    @Shadow
    @Final
    PlayerChatMessage message;
    @Shadow
    @Final
    net.kyori.adventure.text.Component paper$originalMessage;

    @Inject(method = "process", at = @At("HEAD"), cancellable = true)
    private void horizon$fabricAllowChatMessage(CallbackInfo ci) {
        if (FabricApiModule.MESSAGE_API.isLoaded() && !FabricChat.allowChatMessage(this.message, this.player)) {
            ci.cancel();
        }
    }

    @Inject(method = "complete", at = @At("HEAD"))
    private void horizon$fabricChatMessage(AbstractChatEvent event, CallbackInfo ci) {
        if (FabricApiModule.MESSAGE_API.isLoaded() && !event.isCancelled()) {
            PlayerChatMessage sent = event.message().equals(this.paper$originalMessage) ? this.message : this.message.withUnsignedContent(PaperAdventure.asVanilla(event.message()));
            FabricChat.chatMessage(sent, this.player);
        }
    }
}
