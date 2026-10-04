package io.github.safetimamovic.throwablemobs.client;

import io.github.safetimamovic.throwablemobs.carry.CarryRules;
import io.github.safetimamovic.throwablemobs.carry.CarryState;
import io.github.safetimamovic.throwablemobs.carry.DropTarget;
import io.github.safetimamovic.throwablemobs.carry.LocalCarry;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarriedFacing;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.PromptConfig;
import io.github.safetimamovic.throwablemobs.network.DropCarriedPayload;
import io.github.safetimamovic.throwablemobs.network.ChargeThrowPayload;
import io.github.safetimamovic.throwablemobs.network.PickUpPayload;
import io.github.safetimamovic.throwablemobs.network.ThrowPayload;
import io.github.safetimamovic.throwablemobs.physics.ThrowPhysics;
import io.github.safetimamovic.throwablemobs.platform.Services;
import java.util.function.Consumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Client side of carrying and throwing: turns the key mappings into pick-up / put-down / throw requests, locks carried
 * mobs to their carriers, and draws the prompt and charge bar under the crosshair.
 */
public final class CarryClient
{
        private static final int PROMPT_COLOR = 0xFFFFFFFF;
        private static final int REASON_COLOR = 0xFFAAAAAA;
        private static final int BAR_WIDTH = 60;
        private static final int BAR_HEIGHT = 3;
        private static final int BAR_BACKGROUND = 0x80000000;
        private static final int BAR_FILL = 0xFFFFFFFF;

        private static Consumer<CustomPacketPayload> sendToServer = payload ->
        {
        };
        /** Our key that swallowed a use press; vanilla's use stays released until this key is let go. */
        private static @Nullable KeyMapping suppressingUse;
        /** Ticks the Throw key has been held while carrying, for the charge bar; -1 when not charging. */
        private static int chargeTicks = -1;

        private CarryClient()
        {
        }

        /** Called by each loader's client entrypoint with its way of sending a payload to the server. */
        public static void init(Consumer<CustomPacketPayload> sender)
        {
                sendToServer = sender;
        }

        public static void tick(Minecraft minecraft)
        {
                ClientLevel level = minecraft.level;
                LocalPlayer player = minecraft.player;
                if (level == null || player == null)
                {
                        LocalCarry.set(null);
                        return;
                }

                Entity localCarried = null;
                for (Entity entity : level.entitiesForRendering())
                {
                        CarryState state = Services.CARRY_STATE.get(entity);
                        if (state == null)
                        {
                                continue;
                        }
                        Player carrier = level.getPlayerByUUID(state.carrier());
                        if (carrier != null)
                        {
                                lockToCarrier(entity, carrier);
                        }
                        if (state.carrier().equals(player.getUUID()))
                        {
                                localCarried = entity;
                        }
                }
                LocalCarry.set(localCarried);
                if (chargeTicks >= 0 && localCarried instanceof Mob mob && ThrowableMobsConfig.prompt().showTrajectory()
                        && ThrowableMobsConfig.throwing().allowTrajectory())
                {
                        TrajectoryPreview.draw(level, player, mob, chargeTicks);
                }
        }

        /**
         * Start of each client tick, before vanilla handles its key mappings: turns presses of our keys into requests.
         * The defaults share keys with vanilla (Left Shift with Sneak, Right Button with Use), so when one of our keys
         * acts, vanilla's use is swallowed for that press: no trading with the villager you just picked up. Pick up and
         * put down may share a key; only one applies at a time, so it toggles. Throw is the same key without the
         * modifier; with the modifier unbound, put down wins, so Throw needs a key of its own then.
         */
        public static void tickInput(Minecraft minecraft)
        {
                keepUseSuppressed(minecraft);
                boolean pickUpPressed = consumeAll(CarryKeys.PICK_UP);
                boolean putDownPressed = consumeAll(CarryKeys.PUT_DOWN);
                boolean throwPressed = consumeAll(CarryKeys.THROW);
                LocalPlayer player = minecraft.player;
                if (player == null || LocalCarry.get() == null)
                {
                        // The carry ended mid-charge (put down, dropped by the server); it forgets the charge too.
                        chargeTicks = -1;
                }
                if (player == null)
                {
                        return;
                }
                if (chargeTicks >= 0)
                {
                        if (CarryKeys.THROW.isDown())
                        {
                                chargeTicks++;
                        }
                        else
                        {
                                sendToServer.accept(ThrowPayload.INSTANCE);
                                chargeTicks = -1;
                        }
                        return;
                }
                if (!CarryKeys.modifierHeld())
                {
                        if (throwPressed && LocalCarry.get() != null)
                        {
                                sendToServer.accept(ChargeThrowPayload.INSTANCE);
                                suppressUse(minecraft, CarryKeys.THROW);
                                chargeTicks = 0;
                        }
                        return;
                }
                if (LocalCarry.get() != null)
                {
                        if (putDownPressed)
                        {
                                sendToServer.accept(DropCarriedPayload.INSTANCE);
                                suppressUse(minecraft, CarryKeys.PUT_DOWN);
                        }
                }
                else if (pickUpPressed && CarryRules.canUseHands(player)
                        && minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Mob mob
                        && CarryRules.check(player, mob) == null)
                {
                        sendToServer.accept(new PickUpPayload(mob.getId()));
                        suppressUse(minecraft, CarryKeys.PICK_UP);
                }
        }

        private static void suppressUse(Minecraft minecraft, KeyMapping ours)
        {
                KeyMapping use = minecraft.options.keyUse;
                if (ours.same(use))
                {
                        consumeAll(use);
                        use.setDown(false);
                        suppressingUse = ours;
                }
        }

