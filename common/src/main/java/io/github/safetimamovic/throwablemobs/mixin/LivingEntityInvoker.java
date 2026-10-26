package io.github.safetimamovic.throwablemobs.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The mob's own hurt sound, which the radar plays when a profile doesn't name one. */
@Mixin(LivingEntity.class)
public interface LivingEntityInvoker
{
        @Invoker("getHurtSound")
        @Nullable SoundEvent throwablemobs$getHurtSound(DamageSource source);
}
