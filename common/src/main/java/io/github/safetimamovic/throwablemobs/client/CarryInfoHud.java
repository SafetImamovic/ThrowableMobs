package io.github.safetimamovic.throwablemobs.client;

import io.github.safetimamovic.throwablemobs.behavior.MobProfiles;
import io.github.safetimamovic.throwablemobs.behavior.ProfilePhysics;
import io.github.safetimamovic.throwablemobs.carry.CarryRules;
import io.github.safetimamovic.throwablemobs.carry.LocalCarry;
import io.github.safetimamovic.throwablemobs.carry.RadarStatus;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarryInfoConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.ThrowConfig;
import io.github.safetimamovic.throwablemobs.network.RadarStatusPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * A small panel about the carried mob: name and health, weight, traits from its profile, and what its radar senses.
 * Everything but the radar is worked out on the client; the radar line comes from the server
 * ({@link RadarStatusPayload}) and only shows while one is running.
 */
public final class CarryInfoHud
{
        private static final int BACKGROUND = 0xA0000000;
        private static final int TEXT = 0xFFDDDDDD;
        private static final int MUTED = 0xFF999999;
        private static final int RADAR_FAR = 0xFFFFE066;
        private static final int RADAR_NEAR = 0xFFFF4040;
        private static final int RADAR_PING = 0xFFFFFFFF;
        private static final int PADDING = 4;
        private static final int LINE_GAP = 2;
        /** Gravity below this share of the normal one reads as floaty (the chicken has 0.4). */
        private static final double FLOATY = 0.75;
        /** Mass at or above this share of {@code max_mass} reads as heavy. */
        private static final double HEAVY = 0.75;
        /** The radar line flashes this many ticks after each scream. */
        private static final int PING_FLASH_TICKS = 3;

        private CarryInfoHud()
        {
        }

        record Line(Component text, int color)
        {
        }

