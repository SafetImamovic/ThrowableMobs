package io.github.safetimamovic.throwablemobs.carry;

import io.github.safetimamovic.throwablemobs.network.RadarStatusPayload;
import org.jspecify.annotations.Nullable;

/**
 * The client's last {@link RadarStatusPayload}. Kept here rather than in the client package because the payload
 * handler is registered on both sides; only the client ever fills it.
 */
public final class RadarStatus
{
        /** Scans come every 4 ticks; a few missed ones mean the radar stopped (put down, turned off). */
        private static final long STALE_TICKS = 12;

        private static @Nullable RadarStatusPayload last;
        private static long receivedAt;
        private static long pingedAt = Long.MIN_VALUE;

        private RadarStatus()
        {
        }

        public static void onStatus(RadarStatusPayload payload, long gameTime)
        {
                last = payload;
                receivedAt = gameTime;
                if (payload.pinged())
                {
                        pingedAt = gameTime;
                }
        }

        /** The status for {@code mobId}, or {@code null} if that mob has no radar running. */
        public static @Nullable RadarStatusPayload of(int mobId, long gameTime)
        {
                RadarStatusPayload status = last;
                return status != null && status.mobId() == mobId && gameTime - receivedAt <= STALE_TICKS ? status : null;
        }

        /** Ticks since the carried mob last screamed. */
        public static long sincePing(long gameTime)
        {
                return gameTime - pingedAt;
        }
}
