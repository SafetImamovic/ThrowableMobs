package io.github.safetimamovic.throwablemobs.behavior;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.api.ImpactContext;
import io.github.safetimamovic.throwablemobs.api.ImpactResult;
import io.github.safetimamovic.throwablemobs.api.ThrowBehavior;
import io.github.safetimamovic.throwablemobs.api.ThrowContext;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.BehaviorConfig;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The one place that calls {@link ThrowBehavior} hooks, each from a single call site in {@code CarryManager} or
 * {@code ThrowManager}. A behavior that throws is logged and skipped, so a broken behavior from another mod can't
 * crash the server or leave a mob stuck mid-flight.
 */
public final class Hooks
{
        private Hooks()
        {
        }

        public static void pickup(Mob mob, ServerPlayer player)
        {
                ThrowContext ctx = new Context(mob, (ServerLevel) mob.level(), player, Vec3.ZERO, 0);
                each(mob, behavior -> behavior.onPickup(ctx));
        }

        public static void carryTick(Mob mob, ServerPlayer player)
        {
                ThrowContext ctx = new Context(mob, (ServerLevel) mob.level(), player, Vec3.ZERO, 0);
                each(mob, behavior -> behavior.onCarryTick(ctx));
        }

        public static void thrown(Mob mob, @Nullable ServerPlayer player, Vec3 velocity)
        {
                ThrowContext ctx = new Context(mob, (ServerLevel) mob.level(), player, velocity, 0);
                each(mob, behavior -> behavior.onThrow(ctx));
        }

        public static void flightTick(Mob mob, @Nullable ServerPlayer player, Vec3 velocity, int flightTicks)
        {
                ThrowContext ctx = new Context(mob, (ServerLevel) mob.level(), player, velocity, flightTicks);
                each(mob, behavior -> behavior.onFlightTick(ctx));
        }

        public static void land(Mob mob, @Nullable ServerPlayer player, Vec3 velocity, int flightTicks)
        {
                ThrowContext ctx = new Context(mob, (ServerLevel) mob.level(), player, velocity, flightTicks);
                each(mob, behavior -> behavior.onLand(ctx));
        }

        /** {@code catcher} is the context's player; {@code thrower} is passed on to the hook. */
        public static void caught(Mob mob, ServerPlayer catcher, @Nullable ServerPlayer thrower, Vec3 velocity,
                int flightTicks)
        {
                ThrowContext ctx = new Context(mob, (ServerLevel) mob.level(), catcher, velocity, flightTicks);
                each(mob, behavior -> behavior.onCaught(ctx, thrower));
        }

        /** Runs every behavior's {@code onImpact} in order, each on the result of the one before. */
        public static ImpactResult impact(Mob mob, @Nullable ServerPlayer player, Vec3 velocity, int flightTicks,
                @Nullable Entity target, Vec3 relativeVelocity, double energy, int impactIndex, ImpactResult result)
        {
                ImpactContext ctx = new Impact(mob, (ServerLevel) mob.level(), player, velocity, flightTicks, target,
                        relativeVelocity, energy, impactIndex, result);
                ImpactResult current = result;
                for (ThrowBehavior behavior : behaviors(mob))
                {
                        try
                        {
                                current = behavior.onImpact(ctx, current);
                        }
                        catch (RuntimeException e)
                        {
                                logFailure(behavior, mob, e);
                        }
                }
                return current;
        }

        private static void each(Mob mob, Consumer<ThrowBehavior> hook)
        {
                for (ThrowBehavior behavior : behaviors(mob))
                {
                        try
                        {
                                hook.accept(behavior);
                        }
                        catch (RuntimeException e)
                        {
                                logFailure(behavior, mob, e);
                        }
                }
        }

        /** The mob's behaviors, minus {@code disabled_behaviors}; none with {@code behaviors_enabled} off. */
        private static List<ThrowBehavior> behaviors(Mob mob)
        {
                BehaviorConfig config = ThrowableMobsConfig.behaviors();
                List<ThrowBehavior> behaviors = MobProfiles.resolve(mob).behaviors();
                if (!config.enabled() || behaviors == null)
                {
                        return List.of();
                }
                if (config.disabled().isEmpty())
                {
                        return behaviors;
                }
                return behaviors.stream().filter(behavior -> !(behavior instanceof DataBehavior data
                        && config.disabled().contains(data.action().type().id()))).toList();
        }

        private static void logFailure(ThrowBehavior behavior, Mob mob, RuntimeException e)
        {
                Constants.LOG.warn("Throw behavior {} failed for {}", behavior.getClass().getName(), mob, e);
        }

        private record Context(Mob mob, ServerLevel level, @Nullable ServerPlayer player, Vec3 velocity,
                int flightTicks) implements ThrowContext
        {
        }

        private record Impact(Mob mob, ServerLevel level, @Nullable ServerPlayer player, Vec3 velocity,
                int flightTicks, @Nullable Entity target, Vec3 relativeVelocity, double energy, int impactIndex,
                ImpactResult defaultResult) implements ImpactContext
        {
        }
}
