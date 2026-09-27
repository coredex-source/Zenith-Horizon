package io.canvasmc.testplugin.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sheep.class)
public abstract class SheepMixin extends Animal {
    protected SheepMixin(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Unique
    private static final double MAX_SPEED = 2.0;

    @Inject(method = "aiStep", at = @At("HEAD"))
    public void test$crazySheep(CallbackInfo ci) {
        Vec3 movement = this.getDeltaMovement().multiply(2, 2, 2);
        this.setDeltaMovement(movement.lengthSqr() > MAX_SPEED * MAX_SPEED ? movement.normalize().scale(MAX_SPEED) : movement);
    }
}
