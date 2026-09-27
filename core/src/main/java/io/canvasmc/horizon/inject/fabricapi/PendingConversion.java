package io.canvasmc.horizon.inject.fabricapi;

import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

public interface PendingConversion {
    void horizon$convertedFrom(Mob previous, ConversionParams params);

    @Nullable Mob horizon$previous();

    @Nullable ConversionParams horizon$takeParams();
}
