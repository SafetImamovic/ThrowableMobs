package io.github.safetimamovic.throwablemobs.api;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Physics, damage and behaviors for a kind of mob. Every field is optional: a profile only sets what it changes, and
 * profiles stack in layers (defaults, heuristics, {@link ThrowableMob}, Java registration, datapacks by priority).
 * Each layer {@linkplain #over merges} on top of the one below; {@code replace} drops everything below instead.
 *
 * @param replace           drop every layer below this one instead of merging
 * @param mass              game-kg; unset = hitbox volume × {@code default_density}
 * @param gravityMultiplier scales the config {@code gravity}
 * @param dragMultiplier    exponent on the config {@code drag} per tick; 2 = twice the speed loss, 0 = no drag
 * @param restitution       bounciness against entities; unset = config {@code restitution}
 * @param blockRestitution  bounciness against blocks; 0 = no bounce
 * @param maxBounces        bounces off blocks before the flight ends
 * @param bounceImpacts     whether later bounces count as impacts
 * @param selfMultiplier    scales impact damage to this mob
 * @param targetMultiplier  scales impact damage this mob deals
 * @param behaviors         replaces the list below it as a whole; an empty list removes all behaviors
 */
public record MobProfile(boolean replace, @Nullable Double mass, @Nullable Double gravityMultiplier,
        @Nullable Double dragMultiplier, @Nullable Double restitution, @Nullable Double blockRestitution,
        @Nullable Integer maxBounces, @Nullable BounceImpacts bounceImpacts, @Nullable Double selfMultiplier,
        @Nullable Double targetMultiplier, @Nullable List<ThrowBehavior> behaviors)
{
        public static final MobProfile EMPTY = builder().build();

        public MobProfile
        {
                behaviors = behaviors == null ? null : List.copyOf(behaviors);
        }

        /**
         * This profile on top of {@code below}: set fields win, unset ones come from below. The result drops the layers
         * under {@code below} if either of them says {@code replace}.
         */
        public MobProfile over(MobProfile below)
        {
                if (replace)
                {
                        return this;
                }
                return new MobProfile(below.replace,
                        pick(mass, below.mass),
                        pick(gravityMultiplier, below.gravityMultiplier),
                        pick(dragMultiplier, below.dragMultiplier),
                        pick(restitution, below.restitution),
                        pick(blockRestitution, below.blockRestitution),
                        pick(maxBounces, below.maxBounces),
                        pick(bounceImpacts, below.bounceImpacts),
                        pick(selfMultiplier, below.selfMultiplier),
                        pick(targetMultiplier, below.targetMultiplier),
                        pick(behaviors, below.behaviors));
        }

        private static <T> @Nullable T pick(@Nullable T above, @Nullable T below)
        {
                return above != null ? above : below;
        }

        public static Builder builder()
        {
                return new Builder();
        }

        /** For mods registering profiles in Java. */
        public static final class Builder
        {
                private boolean replace;
                private @Nullable Double mass;
                private @Nullable Double gravityMultiplier;
                private @Nullable Double dragMultiplier;
                private @Nullable Double restitution;
                private @Nullable Double blockRestitution;
                private @Nullable Integer maxBounces;
                private @Nullable BounceImpacts bounceImpacts;
                private @Nullable Double selfMultiplier;
                private @Nullable Double targetMultiplier;
                private @Nullable List<ThrowBehavior> behaviors;

                private Builder()
                {
                }

                public Builder replace(boolean replace)
                {
                        this.replace = replace;
                        return this;
                }

                public Builder mass(double mass)
                {
                        this.mass = mass;
                        return this;
                }

                public Builder gravityMultiplier(double multiplier)
                {
                        this.gravityMultiplier = multiplier;
                        return this;
                }

                public Builder dragMultiplier(double multiplier)
                {
                        this.dragMultiplier = multiplier;
                        return this;
                }

                public Builder restitution(double restitution)
                {
                        this.restitution = restitution;
                        return this;
                }

                public Builder blockRestitution(double restitution)
                {
                        this.blockRestitution = restitution;
                        return this;
                }

                public Builder maxBounces(int bounces)
                {
                        this.maxBounces = bounces;
                        return this;
                }

                public Builder bounceImpacts(BounceImpacts bounceImpacts)
                {
                        this.bounceImpacts = bounceImpacts;
                        return this;
                }

                public Builder selfMultiplier(double multiplier)
                {
                        this.selfMultiplier = multiplier;
                        return this;
                }

                public Builder targetMultiplier(double multiplier)
                {
                        this.targetMultiplier = multiplier;
                        return this;
                }

                public Builder behaviors(List<ThrowBehavior> behaviors)
                {
                        this.behaviors = behaviors;
                        return this;
                }

                public MobProfile build()
                {
                        return new MobProfile(replace, mass, gravityMultiplier, dragMultiplier, restitution,
                                blockRestitution, maxBounces, bounceImpacts, selfMultiplier, targetMultiplier,
                                behaviors);
                }
        }
}
