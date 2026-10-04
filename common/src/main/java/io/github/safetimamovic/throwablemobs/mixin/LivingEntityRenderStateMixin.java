package io.github.safetimamovic.throwablemobs.mixin;

import io.github.safetimamovic.throwablemobs.client.FlightPoseState;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.FlightPoseStyle;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
abstract class LivingEntityRenderStateMixin implements FlightPoseState
{
        @Unique
        private @Nullable FlightPoseStyle throwablemobs$flightPose;
        @Unique
        private float throwablemobs$flightPitch;
        @Unique
        private float throwablemobs$flightSpin;

        @Override
        public void throwablemobs$setFlightPose(@Nullable FlightPoseStyle style, float pitch, float spin)
        {
                throwablemobs$flightPose = style;
                throwablemobs$flightPitch = pitch;
                throwablemobs$flightSpin = spin;
        }

        @Override
        public @Nullable FlightPoseStyle throwablemobs$flightPose()
        {
                return throwablemobs$flightPose;
        }

        @Override
        public float throwablemobs$flightPitch()
        {
                return throwablemobs$flightPitch;
        }

        @Override
        public float throwablemobs$flightSpin()
        {
                return throwablemobs$flightSpin;
        }
}
