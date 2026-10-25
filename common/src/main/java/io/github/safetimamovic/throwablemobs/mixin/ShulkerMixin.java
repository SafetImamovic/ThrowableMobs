package io.github.safetimamovic.throwablemobs.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.safetimamovic.throwablemobs.platform.Services;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A shulker snaps to the block grid, teleports when it has no block to stick to and ignores velocity. While carried or
 * thrown it behaves like a riding shulker instead (free position, no teleport) and keeps its velocity, so it sits still
 * on the carrier's head and flies when thrown. Once it's put down or lands, it attaches as usual.
 */
@Mixin(Shulker.class)
abstract class ShulkerMixin
{
        @ModifyExpressionValue(method = {"tick", "setPos"}, at = @At(value = "INVOKE", target = "isPassenger()Z"))
        private boolean throwablemobs$freeWhileHeld(boolean passenger)
        {
                return passenger || throwablemobs$held();
        }

        @Inject(method = "getDeltaMovement", at = @At("HEAD"), cancellable = true)
        private void throwablemobs$velocityWhileHeld(CallbackInfoReturnable<Vec3> cir)
        {
                if (throwablemobs$held())
                {
                        cir.setReturnValue(((EntityAccessor) this).throwablemobs$deltaMovement());
                }
        }

        @Inject(method = "setDeltaMovement", at = @At("HEAD"), cancellable = true)
        private void throwablemobs$setVelocityWhileHeld(Vec3 deltaMovement, CallbackInfo ci)
        {
                if (throwablemobs$held())
                {
                        ((EntityAccessor) this).throwablemobs$setDeltaMovement(deltaMovement);
                        ci.cancel();
                }
        }

        private boolean throwablemobs$held()
        {
                Shulker self = (Shulker) (Object) this;
                return Services.CARRY_STATE.get(self) != null || Services.THROW_STATE.get(self) != null;
        }
}
