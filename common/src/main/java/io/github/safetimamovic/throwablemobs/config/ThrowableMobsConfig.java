package io.github.safetimamovic.throwablemobs.config;

/**
 * Global settings. Hardcoded until M2 adds a config file; every value here becomes a config key then, so code reads
 * settings only through this class.
 */
public final class ThrowableMobsConfig
{
        public static final CarryConfig CARRY = new CarryConfig(
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
                CarriedFacing.CARRIER,
                true,
                GroundFilter.DENY_LIST,
                DimensionChange.TRAVEL,
                CarrierLogout.KEEP_CARRYING,
                PromptAnchor.CROSSHAIR,
                0,
                12);

        private ThrowableMobsConfig()
        {
        }

        /** Client: where the pick-up / put-down prompt is drawn from; the offsets move it from there. */
        public enum PromptAnchor
        {
                /** Screen centre, where the crosshair is. */
                CROSSHAIR,
                /** Top centre of the screen. */
                TOP,
                /** Bottom centre of the screen (use a negative Y offset to sit above the hotbar). */
                BOTTOM
        }

        /** Which way a carried mob faces. */
        public enum CarriedFacing
        {
                /** Turns with its carrier, facing where the carrier looks. */
                CARRIER,
                /** Keeps the direction it faced when picked up. */
                KEEP
        }

        /** Which blocks count as ground to put a carried mob down on. Both need a collision shape to stand on. */
        public enum GroundFilter
        {
                /** Any block except those in {@code #throwablemobs:drop_ground_denied}. */
                DENY_LIST,
                /** Only blocks in {@code #throwablemobs:drop_ground_allowed}. */
                ALLOW_LIST
        }

        /** What happens to a carried mob when its carrier changes dimension (portal, {@code /tp}). */
        public enum DimensionChange
        {
                /** The mob is put down where the carrier left. */
                DROP,
                /** The mob travels with the carrier and stays carried. */
                TRAVEL
        }

        /** What happens to a carried mob when its carrier logs out (or the server stops). */
        public enum CarrierLogout
        {
                /** The mob is put down where the carrier was. */
                DROP,
                /**
                 * The mob stays where it is, frozen, until the carrier logs back in and carries it again. A carrier who
                 * never returns leaves it frozen for good.
                 */
                KEEP_CARRYING
        }

        /**
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
         * @param dropNeedsGround        putting a mob down needs the carrier to look at valid ground within reach (so
         *                               it can't be dropped off a skyblock platform); otherwise it falls back to in
         *                               front of the carrier, or their feet
         * @param dropGroundFilter       which blocks count as valid ground
         * @param onDimensionChange      what happens to a carried mob when its carrier changes dimension
         * @param onCarrierLogout        what happens to a carried mob when its carrier logs out
         * @param promptAnchor           client: where the prompt is drawn from
         * @param promptOffsetX          client: horizontal prompt offset from the anchor, in GUI pixels
         * @param promptOffsetY          client: vertical prompt offset from the anchor, in GUI pixels (down is positive)
         */
        public record CarryConfig(
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
                CarriedFacing carriedFacing,
                boolean dropNeedsGround,
                GroundFilter dropGroundFilter,
                DimensionChange onDimensionChange,
                CarrierLogout onCarrierLogout,
                PromptAnchor promptAnchor,
                int promptOffsetX,
                int promptOffsetY)
        {
        }
}
