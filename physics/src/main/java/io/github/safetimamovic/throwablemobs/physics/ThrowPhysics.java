package io.github.safetimamovic.throwablemobs.physics;

/**
 * Pure physics math for throws and impacts. No Minecraft imports so it can be unit tested and shared
 * between server simulation and client trajectory preview.
 *
 * <p>Units follow vanilla: 1 block = 1 m, velocities in blocks/tick, masses in "game-kg"
 * (only ratios matter). See THROWABLE_MOBS.md §8.
 */
public final class ThrowPhysics
{

        private ThrowPhysics()
        {
        }

        /** Mass from hitbox volume: {@code width² × height × density}. */
        public static double massFromHitbox(double width, double height, double density)
        {
                return width * width * height * density;
        }

        /** Launch speed from a fixed impulse: {@code min(J × charge / m, vMax)}. */
        public static double launchSpeed(double impulse, double charge, double mass, double maxSpeed)
        {
                return Math.min(impulse * charge / mass, maxSpeed);
        }

        /** Reduced mass {@code m₁m₂ / (m₁ + m₂)}. An infinite {@code m2} (a block) yields {@code m1}. */
        public static double reducedMass(double m1, double m2)
        {
                if (Double.isInfinite(m2))
                {
                        return m1;
                }
                return m1 * m2 / (m1 + m2);
        }

        /** Energy available to the collision, in the centre-of-mass frame: {@code ½ μ v_rel²}. */
        public static double collisionEnergy(double m1, double m2, double relativeSpeed)
        {
                return 0.5 * reducedMass(m1, m2) * relativeSpeed * relativeSpeed;
        }

        /**
         * Speed change of the thrown body when it goes from {@code v_rel} to {@code -e × v_rel},
         * spread over {@code collisionTicks}: {@code a = (1 + e) × |v_rel| / Δt}.
         */
        public static double impactDeceleration(double relativeSpeed, double restitution, double collisionTicks)
        {
                return (1.0 + restitution) * relativeSpeed / collisionTicks;
        }
}
