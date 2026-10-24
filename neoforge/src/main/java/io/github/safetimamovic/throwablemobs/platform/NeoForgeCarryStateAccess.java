package io.github.safetimamovic.throwablemobs.platform;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.carry.CarryState;
import io.github.safetimamovic.throwablemobs.platform.services.ICarryStateAccess;
import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jspecify.annotations.Nullable;

public final class NeoForgeCarryStateAccess implements ICarryStateAccess
{
        /** Registered on the mod bus by {@code ThrowableMobsNeoForge}. */
        public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister
                .create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Constants.MOD_ID);

        // No meaningful default: only ever read with getExistingDataOrNull.
        private static final Supplier<AttachmentType<CarryState>> CARRY_STATE = ATTACHMENT_TYPES.register(
                "carry_state",
                () -> AttachmentType.<CarryState>builder(() -> null)
                        //? if >=26.1 {
                        .serialize(CarryState.MAP_CODEC)
                        //?} else
                        /*.serialize(CarryState.CODEC)*/
                        .sync(CarryState.STREAM_CODEC)
                        .build());

        @Override
        public @Nullable CarryState get(Entity entity)
        {
                return entity.getExistingDataOrNull(CARRY_STATE);
        }

        @Override
        public void set(Entity entity, CarryState state)
        {
                entity.setData(CARRY_STATE, state);
        }

        @Override
        public void remove(Entity entity)
        {
                entity.removeData(CARRY_STATE);
        }
}
