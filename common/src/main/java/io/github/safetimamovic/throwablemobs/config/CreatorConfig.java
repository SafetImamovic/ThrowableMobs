package io.github.safetimamovic.throwablemobs.config;

import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarriedFacing;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarrierLogout;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarryConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.DimensionChange;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.GroundFilter;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;

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

        /** A server config isn't loaded on a client outside a world; the defaults apply there. */
        private static <T> T get(ConfigValue<T> value)
        {
                return SPEC.isLoaded() ? value.get() : value.getDefault();
        }
}
