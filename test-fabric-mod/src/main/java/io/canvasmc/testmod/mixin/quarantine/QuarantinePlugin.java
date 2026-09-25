package io.canvasmc.testmod.mixin.quarantine;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

public class QuarantinePlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return Boolean.getBoolean("testmod.quarantine");
    }
}
