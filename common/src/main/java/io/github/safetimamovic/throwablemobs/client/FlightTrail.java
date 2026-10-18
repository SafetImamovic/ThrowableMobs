package io.github.safetimamovic.throwablemobs.client;

import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.platform.Services;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * A faint trail behind fast thrown mobs, drawn by each client from the synced throw state, so it costs no packets.
 * Player config {@code flight_trail}.
 */
public final class FlightTrail
{
        /** Slower than this (blocks/tick), no trail: lobs and the top of an arc stay clean. */
        private static final double MIN_SPEED = 0.4;

        private FlightTrail()
        {
        }

        /** Once per client tick. */
        public static void tick(ClientLevel level)
        {
                if (!ThrowableMobsConfig.hud().flightTrail())
                {
                        return;
                }
                for (Entity entity : level.entitiesForRendering())
                {
                        if (Services.THROW_STATE.get(entity) == null)
                        {
                                continue;
                        }
                        Vec3 moved = entity.position().subtract(entity.xo, entity.yo, entity.zo);
                        if (moved.length() < MIN_SPEED)
                        {
                                continue;
                        }
                        // Where it was last tick, so the trail starts behind the mob, not inside it.
                        level.addParticle(ParticleTypes.CLOUD, entity.xo, entity.yo + entity.getBbHeight() / 2.0,
                                entity.zo, 0.0, 0.0, 0.0);
                }
        }
}
