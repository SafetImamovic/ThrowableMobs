package io.github.safetimamovic.throwablemobs.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.safetimamovic.throwablemobs.api.ImpactContext;
import io.github.safetimamovic.throwablemobs.api.ThrowContext;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.monster.Creeper;
import org.jspecify.annotations.Nullable;

/**
 * Optional conditions on a data-driven behavior, read from the same JSON object as its {@code type} and
 * {@code trigger}. All set conditions must hold.
 *
 * @param minImpactEnergy only impacts with at least this collision energy; ignored outside impacts
 * @param chance          0..1, rolled each time the trigger fires
 * @param chanceSetting   take {@code chance} from this server setting instead, so it can be changed in game
 * @param inWater         the mob must (or must not) be in water
 * @param isCharged       the mob must (or must not) be a charged creeper; non-creepers count as not charged
 * @param firstImpactOnly only the first impact of a flight, not later bounces
 */
public record Conditions(Optional<Double> minImpactEnergy, double chance, Optional<Boolean> inWater,
        Optional<Boolean> isCharged, boolean firstImpactOnly, Optional<ChanceSetting> chanceSetting)
{
        public static final MapCodec<Conditions> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.doubleRange(0.0, 1.0E9).optionalFieldOf("min_impact_energy")
                        .forGetter(Conditions::minImpactEnergy),
                Codec.doubleRange(0.0, 1.0).optionalFieldOf("chance", 1.0).forGetter(Conditions::chance),
                Codec.BOOL.optionalFieldOf("in_water").forGetter(Conditions::inWater),
                Codec.BOOL.optionalFieldOf("is_charged").forGetter(Conditions::isCharged),
                Codec.BOOL.optionalFieldOf("first_impact_only", false).forGetter(Conditions::firstImpactOnly),
                ChanceSetting.CODEC.optionalFieldOf("chance_setting").forGetter(Conditions::chanceSetting))
                .apply(instance, Conditions::new));

        /** @param impact the impact, or {@code null} outside impact triggers */
        public boolean test(ThrowContext ctx, @Nullable ImpactContext impact)
        {
                if (impact != null)
                {
                        if (minImpactEnergy.isPresent() && impact.energy() < minImpactEnergy.get())
                        {
                                return false;
                        }
                        if (firstImpactOnly && impact.impactIndex() > 0)
                        {
                                return false;
                        }
                }
                if (inWater.isPresent() && ctx.mob().isInWater() != inWater.get())
                {
                        return false;
                }
                if (isCharged.isPresent() && isCharged(ctx) != isCharged.get())
                {
                        return false;
                }
                // Rolled last, so a failed condition doesn't use up the roll.
                double chance = chanceSetting.map(ChanceSetting::value).orElse(this.chance);
                return chance >= 1.0 || ctx.level().getRandom().nextDouble() < chance;
        }

        /** Server settings a behavior can take its {@code chance} from. */
        public enum ChanceSetting implements StringRepresentable
        {
                /** {@code egg_chance}: a thrown chicken laying an egg. */
                EGG_CHANCE;

                public static final Codec<ChanceSetting> CODEC = StringRepresentable.fromEnum(ChanceSetting::values);

                double value()
                {
                        return switch (this)
                        {
                                case EGG_CHANCE -> ThrowableMobsConfig.behaviors().eggChance();
                        };
                }

                @Override
                public String getSerializedName()
                {
                        return name().toLowerCase(Locale.ROOT);
                }
        }

        private static boolean isCharged(ThrowContext ctx)
        {
                return ctx.mob() instanceof Creeper creeper && creeper.isPowered();
        }
}
