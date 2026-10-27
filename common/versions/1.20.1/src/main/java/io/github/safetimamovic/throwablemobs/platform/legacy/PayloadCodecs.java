package io.github.safetimamovic.throwablemobs.platform.legacy;

import io.github.safetimamovic.throwablemobs.network.codec.CustomPacketPayload;
import io.github.safetimamovic.throwablemobs.network.codec.StreamCodec;
import io.netty.buffer.Unpooled;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Every payload's codec by channel, for 1.20.1, where payloads go over plain custom-payload channels: each loader
 * registers its payloads here and reads and writes them through it.
 */
public final class PayloadCodecs
{
        private static final Map<ResourceLocation, StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload>> CODECS = new ConcurrentHashMap<>();

        private PayloadCodecs()
        {
        }

        public static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type,
                StreamCodec<? super FriendlyByteBuf, T> codec)
        {
                CODECS.put(type.id(), codec);
        }

        @SuppressWarnings("unchecked")
        public static FriendlyByteBuf write(CustomPacketPayload payload)
        {
                StreamCodec<? super FriendlyByteBuf, CustomPacketPayload> codec = (StreamCodec<? super FriendlyByteBuf, CustomPacketPayload>) codec(
                        payload.type().id());
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                codec.encode(buf, payload);
                return buf;
        }

        public static CustomPacketPayload read(ResourceLocation channel, FriendlyByteBuf buf)
        {
                return codec(channel).decode(buf);
        }

        private static StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload> codec(ResourceLocation channel)
        {
                StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload> codec = CODECS.get(channel);
                if (codec == null)
                {
                        throw new IllegalStateException("No payload registered on " + channel);
                }
                return codec;
        }
}