        static void render(GuiGraphicsExtractor graphics)
        {
                Minecraft minecraft = Minecraft.getInstance();
                CarryInfoConfig config = ThrowableMobsConfig.carryInfo();
                if (!config.show() || minecraft.level == null || !(LocalCarry.get() instanceof Mob mob))
                {
                        return;
                }
                draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 1.0F, config,
                        lines(mob, config, minecraft.level.getGameTime()));
        }

        static List<Line> lines(Mob mob, CarryInfoConfig config, long gameTime)
        {
                List<Line> lines = new ArrayList<>();
                lines.add(new Line(title(mob.getDisplayName(), mob.getHealth(), mob.getMaxHealth(), config), TEXT));
                double mass = CarryRules.mass(mob);
                if (config.weight())
                {
                        lines.add(new Line(Component.translatable("throwablemobs.hud.weight", kilograms(mass)), TEXT));
                }
                if (config.traits())
                {
                        List<Component> traits = traits(MobProfiles.physics(mob), ThrowableMobsConfig.throwing(),
                                mass / ThrowableMobsConfig.carry().maxMass());
                        if (!traits.isEmpty())
                        {
                                lines.add(new Line(join(traits), MUTED));
                        }
                }
                RadarStatusPayload status = RadarStatus.of(mob.getId(), gameTime);
                if (config.radar() && status != null)
                {
                        Entity target = status.targetId() < 0 ? null : mob.level().getEntity(status.targetId());
                        lines.add(radarLine(status.targetId() < 0 ? null
                                : target != null ? target.getDisplayName()
                                        : Component.translatable("throwablemobs.hud.radar.unknown"),
                                status.distance(), status.range(), RadarStatus.sincePing(gameTime)));
                }
                return lines;
        }

        /** What the config screen's preview shows: a carried chicken with a zombie 8 blocks away. */
        static List<Line> sampleLines(CarryInfoConfig config)
        {
                List<Line> lines = new ArrayList<>();
                lines.add(new Line(title(Component.translatable("entity.minecraft.chicken"), 4.0F, 4.0F, config), TEXT));
                if (config.weight())
                {
                        lines.add(new Line(Component.translatable("throwablemobs.hud.weight", kilograms(45.0)), TEXT));
                }
                if (config.traits())
                {
                        lines.add(new Line(Component.translatable("throwablemobs.hud.trait.floaty"), MUTED));
                }
                if (config.radar())
                {
                        long time = System.currentTimeMillis() / 50;
                        lines.add(radarLine(Component.translatable("entity.minecraft.zombie"), 8.0F, 16.0F, time % 24));
                }
                return lines;
        }

        /**
         * Draws {@code lines} in the frame {@code (frameX, frameY, frameWidth, frameHeight)}: the whole screen in game,
         * the preview box in the config screen. Offsets are multiplied by {@code offsetScale}, text keeps its size.
         */
        static void draw(GuiGraphicsExtractor graphics, int frameX, int frameY, int frameWidth, int frameHeight,
                float offsetScale, CarryInfoConfig config, List<Line> lines)
        {
                Font font = Minecraft.getInstance().font;
                int width = 0;
                for (Line line : lines)
                {
                        width = Math.max(width, font.width(line.text()));
                }
                width += PADDING * 2;
                int height = PADDING * 2 + lines.size() * font.lineHeight + (lines.size() - 1) * LINE_GAP;
                int offsetX = Math.round(config.offsetX() * offsetScale);
                int offsetY = Math.round(config.offsetY() * offsetScale);
                int x = switch (config.anchor())
                {
                        case TOP_LEFT, BOTTOM_LEFT -> frameX + offsetX;
                        case TOP_RIGHT, BOTTOM_RIGHT -> frameX + frameWidth - width - offsetX;
                        case CROSSHAIR -> frameX + frameWidth / 2 - width / 2 + offsetX;
                };
                int y = switch (config.anchor())
                {
                        case TOP_LEFT, TOP_RIGHT -> frameY + offsetY;
                        case BOTTOM_LEFT, BOTTOM_RIGHT -> frameY + frameHeight - height - offsetY;
                        case CROSSHAIR -> frameY + frameHeight / 2 + offsetY;
                };
                graphics.fill(x, y, x + width, y + height, BACKGROUND);
                int lineY = y + PADDING;
                for (Line line : lines)
                {
                        graphics.text(font, line.text(), x + PADDING, lineY, line.color());
                        lineY += font.lineHeight + LINE_GAP;
                }
        }

        private static Component title(Component name, float health, float maxHealth, CarryInfoConfig config)
        {
                MutableComponent title = name.copy();
                if (config.health())
                {
                        title.append("  ").append(Component.translatable("throwablemobs.hud.health", Mth.ceil(health),
                                Mth.ceil(maxHealth)).withStyle(ChatFormatting.RED));
                }
                return title;
        }

        /** {@code heaviness}: mass as a share of {@code max_mass}. */
        private static List<Component> traits(ProfilePhysics physics, ThrowConfig config, double heaviness)
        {
                List<Component> traits = new ArrayList<>();
                if (physics.gravityMultiplier().orElse(1.0) < FLOATY)
                {
                        traits.add(Component.translatable("throwablemobs.hud.trait.floaty"));
                }
                if (physics.blockRestitution(config) > 0.0 && physics.maxBounces(config) > 0)
                {
                        traits.add(Component.translatable("throwablemobs.hud.trait.bouncy"));
                }
                if (heaviness >= HEAVY)
                {
                        traits.add(Component.translatable("throwablemobs.hud.trait.heavy"));
                }
                return traits;
        }

        /** {@code target}: the nearest monster's name, {@code null} if there's none in range. */
        private static Line radarLine(@Nullable Component target, float distance, float range, long sincePing)
        {
                if (target == null)
                {
                        return new Line(Component.translatable("throwablemobs.hud.radar.none"), MUTED);
                }
                int color = sincePing < PING_FLASH_TICKS ? RADAR_PING
                        : lerpColor(Mth.clamp(distance / range, 0.0F, 1.0F), RADAR_NEAR, RADAR_FAR);
                return new Line(Component.translatable("throwablemobs.hud.radar", target, Math.round(distance)), color);
        }

        private static Component join(List<Component> parts)
        {
                MutableComponent joined = Component.empty();
                for (int i = 0; i < parts.size(); i++)
                {
                        if (i > 0)
                        {
                                joined.append(", ");
                        }
                        joined.append(parts.get(i));
                }
                return joined;
        }

        private static String kilograms(double mass)
        {
                return mass < 10.0 ? String.format(Locale.ROOT, "%.1f", mass) : String.valueOf(Math.round(mass));
        }

        /** {@code t} 0 = {@code from}, 1 = {@code to}, per channel. */
        private static int lerpColor(float t, int from, int to)
        {
                int color = 0;
                for (int shift = 0; shift < 32; shift += 8)
                {
                        int a = from >>> shift & 0xFF;
                        int b = to >>> shift & 0xFF;
                        color |= Math.round(a + (b - a) * t) << shift;
                }
                return color;
        }
}
