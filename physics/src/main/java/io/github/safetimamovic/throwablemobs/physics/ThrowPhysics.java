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

        /**
         * Carrier slowdown as a fraction of movement speed (0.15 = 15 % slower):
         * {@code clamp(m / mMax × factor, 0, maxSlowdown)}.
         */
        public static double carrySlowdown(double mass, double maxMass, double factor, double maxSlowdown)
        {
                return Math.clamp(mass / maxMass * factor, 0.0, maxSlowdown);
        }

        /**
         * Charge fraction from held ticks, with the bow's ease curve: {@code x = clamp(t / max, 0, 1)},
         * {@code c = (x² + 2x) / 3}. Full charge gives 1.
         */
        public static double chargeFraction(int ticks, int maxTicks)
        {
                double x = Math.clamp((double) ticks / maxTicks, 0.0, 1.0);
                return (x * x + 2.0 * x) / 3.0;
        }

        /** Launch speed from a fixed impulse: {@code min(J × charge / m, vMax)}. */
        public static double launchSpeed(double impulse, double charge, double mass, double maxSpeed)
        {
                return Math.min(impulse * charge / mass, maxSpeed);
        }

        /**
         * One flight tick of velocity, semi-implicit Euler like vanilla projectiles: gravity, then linear
         * drag. The caller then moves the body by the returned velocity. Server flight and client
         * trajectory preview both use this, so the preview matches the real throw.
         */
        public static Vector stepVelocity(Vector velocity, double gravity, double drag)
        {
                return new Vector(velocity.x() * drag, (velocity.y() - gravity) * drag, velocity.z() * drag);
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

        /** Damage from collision energy: {@code min(k × max(0, E − E_min), max)}. */
        public static double impactDamage(double energy, double damagePerEnergy, double minEnergy, double maxDamage)
        {
                return Math.min(damagePerEnergy * Math.max(0.0, energy - minEnergy), maxDamage);
        }

        /**
         * Share of the impact damage the target takes: {@code m₁ / (m₁ + m₂)}, so the lighter body takes
         * more. The thrown body takes the rest. A block (infinite {@code m2}) takes nothing.
         */
        public static double targetDamageShare(double m1, double m2)
        {
                return m1 / (m1 + m2);
        }

        /** Target velocity change per unit of {@code v_rel}: {@code (1 + e) × m₁ / (m₁ + m₂)}. */
        public static double targetKnockback(double m1, double m2, double restitution)
        {
                return (1.0 + restitution) * m1 / (m1 + m2);
        }

        /**
         * Thrown body velocity change per unit of {@code v_rel}: {@code −(1 + e) × m₂ / (m₁ + m₂)}.
         * Against a block (infinite {@code m2}) it bounces back with {@code −(1 + e)}.
         */
        public static double thrownKnockback(double m1, double m2, double restitution)
        {
                if (Double.isInfinite(m2))
                {
                        return -(1.0 + restitution);
                }
                return -(1.0 + restitution) * m2 / (m1 + m2);
        }

        /** Plain 3D vector, so this module stays free of Minecraft's {@code Vec3}. */
        public record Vector(double x, double y, double z)
        {
        }
}
