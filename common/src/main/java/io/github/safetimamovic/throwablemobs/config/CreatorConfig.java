package io.github.safetimamovic.throwablemobs.config;

import io.github.safetimamovic.throwablemobs.api.BounceImpacts;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarriedFacing;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarrierLogout;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarryConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.DimensionChange;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.GroundFilter;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.SelfDamage;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.ThrowConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * The creator file, {@code config/throwablemobs-server.toml}: gameplay rules for pack devs and server admins. Synced to
 * clients, which need it for the prompt and the put-down check. A copy in a world's override folder wins for that world:
 * {@code syncedconfig/} on NeoForge, {@code serverconfig/} on Fabric (Forge Config API Port).
 *
 * <p>NeoForge's config API; on Fabric, Forge Config API Port provides the same classes.
 */
public final class CreatorConfig
{
        public static final String FILE_NAME = "throwablemobs-server.toml";

        public static final ModConfigSpec SPEC;

        private static final BooleanValue REQUIRE_EMPTY_HAND;
        private static final DoubleValue MAX_MASS;
        private static final DoubleValue DEFAULT_DENSITY;
        private static final BooleanValue ALLOW_VILLAGERS;
        private static final BooleanValue OWNER_ONLY_FOR_TAMED;

        private static final DoubleValue SLOWDOWN_FACTOR;
        private static final DoubleValue MAX_SLOWDOWN;
        private static final BooleanValue PROTECT_FROM_SUFFOCATION;
        private static final BooleanValue PROTECT_FROM_FALL;
        private static final BooleanValue PROTECT_FROM_CARRIER;
        private static final EnumValue<CarriedFacing> CARRIED_FACING;
        private static final EnumValue<DimensionChange> ON_DIMENSION_CHANGE;
        private static final EnumValue<CarrierLogout> ON_CARRIER_LOGOUT;

        private static final BooleanValue PUT_DOWN_NEEDS_GROUND;
        private static final EnumValue<GroundFilter> GROUND_FILTER;

        private static final DoubleValue THROW_IMPULSE;
        private static final DoubleValue MAX_THROW_SPEED;
        private static final IntValue MAX_CHARGE_TICKS;
        private static final IntValue MIN_CHARGE_TICKS;
        private static final DoubleValue INHERIT_VELOCITY;
        private static final IntValue THROWER_GRACE_TICKS;
        private static final BooleanValue ALLOW_TRAJECTORY;

        private static final DoubleValue GRAVITY;
        private static final DoubleValue DRAG;
        private static final IntValue MAX_FLIGHT_TICKS;
        private static final BooleanValue AI_IN_FLIGHT;
        private static final DoubleValue BLOCK_RESTITUTION;
        private static final IntValue MAX_BOUNCES;
        private static final DoubleValue MIN_BOUNCE_SPEED;
        private static final EnumValue<BounceImpacts> BOUNCE_IMPACTS;

        private static final DoubleValue DAMAGE_PER_ENERGY;
        private static final DoubleValue MIN_IMPACT_ENERGY;
        private static final DoubleValue MAX_IMPACT_DAMAGE;
        private static final DoubleValue RESTITUTION;
        private static final BooleanValue VANILLA_FALL_DAMAGE;
        private static final EnumValue<SelfDamage> SELF_DAMAGE;

