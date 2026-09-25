package io.canvasmc.horizon.inject.fabricapi.mixin;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.protocol.common.custom.CustomPacketPayload$1")
public abstract class CustomPacketPayloadCodecMixin {

    @Inject(
        method = "writeCap(Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$Type;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void horizon$writeDiscardedRaw(FriendlyByteBuf output, CustomPacketPayload.Type<?> type, CustomPacketPayload payload, CallbackInfo ci) {
        if (payload instanceof DiscardedPayload discarded) {
            output.writeIdentifier(type.id());
            output.writeBytes(discarded.data());
            ci.cancel();
        }
    }
}
