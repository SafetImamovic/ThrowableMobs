package io.github.safetimamovic.throwablemobs.config;

/**
 * Global settings. Hardcoded until M2 adds a config file; every value here becomes a config key then, so code reads
 * settings only through this class.
 */
public final class ThrowableMobsConfig
{
        public static final CarryConfig CARRY = new CarryConfig(
                true,
                true,
                1500.0,
                400.0,
                true,
                true,
                0.5,
                0.6,
                true,
                true,
                true,
                true,
                CarriedFacing.CARRIER);

        private ThrowableMobsConfig()
        {
        }

        /** Which way a carried mob faces. */
        public enum CarriedFacing
        {
                /** Turns with its carrier, facing where the carrier looks. */
                CARRIER,
                /** Keeps the direction it faced when picked up. */
                KEEP
        }

        /**
         * @param requireSneak      pick up / drop needs sneak + use (otherwise use alone)
         * @param requireEmptyHand  pick up needs an empty main hand
         * @param maxMass           heaviest mob a player can lift, in game-kg (see THROWABLE_MOBS.md §8.1)
         * @param defaultDensity    density for mass-from-hitbox, game-kg per block³
         * @param allowVillagers    villagers and wandering traders can be picked up
         * @param ownerOnlyForTamed tamed mobs can only be picked up by their owner
         * @param slowdownFactor    carrier slowdown at {@code maxMass}, before the cap
         * @param maxSlowdown       cap on carrier slowdown (0.6 = 60 % slower)
         * @param showPrompt        client: show the pick-up / drop prompt under the crosshair
         * @param protectFromSuffocation carried mobs take no suffocation damage (their head is in the ceiling in 2-high
         *                               tunnels)
         * @param protectFromFall        carried mobs take no fall damage when their carrier falls
         * @param protectFromCarrier     carried mobs can't be hurt by their carrier
         * @param carriedFacing          which way carried mobs face
         */
        public record CarryConfig(
                boolean requireSneak,
                boolean requireEmptyHand,
                double maxMass,
                double defaultDensity,
                boolean allowVillagers,
                boolean ownerOnlyForTamed,
                double slowdownFactor,
                double maxSlowdown,
                boolean showPrompt,
                boolean protectFromSuffocation,
                boolean protectFromFall,
                boolean protectFromCarrier,
                CarriedFacing carriedFacing)
        {
        }
}
