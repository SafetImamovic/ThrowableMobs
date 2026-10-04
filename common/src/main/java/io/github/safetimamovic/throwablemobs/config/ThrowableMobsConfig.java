package io.github.safetimamovic.throwablemobs.config;

/**
 * Settings, read through this class only. Two files: the creator file ({@link CreatorConfig}, server side, per world,
 * synced to clients) for pack devs and server admins, and the player file ({@link PlayerConfig}, client side, editable
 * in game) for personal preferences. Which mobs can be carried is not configured here but with entity type tags.
 */
public final class ThrowableMobsConfig
{
        private ThrowableMobsConfig()
        {
        }

        /** Current creator settings: the server's values once synced, the defaults before that. */
        public static CarryConfig carry()
        {
                return CreatorConfig.read();
        }

        /** Current creator settings for throwing, flight and impacts. */
        public static ThrowConfig throwing()
        {
                return CreatorConfig.readThrow();
        }

        /** Current player settings; changes in the config screen apply right away. */
        public static HudConfig hud()
        {
                return PlayerConfig.read();
        }

        /** Where a HUD element (prompt, charge bar) is drawn from; its offsets move it from there. */
        public enum HudAnchor
        {
                /** Screen centre, where the crosshair is. */
                CROSSHAIR,
                /** Top centre of the screen. */
                TOP,
                /** Bottom centre of the screen (use a negative Y offset to sit above the hotbar). */
                BOTTOM
        }

        /** How the trajectory preview draws the path. The landing box shows in every style. */
        public enum TrajectoryStyle
        {
                /** A solid line. */
                LINE,
                /** Every other tick's segment, so the line is broken up. */
                DASHED,
                /** One dot per tick, so the spacing shows the speed. */
                DOTS,
                /** No path, just where it lands. */
                LANDING_ONLY
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
                /** Any block except those in {@code #throwablemobs:put_down_ground_denied}. */
                DENY_LIST,
                /** Only blocks in {@code #throwablemobs:put_down_ground_allowed}. */
                ALLOW_LIST
        }

        /** What happens to a carried mob when its carrier changes dimension (portal, {@code /tp}). */
        public enum DimensionChange
        {
                /** The mob is put down where the carrier left. */
                PUT_DOWN,
                /** The mob travels with the carrier and stays carried. */
                TRAVEL
        }

        /** What happens to a carried mob when its carrier logs out (or the server stops). */
        public enum CarrierLogout
        {
                /** The mob is put down where the carrier was. */
                PUT_DOWN,
                /**
                 * The mob stays where it is, frozen, until the carrier logs back in and carries it again. A carrier who
                 * never returns leaves it frozen for good.
                 */
                KEEP_CARRYING
        }

        /**
         * Creator settings, see {@link CreatorConfig} for what each one does.
         */
        public record CarryConfig(
                boolean requireEmptyHand,
                double maxMass,
                double defaultDensity,
                boolean allowVillagers,
                boolean ownerOnlyForTamed,
                double slowdownFactor,
                double maxSlowdown,
                boolean protectFromSuffocation,
                boolean protectFromFall,
                boolean protectFromCarrier,
                CarriedFacing carriedFacing,
                DimensionChange onDimensionChange,
                CarrierLogout onCarrierLogout,
                boolean putDownNeedsGround,
                GroundFilter groundFilter)
        {
        }

        /**
         * Creator settings for throwing, see {@link CreatorConfig} for what each one does. Units: blocks, ticks,
         * blocks/tick, game-kg.
         */
        public record ThrowConfig(
                double throwImpulse,
                double maxThrowSpeed,
                int maxChargeTicks,
                int minChargeTicks,
                double inheritVelocity,
                int throwerGraceTicks,
                boolean allowTrajectory,
                double gravity,
                double drag,
                int maxFlightTicks,
                boolean aiInFlight,
                double damagePerEnergy,
                double minImpactEnergy,
                double maxImpactDamage,
                double restitution)
        {
        }

        /**
         * Player settings, see {@link PlayerConfig} for what each one does.
         */
        public record HudConfig(
                boolean showPrompt,
                HudAnchor promptAnchor,
                int promptOffsetX,
                int promptOffsetY,
                boolean showChargeBar,
                HudAnchor chargeBarAnchor,
                int chargeBarOffsetX,
                int chargeBarOffsetY,
                int chargeBarWidth,
                int chargeBarHeight,
                boolean showTrajectory,
                TrajectoryStyle trajectoryStyle)
        {
        }
}
