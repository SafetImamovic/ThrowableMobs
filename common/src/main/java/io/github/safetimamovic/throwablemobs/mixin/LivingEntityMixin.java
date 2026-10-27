package io.github.safetimamovic.throwablemobs.mixin;

import io.github.safetimamovic.throwablemobs.carry.CarryManager;
import io.github.safetimamovic.throwablemobs.platform.Services;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin
{
        /** The sprint-jump boost is a fixed push, not scaled by movement speed; slow it like walking. */
        //? if >=1.20.5 {
        @ModifyArg(method = "jumpFromGround", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/entity/LivingEntity;addDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
        private Vec3 throwablemobs$slowSprintJumpWhileCarrying(Vec3 boost)
        {
                return boost.scale(CarryManager.speedFactor((LivingEntity) (Object) this));
        }
        //?} else {
        /*// Before, the boost is added to the velocity as x and z; @ModifyArgs would do both but breaks on Forge.
        @ModifyArg(method = "jumpFromGround", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"), index = 0)
        private double throwablemobs$slowSprintJumpXWhileCarrying(double x)
        {
                return x * CarryManager.speedFactor((LivingEntity) (Object) this);
        }

        @ModifyArg(method = "jumpFromGround", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"), index = 2)
        private double throwablemobs$slowSprintJumpZWhileCarrying(double z)
        {
                return z * CarryManager.speedFactor((LivingEntity) (Object) this);
        }
        *///?}

        /**
         * A carried mob sits on its carrier's head, a tick behind on the client. Whenever the carrier rises into it
         * (jumps, steps up), vanilla pushing would shove them forward every tick, making carrying faster than walking.
         */
        @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
        private void throwablemobs$noPushingWhileCarried(CallbackInfo ci)
        {
                if (Services.CARRY_STATE.get((LivingEntity) (Object) this) != null)
                {
                        ci.cancel();
                }
        }
}
