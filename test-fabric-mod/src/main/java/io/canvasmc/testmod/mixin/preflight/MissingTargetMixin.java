package io.canvasmc.testmod.mixin.preflight;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.server.TestmodMissingClass")
public class MissingTargetMixin {
}
