package io.github.safetimamovic.throwablemobs.client;

import io.github.safetimamovic.throwablemobs.carry.CarryRules;
import io.github.safetimamovic.throwablemobs.carry.ThrowManager;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.ThrowConfig;
import io.github.safetimamovic.throwablemobs.physics.ThrowPhysics;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Dotted path of the throw being charged, from the client's own charge, look direction and movement. It runs the
 * server's flight tick by tick: move with vanilla collision ({@link Entity#collideBoundingBox}, what
 * {@link Entity#move} uses), stop at the first block, entity or water, then {@link ThrowPhysics#stepVelocity}. Drawn
 * with vanilla gizmos from the client tick, which vanilla collects per tick and draws every frame until the next one.
 */
final class TrajectoryPreview
{
        private static final int PATH_COLOR = 0xFFFFFFFF;
        private static final float PATH_WIDTH = 2.0F;
        private static final int BLOCK_MARKER_COLOR = 0xFFFFFFFF;
        private static final int ENTITY_MARKER_COLOR = 0xFFFF5555;
        private static final float MARKER_WIDTH = 2.0F;
        /** Below this, two movements count as equal; anything else means a collision cut the move short. */
        private static final double EPSILON = 1.0E-7;

        private TrajectoryPreview()
        {
        }

        static void draw(ClientLevel level, LocalPlayer player, Mob mob, int chargeTicks)
        {
                ThrowConfig config = ThrowableMobsConfig.throwing();
                if (chargeTicks < config.minChargeTicks())
                {
                        return;
                }
                double charge = ThrowPhysics.chargeFraction(Math.min(chargeTicks, config.maxChargeTicks()),
                        config.maxChargeTicks());
                double speed = ThrowPhysics.launchSpeed(config.throwImpulse(), charge, CarryRules.mass(mob),
                        config.maxThrowSpeed());
                // What the server knows of the player's movement: last tick's change in position.
                Vec3 movement = player.position().subtract(player.xo, player.yo, player.zo);
                Vec3 velocity = player.getLookAngle().scale(speed).add(movement.scale(config.inheritVelocity()));

                AABB box = mob.getBoundingBox();
                for (int tick = 1; tick <= config.maxFlightTicks(); tick++)
                {
                        Entity target = findTarget(level, player, mob, box, velocity, tick, config);
                        if (target != null)
                        {
                                Gizmos.cuboid(target.getBoundingBox(), GizmoStyle.stroke(ENTITY_MARKER_COLOR, MARKER_WIDTH));
                                return;
                        }
                        Vec3 allowed = Entity.collideBoundingBox(mob, velocity, box, level, List.of());
                        AABB next = box.move(allowed);
                        Gizmos.line(box.getCenter(), next.getCenter(), PATH_COLOR, PATH_WIDTH);
                        box = next;
                        if (allowed.distanceToSqr(velocity) > EPSILON || inWater(level, box))
                        {
                                Gizmos.cuboid(box, GizmoStyle.stroke(BLOCK_MARKER_COLOR, MARKER_WIDTH));
                                return;
                        }
                        ThrowPhysics.Vector step = ThrowPhysics.stepVelocity(
                                new ThrowPhysics.Vector(velocity.x, velocity.y, velocity.z), config.gravity(),
                                config.drag());
                        velocity = new Vec3(step.x(), step.y(), step.z());
                }
        }

        private static @Nullable Entity findTarget(ClientLevel level, LocalPlayer player, Mob mob, AABB box, Vec3 velocity,
                int tick, ThrowConfig config)
        {
                Entity nearest = null;
                double nearestDistance = Double.MAX_VALUE;
                for (Entity entity : level.getEntities(mob, box.expandTowards(velocity),
                        entity -> ThrowManager.canHit(entity, player.getUUID(), tick, config)))
                {
                        double distance = entity.getBoundingBox().distanceToSqr(box.getCenter());
                        if (distance < nearestDistance)
                        {
                                nearest = entity;
                                nearestDistance = distance;
                        }
                }
                return nearest;
        }

        /** Close to {@link Entity#isInWater}: water at the bottom of the box. */
        private static boolean inWater(ClientLevel level, AABB box)
        {
                BlockPos feet = BlockPos.containing(box.getCenter().x, box.minY + 0.1, box.getCenter().z);
                return level.getFluidState(feet).is(FluidTags.WATER);
        }
}
