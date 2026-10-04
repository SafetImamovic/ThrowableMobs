package io.github.safetimamovic.throwablemobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.FlightPoseStyle;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.HudConfig;
import io.github.safetimamovic.throwablemobs.platform.Services;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * How a thrown mob looks in the air, purely visual: it faces where it flies; with TILT its model tilts with the arc,
 * with HEAD_FIRST it lies along the path head first, like an arrow; both can spin around the flight direction like a
 * thrown football. The hitbox stays upright. The client knows a mob is flying from its synced {@code ThrowState}, and
 * the direction from its movement since the last tick.
 */
public final class FlightPose
{
        /** Slower than this (blocks/tick squared) and the direction is noise; keep the normal pose. */
        private static final double MIN_MOTION_SQR = 1.0E-6;

        private FlightPose()
        {
        }

        /** End of {@code LivingEntityRenderer#extractRenderState}. */
        public static void extract(LivingEntity entity, LivingEntityRenderState state)
        {
                FlightPoseState pose = (FlightPoseState) state;
                pose.throwablemobs$setFlightPose(null, 0.0F, 0.0F);
                HudConfig config = ThrowableMobsConfig.hud();
                FlightPoseStyle style = config.flightPose();
                if (style == FlightPoseStyle.NONE || Services.THROW_STATE.get(entity) == null)
                {
                        return;
                }
                Vec3 motion = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
                if (motion.lengthSqr() < MIN_MOTION_SQR)
                {
                        return;
                }
                state.bodyRot = (float) (Mth.atan2(motion.z, motion.x) * Mth.RAD_TO_DEG) - 90.0F;
                state.yRot = 0.0F;
                state.xRot = 0.0F;
                if (style == FlightPoseStyle.FACE_DIRECTION)
                {
                        return;
                }
                float pitch = (float) (Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG);
                pose.throwablemobs$setFlightPose(style, pitch, state.ageInTicks * config.spiralSpeed());
        }

        /**
         * End of {@code LivingEntityRenderer#setupRotations}: the model already faces {@code bodyRot}; tilt and spin it
         * around the middle of its hitbox. Positive X rotation tips the head backward here (vanilla flips the model
         * afterwards), so negative tips it forward. TILT points the facing direction along the path, so the spin axis is
         * the model's forward (Z); HEAD_FIRST tips the head forward until the up axis is along the path, so it spins
         * around Y. The pose stack is already scaled, so the pivot is in unscaled units.
         */
        public static void apply(LivingEntityRenderState state, PoseStack poseStack)
        {
                FlightPoseState pose = (FlightPoseState) state;
                FlightPoseStyle style = pose.throwablemobs$flightPose();
                if (style == null)
                {
                        return;
                }
                float pitch = pose.throwablemobs$flightPitch();
                float pivot = state.boundingBoxHeight / state.scale / 2.0F;
                poseStack.translate(0.0F, pivot, 0.0F);
                if (style == FlightPoseStyle.HEAD_FIRST)
                {
                        poseStack.rotateDegrees(Axis.XP, pitch - 90.0F);
                        poseStack.rotateDegrees(Axis.YP, pose.throwablemobs$flightSpin());
                }
                else
                {
                        poseStack.rotateDegrees(Axis.XP, pitch);
                        poseStack.rotateDegrees(Axis.ZP, pose.throwablemobs$flightSpin());
                }
                poseStack.translate(0.0F, -pivot, 0.0F);
        }
}
