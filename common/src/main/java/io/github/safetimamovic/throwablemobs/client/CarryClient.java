package io.github.safetimamovic.throwablemobs.client;

import io.github.safetimamovic.throwablemobs.carry.CarryRules;
import io.github.safetimamovic.throwablemobs.carry.CarryState;
import io.github.safetimamovic.throwablemobs.carry.DropTarget;
import io.github.safetimamovic.throwablemobs.carry.LocalCarry;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarriedFacing;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.HudAnchor;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.HudConfig;
import io.github.safetimamovic.throwablemobs.network.DropCarriedPayload;
import io.github.safetimamovic.throwablemobs.network.ChargeThrowPayload;
import io.github.safetimamovic.throwablemobs.network.PickUpPayload;
import io.github.safetimamovic.throwablemobs.network.PlayerEnabledPayload;
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
        private static final int BAR_BACKGROUND = 0x80000000;
        private static final int BAR_FILL = 0xFFFFFFFF;

        private static Consumer<CustomPacketPayload> sendToServer = payload ->
        {
        };
        /** Our key that swallowed a use press; vanilla's use stays released until this key is let go. */
        private static @Nullable KeyMapping suppressingUse;
        /** Same for attack, swallowed by Cancel throw so cancelling doesn't punch. */
        private static boolean suppressingAttack;
        /** Ticks the Throw key has been held while carrying, for the charge bar; -1 when not charging. */
        private static int chargeTicks = -1;
        /** Another mod's key on our pick-up key, checked each tick; while set, picking up is left to that mod. */
        private static @Nullable KeyMapping conflict;
        /** The chat notice about {@link #conflict} went out; once per world joined. */
        private static boolean conflictNoticed;
        /** The player's {@code enabled} as the server last heard it; servers start from enabled. */
        private static boolean sentEnabled = true;

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
                        conflict = null;
                        conflictNoticed = false;
                        sentEnabled = true;
                        return;
                }
                boolean enabled = ThrowableMobsConfig.playerEnabled();
                if (enabled != sentEnabled)
                {
                        // The server assumes enabled, so only a change from that needs telling.
                        sendToServer.accept(new PlayerEnabledPayload(enabled));
                        sentEnabled = enabled;
                }
                conflict = CarryKeys.foreignConflict(minecraft.options);
                if (conflict != null && !conflictNoticed)
                {
                        noticeConflict(minecraft, conflict);
                        conflictNoticed = true;
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
                CatchHighlight.tick(level, player);
                FlightTrail.tick(level);
                //? if <26.1
                /*ShapeCanvas.INSTANCE.clear();*/
                if (chargeTicks >= 0 && localCarried instanceof Mob mob && ThrowableMobsConfig.hud().showTrajectory()
                        && ThrowableMobsConfig.throwing().allowTrajectory())
                {
                        //? if >=26.1 {
                        TrajectoryPreview.draw(level, player, mob, chargeTicks, GizmoCanvas.INSTANCE);
                        //?} else
                        /*TrajectoryPreview.draw(level, player, mob, chargeTicks, ShapeCanvas.INSTANCE);*/
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
                keepAttackSuppressed(minecraft);
                boolean pickUpPressed = consumeAll(CarryKeys.PICK_UP);
                boolean putDownPressed = consumeAll(CarryKeys.PUT_DOWN);
                boolean throwPressed = consumeAll(CarryKeys.THROW);
                boolean cancelPressed = consumeAll(CarryKeys.CANCEL_THROW);
                if (!active())
                {
                        // Presses are still consumed, so none are left over for when it's turned back on.
                        chargeTicks = -1;
                        return;
                }
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
                        if (!CarryRules.canThrow(player))
                        {
                                // Switched to a full slot with require_empty_hand: the server refuses the throw too.
                                chargeTicks = -1;
                        }
                        else if (cancelPressed)
                        {
                                // No ThrowPayload: the server only throws on one, and the next charge replaces its
                                // record of this one.
                                chargeTicks = -1;
                                suppressAttack(minecraft);
                        }
                        else if (CarryKeys.THROW.isDown())
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
                        if (throwPressed && LocalCarry.get() != null && CarryRules.canThrow(player))
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
                else if (pickUpPressed && conflict == null && CarryRules.canUseHands(player)
                        && minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Mob mob
                        && CarryRules.check(player, mob) == null)
                {
                        sendToServer.accept(new PickUpPayload(mob.getId()));
                        suppressUse(minecraft, CarryKeys.PICK_UP);
                }
        }

        /** "[Left Shift] is also Carry On's Carry key, so picking up is left to it. Rebind with /throwablemobs keybinds." */
        private static void noticeConflict(Minecraft minecraft, KeyMapping other)
        {
                //? if >=26.1 {
                minecraft.gui.hud.getChat().addClientSystemMessage(Component.translatable("throwablemobs.conflict",
                        Component.translatable("throwablemobs.prompt.key", other.getTranslatedKeyMessage()),
                        Component.translatable(other.getName()), other.getCategory().label(),
                        ThrowableMobsClientCommand.link("keybinds")));
                //?} else {
                /*minecraft.gui.getChat().addMessage(Component.translatable("throwablemobs.conflict",
                        Component.translatable("throwablemobs.prompt.key", other.getTranslatedKeyMessage()),
                        Component.translatable(other.getName()), Component.translatable(other.getCategory()),
                        ThrowableMobsClientCommand.link("keybinds")));
                *///?}
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

        private static void suppressAttack(Minecraft minecraft)
        {
                KeyMapping attack = minecraft.options.keyAttack;
                if (CarryKeys.CANCEL_THROW.same(attack))
                {
                        consumeAll(attack);
                        attack.setDown(false);
                        suppressingAttack = true;
                }
        }

        /** Holding attack keeps mining, so keep it released until Cancel throw is let go. */
        private static void keepAttackSuppressed(Minecraft minecraft)
        {
                if (!suppressingAttack)
                {
                        return;
                }
                if (CarryKeys.CANCEL_THROW.isDown())
                {
                        minecraft.options.keyAttack.setDown(false);
                }
                else
                {
                        suppressingAttack = false;
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
                HudConfig config = ThrowableMobsConfig.hud();
                if (player == null || player.isSpectator() || !active())
                {
                        return;
                }
                if (chargeTicks >= 0 && config.showChargeBar() && LocalCarry.get() != null)
                {
                        int max = ThrowableMobsConfig.throwing().maxChargeTicks();
                        drawChargeBar(graphics, graphics.guiWidth(), graphics.guiHeight(), config,
                                ThrowPhysics.chargeFraction(Math.min(chargeTicks, max), max));
                }
                if (!config.showPrompt())
                {
                        return;
                }

                Component prompt;
                Component reason = null;
                if (LocalCarry.get() instanceof Mob carried)
                {
                        KeyMapping hint = chargeTicks >= 0 ? CarryKeys.CANCEL_THROW : CarryKeys.THROW;
                        if (!hint.isUnbound())
                        {
                                reason = Component.translatable(chargeTicks >= 0 ? "throwablemobs.prompt.cancel_throw"
                                        : "throwablemobs.prompt.throw", Component.translatable(
                                                "throwablemobs.prompt.key", hint.getTranslatedKeyMessage()));
                        }
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
                        if (!CarryRules.canUseHands(player) || keys == null || conflict != null)
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

                drawPrompt(graphics, promptX(graphics.guiWidth(), config), promptY(graphics.guiHeight(), config),
                        prompt, reason);
        }

        /** The server's and this player's {@code enabled}: off, the keys are vanilla's again and nothing is drawn. */
        static boolean active()
        {
                return ThrowableMobsConfig.enabled() && ThrowableMobsConfig.playerEnabled();
        }

        /** The pick-up prompt, for the config screen's preview; empty if Pick up is unbound. */
        static Component samplePrompt()
        {
                Component keys = inputKeys(CarryKeys.PICK_UP);
                return keys != null ? Component.translatable("throwablemobs.prompt.pick_up", keys) : Component.empty();
        }

        /** Centre of the prompt's first line on a screen {@code guiWidth} wide. */
        static int promptX(int guiWidth, HudConfig config)
        {
                return guiWidth / 2 + config.promptOffsetX();
        }

        static int promptY(int guiHeight, HudConfig config)
        {
                return anchorY(guiHeight, config.promptAnchor()) + config.promptOffsetY();
        }

        static void drawPrompt(GuiGraphicsExtractor graphics, int x, int y, Component prompt,
                @Nullable Component reason)
        {
                Minecraft minecraft = Minecraft.getInstance();
                graphics.centeredText(minecraft.font, prompt, x, y, PROMPT_COLOR);
                if (reason != null)
                {
                        graphics.centeredText(minecraft.font, reason, x, y + minecraft.font.lineHeight + 1,
                                REASON_COLOR);
                }
        }

        /**
         * On a screen of the given size. {@code charge} from 0 to 1: fills like a bow's pull, same ease curve as the
         * charge the server measures.
         */
        static void drawChargeBar(GuiGraphicsExtractor graphics, int guiWidth, int guiHeight, HudConfig config,
                double charge)
        {
                int width = config.chargeBarWidth();
                int left = guiWidth / 2 + config.chargeBarOffsetX() - width / 2;
                int top = anchorY(guiHeight, config.chargeBarAnchor()) + config.chargeBarOffsetY();
                int bottom = top + config.chargeBarHeight();
                graphics.fill(left, top, left + width, bottom, BAR_BACKGROUND);
                graphics.fill(left, top, left + (int) Math.round(width * charge), bottom, BAR_FILL);
        }

        private static int anchorY(int guiHeight, HudAnchor anchor)
        {
                return switch (anchor)
                {
                        case CROSSHAIR -> guiHeight / 2;
                        case TOP -> 0;
                        case BOTTOM -> guiHeight;
                };
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
                //? if >=26.1 {
                entity.getInterpolation().cancel();
                //?} else {
                /*// Zero steps: drops the interpolation still running towards an older server position.
                entity.lerpTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot(), 0);
                *///?}
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
