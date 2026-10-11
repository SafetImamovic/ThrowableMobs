package io.github.safetimamovic.throwablemobs.behavior.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.api.BehaviorAction;
import io.github.safetimamovic.throwablemobs.api.BehaviorType;
import io.github.safetimamovic.throwablemobs.api.ThrowContext;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Mob;

/**
 * {@code throwablemobs:particles}: {@code count} particles around the middle of the mob, spread over {@code spread}
 * blocks. Use with {@code "trigger": "flight_tick"} for a trail.
 */
public record ParticlesAction(ParticleOptions particle, int count, double spread, double speed)
        implements BehaviorAction
{
        public static final MapCodec<ParticlesAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ParticleTypes.CODEC.fieldOf("particle").forGetter(ParticlesAction::particle),
                Codec.intRange(1, 1000).optionalFieldOf("count", 4).forGetter(ParticlesAction::count),
                Codec.doubleRange(0.0, 16.0).optionalFieldOf("spread", 0.2).forGetter(ParticlesAction::spread),
                Codec.doubleRange(0.0, 10.0).optionalFieldOf("speed", 0.0).forGetter(ParticlesAction::speed))
                .apply(instance, ParticlesAction::new));

        public static final BehaviorType<ParticlesAction> TYPE = new BehaviorType<>(Constants.id("particles"), CODEC);

        @Override
        public BehaviorType<?> type()
        {
                return TYPE;
        }

        @Override
        public void run(ThrowContext ctx)
        {
                Mob mob = ctx.mob();
                ctx.level().sendParticles(particle, mob.getX(), mob.getY() + mob.getBbHeight() / 2.0, mob.getZ(), count,
                        spread, spread, spread, speed);
        }
}
