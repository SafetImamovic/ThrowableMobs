package io.github.safetimamovic.throwablemobs.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.safetimamovic.throwablemobs.client.FlightPose;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//?} else {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.safetimamovic.throwablemobs.client.FlightPose.Angles;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
*///?}

/** Thrown mobs' flight pose, see {@link FlightPose}. */
@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin
{
        //? if >=1.21.2 {
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
        //?} else {
        /*// No render state before 1.21.2: each step asks FlightPose about the entity itself.

        /^* The body faces the flight direction. *^/
        @ModifyVariable(method = "setupRotations", at = @At("HEAD"), argsOnly = true, ordinal = 1)
        private float throwablemobs$flightBodyRot(float bodyRot, LivingEntity entity, PoseStack poseStack, float bob,
                float yBodyRot, float partialTick, float scale)
        {
                Angles angles = FlightPose.of(entity, entity.tickCount + partialTick);
                return angles != null ? angles.bodyRot() : bodyRot;
        }

        @Inject(method = "setupRotations", at = @At("TAIL"))
        private void throwablemobs$applyFlightPose(LivingEntity entity, PoseStack poseStack, float bob, float bodyRot,
                float partialTick, float scale, CallbackInfo ci)
        {
                Angles angles = FlightPose.of(entity, entity.tickCount + partialTick);
                if (angles != null && angles.tilt() != null)
                {
                        FlightPose.tilt(poseStack, angles.tilt(), angles.pitch(), angles.spin(),
                                entity.getBbHeight() / scale / 2.0F);
                }
        }

        /^* The head looks straight ahead, along the body. *^/
        @WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"))
        private void throwablemobs$flightHead(EntityModel<Entity> model, Entity entity, float limbSwing,
                float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Operation<Void> original)
        {
                if (entity instanceof LivingEntity living && FlightPose.of(living, ageInTicks) != null)
                {
                        netHeadYaw = 0.0F;
                        headPitch = 0.0F;
                }
                original.call(model, entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        }
        *///?}
}
