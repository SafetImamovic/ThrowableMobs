package io.github.safetimamovic.throwablemobs.api;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;

/**
 * A kind of {@link BehaviorAction} that mob profiles can use by id: {@code {"type": "throwablemobs:explode", ...}}.
 * The codec reads the fields next to {@code "type"}. Register with {@link ThrowableMobsApi#registerBehaviorType}.
 */
public record BehaviorType<A extends BehaviorAction>(Identifier id, MapCodec<A> codec)
{
}
