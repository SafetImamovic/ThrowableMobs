package io.github.safetimamovic.throwablemobs.network;

import io.github.safetimamovic.throwablemobs.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: the player's own {@code enabled} setting. Only sent once it's off, and again when it's turned back
 * on, so the server treats players it never heard from as enabled. Off: the player can't pick up, throw or catch, and
 * whatever they carry is put down.
 */
public record PlayerEnabledPayload(boolean enabled) implements CustomPacketPayload
{
        public static final CustomPacketPayload.Type<PlayerEnabledPayload> TYPE = new CustomPacketPayload.Type<>(
                Constants.id("player_enabled"));

        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerEnabledPayload> STREAM_CODEC = StreamCodec
                .composite(ByteBufCodecs.BOOL, PlayerEnabledPayload::enabled, PlayerEnabledPayload::new);

        @Override
        public CustomPacketPayload.Type<PlayerEnabledPayload> type()
        {
                return TYPE;
        }
}
