package io.github.safetimamovic.throwablemobs.mixin;

import io.github.safetimamovic.throwablemobs.carry.CarryManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin
{
        /** The sprint-jump boost is a fixed push, not scaled by movement speed; slow it like walking. */
        @ModifyArg(method = "jumpFromGround", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/entity/LivingEntity;addDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
        private Vec3 throwablemobs$slowSprintJumpWhileCarrying(Vec3 boost)
        {
                return boost.scale(CarryManager.speedFactor((LivingEntity) (Object) this));
        }
}
