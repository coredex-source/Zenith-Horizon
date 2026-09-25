package io.canvasmc.testmod.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
    @Accessor("tickCount")
    int testmod$tickCount();

    @Invoker("logFullTickTime")
    void testmod$logFullTickTime();
}
