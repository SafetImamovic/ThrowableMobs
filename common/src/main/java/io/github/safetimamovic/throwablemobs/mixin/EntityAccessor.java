package io.github.safetimamovic.throwablemobs.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The real velocity field, for entities like the shulker that override its getter and setter. */
@Mixin(Entity.class)
public interface EntityAccessor
{
        @Accessor("deltaMovement")
        Vec3 throwablemobs$deltaMovement();

        @Accessor("deltaMovement")
        void throwablemobs$setDeltaMovement(Vec3 deltaMovement);
}
