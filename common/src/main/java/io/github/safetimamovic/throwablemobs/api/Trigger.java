package io.github.safetimamovic.throwablemobs.api;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** When a data-driven behavior runs, the {@code "trigger"} field of a behavior in a mob profile. */
public enum Trigger implements StringRepresentable
{
        PICKUP,
        CARRY_TICK,
        THROW,
        FLIGHT_TICK,
        /** Any impact, block or entity. */
        IMPACT,
        IMPACT_ENTITY,
        IMPACT_BLOCK,
        LAND;

        public static final Codec<Trigger> CODEC = StringRepresentable.fromEnum(Trigger::values);

        @Override
        public String getSerializedName()
        {
                return name().toLowerCase(Locale.ROOT);
        }
}
