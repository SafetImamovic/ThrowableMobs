package io.github.safetimamovic.throwablemobs.platform.services;

import io.github.safetimamovic.throwablemobs.carry.CarryState;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * Reads and writes the {@link CarryState} attachment. Each loader backs this with its own data attachment API
 * (Fabric Data Attachment API, NeoForge {@code AttachmentType}); both persist the state with the entity and sync it to
 * tracking clients.
 */
public interface ICarryStateAccess
{
        @Nullable
        CarryState get(Entity entity);

        void set(Entity entity, CarryState state);

        void remove(Entity entity);
}
