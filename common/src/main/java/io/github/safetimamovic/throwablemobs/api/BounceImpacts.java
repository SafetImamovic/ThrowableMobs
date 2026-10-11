package io.github.safetimamovic.throwablemobs.api;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** Whether bounces after the first impact deal damage and fire impact behaviors. */
public enum BounceImpacts implements StringRepresentable
{
        EVERY,
        FIRST_ONLY;

        public static final Codec<BounceImpacts> CODEC = StringRepresentable.fromEnum(BounceImpacts::values);

        @Override
        public String getSerializedName()
        {
                return name().toLowerCase(Locale.ROOT);
        }
}
