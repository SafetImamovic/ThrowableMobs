package io.github.safetimamovic.throwablemobs.compat;

import com.raiiiden.ragdollified.api.RagdollHandle;
import com.raiiiden.ragdollified.api.RagdollSpawnTransform;
import com.raiiiden.ragdollified.api.RagdollState;
import com.raiiiden.ragdollified.api.RagdollifiedApi;
import com.raiiiden.ragdollified.api.SpawnOptions;
import com.raiiiden.ragdollified.client.ClientRagdollManager;
import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.platform.Services;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Player config {@code ragdoll_in_flight}: with <a href="https://modrinth.com/mod/ragdollified">Ragdollified</a>
 * installed, a thrown mob goes limp in the air. Each client hides the mob and flies its ragdoll along with it: the
 * ragdoll starts with the throw's speed and every tick the whole body gets a small push back onto the mob's path, so the
 * limbs flop freely but the flight, the hits and the landing stay ours. The ragdoll goes when the flight ends; a mob
 * that dies in the air keeps it as its death ragdoll. Ragdollified is optional, so its classes are only touched once
 * it's known to be there.
 */
public final class RagdollifiedCompat
{
        public static final String MOD_ID = "ragdollified";

        /**
         * Ragdollified's part index for a push on the whole body: every part gets the same velocity, so the pose is
         * left alone. Not part of its API (the queue is how its own network impulses reach the physics thread).
         */
        private static final int WHOLE_BODY = -3;

        /** How much of the difference to the mob's velocity is made up each tick. */
        private static final double VELOCITY_GAIN = 0.5;

        /** Per second: how fast a gap to the mob's position is closed. */
        private static final double POSITION_GAIN = 4.0;

        /** Blocks per second; the most one tick's push can be, so a glitch can't fling the ragdoll away. */
        private static final double MAX_PUSH = 40.0;

        /** Mobs in flight by entity id; a {@code null} value is one Ragdollified can't ragdoll, so it isn't retried. */
        private static final Map<Integer, @Nullable Flight> FLIGHTS = new HashMap<>();

        private static @Nullable ClientLevel trackedLevel;

        private static boolean broken;

        private RagdollifiedCompat()
        {
        }

        private static final class Flight
        {
                private final LivingEntity entity;
                private final RagdollHandle handle;
                /** Where the ragdoll's torso was last tick, for its velocity; {@code null} until it's built. */
                private @Nullable Vec3 lastTorso;

                private Flight(LivingEntity entity, RagdollHandle handle)
                {
                        this.entity = entity;
                        this.handle = handle;
                }
        }

        /** Once per client tick. */
        public static void tick(Minecraft minecraft)
        {
                if (broken)
                {
                        return;
                }
                try
                {
                        update(minecraft.level);
                }
                catch (LinkageError e)
                {
                        // A Ragdollified version whose classes changed: thrown mobs just fly as usual.
                        Constants.LOG.warn("Ragdollified isn't the version we know, ragdolls in flight are off", e);
                        broken = true;
                        FLIGHTS.clear();
                }
        }

        private static void update(@Nullable ClientLevel level)
        {
                if (level != trackedLevel)
                {
                        // The old level's ragdolls went with it.
                        FLIGHTS.clear();
                        trackedLevel = level;
                }
                if (level == null)
                {
                        return;
                }
                boolean on = ThrowableMobsConfig.hud().ragdollInFlight();
                Iterator<Map.Entry<Integer, @Nullable Flight>> it = FLIGHTS.entrySet().iterator();
                while (it.hasNext())
                {
                        Map.Entry<Integer, @Nullable Flight> entry = it.next();
                        Flight flight = entry.getValue();
                        Entity entity = flight != null ? flight.entity : level.getEntity(entry.getKey());
                        if (on && entity != null && !entity.isRemoved() && Services.THROW_STATE.get(entity) != null
                                && !(entity instanceof LivingEntity living && living.isDeadOrDying()))
                        {
                                if (flight != null)
                                {
                                        steer(flight);
                                }
                                continue;
                        }
                        it.remove();
                        if (flight != null)
                        {
                                land(flight);
                        }
                }
                if (!on)
                {
                        return;
                }
                for (Entity entity : level.entitiesForRendering())
                {
                        // From its first tick of movement: on the launch tick it hasn't moved yet, and the ragdoll
                        // should start with the throw's speed, not at rest.
                        if (entity instanceof LivingEntity living && !living.isDeadOrDying()
                                && !FLIGHTS.containsKey(entity.getId()) && Services.THROW_STATE.get(entity) != null
                                && movement(entity).lengthSqr() > 1.0E-6)
                        {
                                FLIGHTS.put(entity.getId(), takeOff(living));
                        }
                }
        }

        private static @Nullable Flight takeOff(LivingEntity entity)
        {
                SpawnOptions options = SpawnOptions.builder()
                        .hideEntity(true)
                        // Kept until the flight ends, not timed out like a death ragdoll.
                        .persistent(true)
                        // Ragdollified's velocities are in blocks per second.
                        .spawnTransform(new RagdollSpawnTransform(entity.position(), entity.yBodyRot, entity.getXRot(),
                                movement(entity).scale(20.0), false))
                        .build();
                RagdollHandle handle = RagdollifiedApi.spawn(entity, options);
                // Not exists() yet: Ragdollified builds the ragdoll on the next frame, until then it's only pending.
                if (handle == null)
                {
                        // A body Ragdollified has no ragdoll for, or turned off for this mob in its config.
                        return null;
                }
                return new Flight(entity, handle);
        }

        /** Pushes the whole ragdoll toward the mob's velocity and position, leaving its limbs alone. */
        private static void steer(Flight flight)
        {
                RagdollState state = flight.handle.getState().orElse(null);
                if (state == null)
                {
                        // Not built yet.
                        return;
                }
                Vec3 torso = state.torsoPosition();
                Vec3 lastTorso = flight.lastTorso;
                flight.lastTorso = torso;
                if (lastTorso == null)
                {
                        return;
                }
                Vec3 velocity = torso.subtract(lastTorso).scale(20.0);
                Vec3 middle = flight.entity.position().add(0.0, flight.entity.getBbHeight() / 2.0, 0.0);
                Vec3 push = movement(flight.entity).scale(20.0).subtract(velocity).scale(VELOCITY_GAIN)
                        .add(middle.subtract(torso).scale(POSITION_GAIN));
                if (push.lengthSqr() > MAX_PUSH * MAX_PUSH)
                {
                        push = push.normalize().scale(MAX_PUSH);
                }
                ClientRagdollManager.enqueueImpulse(flight.entity.getId(), WHOLE_BODY, (float) push.x, (float) push.y,
                        (float) push.z);
        }

        /** Alive: the mob stands up where it landed. Dead: the ragdoll stays as its death ragdoll. */
        private static void land(Flight flight)
        {
                if (flight.entity.isDeadOrDying())
                {
                        flight.handle.setPersistent(false).restartDeathLifetime();
                        return;
                }
                flight.handle.remove();
                RagdollifiedApi.setEntityHidden(flight.entity, false);
        }

        /** What it moved last tick, in blocks: the synced velocity of a mob isn't always up to date on clients. */
        private static Vec3 movement(Entity entity)
        {
                return entity.position().subtract(entity.xo, entity.yo, entity.zo);
        }
}
