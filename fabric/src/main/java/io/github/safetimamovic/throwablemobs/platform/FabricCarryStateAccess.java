//? if >=1.20.5 {
// Before 1.20.5 Services uses the shared platform.legacy implementation instead.
package io.github.safetimamovic.throwablemobs.platform;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.carry.CarryState;
import io.github.safetimamovic.throwablemobs.platform.services.ICarryStateAccess;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public final class FabricCarryStateAccess implements ICarryStateAccess
{
        // Registered when Services loads this class during mod init.
        private static final AttachmentType<CarryState> CARRY_STATE = AttachmentRegistry.create(
                Constants.id("carry_state"),
                builder -> builder.persistent(CarryState.CODEC)
                        .syncWith(CarryState.STREAM_CODEC, AttachmentSyncPredicate.all()));

        @Override
        public @Nullable CarryState get(Entity entity)
        {
                return entity.getAttached(CARRY_STATE);
        }

        @Override
        public void set(Entity entity, CarryState state)
        {
                entity.setAttached(CARRY_STATE, state);
        }

        @Override
        public void remove(Entity entity)
        {
                entity.removeAttached(CARRY_STATE);
        }
}
//?}