        static
        {
                ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

                builder.comment("Who can pick up what. Which mobs can never be carried is set with the entity type",
                        "tag #throwablemobs:unthrowable; pets only their owner can carry with #throwablemobs:owner_only.")
                        .push("pick_up");
                REQUIRE_EMPTY_HAND = builder.comment("Picking up needs an empty main hand.")
                        .define("require_empty_hand", true);
                MAX_MASS = builder.comment("Heaviest mob a player can lift, in game-kg. Mass comes from the mob's",
                                "hitbox volume times default_density (a cow is about 450, an iron golem about 2100).")
                        .defineInRange("max_mass", 1500.0, 1.0, 1_000_000.0);
                DEFAULT_DENSITY = builder.comment("Density used to get a mob's mass from its hitbox, game-kg per block³.")
                        .defineInRange("default_density", 400.0, 1.0, 100_000.0);
                ALLOW_VILLAGERS = builder.comment("Villagers and wandering traders can be picked up.")
                        .define("allow_villagers", true);
                OWNER_ONLY_FOR_TAMED = builder.comment("Tamed mobs can only be picked up by their owner.")
                        .define("owner_only_for_tamed", true);
                builder.pop();

                builder.comment("While a mob is carried.").push("carry");
                SLOWDOWN_FACTOR = builder.comment("How much a mob at max_mass slows its carrier, before max_slowdown.",
                                "Slowdown = mass / max_mass × slowdown_factor, so 0.5 makes a cow about 15 % slower.")
                        .defineInRange("slowdown_factor", 0.5, 0.0, 10.0);
                MAX_SLOWDOWN = builder.comment("Cap on the carrier slowdown (0.6 = at most 60 % slower).")
                        .defineInRange("max_slowdown", 0.6, 0.0, 0.95);
                PROTECT_FROM_SUFFOCATION = builder.comment("Carried mobs take no suffocation damage (their head is in",
                                "the ceiling in 2-high tunnels).")
                        .define("protect_from_suffocation", true);
                PROTECT_FROM_FALL = builder.comment("Carried mobs take no fall damage when their carrier falls.")
                        .define("protect_from_fall", true);
                PROTECT_FROM_CARRIER = builder.comment("Carried mobs can't be hurt by their carrier.")
                        .define("protect_from_carrier", true);
                CARRIED_FACING = builder.comment("Which way carried mobs face. CARRIER: where the carrier looks.",
                                "KEEP: the direction they faced when picked up.")
                        .defineEnum("carried_facing", CarriedFacing.CARRIER);
                ON_DIMENSION_CHANGE = builder.comment("When the carrier changes dimension (portal, /tp). TRAVEL: the",
                                "mob comes along and stays carried. PUT_DOWN: it is put down where the carrier left.")
                        .defineEnum("on_dimension_change", DimensionChange.TRAVEL);
                ON_CARRIER_LOGOUT = builder.comment("When the carrier logs out or the server stops. KEEP_CARRYING:",
                                "the mob waits, frozen, and is carried again when they log back in (a carrier who",
                                "never returns leaves it frozen). PUT_DOWN: it is put down where the carrier was.")
                        .defineEnum("on_carrier_logout", CarrierLogout.KEEP_CARRYING);
                builder.pop();

                builder.comment("Putting a carried mob down.").push("put_down");
                PUT_DOWN_NEEDS_GROUND = builder.comment("The carrier must look at valid ground within reach, so mobs",
                                "can't be dropped off a skyblock platform. Off: put down in front of the carrier, or",
                                "at their feet, when not looking at ground.")
                        .define("put_down_needs_ground", true);
                GROUND_FILTER = builder.comment("Which blocks count as ground (they also need something to stand on).",
                                "DENY_LIST: any block except #throwablemobs:put_down_ground_denied.",
                                "ALLOW_LIST: only #throwablemobs:put_down_ground_allowed.")
                        .defineEnum("ground_filter", GroundFilter.DENY_LIST);
                builder.pop();

                builder.comment("Throwing a carried mob: hold the Throw key to charge, let go to throw.").push("throw");
                THROW_IMPULSE = builder.comment("Strength of a full-charge throw. Launch speed = impulse × charge / mass,",
                                "so light mobs fly fast and heavy ones barely leave your hands (150 throws a cow at",
                                "about 0.33 blocks/tick).")
                        .defineInRange("throw_impulse", 150.0, 0.0, 1_000_000.0);
                MAX_THROW_SPEED = builder.comment("Launch speed cap in blocks/tick, so light mobs don't fly forever",
                                "(a full-charge arrow is about 3; 1.0 throws a chicken about 19 blocks).")
                        .defineInRange("max_throw_speed", 1.0, 0.0, 10.0);
                MAX_CHARGE_TICKS = builder.comment("Ticks to reach full charge (20 ticks = 1 second, same as a bow).")
                        .defineInRange("max_charge_ticks", 20, 1, 200);
                MIN_CHARGE_TICKS = builder.comment("Letting go sooner than this cancels the throw.")
                        .defineInRange("min_charge_ticks", 3, 0, 200);
                INHERIT_VELOCITY = builder.comment("Share of the thrower's own movement added to the throw: 1 = all of",
                                "it, so sprint-throws go further; 0 = no running bonus; 2 = double.")
                        .defineInRange("inherit_velocity", 1.0, 0.0, 2.0);
                THROWER_GRACE_TICKS = builder.comment("Ticks after the throw during which the mob can't hit its",
                                "thrower.")
                        .defineInRange("thrower_grace_ticks", 5, 0, 100);
                ALLOW_TRAJECTORY = builder.comment("Players may see the trajectory preview while charging (each one",
                                "can still hide it in their own settings). Off for everyone when false.")
                        .define("allow_trajectory", true);
                builder.pop();

                builder.comment("A thrown mob in the air.").push("flight");
                GRAVITY = builder.comment("Downward acceleration in blocks/tick² (arrows use 0.05).")
                        .defineInRange("gravity", 0.05, 0.0, 1.0);
                DRAG = builder.comment("Share of the speed kept each tick (arrows use 0.99; lower = more air drag).")
                        .defineInRange("drag", 0.99, 0.0, 1.0);
                MAX_FLIGHT_TICKS = builder.comment("Flights end after this many ticks; the mob then falls normally.")
                        .defineInRange("max_flight_ticks", 200, 1, 6000);
                AI_IN_FLIGHT = builder.comment("Thrown mobs get their AI back at launch instead of when they land",
                                "(chickens flap, skeletons turn). Their own movement can then bend the flight a",
                                "little away from the trajectory preview.")
                        .define("ai_in_flight", false);
                BLOCK_RESTITUTION = builder.comment("Bounciness off blocks for every mob: 0 = no bounce, the flight ends",
                                "at the first block; 1 = keeps all its speed. Mob profiles can override it per mob.")
                        .defineInRange("block_restitution", 0.0, 0.0, 2.0);
                MAX_BOUNCES = builder.comment("Bounces off blocks before the flight ends. Mob profiles can override it.")
                        .defineInRange("max_bounces", 3, 0, 100);
                MIN_BOUNCE_SPEED = builder.comment("A bounce slower than this (blocks/tick, away from the block) ends the",
                                "flight, so mobs stop instead of skipping along the ground forever.")
                        .defineInRange("min_bounce_speed", 0.15, 0.0, 10.0);
                BOUNCE_IMPACTS = builder.comment("Whether bounces after the first impact deal damage too: EVERY or",
                                "FIRST_ONLY. Mob profiles can override it.")
                        .defineEnum("bounce_impacts", BounceImpacts.EVERY);
                builder.pop();

                builder.comment("When a thrown mob hits a block or another entity. Damage comes from the collision",
                        "energy, ½ × reduced mass × speed², in game-kg × (blocks/tick)².").push("impact");
                DAMAGE_PER_ENERGY = builder.comment("Damage per unit of collision energy above min_impact_energy.")
                        .defineInRange("damage_per_energy", 0.1, 0.0, 100.0);
                MIN_IMPACT_ENERGY = builder.comment("Collisions below this energy do no damage.")
                        .defineInRange("min_impact_energy", 10.0, 0.0, 1_000_000.0);
                MAX_IMPACT_DAMAGE = builder.comment("Damage cap for one impact, before it is split between the two",
                                "bodies (the lighter one takes more).")
                        .defineInRange("max_impact_damage", 20.0, 0.0, 1000.0);
                RESTITUTION = builder.comment("Bounciness: 0 = the bodies stick together, 1 = perfectly elastic.",
                                "Sets how much momentum the hit entity gets as knockback.")
                        .defineInRange("restitution", 0.3, 0.0, 1.0);
                VANILLA_FALL_DAMAGE = builder.comment("Thrown mobs also take vanilla fall damage when they land, on top",
                                "of the impact damage (measured from the top of the arc; mobs immune to fall damage",
                                "stay immune). Off: impact damage replaces fall damage during the flight.")
                        .define("vanilla_fall_damage", false);
                SELF_DAMAGE = builder.comment("Whether thrown mobs take damage from their own impacts: ALL, NONE, or",
                                "RESPECT_TAG = all but mobs in #throwablemobs:no_self_damage (by default the mobs",
                                "immune to fall damage, like chickens and cats). Mob profiles scale it per mob.")
                        .defineEnum("self_damage", SelfDamage.RESPECT_TAG);
                builder.pop();

                SPEC = builder.build();
        }

