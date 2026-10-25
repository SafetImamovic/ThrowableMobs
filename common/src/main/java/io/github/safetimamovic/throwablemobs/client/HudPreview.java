package io.github.safetimamovic.throwablemobs.client;

import dev.isxander.yacl3.gui.image.ImageRenderer;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.HudConfig;
import io.github.safetimamovic.throwablemobs.physics.ThrowPhysics;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

/**
 * The prompt and charge bar in YACL's description pane, live: a scaled-down screen with the values being edited, not
 * yet saved, drawn by the HUD's own code. The charge bar fills over and over. The prompt keeps its real text size, or
 * it couldn't be read, so only its position is to scale.
 */
final class HudPreview implements ImageRenderer
{
        private static final int SCREEN = 0xFF2B2B2B;
        private static final int FRAME = 0xFF8B8B8B;
        private static final int CROSSHAIR = 0xFFDDDDDD;
        private static final int HOTBAR = 0x60FFFFFF;
        /** One charge-up and a short pause, in milliseconds. */
        private static final long CYCLE = 1600;

        /** A setting's pending value by its key in the player file, {@code null} if it's not on the screen. */
        private final Function<String, @Nullable Object> pending;

        HudPreview(Function<String, @Nullable Object> pending)
        {
                this.pending = pending;
        }

        @Override
        public int render(GuiGraphicsExtractor graphics, int x, int y, int renderWidth, float tickDelta)
        {
                Minecraft minecraft = Minecraft.getInstance();
                int guiWidth = minecraft.getWindow().getGuiScaledWidth();
                int guiHeight = minecraft.getWindow().getGuiScaledHeight();
                float scale = (float) renderWidth / guiWidth;
                int height = Math.round(guiHeight * scale);
                HudConfig config = config();

                graphics.fill(x - 1, y - 1, x + renderWidth + 1, y + height + 1, FRAME);
                graphics.fill(x, y, x + renderWidth, y + height, SCREEN);
                graphics.enableScissor(x, y, x + renderWidth, y + height);
                //? if >=26.1 {
                graphics.pose().pushMatrix();
                graphics.pose().translate(x, y);
                graphics.pose().scale(scale);
                //?} else {
                /*graphics.pose().pushPose();
                graphics.pose().translate(x, y, 0);
                graphics.pose().scale(scale, scale, 1);
                *///?}
                // The crosshair and hotbar, to see where things sit.
                graphics.fill(guiWidth / 2 - 4, guiHeight / 2, guiWidth / 2 + 5, guiHeight / 2 + 1, CROSSHAIR);
                graphics.fill(guiWidth / 2, guiHeight / 2 - 4, guiWidth / 2 + 1, guiHeight / 2 + 5, CROSSHAIR);
                graphics.fill(guiWidth / 2 - 91, guiHeight - 22, guiWidth / 2 + 91, guiHeight, HOTBAR);
                if (config.showChargeBar())
                {
                        long time = System.currentTimeMillis() % CYCLE;
                        int ticks = (int) (time * 25 / CYCLE);
                        CarryClient.drawChargeBar(graphics, guiWidth, guiHeight, config,
                                ThrowPhysics.chargeFraction(ticks, 20));
                }
                //? if >=26.1 {
                graphics.pose().popMatrix();
                //?} else
                /*graphics.pose().popPose();*/
                if (config.showPrompt())
                {
                        int promptX = x + Math.round(CarryClient.promptX(guiWidth, config) * scale);
                        int promptY = y + Math.round(CarryClient.promptY(guiHeight, config) * scale);
                        CarryClient.drawPrompt(graphics, promptX, promptY, CarryClient.samplePrompt(), null);
                }
                graphics.disableScissor();
                return height;
        }

        /** The saved settings, with the prompt and charge bar ones as they are on the screen. */
        private HudConfig config()
        {
                HudConfig saved = ThrowableMobsConfig.hud();
                return new HudConfig(
                        value("show_prompt", saved.showPrompt()),
                        value("prompt_anchor", saved.promptAnchor()),
                        value("prompt_offset_x", saved.promptOffsetX()),
                        value("prompt_offset_y", saved.promptOffsetY()),
                        value("show_charge_bar", saved.showChargeBar()),
                        value("charge_bar_anchor", saved.chargeBarAnchor()),
                        value("charge_bar_offset_x", saved.chargeBarOffsetX()),
                        value("charge_bar_offset_y", saved.chargeBarOffsetY()),
                        value("charge_bar_width", saved.chargeBarWidth()),
                        value("charge_bar_height", saved.chargeBarHeight()),
                        saved.showTrajectory(),
                        saved.trajectoryStyle(),
                        saved.flightPose(),
                        saved.spiralSpeed(),
                        saved.catchHighlight(),
                        saved.flightTrail(),
                        saved.ragdollInFlight());
        }

        @SuppressWarnings("unchecked")
        private <T> T value(String key, T saved)
        {
                Object value = pending.apply(key);
                return saved.getClass().isInstance(value) ? (T) value : saved;
        }

        @Override
        public void close()
        {
        }
}
