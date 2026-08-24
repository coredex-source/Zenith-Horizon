package io.canvasmc.horizon.inject.mixin.spark;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.canvasmc.horizon.inject.HorizonClassSourceLookup;
import me.lucko.spark.paper.common.SparkPlugin;
import me.lucko.spark.paper.common.sampler.source.ClassSourceLookup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "me.lucko.spark.paper.common.SparkPlatform")
public class SparkPlatformMixin {

    @Shadow
    @Final
    private SparkPlugin plugin;

    @ModifyReturnValue(method = "createClassSourceLookup", at = @At("RETURN"))
    public ClassSourceLookup horizon$modifySourceLookup(final ClassSourceLookup original) {

        // "original" would be the platform lookup, which we can't use since it will
        // potentially exclude the horizon plugins from the "plugins" tab

        return new HorizonClassSourceLookup(this.plugin.createClassFinder(), original);
    }
}
