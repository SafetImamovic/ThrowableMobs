package io.github.safetimamovic.throwablemobs.config;

import io.github.safetimamovic.throwablemobs.api.BounceImpacts;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.BehaviorConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CatchConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarriedFacing;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarrierLogout;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarryConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.DimensionChange;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.EffectsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.GroundFilter;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.SelfCatch;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.SelfDamage;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.ThrowConfig;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
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

        private static final BooleanValue ENABLED;

        private static final BooleanValue REQUIRE_EMPTY_HAND;
        private static final DoubleValue MAX_MASS;
        private static final DoubleValue DEFAULT_DENSITY;
        private static final BooleanValue ALLOW_VILLAGERS;
        private static final BooleanValue OWNER_ONLY_FOR_TAMED;
        private static final IntValue PICK_UP_COOLDOWN_TICKS;

        private static final DoubleValue SLOWDOWN_FACTOR;
        private static final DoubleValue MAX_SLOWDOWN;
        private static final BooleanValue PROTECT_FROM_SUFFOCATION;
        private static final BooleanValue PROTECT_FROM_FALL;
        private static final BooleanValue PROTECT_FROM_CARRIER;
        private static final EnumValue<CarriedFacing> CARRIED_FACING;
        private static final EnumValue<DimensionChange> ON_DIMENSION_CHANGE;
        private static final EnumValue<CarrierLogout> ON_CARRIER_LOGOUT;
        private static final IntValue CARRIER_LOGOUT_SECONDS;

        private static final BooleanValue PUT_DOWN_NEEDS_GROUND;
        private static final EnumValue<GroundFilter> GROUND_FILTER;
        private static final IntValue PUT_DOWN_COOLDOWN_TICKS;

        private static final DoubleValue THROW_IMPULSE;
        private static final DoubleValue STRENGTH_EFFECT_BONUS;
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
        private static final BooleanValue PVP;

        private static final BooleanValue CATCHING_ENABLED;
        private static final DoubleValue CATCH_RADIUS;
        private static final DoubleValue CATCH_CONE_DEGREES;
        private static final EnumValue<SelfCatch> SELF_CATCH;
        private static final IntValue SELF_CATCH_DELAY_TICKS;
        private static final DoubleValue LATENCY_COMPENSATION;
        private static final IntValue MAX_LATENCY_TICKS;
        private static final DoubleValue CATCH_RECOIL;
        private static final BooleanValue CATCH_DAMAGE;
        private static final DoubleValue CATCH_ABSORB_MOMENTUM;
        private static final DoubleValue CATCH_DAMAGE_FACTOR;

        private static final BooleanValue SOUNDS;
        private static final BooleanValue PARTICLES;
        private static final DoubleValue EFFECT_MIN_ENERGY;
        private static final DoubleValue EFFECT_FULL_ENERGY;

        private static final BooleanValue BEHAVIORS_ENABLED;
        private static final BooleanValue AUTO_DETECT_BEHAVIORS;
        private static final ConfigValue<List<? extends String>> DISABLED_BEHAVIORS;
        private static final DoubleValue EGG_CHANCE;

        static
        {
                ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

                builder.comment("The whole mod on this server.").push("general");
                ENABLED = builder.comment("Off: nobody can pick up, throw or catch mobs. Carried mobs are put down and",
                                "thrown mobs fall, right away. Mob profiles and the other settings are kept.")
                        .define("enabled", true);
                builder.pop();

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
                PICK_UP_COOLDOWN_TICKS = builder.comment("Ticks after putting down or throwing a mob before the same",
                                "player can pick one up again (20 = one second). 0 = no cooldown. Each player can add",
                                "a longer one of their own.")
                        .defineInRange("pick_up_cooldown_ticks", 0, 0, 1200);
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
                ON_CARRIER_LOGOUT = builder.comment("When the carrier logs out or the server stops.",
                                "PUT_DOWN: the mob is put down at the carrier's feet and is free.",
                                "RESUME: put down too, but carried again if the carrier logs back in within",
                                "carrier_logout_seconds and the mob is within 16 blocks.",
                                "KEEP_CARRYING: the mob waits at the carrier's feet, frozen, and is carried again when",
                                "they log back in; it's put down after carrier_logout_seconds.",
                                "TAKE_ALONG: the mob leaves the world with the carrier (saved in their player data) and",
                                "is carried again whenever they log back in.")
                        .defineEnum("on_carrier_logout", CarrierLogout.PUT_DOWN);
                CARRIER_LOGOUT_SECONDS = builder.comment("How long RESUME remembers the carrier and KEEP_CARRYING waits",
                                "for them, in seconds. 0 = forever.")
                        .defineInRange("carrier_logout_seconds", 60, 0, 604_800);
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
                PUT_DOWN_COOLDOWN_TICKS = builder.comment("Ticks after picking up or catching a mob before it can be",
                                "put down (20 = one second). 0 = no cooldown. Each player can add a longer one of",
                                "their own.")
                        .defineInRange("put_down_cooldown_ticks", 0, 0, 1200);
                builder.pop();

                builder.comment("Throwing a carried mob: hold the Throw key to charge, let go to throw.").push("throw");
                THROW_IMPULSE = builder.comment("Strength of a full-charge throw. Launch speed = impulse × charge / mass,",
                                "so light mobs fly fast and heavy ones barely leave your hands (150 throws a cow at",
                                "about 0.33 blocks/tick).")
                        .defineInRange("throw_impulse", 150.0, 0.0, 1_000_000.0);
                STRENGTH_EFFECT_BONUS = builder.comment("Extra impulse per level of the Strength effect: 0.25 makes",
                                "Strength II throw 50 % harder. 0 = the effect doesn't help.")
                        .defineInRange("strength_effect_bonus", 0.25, 0.0, 10.0);
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
                THROWER_GRACE_TICKS = builder.comment("Ticks after the throw before the mob can hit its thrower.")
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
                BLOCK_RESTITUTION = builder.comment("Bounciness off blocks for every mob: 0 = no bounce, the flight",
                                "ends at the first block; 1 = keeps all its speed. Mob profiles can override it.")
                        .defineInRange("block_restitution", 0.0, 0.0, 2.0);
                MAX_BOUNCES = builder.comment("Bounces off blocks before the flight ends. Profiles can override it.")
                        .defineInRange("max_bounces", 3, 0, 100);
                MIN_BOUNCE_SPEED = builder.comment("A bounce slower than this (blocks/tick, away from the block) ends",
                                "the flight, so mobs stop instead of skipping along the ground forever.")
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
                PVP = builder.comment("Thrown mobs can hurt and knock back players. Off: they don't, and players can",
                                "still catch them. Scoreboard teams without friendly fire are always safe from",
                                "their teammates' throws.")
                        .define("pvp", true);
                builder.pop();

                builder.comment("Catching a mob someone threw: an empty main hand, not carrying, looking at it, and",
                        "it comes close enough. The catcher must be allowed to pick the mob up.").push("catch");
                CATCHING_ENABLED = builder.comment("Off: thrown mobs can't be caught, they hit players as before.")
                        .define("catching_enabled", true);
                CATCH_RADIUS = builder.comment("How close the mob's path must pass the catcher's eyes, in blocks.")
                        .defineInRange("catch_radius", 1.5, 0.0, 16.0);
                CATCH_CONE_DEGREES = builder.comment("The catcher must look within this many degrees of the mob.")
                        .defineInRange("catch_cone_degrees", 60.0, 0.0, 180.0);
                SELF_CATCH = builder.comment("When the thrower can catch their own mob. NEVER. AFTER_BOUNCE: once it",
                                "bounced off a block (a wall throw coming back). AFTER_DELAY: after self_catch_delay_ticks.",
                                "AFTER_BOUNCE_OR_DELAY: whichever comes first.")
                        .defineEnum("self_catch", SelfCatch.AFTER_BOUNCE);
                SELF_CATCH_DELAY_TICKS = builder.comment("Ticks after the throw before the thrower can catch their",
                                "own mob, with self_catch AFTER_DELAY or AFTER_BOUNCE_OR_DELAY.")
                        .defineInRange("self_catch_delay_ticks", 10, 0, 200);
                LATENCY_COMPENSATION = builder.comment("Players with ping see the mob late, so the radius grows by",
                                "speed × ping in ticks × this. 0 = off.")
                        .defineInRange("latency_compensation", 1.0, 0.0, 4.0);
                MAX_LATENCY_TICKS = builder.comment("Ping above this many ticks counts as this much.")
                        .defineInRange("max_latency_ticks", 6, 0, 40);
                CATCH_RECOIL = builder.comment("Knockback on the catcher per unit of the mob's momentum",
                                "(mass × speed, game-kg × blocks/tick).")
                        .defineInRange("catch_recoil", 0.002, 0.0, 1.0);
                CATCH_DAMAGE = builder.comment("Hard catches hurt the catcher.")
                        .define("catch_damage", false);
                CATCH_ABSORB_MOMENTUM = builder.comment("Momentum a catcher takes without damage.")
                        .defineInRange("catch_absorb_momentum", 200.0, 0.0, 1_000_000.0);
                CATCH_DAMAGE_FACTOR = builder.comment("Damage per unit of momentum above catch_absorb_momentum.")
                        .defineInRange("catch_damage_factor", 0.02, 0.0, 100.0);
                builder.pop();

                builder.comment("Sounds and particles on throws, bounces, impacts and catches, for everyone nearby.",
                        "The flight trail is a player setting.").push("effects");
                SOUNDS = builder.comment("Play our sounds. Resource packs can replace them one by one.")
                        .define("sounds", true);
                PARTICLES = builder.comment("Particle bursts: a puff on throws and catches, block dust on impacts.")
                        .define("particles", true);
                EFFECT_MIN_ENERGY = builder.comment("Bounces and impacts with less collision energy than this are",
                                "silent, so a mob rolling along the ground doesn't keep thudding.")
                        .defineInRange("effect_min_energy", 2.0, 0.0, 10_000.0);
                EFFECT_FULL_ENERGY = builder.comment("Collision energy for full volume and the most particles. A",
                                "hard creeper throw reaches about 46.")
                        .defineInRange("effect_full_energy", 40.0, 0.0, 10_000.0);
                builder.pop();

                builder.comment("What mobs do when picked up, thrown and when they hit something (creepers explode,",
                        "endermen teleport...). Set per mob with mob profiles in datapacks.").push("behaviors");
                BEHAVIORS_ENABLED = builder.comment("Off: no mob behaviors run. Mob profile physics",
                                "(mass, gravity, bounces) still applies.")
                        .define("behaviors_enabled", true);
                AUTO_DETECT_BEHAVIORS = builder.comment("Guess behaviors for modded mobs from what they are: a",
                                "modded creeper explodes, a modded slime bounces and splits. A mob profile",
                                "for the mob overrides the guess.")
                        .define("auto_detect_behaviors", true);
                DISABLED_BEHAVIORS = builder.comment("Behavior types that never run, e.g. [\"throwablemobs:explode\"].")
                        .defineListAllowEmpty("disabled_behaviors", List.of(), () -> "throwablemobs:explode",
                                value -> value instanceof String id && Identifier.tryParse(id) != null);
                EGG_CHANCE = builder.comment("Chance a thrown chicken lays an egg when it lands (0 = never, 1 =",
                                "every throw). Rolled once per throw.")
                        .defineInRange("egg_chance", 0.25, 0.0, 1.0);
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
                        get(CARRIER_LOGOUT_SECONDS),
                        get(PUT_DOWN_NEEDS_GROUND),
                        get(GROUND_FILTER),
                        get(PICK_UP_COOLDOWN_TICKS),
                        get(PUT_DOWN_COOLDOWN_TICKS));
        }

        static ThrowConfig readThrow()
        {
                return new ThrowConfig(
                        get(THROW_IMPULSE),
                        get(STRENGTH_EFFECT_BONUS),
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
                        get(SELF_DAMAGE),
                        get(PVP));
        }

        static CatchConfig readCatch()
        {
                return new CatchConfig(
                        get(CATCHING_ENABLED),
                        get(CATCH_RADIUS),
                        get(CATCH_CONE_DEGREES),
                        get(SELF_CATCH),
                        get(SELF_CATCH_DELAY_TICKS),
                        get(LATENCY_COMPENSATION),
                        get(MAX_LATENCY_TICKS),
                        get(CATCH_RECOIL),
                        get(CATCH_DAMAGE),
                        get(CATCH_ABSORB_MOMENTUM),
                        get(CATCH_DAMAGE_FACTOR));
        }

        static EffectsConfig readEffects()
        {
                return new EffectsConfig(get(SOUNDS), get(PARTICLES), get(EFFECT_MIN_ENERGY), get(EFFECT_FULL_ENERGY));
        }

        static BehaviorConfig readBehaviors()
        {
                Set<Identifier> disabled = new HashSet<>();
                for (String id : get(DISABLED_BEHAVIORS))
                {
                        Identifier parsed = Identifier.tryParse(id);
                        if (parsed != null)
                        {
                                disabled.add(parsed);
                        }
                }
                return new BehaviorConfig(get(BEHAVIORS_ENABLED), get(AUTO_DETECT_BEHAVIORS), Set.copyOf(disabled),
                        get(EGG_CHANCE));
        }

        static boolean enabled()
        {
                return get(ENABLED);
        }

        /** A server config isn't loaded on a client outside a world; the defaults apply there. */
        private static <T> T get(ConfigValue<T> value)
        {
                return SPEC.isLoaded() ? value.get() : value.getDefault();
        }
}
