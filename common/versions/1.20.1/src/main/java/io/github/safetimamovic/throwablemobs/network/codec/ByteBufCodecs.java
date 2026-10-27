package io.github.safetimamovic.throwablemobs.network.codec;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;

/** The part of Minecraft's {@code ByteBufCodecs} (1.20.5+) the mod uses, for 1.20.1; see {@link StreamCodec}. */
public final class ByteBufCodecs
{
        public static final StreamCodec<FriendlyByteBuf, Boolean> BOOL = StreamCodec.of(FriendlyByteBuf::writeBoolean,
                FriendlyByteBuf::readBoolean);
        public static final StreamCodec<FriendlyByteBuf, Integer> VAR_INT = StreamCodec.of(FriendlyByteBuf::writeVarInt,
                FriendlyByteBuf::readVarInt);
        public static final StreamCodec<FriendlyByteBuf, Long> VAR_LONG = StreamCodec.of(FriendlyByteBuf::writeVarLong,
                FriendlyByteBuf::readVarLong);
        public static final StreamCodec<FriendlyByteBuf, Float> FLOAT = StreamCodec.of(FriendlyByteBuf::writeFloat,
                FriendlyByteBuf::readFloat);
        public static final StreamCodec<FriendlyByteBuf, Double> DOUBLE = StreamCodec.of(FriendlyByteBuf::writeDouble,
                FriendlyByteBuf::readDouble);
        /** {@code UUIDUtil.STREAM_CODEC} from 1.20.5 on. */
        public static final StreamCodec<FriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(FriendlyByteBuf::writeUUID,
                FriendlyByteBuf::readUUID);

        private ByteBufCodecs()
        {
        }

        public static <B extends FriendlyByteBuf, V> StreamCodec<B, Optional<V>> optional(StreamCodec<? super B, V> codec)
        {
                return StreamCodec.of((buf, value) -> buf.writeOptional(value, (b, v) -> codec.encode(buf, v)),
                        buf -> buf.readOptional(b -> codec.decode(buf)));
        }

        public static <B extends FriendlyByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
                IntFunction<? extends M> factory, StreamCodec<? super B, K> keyCodec,
                StreamCodec<? super B, V> valueCodec)
        {
                return StreamCodec.of((buf, map) ->
                {
                        buf.writeVarInt(map.size());
                        map.forEach((key, value) ->
                        {
                                keyCodec.encode(buf, key);
                                valueCodec.encode(buf, value);
                        });
                }, buf ->
                {
                        int size = buf.readVarInt();
                        M map = factory.apply(size);
                        for (int i = 0; i < size; i++)
                        {
                                K key = keyCodec.decode(buf);
                                map.put(key, valueCodec.decode(buf));
                        }
                        return map;
                });
        }

        /** By network ID, like the 1.20.5+ one; built-in registries only, which is all the mod sends. */
        @SuppressWarnings("unchecked")
        public static <B extends FriendlyByteBuf, T> StreamCodec<B, T> registry(
                ResourceKey<? extends Registry<T>> key)
        {
                Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.getOrThrow((ResourceKey) key);
                return StreamCodec.of((buf, value) -> buf.writeId(registry, value), buf -> buf.readById(registry));
        }
}
