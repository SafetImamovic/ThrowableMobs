package io.github.safetimamovic.throwablemobs.api;

import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * How an impact plays out. Immutable: behaviors return a changed copy from {@link ThrowBehavior#onImpact}.
 *
 * @param targetDamage        damage to the hit entity; ignored for a block hit
 * @param selfDamage          damage to the thrown mob
 * @param knockbackMultiplier scales the knockback on the hit entity; 0 turns it off
 * @param bounceVelocity      velocity to keep flying with after a block hit, or {@code null} to end the flight there.
 *                            Entity hits always end the flight.
 */
public record ImpactResult(double targetDamage, double selfDamage, double knockbackMultiplier,
        @Nullable Vec3 bounceVelocity)
{
        public ImpactResult withTargetDamage(double damage)
        {
                return new ImpactResult(damage, selfDamage, knockbackMultiplier, bounceVelocity);
        }

        public ImpactResult withSelfDamage(double damage)
        {
                return new ImpactResult(targetDamage, damage, knockbackMultiplier, bounceVelocity);
        }

        public ImpactResult withKnockbackMultiplier(double multiplier)
        {
                return new ImpactResult(targetDamage, selfDamage, multiplier, bounceVelocity);
        }

        /** Keep flying with {@code velocity} after a block hit. */
        public ImpactResult bounce(Vec3 velocity)
        {
                return new ImpactResult(targetDamage, selfDamage, knockbackMultiplier, velocity);
        }

        /** End the flight here. */
        public ImpactResult land()
        {
                return new ImpactResult(targetDamage, selfDamage, knockbackMultiplier, null);
        }

        public boolean endsFlight()
        {
                return bounceVelocity == null;
        }
}
