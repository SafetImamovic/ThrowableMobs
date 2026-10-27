package io.github.safetimamovic.throwablemobs.platform.legacy;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.carry.CarryState;
import io.github.safetimamovic.throwablemobs.carry.ThrowState;
import io.github.safetimamovic.throwablemobs.network.codec.ByteBufCodecs;
import io.github.safetimamovic.throwablemobs.network.codec.CustomPacketPayload;
import io.github.safetimamovic.throwablemobs.network.codec.StreamCodec;
import io.github.safetimamovic.throwablemobs.platform.Services;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Server → client (1.20.1 only): an entity's carry and throw state, what the synced attachments do on newer versions.
 * Sent when either changes and when a player starts tracking the entity.
 */
public record EntityStateSyncPayload(int entityId, Optional<CarryState> carry, Optional<ThrowState> thrown)
        implements CustomPacketPayload
{
        public static final CustomPacketPayload.Type<EntityStateSyncPayload> TYPE = new CustomPacketPayload.Type<>(
                Constants.id("entity_state"));

        public static final StreamCodec<FriendlyByteBuf, EntityStateSyncPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, EntityStateSyncPayload::entityId,
                ByteBufCodecs.optional(CarryState.STREAM_CODEC), EntityStateSyncPayload::carry,
                ByteBufCodecs.optional(ThrowState.STREAM_CODEC), EntityStateSyncPayload::thrown,
                EntityStateSyncPayload::new);

        @Override
        public CustomPacketPayload.Type<EntityStateSyncPayload> type()
        {
                return TYPE;
        }

        static EntityStateSyncPayload of(Entity entity)
        {
                EntityStates states = (EntityStates) entity;
                return new EntityStateSyncPayload(entity.getId(), Optional.ofNullable(states.throwablemobs$carryState()),
                        Optional.ofNullable(states.throwablemobs$throwState()));
        }

        /** After the state changed on the server: to everyone who sees the entity. */
        static void broadcast(Entity entity)
        {
                if (!entity.level().isClientSide())
                {
                        Services.PLATFORM.sendToTracking(entity, of(entity));
                }
        }

        /** A player started seeing {@code entity}: its state, if it has any. */
        public static void onStartTracking(Entity entity, ServerPlayer player)
        {
                EntityStates states = (EntityStates) entity;
                if (states.throwablemobs$carryState() != null || states.throwablemobs$throwState() != null)
                {
                        Services.PLATFORM.sendToPlayer(player, of(entity));
                }
        }
}
