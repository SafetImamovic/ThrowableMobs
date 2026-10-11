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
        /** Below this, a shorter move along an axis is rounding, not a collision. Vanilla collision uses 1e-7. */
        private static final double BLOCKED_EPSILON = 1.0E-7;

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

        /**
         * Per-tick drag with a per-mob multiplier: {@code drag ^ multiplier}. A multiplier of 2 loses twice as much
         * speed per tick in log terms (0.99 → 0.9801), 0 turns drag off.
         */
        public static double scaledDrag(double drag, double multiplier)
        {
                return Math.pow(drag, multiplier);
        }

        /**
         * The part of {@code wanted} that went into blocks during a tick that only got {@code moved}: the full speed
         * along every blocked axis, zero along the free ones. Impact damage uses this, so hitting a wall head-on hurts
         * more than skimming along the ground, whatever the distance to the block was at the start of the tick.
         */
        public static Vector impactVelocity(Vector wanted, Vector moved)
        {
                return new Vector(blocked(wanted.x(), moved.x()) ? wanted.x() : 0.0,
                        blocked(wanted.y(), moved.y()) ? wanted.y() : 0.0,
                        blocked(wanted.z(), moved.z()) ? wanted.z() : 0.0);
        }

        /**
         * Bounce off blocks after a tick that tried to move by {@code wanted} but only got {@code moved} (block
         * collision clamps each axis on its own). Every blocked axis is reversed and scaled by the restitution
         * {@code e}; free axes keep their speed. An axis that moved further than wanted (a step up) isn't blocked.
         *
         * With {@code e = 0} this is what's left after a landing: the free axes only.
         *
         * @return the velocity after the bounce and the rebound speed along the blocked axes, which the caller compares
         *         with {@code min_bounce_speed} to end the flight
         */
        public static Bounce bounce(Vector wanted, Vector moved, double restitution)
        {
                boolean bx = blocked(wanted.x(), moved.x());
                boolean by = blocked(wanted.y(), moved.y());
                boolean bz = blocked(wanted.z(), moved.z());
                Vector velocity = new Vector(bx ? -wanted.x() * restitution : wanted.x(),
                        by ? -wanted.y() * restitution : wanted.y(), bz ? -wanted.z() * restitution : wanted.z());
                double rx = bx ? velocity.x() : 0.0;
                double ry = by ? velocity.y() : 0.0;
                double rz = bz ? velocity.z() : 0.0;
                return new Bounce(velocity, Math.sqrt(rx * rx + ry * ry + rz * rz));
        }

        private static boolean blocked(double wanted, double moved)
        {
                return Math.abs(moved) < Math.abs(wanted) - BLOCKED_EPSILON;
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
                public double length()
                {
                        return Math.sqrt(x * x + y * y + z * z);
                }
        }

        /**
         * Result of {@link #bounce}.
         *
         * @param reboundSpeed speed along the blocked axes after the bounce; 0 when nothing was blocked
         */
        public record Bounce(Vector velocity, double reboundSpeed)
        {
        }
}
