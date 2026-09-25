package io.canvasmc.testmod.mixin.preflight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.minecraft.server.TestmodMissingPseudoClass")
public class PseudoTargetMixin {
}
