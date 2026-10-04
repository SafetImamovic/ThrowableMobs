package io.github.safetimamovic.throwablemobs.client;

import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.FlightPoseStyle;
import org.jspecify.annotations.Nullable;

/**
 * Extra fields on {@code LivingEntityRenderState}, added by a mixin: how a thrown mob's model is tilted and spun this
 * frame. Filled while extracting the render state, read while setting up the model's rotations.
 */
public interface FlightPoseState
{
        /** @param style {@link FlightPoseStyle#TILT} or {@link FlightPoseStyle#HEAD_FIRST}; {@code null} for none */
        void throwablemobs$setFlightPose(@Nullable FlightPoseStyle style, float pitch, float spin);

        @Nullable
        FlightPoseStyle throwablemobs$flightPose();

        /** Pitch of the flight direction in degrees, positive = upward. */
        float throwablemobs$flightPitch();

        /** Degrees around the flight direction. */
        float throwablemobs$flightSpin();
}
