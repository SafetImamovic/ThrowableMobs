package io.github.safetimamovic.throwablemobs.behavior.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.api.BehaviorAction;
import io.github.safetimamovic.throwablemobs.api.BehaviorType;
import io.github.safetimamovic.throwablemobs.api.ImpactContext;
import io.github.safetimamovic.throwablemobs.api.ImpactResult;
import io.github.safetimamovic.throwablemobs.api.ThrowContext;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/**
 * {@code throwablemobs:explode}: an explosion at the mob. Power {@code base + per_energy × impact energy}, capped at
 * {@code max}, times {@code charged_multiplier} for a charged creeper. Block damage follows the {@code mobGriefing}
 * gamerule, like a creeper's. With {@code consume_mob} the mob is gone afterwards, like a creeper that blew up.
 */
public record ExplodeAction(float base, float perEnergy, float max, float chargedMultiplier, boolean fire,
        boolean consumeMob) implements BehaviorAction
{
        public static final MapCodec<ExplodeAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.floatRange(0.0F, 100.0F).optionalFieldOf("base", 1.5F).forGetter(ExplodeAction::base),
                Codec.floatRange(0.0F, 10.0F).optionalFieldOf("per_energy", 0.01F).forGetter(ExplodeAction::perEnergy),
                Codec.floatRange(0.0F, 100.0F).optionalFieldOf("max", 4.0F).forGetter(ExplodeAction::max),
                Codec.floatRange(0.0F, 10.0F).optionalFieldOf("charged_multiplier", 2.0F)
                        .forGetter(ExplodeAction::chargedMultiplier),
                Codec.BOOL.optionalFieldOf("fire", false).forGetter(ExplodeAction::fire),
                Codec.BOOL.optionalFieldOf("consume_mob", true).forGetter(ExplodeAction::consumeMob))
                .apply(instance, ExplodeAction::new));

        public static final BehaviorType<ExplodeAction> TYPE = new BehaviorType<>(Constants.id("explode"), CODEC);

        @Override
        public BehaviorType<?> type()
        {
                return TYPE;
        }

        @Override
        public void run(ThrowContext ctx)
        {
                explode(ctx, 0.0);
        }

        @Override
        public ImpactResult onImpact(ImpactContext ctx, ImpactResult current)
        {
                explode(ctx, ctx.energy());
                return current.land();
        }

        private void explode(ThrowContext ctx, double energy)
        {
                Mob mob = ctx.mob();
                float power = Math.min(base + perEnergy * (float) energy, max);
                if (mob instanceof Creeper creeper && creeper.isPowered())
                {
                        power *= chargedMultiplier;
                }
                ctx.level().explode(mob, mob.getX(), mob.getY(), mob.getZ(), power, fire,
                        Level.ExplosionInteraction.MOB);
                if (consumeMob)
                {
                        mob.discard();
                }
        }
}
