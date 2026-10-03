package io.github.safetimamovic.throwablemobs.client;

import io.github.safetimamovic.throwablemobs.carry.CarryRules;
import io.github.safetimamovic.throwablemobs.carry.CarryState;
import io.github.safetimamovic.throwablemobs.carry.DropTarget;
import io.github.safetimamovic.throwablemobs.carry.LocalCarry;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarriedFacing;
import io.github.safetimamovic.throwablemobs.network.DropCarriedPayload;
import io.github.safetimamovic.throwablemobs.platform.Services;
import java.util.function.Consumer;
import net.minecraft.client.DeltaTracker;
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

/**
 * Client side of carrying: locks carried mobs to their carriers, turns sneak + use on air into a drop request, and
 * draws the pick-up / drop prompt under the crosshair.
 */
public final class CarryClient
{
        private static final int PROMPT_COLOR = 0xFFFFFFFF;
        private static final int REASON_COLOR = 0xFFAAAAAA;

        private static Consumer<CustomPacketPayload> sendToServer = payload ->
        {
        };
        private static boolean useWasDown;

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

                // Right-clicking air with an empty hand sends nothing in vanilla, so ask the server to drop.
                // Clicks on an entity already reach the server as an interaction (CarryManager.onUseEntity).
                boolean useDown = minecraft.options.keyUse.isDown();
                if (useDown && !useWasDown && localCarried != null && CarryRules.isCarryInput(player)
                        && !(minecraft.hitResult instanceof EntityHitResult))
                {
                        sendToServer.accept(DropCarriedPayload.INSTANCE);
                }
                useWasDown = useDown;
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
                                prompt = Component.translatable("throwablemobs.prompt.drop", inputKeys(minecraft));
                        }
                }
                else if (minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Mob mob)
                {
                        if (ThrowableMobsConfig.CARRY.requireEmptyHand() && !player.getMainHandItem().isEmpty())
                        {
                                return;
                        }
                        CarryRules.Denial denial = CarryRules.check(player, mob);
                        if (denial == null)
                        {
                                prompt = Component.translatable("throwablemobs.prompt.pick_up", inputKeys(minecraft));
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

                int x = graphics.guiWidth() / 2;
                int y = graphics.guiHeight() / 2 + 12;
                graphics.centeredText(minecraft.font, prompt, x, y, PROMPT_COLOR);
                if (reason != null)
                {
                        graphics.centeredText(minecraft.font, reason, x, y + minecraft.font.lineHeight + 1,
                                REASON_COLOR);
                }
        }

        /** Current key names, so the text follows rebinding. */
        private static Component inputKeys(Minecraft minecraft)
        {
                Component use = minecraft.options.keyUse.getTranslatedKeyMessage();
                if (!ThrowableMobsConfig.CARRY.requireSneak())
                {
                        return Component.translatable("throwablemobs.prompt.key", use);
                }
                return Component.translatable("throwablemobs.prompt.keys",
                        minecraft.options.keyShift.getTranslatedKeyMessage(), use);
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
