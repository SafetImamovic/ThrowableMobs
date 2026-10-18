package io.github.safetimamovic.throwablemobs.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.safetimamovic.throwablemobs.api.BehaviorAction;
import io.github.safetimamovic.throwablemobs.api.ImpactContext;
import io.github.safetimamovic.throwablemobs.api.ImpactResult;
import io.github.safetimamovic.throwablemobs.api.ThrowBehavior;
import io.github.safetimamovic.throwablemobs.api.ThrowContext;
import io.github.safetimamovic.throwablemobs.api.Trigger;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * A behavior from a mob profile's {@code behaviors} list: an action, when it runs, and under which conditions, all in
 * one JSON object:
 *
 * <pre>{@code
 * { "type": "throwablemobs:explode", "trigger": "impact", "min_impact_energy": 20, "base": 1.5 }
 * }</pre>
 *
 * {@code trigger} defaults to {@code impact}.
 */
public record DataBehavior(Trigger trigger, Conditions conditions, BehaviorAction action) implements ThrowBehavior
{
        public static final Codec<DataBehavior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Trigger.CODEC.optionalFieldOf("trigger", Trigger.IMPACT).forGetter(DataBehavior::trigger),
                Conditions.MAP_CODEC.forGetter(DataBehavior::conditions),
                BehaviorRegistry.ACTION_MAP_CODEC.forGetter(DataBehavior::action))
                .apply(instance, DataBehavior::new));

        @Override
        public void onPickup(ThrowContext ctx)
        {
                run(Trigger.PICKUP, ctx);
        }

        @Override
        public void onCarryTick(ThrowContext ctx)
        {
                run(Trigger.CARRY_TICK, ctx);
        }

        @Override
        public void onThrow(ThrowContext ctx)
        {
                run(Trigger.THROW, ctx);
        }

        @Override
        public void onFlightTick(ThrowContext ctx)
        {
                run(Trigger.FLIGHT_TICK, ctx);
        }

        @Override
        public void onCaught(ThrowContext ctx, @Nullable ServerPlayer thrower)
        {
                run(Trigger.CAUGHT, ctx);
        }

        @Override
        public void onLand(ThrowContext ctx)
        {
                run(Trigger.LAND, ctx);
        }

        @Override
        public ImpactResult onImpact(ImpactContext ctx, ImpactResult current)
        {
                boolean fires = switch (trigger)
                {
                        case IMPACT -> true;
                        case IMPACT_ENTITY -> !ctx.isBlockHit();
                        case IMPACT_BLOCK -> ctx.isBlockHit();
                        default -> false;
                };
                return fires && conditions.test(ctx, ctx) ? action.onImpact(ctx, current) : current;
        }

        private void run(Trigger when, ThrowContext ctx)
        {
                if (trigger == when && conditions.test(ctx, null))
                {
                        action.run(ctx);
                }
        }
}
