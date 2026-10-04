package io.github.safetimamovic.throwablemobs.platform.services;

import io.github.safetimamovic.throwablemobs.carry.ThrowState;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * Reads and writes the {@link ThrowState} attachment. Each loader backs this with its own data attachment API; both
 * persist the state with the entity and sync it to tracking clients.
 */
public interface IThrowStateAccess
{
        @Nullable
        ThrowState get(Entity entity);

        void set(Entity entity, ThrowState state);

        void remove(Entity entity);
}
