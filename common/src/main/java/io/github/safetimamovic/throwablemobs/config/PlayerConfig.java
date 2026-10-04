package io.github.safetimamovic.throwablemobs.config;

import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.HudAnchor;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.HudConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.TrajectoryStyle;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
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

        static
        {
                ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

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
                        get(TRAJECTORY_STYLE));
        }

        /** Not loaded on a dedicated server, which has no client config; the defaults apply there. */
        private static <T> T get(ConfigValue<T> value)
        {
                return SPEC.isLoaded() ? value.get() : value.getDefault();
        }
}
