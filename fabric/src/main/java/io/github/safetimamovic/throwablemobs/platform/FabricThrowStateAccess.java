//? if >=1.20.5 {
// Before 1.20.5 Services uses the shared platform.legacy implementation instead.
package io.github.safetimamovic.throwablemobs.platform;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.carry.ThrowState;
import io.github.safetimamovic.throwablemobs.platform.services.IThrowStateAccess;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public final class FabricThrowStateAccess implements IThrowStateAccess
{
        // Registered when Services loads this class during mod init.
        private static final AttachmentType<ThrowState> THROW_STATE = AttachmentRegistry.create(
                Constants.id("throw_state"),
                builder -> builder.persistent(ThrowState.CODEC)
                        .syncWith(ThrowState.STREAM_CODEC, AttachmentSyncPredicate.all()));

        @Override
        public @Nullable ThrowState get(Entity entity)
        {
                return entity.getAttached(THROW_STATE);
        }

        @Override
        public void set(Entity entity, ThrowState state)
        {
                entity.setAttached(THROW_STATE, state);
        }

        @Override
        public void remove(Entity entity)
        {
                entity.removeAttached(THROW_STATE);
        }
}
//?}
