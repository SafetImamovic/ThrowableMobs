package io.github.safetimamovic.throwablemobs.carry;

import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.HungerConfig;
import net.minecraft.server.level.ServerPlayer;

/** Extra server-side food costs, applied through vanilla exhaustion so saturation and other food mods still work. */
public final class CarryHunger
{
        private CarryHunger()
        {
        }

        public static void pickUp(ServerPlayer player)
        {
                exhaust(player, ThrowableMobsConfig.hunger().pickUpExhaustion());
        }

        public static void thrown(ServerPlayer player)
        {
                exhaust(player, ThrowableMobsConfig.hunger().throwExhaustion());
        }

        /** Uses vanilla's accepted movement deltas, rather than tracking positions across teleports and logouts. */
        public static void move(ServerPlayer player, double dx, double dz)
        {
                if (!CarryManager.enabledFor(player) || CarryManager.carriedBy(player) == null
                        || !player.onGround() || player.isPassenger() || player.isInWater() || player.onClimbable()
                        || player.getAbilities().flying || player.isFallFlying())
                {
                        return;
                }
                HungerConfig config = ThrowableMobsConfig.hunger();
                double rate = player.isSprinting() ? config.sprintExhaustionPerBlock() : config.walkExhaustionPerBlock();
                exhaust(player, Math.hypot(dx, dz) * rate);
        }

        private static void exhaust(ServerPlayer player, double amount)
        {
                if (amount > 0.0 && !player.isCreative() && !player.isSpectator())
                {
                        player.causeFoodExhaustion((float) amount);
                }
        }
}
