package io.github.safetimamovic.throwablemobs.network;

import io.github.safetimamovic.throwablemobs.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: "drop what I'm carrying". Needed because right-clicking air with an empty hand sends no packet in
 * vanilla. The server re-checks that the sender is carrying something.
 */
public record DropCarriedPayload() implements CustomPacketPayload
{
        public static final DropCarriedPayload INSTANCE = new DropCarriedPayload();

        public static final CustomPacketPayload.Type<DropCarriedPayload> TYPE = new CustomPacketPayload.Type<>(
                Constants.id("drop_carried"));

        public static final StreamCodec<RegistryFriendlyByteBuf, DropCarriedPayload> STREAM_CODEC = StreamCodec
                .unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<DropCarriedPayload> type()
        {
                return TYPE;
        }
}
