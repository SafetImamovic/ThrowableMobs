package io.github.safetimamovic.throwablemobs.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContextSource;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * {@code throwablemobs:throw_event}: one criterion for everything around a throw, told apart by {@code event}. Optional
 * conditions: {@code entity} (the mob, a loot condition like vanilla's {@code tame_animal}), {@code min_charge}
 * (throws, 0 – 1), {@code min_distance} (landings, blocks from the launch point), {@code ignited} (a lit creeper).
 */
public class ThrowEventTrigger extends SimpleCriterionTrigger<ThrowEventTrigger.TriggerInstance>
{
        @Override
        public Codec<TriggerInstance> codec()
        {
                return TriggerInstance.CODEC;
        }

        /**
         * @param charge   throw charge 0 – 1, 0 for other events
         * @param distance blocks from the launch point, 0 for other events
         */
        public void trigger(ServerPlayer player, Event event, Mob mob, double charge, double distance)
        {
                LootContext mobContext = EntityPredicate.createContext(player, mob);
                boolean ignited = mob instanceof Creeper creeper && (creeper.isIgnited() || creeper.getSwellDir() > 0);
                trigger(player, instance -> instance.matches(event, mobContext, charge, distance, ignited));
        }

        /** What happened. */
        public enum Event implements StringRepresentable
        {
                /** A player picked the mob up. */
                PICKUP,
                /** A player threw it. */
                THROW,
                /** A player caught it in the air. */
                CATCH,
                /** Its flight ended on the ground or in water; the thrower gets it. */
                LAND,
                /** The {@code teleport_random} behavior moved it during a throw. */
                TELEPORT,
                /** A mob caught from someone else, thrown again, hit that someone. */
                RETURN_HIT;

                public static final Codec<Event> CODEC = StringRepresentable.fromEnum(Event::values);

                @Override
                public String getSerializedName()
                {
                        return name().toLowerCase(Locale.ROOT);
                }
        }

        public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Event event,
                Optional<Holder<LootItemCondition>> entity, Optional<Double> minCharge, Optional<Double> minDistance,
                Optional<Boolean> ignited) implements SimpleCriterionTrigger.SimpleInstance
        {
                public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                        Event.CODEC.fieldOf("event").forGetter(TriggerInstance::event),
                        LootItemCondition.CODEC.optionalFieldOf("entity").forGetter(TriggerInstance::entity),
                        Codec.doubleRange(0.0, 1.0).optionalFieldOf("min_charge").forGetter(TriggerInstance::minCharge),
                        Codec.doubleRange(0.0, Double.MAX_VALUE).optionalFieldOf("min_distance")
                                .forGetter(TriggerInstance::minDistance),
                        Codec.BOOL.optionalFieldOf("ignited").forGetter(TriggerInstance::ignited))
                        .apply(instance, TriggerInstance::new));

                boolean matches(Event happened, LootContext mobContext, double charge, double distance, boolean lit)
                {
                        return event == happened
                                && minCharge.map(min -> charge >= min).orElse(true)
                                && minDistance.map(min -> distance >= min).orElse(true)
                                && ignited.map(wanted -> wanted == lit).orElse(true)
                                && entity.map(condition -> condition.value().test(mobContext)).orElse(true);
                }

                @Override
                public void validate(ValidationContextSource validator)
                {
                        SimpleCriterionTrigger.SimpleInstance.super.validate(validator);
                        Validatable.validateHolder(validator.entityContext(), "entity", entity);
                }
        }
}
