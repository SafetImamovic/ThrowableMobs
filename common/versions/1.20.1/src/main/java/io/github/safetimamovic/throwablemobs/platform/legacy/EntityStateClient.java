package io.github.safetimamovic.throwablemobs.platform.legacy;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Client side of {@link EntityStateSyncPayload} (1.20.1 only). */
public final class EntityStateClient
{
        private EntityStateClient()
        {
        }

        /** On the client's main thread. */
        public static void onSync(EntityStateSyncPayload payload)
        {
                Minecraft minecraft = Minecraft.getInstance();
                Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(payload.entityId());
                if (entity instanceof EntityStates states)
                {
                        states.throwablemobs$setCarryState(payload.carry().orElse(null));
                        states.throwablemobs$setThrowState(payload.thrown().orElse(null));
                }
        }
}
