package io.github.safetimamovic.throwablemobs.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** A thrown mob hitting a block or an entity. */
public interface ImpactContext extends ThrowContext
{
        /** The entity that was hit, or {@code null} for a block. */
        @Nullable Entity target();

        default boolean isBlockHit()
        {
                return target() == null;
        }

        /** Velocity of the thrown mob relative to the target; for a block, only the blocked part of the velocity. */
        Vec3 relativeVelocity();

        /** Collision energy {@code ½ μ v_rel²} in game units (THROWABLE_MOBS.md §8.4). */
        double energy();

        /** 0 for the first impact of a flight, 1 for the first bounce, and so on. */
        int impactIndex();

        /** The impact as physics and config would resolve it without any behavior. */
        ImpactResult defaultResult();
}