        private CreatorConfig()
        {
        }

        static CarryConfig read()
        {
                return new CarryConfig(
                        get(REQUIRE_EMPTY_HAND),
                        get(MAX_MASS),
                        get(DEFAULT_DENSITY),
                        get(ALLOW_VILLAGERS),
                        get(OWNER_ONLY_FOR_TAMED),
                        get(SLOWDOWN_FACTOR),
                        get(MAX_SLOWDOWN),
                        get(PROTECT_FROM_SUFFOCATION),
                        get(PROTECT_FROM_FALL),
                        get(PROTECT_FROM_CARRIER),
                        get(CARRIED_FACING),
                        get(ON_DIMENSION_CHANGE),
                        get(ON_CARRIER_LOGOUT),
                        get(PUT_DOWN_NEEDS_GROUND),
                        get(GROUND_FILTER));
        }

        static ThrowConfig readThrow()
        {
                return new ThrowConfig(
                        get(THROW_IMPULSE),
                        get(MAX_THROW_SPEED),
                        get(MAX_CHARGE_TICKS),
                        get(MIN_CHARGE_TICKS),
                        get(INHERIT_VELOCITY),
                        get(THROWER_GRACE_TICKS),
                        get(ALLOW_TRAJECTORY),
                        get(GRAVITY),
                        get(DRAG),
                        get(MAX_FLIGHT_TICKS),
                        get(AI_IN_FLIGHT),
                        get(BLOCK_RESTITUTION),
                        get(MAX_BOUNCES),
                        get(MIN_BOUNCE_SPEED),
                        get(BOUNCE_IMPACTS),
                        get(DAMAGE_PER_ENERGY),
                        get(MIN_IMPACT_ENERGY),
                        get(MAX_IMPACT_DAMAGE),
                        get(RESTITUTION),
                        get(VANILLA_FALL_DAMAGE),
                        get(SELF_DAMAGE));
        }

        /** A server config isn't loaded on a client outside a world; the defaults apply there. */
        private static <T> T get(ConfigValue<T> value)
        {
                return SPEC.isLoaded() ? value.get() : value.getDefault();
        }
}
