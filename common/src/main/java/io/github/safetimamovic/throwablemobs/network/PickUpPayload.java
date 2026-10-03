package io.github.safetimamovic.throwablemobs.network;

import io.github.safetimamovic.throwablemobs.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: "pick up this mob", sent by the dedicated pick-up key (sneak + use goes through vanilla's entity
 * interaction instead). The server re-checks reach and {@code CarryRules}.
 *
 * @param entityId network id of the mob the client is looking at
 */
public record PickUpPayload(int entityId) implements CustomPacketPayload
{
        public static final CustomPacketPayload.Type<PickUpPayload> TYPE = new CustomPacketPayload.Type<>(
                Constants.id("pick_up"));

        public static final StreamCodec<RegistryFriendlyByteBuf, PickUpPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, PickUpPayload::entityId,
                PickUpPayload::new);

        @Override
        public CustomPacketPayload.Type<PickUpPayload> type()
        {
                return TYPE;
        }
}
