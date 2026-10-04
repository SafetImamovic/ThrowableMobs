package io.github.safetimamovic.throwablemobs.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.safetimamovic.throwablemobs.client.FlightPose;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Thrown mobs' flight pose, see {@link FlightPose}. */
@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin
{
        @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;"
                + "Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
        private void throwablemobs$extractFlightPose(LivingEntity entity, LivingEntityRenderState state,
                float partialTick, CallbackInfo ci)
        {
                FlightPose.extract(entity, state);
        }

        @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;"
                + "Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("TAIL"))
        private void throwablemobs$applyFlightPose(LivingEntityRenderState state, PoseStack poseStack, float bodyRot,
                float scale, CallbackInfo ci)
        {
                FlightPose.apply(state, poseStack);
        }
}
