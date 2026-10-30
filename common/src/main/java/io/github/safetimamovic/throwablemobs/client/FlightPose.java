package io.github.safetimamovic.throwablemobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.FlightPoseStyle;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.HudConfig;
import io.github.safetimamovic.throwablemobs.platform.Services;
//? if >=1.21.2
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * How a thrown mob looks in the air, purely visual: it faces where it flies; with TILT its model tilts with the arc,
 * with HEAD_FIRST it lies along the path head first, like an arrow; both can spin around the flight direction like a
 * thrown football. The hitbox stays upright. The client knows a mob is flying from its synced {@code ThrowState}, and
 * the direction from its movement since the last tick. The renderer mixin applies it: through the render state where
 * there is one (1.21.2 and later), straight from the entity before.
 */
public final class FlightPose
{
        /** Slower than this (blocks/tick squared) and the direction is noise; keep the normal pose. */
        private static final double MIN_MOTION_SQR = 1.0E-6;

        private FlightPose()
        {
        }

        /**
         * A flying mob's pose this frame: body facing the flight direction, head straight ahead.
         *
         * @param tilt  {@link FlightPoseStyle#TILT} or {@link FlightPoseStyle#HEAD_FIRST}; {@code null} for none
         * @param pitch pitch of the flight direction in degrees, positive = upward
         * @param spin  degrees around the flight direction
         */
        public record Angles(float bodyRot, @Nullable FlightPoseStyle tilt, float pitch, float spin)
        {
        }

        /** {@code null} if the mob keeps its normal pose: not thrown, flight pose off, or barely moving. */
        public static @Nullable Angles of(LivingEntity entity, float ageInTicks)
        {
                HudConfig config = ThrowableMobsConfig.hud();
                FlightPoseStyle style = config.flightPose();
                if (style == FlightPoseStyle.NONE || Services.THROW_STATE.get(entity) == null)
                {
                        return null;
                }
                Vec3 motion = new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
                if (motion.lengthSqr() < MIN_MOTION_SQR)
                {
                        return null;
                }
                float bodyRot = (float) (Mth.atan2(motion.z, motion.x) * Mth.RAD_TO_DEG) - 90.0F;
                if (style == FlightPoseStyle.FACE_DIRECTION)
                {
                        return new Angles(bodyRot, null, 0.0F, 0.0F);
                }
                float pitch = (float) (Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG);
                return new Angles(bodyRot, style, pitch, ageInTicks * config.spiralSpeed());
        }

        //? if >=1.21.2 {
        /** End of {@code LivingEntityRenderer#extractRenderState}. */
        public static void extract(LivingEntity entity, LivingEntityRenderState state)
        {
                FlightPoseState pose = (FlightPoseState) state;
                Angles angles = of(entity, state.ageInTicks);
                if (angles == null)
                {
                        pose.throwablemobs$setFlightPose(null, 0.0F, 0.0F);
                        return;
                }
                state.bodyRot = angles.bodyRot();
                state.yRot = 0.0F;
                state.xRot = 0.0F;
                pose.throwablemobs$setFlightPose(angles.tilt(), angles.pitch(), angles.spin());
        }

        /** End of {@code LivingEntityRenderer#setupRotations}, see {@link #tilt}. */
        public static void apply(LivingEntityRenderState state, PoseStack poseStack)
        {
                FlightPoseState pose = (FlightPoseState) state;
                FlightPoseStyle style = pose.throwablemobs$flightPose();
                if (style != null)
                {
                        tilt(poseStack, style, pose.throwablemobs$flightPitch(), pose.throwablemobs$flightSpin(),
                                state.boundingBoxHeight / state.scale / 2.0F);
                }
        }
        //?}

        /**
         * End of {@code LivingEntityRenderer#setupRotations}: the model already faces {@link Angles#bodyRot}; tilt and
         * spin it around the middle of its hitbox. Positive X rotation tips the head backward here (vanilla flips the
         * model afterwards), so negative tips it forward. TILT points the facing direction along the path, so the spin
         * axis is the model's forward (Z); HEAD_FIRST tips the head forward until the up axis is along the path, so it
         * spins around Y. The pose stack is already scaled, so the pivot (half the hitbox height) is in unscaled units.
         */
        public static void tilt(PoseStack poseStack, FlightPoseStyle style, float pitch, float spin, float pivot)
        {
                poseStack.translate(0.0F, pivot, 0.0F);
                if (style == FlightPoseStyle.HEAD_FIRST)
                {
                        rotate(poseStack, Axis.XP, pitch - 90.0F);
                        rotate(poseStack, Axis.YP, spin);
                }
                else
                {
                        rotate(poseStack, Axis.XP, pitch);
                        rotate(poseStack, Axis.ZP, spin);
                }
                poseStack.translate(0.0F, -pivot, 0.0F);
        }

        private static void rotate(PoseStack poseStack, Axis axis, float degrees)
        {
                //? if >=26.3 {
                poseStack.rotateDegrees(axis, degrees);
                //?} else
                /*poseStack.mulPose(axis.rotationDegrees(degrees));*/
        }
}
