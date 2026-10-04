package io.github.safetimamovic.throwablemobs.network;

import io.github.safetimamovic.throwablemobs.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: "I started charging a throw" (Throw key pressed while carrying). The server notes the time;
 * it measures the charge itself and never trusts a charge sent by the client.
 */
public record ChargeThrowPayload() implements CustomPacketPayload
{
        public static final ChargeThrowPayload INSTANCE = new ChargeThrowPayload();

        public static final CustomPacketPayload.Type<ChargeThrowPayload> TYPE = new CustomPacketPayload.Type<>(
                Constants.id("charge_throw"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChargeThrowPayload> STREAM_CODEC = StreamCodec
                .unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<ChargeThrowPayload> type()
        {
                return TYPE;
        }
}
