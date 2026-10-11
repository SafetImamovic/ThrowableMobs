package io.github.safetimamovic.throwablemobs.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrowPhysicsTest
{

        private static final double EPS = 1e-9;

        @Test
        void lighterMobsFlyFaster()
        {
                double chicken = ThrowPhysics.massFromHitbox(0.4, 0.7, 400);
                double cow = ThrowPhysics.massFromHitbox(0.9, 1.4, 400);
                assertTrue(ThrowPhysics.launchSpeed(300, 1, chicken, 10) > ThrowPhysics.launchSpeed(300, 1, cow, 10));
        }

        @Test
        void heavierMobsSlowTheCarrierMore()
        {
                double chicken = ThrowPhysics.massFromHitbox(0.4, 0.7, 400);
                double cow = ThrowPhysics.massFromHitbox(0.9, 1.4, 400);
                double chickenSlowdown = ThrowPhysics.carrySlowdown(chicken, 1500, 0.5, 0.6);
                double cowSlowdown = ThrowPhysics.carrySlowdown(cow, 1500, 0.5, 0.6);
                assertTrue(chickenSlowdown < 0.02);
                assertTrue(cowSlowdown > chickenSlowdown && cowSlowdown < 0.2);
        }

        @Test
        void carrySlowdownIsCapped()
        {
                assertEquals(0.6, ThrowPhysics.carrySlowdown(1e9, 1500, 0.5, 0.6), EPS);
                assertEquals(0.0, ThrowPhysics.carrySlowdown(0, 1500, 0.5, 0.6), EPS);
        }

        @Test
        void launchSpeedIsCapped()
        {
                assertEquals(3.0, ThrowPhysics.launchSpeed(1e9, 1, 1, 3.0), EPS);
        }

        @Test
        void blockActsAsInfiniteMass()
        {
                assertEquals(50.0, ThrowPhysics.reducedMass(50, Double.POSITIVE_INFINITY), EPS);
                assertEquals(0.5 * 50 * 4, ThrowPhysics.collisionEnergy(50, Double.POSITIVE_INFINITY, 2), EPS);
        }

        @Test
        void equalMassesHalveCollisionEnergy()
        {
                assertEquals(ThrowPhysics.collisionEnergy(10, Double.POSITIVE_INFINITY, 3) / 2,
                        ThrowPhysics.collisionEnergy(10, 10, 3), EPS);
        }

        @Test
        void chargeFollowsTheBowCurve()
        {
                assertEquals(0.0, ThrowPhysics.chargeFraction(0, 20), EPS);
                assertEquals((0.25 + 1.0) / 3.0, ThrowPhysics.chargeFraction(10, 20), EPS);
                assertEquals(1.0, ThrowPhysics.chargeFraction(20, 20), EPS);
                assertEquals(1.0, ThrowPhysics.chargeFraction(100, 20), EPS);
        }

        @Test
        void stepAppliesGravityThenDrag()
        {
                ThrowPhysics.Vector v = ThrowPhysics.stepVelocity(new ThrowPhysics.Vector(1, 0, -2), 0.05, 0.99);
                assertEquals(0.99, v.x(), EPS);
                assertEquals(-0.05 * 0.99, v.y(), EPS);
                assertEquals(-1.98, v.z(), EPS);
        }

        @Test
        void dragShortensTheThrow()
        {
                assertTrue(range(0.99) < range(1.0));
        }

        /** Horizontal distance of a 45° throw at 1 b/t until it falls back to launch height. */
        private static double range(double drag)
        {
                ThrowPhysics.Vector v = new ThrowPhysics.Vector(Math.sqrt(0.5), Math.sqrt(0.5), 0);
                double x = 0;
                double y = 0;
                do
                {
                        v = ThrowPhysics.stepVelocity(v, 0.05, drag);
                        x += v.x();
                        y += v.y();
                }
                while (y > 0);
                return x;
        }

        @Test
        void impactDamageHasThresholdAndCap()
        {
                assertEquals(0.0, ThrowPhysics.impactDamage(5, 0.1, 10, 20), EPS);
                assertEquals(3.0, ThrowPhysics.impactDamage(40, 0.1, 10, 20), EPS);
                assertEquals(20.0, ThrowPhysics.impactDamage(1e9, 0.1, 10, 20), EPS);
        }

        @Test
        void lighterBodyTakesMoreDamage()
        {
                double chicken = 45;
                double cow = 454;
                assertTrue(ThrowPhysics.targetDamageShare(chicken, cow) < 0.5);
                assertEquals(0.0, ThrowPhysics.targetDamageShare(chicken, Double.POSITIVE_INFINITY), EPS);
        }

        @Test
        void knockbackConservesMomentum()
        {
                double m1 = 45;
                double m2 = 454;
                double e = 0.3;
                assertEquals(0.0, m1 * ThrowPhysics.thrownKnockback(m1, m2, e) + m2 * ThrowPhysics.targetKnockback(m1, m2, e), EPS);
        }

        @Test
        void blockBouncesTheThrownBodyBack()
        {
                assertEquals(-1.3, ThrowPhysics.thrownKnockback(45, Double.POSITIVE_INFINITY, 0.3), EPS);
                assertEquals(0.0, ThrowPhysics.targetKnockback(45, Double.POSITIVE_INFINITY, 0.3), EPS);
        }

        @Test
        void softLandingsReduceDeceleration()
        {
                assertTrue(ThrowPhysics.impactDeceleration(2, 0.1, 4) < ThrowPhysics.impactDeceleration(2, 0.1, 1));
        }

        @Test
        void dragMultiplierScalesSpeedLoss()
        {
                assertEquals(0.99, ThrowPhysics.scaledDrag(0.99, 1), EPS);
                assertEquals(0.99 * 0.99, ThrowPhysics.scaledDrag(0.99, 2), EPS);
                assertEquals(1.0, ThrowPhysics.scaledDrag(0.99, 0), EPS);
        }

        @Test
        void floorBounceReversesOnlyTheFall()
        {
                ThrowPhysics.Bounce bounce = ThrowPhysics.bounce(new ThrowPhysics.Vector(0.5, -1.0, 0.2),
                        new ThrowPhysics.Vector(0.5, -0.3, 0.2), 0.8);
                assertVector(0.5, 0.8, 0.2, bounce.velocity());
                assertEquals(0.8, bounce.reboundSpeed(), EPS);
        }

        @Test
        void cornerBounceReversesBothBlockedAxes()
        {
                ThrowPhysics.Bounce bounce = ThrowPhysics.bounce(new ThrowPhysics.Vector(1.0, 0.0, -1.0),
                        new ThrowPhysics.Vector(0.4, 0.0, -0.1), 0.5);
                assertVector(-0.5, 0.0, 0.5, bounce.velocity());
                assertEquals(Math.sqrt(0.5), bounce.reboundSpeed(), EPS);
        }

        @Test
        void zeroRestitutionStopsTheBlockedAxis()
        {
                ThrowPhysics.Bounce bounce = ThrowPhysics.bounce(new ThrowPhysics.Vector(1.0, 0.0, 0.0),
                        new ThrowPhysics.Vector(0.2, 0.0, 0.0), 0.0);
                assertEquals(0.0, bounce.velocity().x(), EPS);
                assertEquals(0.0, bounce.reboundSpeed(), EPS);
        }

        @Test
        void freeMovementAndStepUpsDontBounce()
        {
                ThrowPhysics.Vector wanted = new ThrowPhysics.Vector(0.5, 0.1, 0.0);
                ThrowPhysics.Bounce free = ThrowPhysics.bounce(wanted, wanted, 0.8);
                assertVector(0.5, 0.1, 0.0, free.velocity());
                assertEquals(0.0, free.reboundSpeed(), EPS);

                // Stepping onto a slab moves further up than wanted: not a collision.
                ThrowPhysics.Bounce step = ThrowPhysics.bounce(wanted, new ThrowPhysics.Vector(0.5, 0.6, 0.0), 0.8);
                assertVector(0.5, 0.1, 0.0, step.velocity());
        }

        @Test
        void impactUsesTheFullSpeedIntoTheBlock()
        {
                // Falling at 2 b/t, touched the ground after 1.8: the impact is 2, not the 0.2 left over.
                ThrowPhysics.Vector impact = ThrowPhysics.impactVelocity(new ThrowPhysics.Vector(0.5, -2.0, 0.0),
                        new ThrowPhysics.Vector(0.5, -1.8, 0.0));
                assertVector(0.0, -2.0, 0.0, impact);
                assertEquals(2.0, impact.length(), EPS);
        }

        @Test
        void landingKeepsOnlyTheFreeAxes()
        {
                ThrowPhysics.Bounce landing = ThrowPhysics.bounce(new ThrowPhysics.Vector(0.5, -2.0, 0.0),
                        new ThrowPhysics.Vector(0.5, -1.8, 0.0), 0.0);
                assertVector(0.5, 0.0, 0.0, landing.velocity());
        }

        private static void assertVector(double x, double y, double z, ThrowPhysics.Vector actual)
        {
                assertEquals(x, actual.x(), EPS);
                assertEquals(y, actual.y(), EPS);
                assertEquals(z, actual.z(), EPS);
        }
}