        /** Vanilla repeats use every few ticks while the key is held, so keep it released until ours is let go. */
        private static void keepUseSuppressed(Minecraft minecraft)
        {
                if (suppressingUse == null)
                {
                        return;
                }
                if (suppressingUse.isDown())
                {
                        minecraft.options.keyUse.setDown(false);
                }
                else
                {
                        suppressingUse = null;
                }
        }

        private static boolean consumeAll(KeyMapping key)
        {
                boolean pressed = false;
                while (key.consumeClick())
                {
                        pressed = true;
                }
                return pressed;
        }

        /**
         * HUD layer under the crosshair: "[Shift] + [Right Button] to pick up", "Can't pick this one up", or, while
         * carrying, put down and throw, plus the charge bar while charging.
         */
        public static void renderPrompt(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker)
        {
                Minecraft minecraft = Minecraft.getInstance();
                LocalPlayer player = minecraft.player;
                if (!ThrowableMobsConfig.prompt().showPrompt() || player == null || player.isSpectator())
                {
                        return;
                }

                Component prompt;
                Component reason = null;
                boolean charging = false;
                if (LocalCarry.get() instanceof Mob carried)
                {
                        if (!CarryKeys.THROW.isUnbound())
                        {
                                reason = Component.translatable("throwablemobs.prompt.throw",
                                        Component.translatable("throwablemobs.prompt.key",
                                                CarryKeys.THROW.getTranslatedKeyMessage()));
                        }
                        charging = chargeTicks >= 0 && ThrowableMobsConfig.prompt().showChargeBar();
                        if (ThrowableMobsConfig.carry().putDownNeedsGround() && DropTarget.find(player, carried) == null)
                        {
                                prompt = Component.translatable("throwablemobs.carry.need_ground");
                        }
                        else
                        {
                                Component keys = inputKeys(CarryKeys.PUT_DOWN);
                                prompt = keys != null ? Component.translatable("throwablemobs.prompt.drop", keys)
                                        : Component.empty();
                        }
                }
                else if (minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Mob mob)
                {
                        Component keys = inputKeys(CarryKeys.PICK_UP);
                        if (!CarryRules.canUseHands(player) || keys == null)
                        {
                                return;
                        }
                        CarryRules.Denial denial = CarryRules.check(player, mob);
                        if (denial == null)
                        {
                                prompt = Component.translatable("throwablemobs.prompt.pick_up", keys);
                        }
                        else
                        {
                                prompt = Component.translatable("throwablemobs.prompt.cannot");
                                reason = denial.message();
                        }
                }
                else
                {
                        return;
                }

                PromptConfig config = ThrowableMobsConfig.prompt();
                int anchorY = switch (config.anchor())
                {
                        case CROSSHAIR -> graphics.guiHeight() / 2;
                        case TOP -> 0;
                        case BOTTOM -> graphics.guiHeight();
                };
                int x = graphics.guiWidth() / 2 + config.offsetX();
                int y = anchorY + config.offsetY();
                graphics.centeredText(minecraft.font, prompt, x, y, PROMPT_COLOR);
                int line = minecraft.font.lineHeight + 1;
                if (reason != null)
                {
                        graphics.centeredText(minecraft.font, reason, x, y + line, REASON_COLOR);
                }
                if (charging)
                {
                        int max = ThrowableMobsConfig.throwing().maxChargeTicks();
                        double charge = ThrowPhysics.chargeFraction(Math.min(chargeTicks, max), max);
                        int left = x - BAR_WIDTH / 2;
                        int top = y + 2 * line + 1;
                        graphics.fill(left, top, left + BAR_WIDTH, top + BAR_HEIGHT, BAR_BACKGROUND);
                        graphics.fill(left, top, left + (int) Math.round(BAR_WIDTH * charge), top + BAR_HEIGHT, BAR_FILL);
                }
        }

        /** Current key names, so the text follows rebinding; {@code null} if the action's key is unbound. */
        private static @Nullable Component inputKeys(KeyMapping action)
        {
                if (action.isUnbound())
                {
                        return null;
                }
                if (CarryKeys.MODIFIER.isUnbound())
                {
                        return Component.translatable("throwablemobs.prompt.key", action.getTranslatedKeyMessage());
                }
                return Component.translatable("throwablemobs.prompt.keys",
                        CarryKeys.MODIFIER.getTranslatedKeyMessage(), action.getTranslatedKeyMessage());
        }

        /**
         * Server position updates arrive late and interpolated, so snap the mob onto its carrier each tick: current
         * position from the carrier's current one, previous from the carrier's previous one. Render interpolation then
         * moves both in lockstep.
         */
        private static void lockToCarrier(Entity entity, Player carrier)
        {
                double height = carrier.getBbHeight();
                entity.getInterpolation().cancel();
                entity.setPos(carrier.getX(), carrier.getY() + height, carrier.getZ());
                entity.xo = carrier.xo;
                entity.yo = carrier.yo + height;
                entity.zo = carrier.zo;
                entity.xOld = entity.xo;
                entity.yOld = entity.yo;
                entity.zOld = entity.zo;
                entity.setDeltaMovement(Vec3.ZERO);
                if (ThrowableMobsConfig.carry().carriedFacing() == CarriedFacing.CARRIER
                        && entity instanceof LivingEntity living)
                {
                        // Previous rotations from the carrier's too, so turning interpolates like the position.
                        living.setYRot(carrier.getYRot());
                        living.yRotO = carrier.yRotO;
                        living.yHeadRot = carrier.getYRot();
                        living.yHeadRotO = carrier.yRotO;
                        living.yBodyRot = carrier.getYRot();
                        living.yBodyRotO = carrier.yRotO;
                }
        }
}
