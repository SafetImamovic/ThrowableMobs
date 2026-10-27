package io.github.safetimamovic.throwablemobs.platform.legacy;

import io.github.safetimamovic.throwablemobs.carry.ThrowState;
import io.github.safetimamovic.throwablemobs.platform.services.IThrowStateAccess;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/** {@link IThrowStateAccess} for 1.20.1, on both loaders; see {@link EntityStates}. */
public final class LegacyThrowStateAccess implements IThrowStateAccess
{
        @Override
        public @Nullable ThrowState get(Entity entity)
        {
                return ((EntityStates) entity).throwablemobs$throwState();
        }

        @Override
        public void set(Entity entity, ThrowState state)
        {
                ((EntityStates) entity).throwablemobs$setThrowState(state);
                EntityStateSyncPayload.broadcast(entity);
        }

        @Override
        public void remove(Entity entity)
        {
                if (get(entity) != null)
                {
                        ((EntityStates) entity).throwablemobs$setThrowState(null);
                        EntityStateSyncPayload.broadcast(entity);
                }
        }
}
