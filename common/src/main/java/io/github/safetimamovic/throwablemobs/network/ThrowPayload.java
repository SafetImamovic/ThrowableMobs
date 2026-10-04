package io.github.safetimamovic.throwablemobs.network;

import io.github.safetimamovic.throwablemobs.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: "let go, throw it" (Throw key released). The server works out the charge from the
 * time since {@link ChargeThrowPayload} and re-checks that the sender still carries the mob.
 */
public record ThrowPayload() implements CustomPacketPayload
{
        public static final ThrowPayload INSTANCE = new ThrowPayload();

        public static final CustomPacketPayload.Type<ThrowPayload> TYPE = new CustomPacketPayload.Type<>(
                Constants.id("throw"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ThrowPayload> STREAM_CODEC = StreamCodec
                .unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<ThrowPayload> type()
        {
                return TYPE;
        }
}
