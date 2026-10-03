package io.github.safetimamovic.throwablemobs.carry;

import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * What the local player carries, as seen by the client. Updated each client tick by {@code CarryClient}. Lives outside
 * the client package and uses no client classes, so code shared by both sides can read it; on a dedicated server it
 * simply stays empty.
 */
public final class LocalCarry
{
        private static @Nullable Entity carried;

        private LocalCarry()
        {
        }

        public static @Nullable Entity get()
        {
                return carried;
        }

        public static void set(@Nullable Entity entity)
        {
                carried = entity;
        }
}
