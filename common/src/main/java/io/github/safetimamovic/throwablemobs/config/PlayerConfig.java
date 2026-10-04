package io.github.safetimamovic.throwablemobs.config;

import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.PromptAnchor;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.PromptConfig;
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
        private static final EnumValue<PromptAnchor> PROMPT_ANCHOR;
        private static final IntValue PROMPT_OFFSET_X;
        private static final IntValue PROMPT_OFFSET_Y;
        private static final BooleanValue SHOW_CHARGE_BAR;
        private static final BooleanValue SHOW_TRAJECTORY;

        static
        {
                ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

                builder.comment("The pick-up / put-down / throw prompt.").push("prompt");
                SHOW_PROMPT = builder.comment("Show the prompt when looking at a mob or while carrying one.")
                        .define("show_prompt", true);
                PROMPT_ANCHOR = builder.comment("Where the prompt is drawn from: CROSSHAIR (screen centre), TOP or",
                                "BOTTOM (use a negative Y offset to sit above the hotbar, e.g. -72).")
                        .defineEnum("prompt_anchor", PromptAnchor.CROSSHAIR);
                PROMPT_OFFSET_X = builder.comment("Horizontal offset from the anchor, in GUI pixels (positive = right).")
                        .defineInRange("prompt_offset_x", 0, -4096, 4096);
                PROMPT_OFFSET_Y = builder.comment("Vertical offset from the anchor, in GUI pixels (positive = down).")
                        .defineInRange("prompt_offset_y", 12, -4096, 4096);
                SHOW_CHARGE_BAR = builder.comment("Show a charge bar under the prompt while charging a throw.")
                        .define("show_charge_bar", true);
                SHOW_TRAJECTORY = builder.comment("Show where the mob will fly and land while charging a throw (if",
                                "the server allows it).")
                        .define("show_trajectory", true);
                builder.pop();

                SPEC = builder.build();
        }

        private PlayerConfig()
        {
        }

        static PromptConfig read()
        {
                return new PromptConfig(get(SHOW_PROMPT), get(PROMPT_ANCHOR), get(PROMPT_OFFSET_X), get(PROMPT_OFFSET_Y),
                        get(SHOW_CHARGE_BAR), get(SHOW_TRAJECTORY));
        }

        /** Not loaded on a dedicated server, which has no client config; the defaults apply there. */
        private static <T> T get(ConfigValue<T> value)
        {
                return SPEC.isLoaded() ? value.get() : value.getDefault();
        }
}
