package io.github.safetimamovic.throwablemobs.carry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * Attached to a mob while it flies after a throw. Saved with the mob, so a mob that loads mid-flight (chunk unload,
 * server stop) gets its original state back. Not synced: clients just see the server move the mob.
 *
 * @param thrower           UUID of the throwing player, credited for kills
 * @param originalNoAi      the mob's {@code NoAI} flag before it was picked up, restored when the flight ends
 * @param originalNoGravity the mob's {@code NoGravity} flag before it was picked up, restored when the flight ends
 * @param launchTick        game time of the throw
 */
public record ThrowState(UUID thrower, boolean originalNoAi, boolean originalNoGravity, long launchTick)
{
        public static final MapCodec<ThrowState> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("thrower").forGetter(ThrowState::thrower),
                Codec.BOOL.fieldOf("original_no_ai").forGetter(ThrowState::originalNoAi),
                Codec.BOOL.fieldOf("original_no_gravity").forGetter(ThrowState::originalNoGravity),
                Codec.LONG.fieldOf("launch_tick").forGetter(ThrowState::launchTick))
                .apply(instance, ThrowState::new));

        public static final Codec<ThrowState> CODEC = MAP_CODEC.codec();
}
