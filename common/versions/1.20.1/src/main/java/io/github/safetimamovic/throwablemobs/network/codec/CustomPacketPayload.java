package io.github.safetimamovic.throwablemobs.network.codec;

import net.minecraft.resources.ResourceLocation;

/**
 * Minecraft's {@code CustomPacketPayload} (1.20.5+) for 1.20.1, which has no payload type: each loader sends these
 * on a channel named after {@link Type#id()}, encoded with the payload's {@code STREAM_CODEC}.
 */
public interface CustomPacketPayload
{
        Type<? extends CustomPacketPayload> type();

        record Type<T extends CustomPacketPayload>(ResourceLocation id)
        {
        }
}
