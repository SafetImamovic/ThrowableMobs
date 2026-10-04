package io.github.safetimamovic.throwablemobs.platform;

import io.github.safetimamovic.throwablemobs.carry.ThrowState;
import io.github.safetimamovic.throwablemobs.platform.services.IThrowStateAccess;
import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jspecify.annotations.Nullable;

public final class NeoForgeThrowStateAccess implements IThrowStateAccess
{
        // Same register as the carry state; no meaningful default, only ever read with getExistingDataOrNull.
        private static final Supplier<AttachmentType<ThrowState>> THROW_STATE = NeoForgeCarryStateAccess.ATTACHMENT_TYPES
                .register("throw_state", () -> AttachmentType.<ThrowState>builder(() -> null)
                        .serialize(ThrowState.MAP_CODEC)
                        .sync(ThrowState.STREAM_CODEC)
                        .build());

        @Override
        public @Nullable ThrowState get(Entity entity)
        {
                return entity.getExistingDataOrNull(THROW_STATE);
        }

        @Override
        public void set(Entity entity, ThrowState state)
        {
                entity.setData(THROW_STATE, state);
        }

        @Override
        public void remove(Entity entity)
        {
                entity.removeData(THROW_STATE);
        }
}
