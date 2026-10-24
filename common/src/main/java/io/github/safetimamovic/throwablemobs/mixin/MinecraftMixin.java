package io.github.safetimamovic.throwablemobs.mixin;

import io.github.safetimamovic.throwablemobs.client.CatchHighlight;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The catch outline, see {@link CatchHighlight}: vanilla's glowing outline, in the mob's team color (white without). */
@Mixin(Minecraft.class)
abstract class MinecraftMixin
{
        @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
        private void throwablemobs$glowWhenCatchable(Entity entity, CallbackInfoReturnable<Boolean> cir)
        {
                if (!cir.getReturnValueZ() && CatchHighlight.isHighlighted(entity))
                {
                        cir.setReturnValue(true);
                }
        }
}
