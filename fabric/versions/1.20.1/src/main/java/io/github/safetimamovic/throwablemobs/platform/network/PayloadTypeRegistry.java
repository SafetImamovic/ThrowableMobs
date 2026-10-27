package io.github.safetimamovic.throwablemobs.platform.network;

import io.github.safetimamovic.throwablemobs.network.codec.CustomPacketPayload;
import io.github.safetimamovic.throwablemobs.network.codec.StreamCodec;
import io.github.safetimamovic.throwablemobs.platform.legacy.PayloadCodecs;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Fabric API's {@code PayloadTypeRegistry} (1.20.5+) for 1.20.1, where payloads are plain channels: Stonecutter points
 * the sources' import here, see {@link ServerPlayNetworking}.
 */
public final class PayloadTypeRegistry
{
        private static final PayloadTypeRegistry INSTANCE = new PayloadTypeRegistry();

        private PayloadTypeRegistry()
        {
        }

        public static PayloadTypeRegistry playC2S()
        {
                return INSTANCE;
        }

        public static PayloadTypeRegistry playS2C()
        {
                return INSTANCE;
        }

        public <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type,
                StreamCodec<? super FriendlyByteBuf, T> codec)
        {
                PayloadCodecs.register(type, codec);
        }
}
