package io.github.safetimamovic.throwablemobs.effect;

import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.EffectsConfig;
import io.github.safetimamovic.throwablemobs.physics.ThrowPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Sounds and particle bursts around a throw, sent by the server so everyone nearby gets them. Louder and bigger the
 * harder it was: charge for throws, collision energy for bounces and impacts, momentum for catches. Off with the
 * creator settings {@code sounds} / {@code particles}.
 */
public final class ThrowEffects
{
        /** Momentum (mass × speed, game-kg × blocks/tick) for a full-strength catch. */
        private static final double FULL_CATCH_MOMENTUM = 200.0;

        private ThrowEffects()
        {
        }

        public static void thrown(ServerLevel level, Mob mob, double charge)
        {
                EffectsConfig config = ThrowableMobsConfig.effects();
                float strength = (float) charge;
                sound(level, mob, ModSounds.THROW, config, 0.4F + 0.6F * strength, 0.8F + 0.5F * strength);
                particles(level, config, ParticleTypes.POOF, center(mob), 2 + Math.round(4 * strength), 0.2);
        }

        /**
         * A block hit the mob bounces off.
         *
         * @param into the velocity into the block
         */
        public static void bounced(ServerLevel level, Mob mob, Vec3 into, double energy)
        {
                blockHit(level, mob, into, energy, ModSounds.BOUNCE);
        }

        /**
         * A block hit that ends the flight.
         *
         * @param into the velocity into the block
         */
        public static void landed(ServerLevel level, Mob mob, Vec3 into, double energy)
        {
                blockHit(level, mob, into, energy, ModSounds.IMPACT);
        }

        /** The mob hit an entity. */
        public static void hitEntity(ServerLevel level, Mob mob, Entity target, double energy)
        {
                EffectsConfig config = ThrowableMobsConfig.effects();
                double strength = ThrowPhysics.effectStrength(energy, config.minEnergy(), config.fullEnergy());
                if (strength <= 0.0)
                {
                        return;
                }
                sound(level, mob, ModSounds.IMPACT, config, (float) (0.3 + 0.7 * strength), 1.0F);
                if (strength >= 0.5)
                {
                        particles(level, config, ParticleTypes.CRIT, center(target), (int) Math.round(10 * strength),
                                0.3);
                }
        }

        public static void caught(ServerLevel level, Mob mob, double momentum)
        {
                EffectsConfig config = ThrowableMobsConfig.effects();
                double strength = ThrowPhysics.effectStrength(momentum, 0.0, FULL_CATCH_MOMENTUM);
                sound(level, mob, ModSounds.CATCH, config, (float) (0.5 + 0.5 * strength),
                        (float) (1.1 - 0.3 * strength));
                particles(level, config, ParticleTypes.CLOUD, center(mob), 2 + (int) Math.round(4 * strength), 0.25);
        }

        private static void blockHit(ServerLevel level, Mob mob, Vec3 into, double energy, SoundEvent sound)
        {
                EffectsConfig config = ThrowableMobsConfig.effects();
                double strength = ThrowPhysics.effectStrength(energy, config.minEnergy(), config.fullEnergy());
                if (strength <= 0.0)
                {
                        return;
                }
                sound(level, mob, sound, config, (float) (0.3 + 0.7 * strength), 0.9F + 0.2F * level.getRandom()
                        .nextFloat());
                if (into.lengthSqr() == 0.0)
                {
                        return;
                }
                // The block it hit: just past the hitbox, the way it was going.
                double reach = Math.max(mob.getBbWidth(), mob.getBbHeight()) / 2.0 + 0.3;
                Vec3 at = center(mob).add(into.normalize().scale(reach));
                BlockState block = level.getBlockState(BlockPos.containing(at));
                if (!block.isAir())
                {
                        particles(level, config, new BlockParticleOption(ParticleTypes.BLOCK, block), at,
                                4 + (int) Math.round(16 * strength), 0.3);
                }
        }

        private static void sound(ServerLevel level, Mob mob, SoundEvent sound, EffectsConfig config, float volume,
                float pitch)
        {
                if (config.sounds())
                {
                        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), sound, SoundSource.NEUTRAL, volume,
                                pitch);
                }
        }

        private static void particles(ServerLevel level, EffectsConfig config, ParticleOptions particle, Vec3 at,
                int count, double spread)
        {
                if (config.particles())
                {
                        level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, 0.02);
                }
        }

        private static Vec3 center(Entity entity)
        {
                return entity.getBoundingBox().getCenter();
        }
}
