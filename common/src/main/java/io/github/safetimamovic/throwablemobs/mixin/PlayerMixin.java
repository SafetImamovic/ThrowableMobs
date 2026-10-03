package io.github.safetimamovic.throwablemobs.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.safetimamovic.throwablemobs.carry.CarryManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
abstract class PlayerMixin
{
        /** Air control ignores the movement speed attribute, so without this the carry slowdown vanishes mid-jump. */
        @ModifyReturnValue(method = "getFlyingSpeed", at = @At("RETURN"))
        private float throwablemobs$slowAirControlWhileCarrying(float speed)
        {
                return (float) (speed * CarryManager.speedFactor((Player) (Object) this));
        }
}
