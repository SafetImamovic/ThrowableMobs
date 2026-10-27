package io.github.safetimamovic.throwablemobs.platform.legacy;

import io.github.safetimamovic.throwablemobs.carry.CarryState;
import io.github.safetimamovic.throwablemobs.platform.services.ICarryStateAccess;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/** {@link ICarryStateAccess} for 1.20.1, on both loaders; see {@link EntityStates}. */
public final class LegacyCarryStateAccess implements ICarryStateAccess
{
        @Override
        public @Nullable CarryState get(Entity entity)
        {
                return ((EntityStates) entity).throwablemobs$carryState();
        }

        @Override
        public void set(Entity entity, CarryState state)
        {
                ((EntityStates) entity).throwablemobs$setCarryState(state);
                EntityStateSyncPayload.broadcast(entity);
        }

        @Override
        public void remove(Entity entity)
        {
                if (get(entity) != null)
                {
                        ((EntityStates) entity).throwablemobs$setCarryState(null);
                        EntityStateSyncPayload.broadcast(entity);
                }
        }
}
