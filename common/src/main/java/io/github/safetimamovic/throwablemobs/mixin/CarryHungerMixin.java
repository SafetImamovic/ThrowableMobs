package io.github.safetimamovic.throwablemobs.mixin;

import io.github.safetimamovic.throwablemobs.carry.CarryHunger;
import net.minecraft.server.level.ServerPlayer;
//? if <1.21
/*import net.minecraft.world.entity.player.Player;*/
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Movement statistics moved from Player to ServerPlayer in 1.21.
//? if >=1.21 {
@Mixin(ServerPlayer.class)
//?} else
/*@Mixin(Player.class)*/
abstract class CarryHungerMixin
{
        @Inject(method = "checkMovementStatistics", at = @At("HEAD"))
        private void throwablemobs$carryExhaustion(double dx, double dy, double dz, CallbackInfo ci)
        {
                if ((Object) this instanceof ServerPlayer player)
                {
                        CarryHunger.move(player, dx, dz);
                }
        }
}
