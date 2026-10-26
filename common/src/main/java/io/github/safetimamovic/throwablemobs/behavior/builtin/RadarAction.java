package io.github.safetimamovic.throwablemobs.behavior.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.api.BehaviorAction;
import io.github.safetimamovic.throwablemobs.api.BehaviorType;
import io.github.safetimamovic.throwablemobs.api.ThrowContext;
import io.github.safetimamovic.throwablemobs.mixin.LivingEntityInvoker;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import org.jspecify.annotations.Nullable;

/**
 * {@code throwablemobs:radar}: the mob panics when a monster comes near, louder in rhythm the closer the nearest one
 * is, like a Geiger counter. Use with {@code "trigger": "carry_tick"}. Monsters are anything vanilla counts as an
 * enemy ({@link Enemy}), so modded monsters work too.
 */
public record RadarAction(double range, int slowestIntervalTicks, int fastestIntervalTicks,
        Optional<SoundEvent> sound, float volume, boolean lineOfSight) implements BehaviorAction
{
        /** How often the area is searched. Intervals are rounded to multiples of this, so no state is needed. */
        private static final int SCAN_TICKS = 4;

        public static final MapCodec<RadarAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.doubleRange(1.0, 64.0).optionalFieldOf("range", 16.0).forGetter(RadarAction::range),
                Codec.intRange(SCAN_TICKS, 1200).optionalFieldOf("slowest_interval_ticks", 40)
                        .forGetter(RadarAction::slowestIntervalTicks),
                Codec.intRange(SCAN_TICKS, 1200).optionalFieldOf("fastest_interval_ticks", 8)
                        .forGetter(RadarAction::fastestIntervalTicks),
                BuiltInRegistries.SOUND_EVENT.byNameCodec().optionalFieldOf("sound").forGetter(RadarAction::sound),
                Codec.floatRange(0.0F, 10.0F).optionalFieldOf("volume", 1.0F).forGetter(RadarAction::volume),
                Codec.BOOL.optionalFieldOf("line_of_sight", false).forGetter(RadarAction::lineOfSight))
                .apply(instance, RadarAction::new));

        public static final BehaviorType<RadarAction> TYPE = new BehaviorType<>(Constants.id("radar"), CODEC);

        @Override
        public BehaviorType<?> type()
        {
                return TYPE;
        }

        @Override
        public void run(ThrowContext ctx)
        {
                ServerLevel level = ctx.level();
                long time = level.getGameTime();
                if (time % SCAN_TICKS != 0)
                {
                        return;
                }
                Mob mob = ctx.mob();
                LivingEntity nearest = nearestMonster(mob);
                if (nearest == null)
                {
                        return;
                }
                // 0 = right next to the mob, 1 = at the edge of the range.
                float far = (float) Mth.clamp(Math.sqrt(mob.distanceToSqr(nearest)) / range, 0.0, 1.0);
                int interval = Math.round(Mth.lerp(far, fastestIntervalTicks, slowestIntervalTicks) / SCAN_TICKS)
                        * SCAN_TICKS;
                if (time % Math.max(SCAN_TICKS, interval) != 0)
                {
                        return;
                }
                SoundEvent scream = sound.orElseGet(
                        () -> ((LivingEntityInvoker) mob).throwablemobs$getHurtSound(mob.damageSources().generic()));
                if (scream == null)
                {
                        return;
                }
                float pitch = 1.0F + 0.5F * (1.0F - far) + (level.getRandom().nextFloat() - 0.5F) * 0.1F;
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), scream, mob.getSoundSource(), volume, pitch);
        }

        private @Nullable LivingEntity nearestMonster(Mob mob)
        {
                double rangeSqr = range * range;
                LivingEntity nearest = null;
                double nearestSqr = Double.MAX_VALUE;
                for (LivingEntity entity : mob.level().getEntitiesOfClass(LivingEntity.class,
                        mob.getBoundingBox().inflate(range), entity -> entity instanceof Enemy && entity != mob
                                && entity.isAlive()))
                {
                        double distanceSqr = mob.distanceToSqr(entity);
                        if (distanceSqr <= rangeSqr && distanceSqr < nearestSqr
                                && (!lineOfSight || mob.hasLineOfSight(entity)))
                        {
                                nearest = entity;
                                nearestSqr = distanceSqr;
                        }
                }
                return nearest;
        }
}
