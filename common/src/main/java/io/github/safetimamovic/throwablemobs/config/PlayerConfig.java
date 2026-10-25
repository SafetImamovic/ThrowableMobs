package io.github.safetimamovic.throwablemobs.config;

import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.FlightPoseStyle;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.HudAnchor;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.HudConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.TrajectoryStyle;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * The player file, {@code throwablemobs-client.toml}: personal preferences, editable in game from the mod list (Mod
 * Menu on Fabric). Values are read every time, so changes apply right away. Key mappings are in Controls instead.
 */
public final class PlayerConfig
{
        public static final String FILE_NAME = "throwablemobs-client.toml";

        public static final ModConfigSpec SPEC;

        private static final BooleanValue ENABLED;

        private static final IntValue PICK_UP_COOLDOWN_TICKS;
        private static final IntValue PUT_DOWN_COOLDOWN_TICKS;

        private static final BooleanValue SHOW_PROMPT;
        private static final EnumValue<HudAnchor> PROMPT_ANCHOR;
        private static final IntValue PROMPT_OFFSET_X;
        private static final IntValue PROMPT_OFFSET_Y;

        private static final BooleanValue SHOW_CHARGE_BAR;
        private static final EnumValue<HudAnchor> CHARGE_BAR_ANCHOR;
        private static final IntValue CHARGE_BAR_OFFSET_X;
        private static final IntValue CHARGE_BAR_OFFSET_Y;
        private static final IntValue CHARGE_BAR_WIDTH;
        private static final IntValue CHARGE_BAR_HEIGHT;

        private static final BooleanValue SHOW_TRAJECTORY;
        private static final EnumValue<TrajectoryStyle> TRAJECTORY_STYLE;

        private static final EnumValue<FlightPoseStyle> FLIGHT_POSE;
        private static final DoubleValue SPIRAL_SPEED;

        private static final BooleanValue CATCH_HIGHLIGHT;

        private static final BooleanValue FLIGHT_TRAIL;
        private static final BooleanValue RAGDOLL_IN_FLIGHT;

