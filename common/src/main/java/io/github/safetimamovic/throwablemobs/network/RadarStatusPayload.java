package io.github.safetimamovic.throwablemobs.network;

import io.github.safetimamovic.throwablemobs.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → carrier, on every radar scan of the mob they carry: what it senses, for the carry HUD. Radars are server
 * side behaviors, so the client can't work this out itself.
 *
 * @param mobId    network id of the carried mob
 * @param targetId network id of the nearest monster, or -1 if none is in range
 * @param distance blocks to that monster, 0 if none
 * @param range    the radar's range in blocks
 * @param pinged   the mob screamed on this scan
 */
public record RadarStatusPayload(int mobId, int targetId, float distance, float range, boolean pinged)
        implements CustomPacketPayload
{
        public static final CustomPacketPayload.Type<RadarStatusPayload> TYPE = new CustomPacketPayload.Type<>(
                Constants.id("radar_status"));

        public static final StreamCodec<RegistryFriendlyByteBuf, RadarStatusPayload> STREAM_CODEC = StreamCodec
                .composite(
                        ByteBufCodecs.VAR_INT, RadarStatusPayload::mobId,
                        ByteBufCodecs.VAR_INT, RadarStatusPayload::targetId,
                        ByteBufCodecs.FLOAT, RadarStatusPayload::distance,
                        ByteBufCodecs.FLOAT, RadarStatusPayload::range,
                        ByteBufCodecs.BOOL, RadarStatusPayload::pinged,
                        RadarStatusPayload::new);

        @Override
        public CustomPacketPayload.Type<RadarStatusPayload> type()
        {
                return TYPE;
        }
}
