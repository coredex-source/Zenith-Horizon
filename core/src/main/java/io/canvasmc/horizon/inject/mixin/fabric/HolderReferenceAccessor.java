package io.canvasmc.horizon.inject.mixin.fabric;

import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Holder.Reference.class)
public interface HolderReferenceAccessor {

    @Invoker("bindValue")
    void horizon$bindValue(Object value);
}