        static
        {
                ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

                builder.comment("The whole mod, for you.").push("general");
                ENABLED = builder.comment("Off: your keys are plain Sneak and Use again, no prompt or charge bar,",
                                "and you don't catch mobs thrown at you. Anything you carry is put down. Other",
                                "players can still throw mobs, and those can still hit you.")
                        .define("enabled", true);
                builder.pop();

                builder.comment("Short pauses so a double press doesn't pick a mob up and put it straight back down.",
                        "The server may set longer ones; the longer one counts.").push("cooldown");
                PICK_UP_COOLDOWN_TICKS = builder.comment("Ticks after putting down or throwing a mob before you can",
                                "pick one up again (20 = one second). 0 = none.")
                        .defineInRange("pick_up_cooldown_ticks", 6, 0, 1200);
                PUT_DOWN_COOLDOWN_TICKS = builder.comment("Ticks after picking up or catching a mob before you can put",
                                "it down (20 = one second). 0 = none.")
                        .defineInRange("put_down_cooldown_ticks", 6, 0, 1200);
                builder.pop();

                builder.comment("The pick-up / put-down / throw prompt.").push("prompt");
                SHOW_PROMPT = builder.comment("Show the prompt when looking at a mob or while carrying one.")
                        .define("show_prompt", true);
                PROMPT_ANCHOR = builder.comment("Where the prompt is drawn from: CROSSHAIR (screen centre), TOP or",
                                "BOTTOM (use a negative Y offset to sit above the hotbar, e.g. -72).")
                        .defineEnum("prompt_anchor", HudAnchor.CROSSHAIR);
                PROMPT_OFFSET_X = builder.comment("Horizontal offset from the anchor, in GUI pixels (positive = right).")
                        .defineInRange("prompt_offset_x", 0, -4096, 4096);
                PROMPT_OFFSET_Y = builder.comment("Vertical offset from the anchor, in GUI pixels (positive = down).")
                        .defineInRange("prompt_offset_y", 12, -4096, 4096);
                builder.pop();

                builder.comment("The bar that fills while charging a throw.").push("charge_bar");
                SHOW_CHARGE_BAR = builder.comment("Show the charge bar while charging a throw.")
                        .define("show_charge_bar", true);
                CHARGE_BAR_ANCHOR = builder.comment("Where the bar is drawn from: CROSSHAIR (screen centre), TOP or",
                                "BOTTOM. The bar is centred horizontally on the anchor.")
                        .defineEnum("charge_bar_anchor", HudAnchor.BOTTOM);
                CHARGE_BAR_OFFSET_X = builder.comment("Horizontal offset from the anchor, in GUI pixels (positive =",
                                "right).")
                        .defineInRange("charge_bar_offset_x", 0, -4096, 4096);
                CHARGE_BAR_OFFSET_Y = builder.comment("Vertical offset of the bar's top edge from the anchor, in GUI",
                                "pixels (positive = down). The default sits above the hotbar.")
                        .defineInRange("charge_bar_offset_y", -90, -4096, 4096);
                CHARGE_BAR_WIDTH = builder.comment("Width in GUI pixels.")
                        .defineInRange("charge_bar_width", 120, 1, 1024);
                CHARGE_BAR_HEIGHT = builder.comment("Height in GUI pixels.")
                        .defineInRange("charge_bar_height", 10, 1, 256);
                builder.pop();

                builder.comment("The preview of where a thrown mob will fly and land, shown while charging (if the",
                        "server allows it).").push("trajectory");
                SHOW_TRAJECTORY = builder.comment("Show the trajectory preview.")
                        .define("show_trajectory", true);
                TRAJECTORY_STYLE = builder.comment("How the path is drawn: LINE (solid), DASHED, DOTS (one per tick),",
                                "or LANDING_ONLY (just the box where it lands). The landing box shows in every style.")
                        .defineEnum("trajectory_style", TrajectoryStyle.LINE);
                builder.pop();

                builder.comment("How thrown mobs look in the air. Visual only: hitboxes and hits don't change.")
                        .push("flight_pose");
                FLIGHT_POSE = builder.comment("NONE: as vanilla. FACE_DIRECTION: turns to face where it flies.",
                                "TILT: also tilts with the arc, nose up then nose down. HEAD_FIRST: like an arrow,",
                                "the body lies along the path with the head leading.")
                        .defineEnum("flight_pose", FlightPoseStyle.HEAD_FIRST);
                SPIRAL_SPEED = builder.comment("Spin around the flight direction with TILT or HEAD_FIRST, like a",
                                "thrown football, in degrees per tick (36 = one turn every half second, 0 = no spin).")
                        .defineInRange("spiral_speed", 0.0, 0.0, 360.0);
                builder.pop();

                builder.comment("Catching mobs other players throw at you.").push("catch");
                CATCH_HIGHLIGHT = builder.comment("Outline a thrown mob when it's coming your way and you can catch it.")
                        .define("catch_highlight", true);
                builder.pop();

                builder.comment("Effects drawn on your screen only.").push("effects");
                FLIGHT_TRAIL = builder.comment("A faint trail behind fast thrown mobs.")
                        .define("flight_trail", true);
                RAGDOLL_IN_FLIGHT = builder.comment("With Ragdollified installed (NeoForge), thrown mobs go limp in the",
                                "air and get back up when they land. Visual only, like flight_pose.")
                        .define("ragdoll_in_flight", true);
                builder.pop();

                SPEC = builder.build();
        }

        private PlayerConfig()
        {
        }

        static HudConfig read()
        {
                return new HudConfig(
                        get(SHOW_PROMPT),
                        get(PROMPT_ANCHOR),
                        get(PROMPT_OFFSET_X),
                        get(PROMPT_OFFSET_Y),
                        get(SHOW_CHARGE_BAR),
                        get(CHARGE_BAR_ANCHOR),
                        get(CHARGE_BAR_OFFSET_X),
                        get(CHARGE_BAR_OFFSET_Y),
                        get(CHARGE_BAR_WIDTH),
                        get(CHARGE_BAR_HEIGHT),
                        get(SHOW_TRAJECTORY),
                        get(TRAJECTORY_STYLE),
                        get(FLIGHT_POSE),
                        get(SPIRAL_SPEED).floatValue(),
                        get(CATCH_HIGHLIGHT),
                        get(FLIGHT_TRAIL),
                        get(RAGDOLL_IN_FLIGHT),
                        get(PICK_UP_COOLDOWN_TICKS),
                        get(PUT_DOWN_COOLDOWN_TICKS));
        }

        static boolean enabled()
        {
                return get(ENABLED);
        }

        /** Not loaded on a dedicated server, which has no client config; the defaults apply there. */
        private static <T> T get(ConfigValue<T> value)
        {
                return SPEC.isLoaded() ? value.get() : value.getDefault();
        }
}
