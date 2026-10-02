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
        void softLandingsReduceDeceleration()
        {
                assertTrue(ThrowPhysics.impactDeceleration(2, 0.1, 4) < ThrowPhysics.impactDeceleration(2, 0.1, 1));
        }
}
