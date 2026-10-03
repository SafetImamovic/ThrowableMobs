package io.github.safetimamovic.throwablemobs.client;

import io.github.safetimamovic.throwablemobs.carry.CarryRules;
import io.github.safetimamovic.throwablemobs.carry.CarryState;
import io.github.safetimamovic.throwablemobs.carry.DropTarget;
import io.github.safetimamovic.throwablemobs.carry.LocalCarry;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarriedFacing;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarryConfig;
import io.github.safetimamovic.throwablemobs.network.DropCarriedPayload;
import io.github.safetimamovic.throwablemobs.network.PickUpPayload;
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
 * Client side of carrying: turns the key mappings into pick-up / put-down requests, locks carried mobs to their
 * carriers, and draws the prompt under the crosshair.
 */
public final class CarryClient
{
        private static final int PROMPT_COLOR = 0xFFFFFFFF;
        private static final int REASON_COLOR = 0xFFAAAAAA;

        private static Consumer<CustomPacketPayload> sendToServer = payload ->
        {
        };
        /** Our key that swallowed a use press; vanilla's use stays released until this key is let go. */
        private static @Nullable KeyMapping suppressingUse;

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
        }

        /**
         * Start of each client tick, before vanilla handles its key mappings: turns presses of our keys into requests.
         * The defaults share keys with vanilla (Left Shift with Sneak, Right Button with Use), so when one of our keys
         * acts, vanilla's use is swallowed for that press: no trading with the villager you just picked up. Pick up and
         * put down may share a key; only one applies at a time, so it toggles.
         */
        public static void tickInput(Minecraft minecraft)
        {
                keepUseSuppressed(minecraft);
                boolean pickUpPressed = consumeAll(CarryKeys.PICK_UP);
                boolean putDownPressed = consumeAll(CarryKeys.PUT_DOWN);
                LocalPlayer player = minecraft.player;
                if (player == null || !CarryKeys.modifierHeld())
                {
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

        /** HUD layer under the crosshair: "[Shift] + [Right Button] to pick up", "Can't pick this one up", or drop. */
        public static void renderPrompt(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker)
        {
                Minecraft minecraft = Minecraft.getInstance();
                LocalPlayer player = minecraft.player;
                if (!ThrowableMobsConfig.CARRY.showPrompt() || player == null || player.isSpectator())
                {
                        return;
                }

                Component prompt;
                Component reason = null;
                if (LocalCarry.get() instanceof Mob carried)
                {
                        if (ThrowableMobsConfig.CARRY.dropNeedsGround() && DropTarget.find(player, carried) == null)
                        {
                                prompt = Component.translatable("throwablemobs.carry.need_ground");
                        }
                        else
                        {
                                Component keys = inputKeys(CarryKeys.PUT_DOWN);
                                if (keys == null)
                                {
                                        return;
                                }
                                prompt = Component.translatable("throwablemobs.prompt.drop", keys);
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

                CarryConfig config = ThrowableMobsConfig.CARRY;
                int anchorY = switch (config.promptAnchor())
                {
                        case CROSSHAIR -> graphics.guiHeight() / 2;
                        case TOP -> 0;
                        case BOTTOM -> graphics.guiHeight();
                };
                int x = graphics.guiWidth() / 2 + config.promptOffsetX();
                int y = anchorY + config.promptOffsetY();
                graphics.centeredText(minecraft.font, prompt, x, y, PROMPT_COLOR);
                if (reason != null)
                {
                        graphics.centeredText(minecraft.font, reason, x, y + minecraft.font.lineHeight + 1,
                                REASON_COLOR);
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
                if (ThrowableMobsConfig.CARRY.carriedFacing() == CarriedFacing.CARRIER
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
