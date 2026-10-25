package io.github.safetimamovic.throwablemobs.config;

import io.github.safetimamovic.throwablemobs.api.BounceImpacts;
import java.util.Set;
import net.minecraft.resources.Identifier;

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

        /** The server's {@code enabled}: off, nobody picks up, throws or catches. Synced to clients. */
        public static boolean enabled()
        {
                return CreatorConfig.enabled();
        }

        /** This client's own {@code enabled}; the server hears of it through {@code PlayerEnabledPayload}. */
        public static boolean playerEnabled()
        {
                return PlayerConfig.enabled();
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

        /** Current creator settings for catching thrown mobs. */
        public static CatchConfig catching()
        {
                return CreatorConfig.readCatch();
        }

        /** Current creator settings for sounds and particles. */
        public static EffectsConfig effects()
        {
                return CreatorConfig.readEffects();
        }

        /** Current creator settings for mob behaviors. */
        public static BehaviorConfig behaviors()
        {
                return CreatorConfig.readBehaviors();
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

        /** How a thrown mob looks in the air. Visual only: the hitbox stays upright. */
        public enum FlightPoseStyle
        {
                /** As vanilla: it keeps facing wherever it faced. */
                NONE,
                /** Turns to face where it flies, staying upright. */
                FACE_DIRECTION,
                /** Faces where it flies and tilts with the arc: nose up on the way up, nose down on the way down. */
                TILT,
                /** Like an arrow: the body lies along the path, head first. */
                HEAD_FIRST
        }

        /** Which way a carried mob faces. */
        public enum CarriedFacing
        {
                /** Turns with its carrier, facing where the carrier looks. */
                CARRIER,
                /** Keeps the direction it faced when picked up. */
                KEEP
        }

        /** Whether a thrown mob takes damage from its own impacts. */
        public enum SelfDamage
        {
                ALL,
                /** All but mobs in {@code #throwablemobs:no_self_damage}. */
                RESPECT_TAG,
                NONE
        }

        /** When the thrower can catch their own mob. */
        public enum SelfCatch
        {
                NEVER,
                /** Once it bounced off a block, like a throw at a wall coming back. */
                AFTER_BOUNCE,
                /** After {@code self_catch_delay_ticks}. */
                AFTER_DELAY,
                /** After a bounce or the delay, whichever comes first. */
                AFTER_BOUNCE_OR_DELAY;

                /** Whether the thrower may catch it now, {@code ticks} after the throw. */
                public boolean allows(long ticks, boolean bounced, int delayTicks)
                {
                        boolean delayed = ticks > delayTicks;
                        return switch (this)
                        {
                                case NEVER -> false;
                                case AFTER_BOUNCE -> bounced;
                                case AFTER_DELAY -> delayed;
                                case AFTER_BOUNCE_OR_DELAY -> bounced || delayed;
                        };
                }
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

        /**
         * What happens to a carried mob when its carrier logs out (or the server stops). The timed options use
         * {@link CarryConfig#carrierLogoutSeconds()}.
         */
        public enum CarrierLogout
        {
                /** The mob is put down at the carrier's feet and is free right away. */
                PUT_DOWN,
                /**
                 * Put down like {@link #PUT_DOWN}, but if the carrier logs back in within the time limit and the mob is
                 * still nearby, it's carried again.
                 */
                RESUME,
                /**
                 * The mob waits at the carrier's feet, frozen, until they log back in and carry it again; after the time
                 * limit it's put down.
                 */
                KEEP_CARRYING,
                /**
                 * The mob leaves the world with the carrier, saved in their player data, and is carried again whenever
                 * they log back in.
                 */
                TAKE_ALONG
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
                int carrierLogoutSeconds,
                boolean putDownNeedsGround,
                GroundFilter groundFilter,
                int pickUpCooldownTicks,
                int putDownCooldownTicks)
        {
        }

        /**
         * Creator settings for throwing, see {@link CreatorConfig} for what each one does. Units: blocks, ticks,
         * blocks/tick, game-kg.
         */
        public record ThrowConfig(
                double throwImpulse,
                double strengthEffectBonus,
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
                double blockRestitution,
                int maxBounces,
                double minBounceSpeed,
                BounceImpacts bounceImpacts,
                double damagePerEnergy,
                double minImpactEnergy,
                double maxImpactDamage,
                double restitution,
                boolean vanillaFallDamage,
                SelfDamage selfDamage,
                boolean pvp)
        {
        }

        /**
         * Creator settings for catching, see {@link CreatorConfig} for what each one does. Units: blocks, degrees,
         * ticks, game-kg × blocks/tick.
         */
        public record CatchConfig(
                boolean enabled,
                double radius,
                double coneDegrees,
                SelfCatch selfCatch,
                int selfCatchDelayTicks,
                double latencyCompensation,
                int maxLatencyTicks,
                double recoil,
                boolean damage,
                double absorbMomentum,
                double damageFactor)
        {
        }

        /**
         * Creator settings for sounds and particles, see {@link CreatorConfig} for what each one does. Energy in the
         * same units as impact damage.
         */
        public record EffectsConfig(boolean sounds, boolean particles, double minEnergy, double fullEnergy)
        {
        }

        /**
         * Creator settings for mob behaviors, see {@link CreatorConfig} for what each one does.
         *
         * @param disabled behavior type ids that never run
         */
        public record BehaviorConfig(boolean enabled, boolean autoDetect, Set<Identifier> disabled)
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
                TrajectoryStyle trajectoryStyle,
                FlightPoseStyle flightPose,
                float spiralSpeed,
                boolean catchHighlight,
                boolean flightTrail,
                boolean ragdollInFlight,
                int pickUpCooldownTicks,
                int putDownCooldownTicks)
        {
        }
}
