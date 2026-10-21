package io.github.safetimamovic.throwablemobs.carry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Attached to a mob while a player carries it. Saved with the mob, so a mob that loads with this state but no
 * online carrier (server crash, restart) gets restored, and synced to every client tracking the mob so they can
 * lock it to the carrier and show the right prompt.
 *
 * @param carrier           UUID of the carrying player
 * @param originalNoAi      the mob's {@code NoAI} flag before pickup, restored on drop
 * @param originalNoGravity the mob's {@code NoGravity} flag before pickup, restored on drop
 * @param pickupTick        game time of the pickup
 * @param waitingSince      game time the carrier logged out, for a mob waiting for them
 *                          ({@code KEEP_CARRYING}); -1 while carried
 */
public record CarryState(UUID carrier, boolean originalNoAi, boolean originalNoGravity, long pickupTick,
        long waitingSince)
{
        public static final MapCodec<CarryState> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("carrier").forGetter(CarryState::carrier),
                Codec.BOOL.fieldOf("original_no_ai").forGetter(CarryState::originalNoAi),
                Codec.BOOL.fieldOf("original_no_gravity").forGetter(CarryState::originalNoGravity),
                Codec.LONG.fieldOf("pickup_tick").forGetter(CarryState::pickupTick),
                Codec.LONG.optionalFieldOf("waiting_since", -1L).forGetter(CarryState::waitingSince))
                .apply(instance, CarryState::new));

        public static final Codec<CarryState> CODEC = MAP_CODEC.codec();

        public static final StreamCodec<RegistryFriendlyByteBuf, CarryState> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, CarryState::carrier,
                ByteBufCodecs.BOOL, CarryState::originalNoAi,
                ByteBufCodecs.BOOL, CarryState::originalNoGravity,
                ByteBufCodecs.VAR_LONG, CarryState::pickupTick,
                ByteBufCodecs.VAR_LONG, CarryState::waitingSince,
                CarryState::new);

        /** A new carry. */
        public CarryState(UUID carrier, boolean originalNoAi, boolean originalNoGravity, long pickupTick)
        {
                this(carrier, originalNoAi, originalNoGravity, pickupTick, -1L);
        }

        public boolean isWaiting()
        {
                return waitingSince >= 0L;
        }

        /** The same carry, now waiting for its carrier since {@code gameTime}. */
        public CarryState waitingFrom(long gameTime)
        {
                return new CarryState(carrier, originalNoAi, originalNoGravity, pickupTick, gameTime);
        }

        /** The same carry, carried again. */
        public CarryState resumed()
        {
                return new CarryState(carrier, originalNoAi, originalNoGravity, pickupTick, -1L);
        }
}
