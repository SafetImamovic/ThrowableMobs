package io.github.safetimamovic.throwablemobs.api;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * What a mob does when it's picked up, carried, thrown, in flight and when it hits something. Every method has a
 * default that does nothing, so a behavior only overrides what it needs. Our own vanilla behaviors go through this
 * interface too.
 *
 * <p>All hooks run on the server thread. Register with {@link ThrowableMobsApi#registerBehavior}, or implement
 * {@link ThrowableMob} on the entity class.
 */
public interface ThrowBehavior
{
        /** A player picked the mob up. */
        default void onPickup(ThrowContext ctx)
        {
        }

        /** Every server tick while the mob is carried. */
        default void onCarryTick(ThrowContext ctx)
        {
        }

        /** The mob was thrown; {@link ThrowContext#velocity()} is the launch velocity. */
        default void onThrow(ThrowContext ctx)
        {
        }

        /** Every server tick in the air, before the mob moves. */
        default void onFlightTick(ThrowContext ctx)
        {
        }

        /**
         * The mob hit a block or an entity. Return {@code current} unchanged to keep the impact as it is, or a changed
         * copy: other damage, other knockback, a bounce, or a landing. Behaviors run in order; each one gets the result
         * of the one before, the first gets {@link ImpactContext#defaultResult()}.
         */
        default ImpactResult onImpact(ImpactContext ctx, ImpactResult current)
        {
                return current;
        }

        /**
         * A player caught the mob in the air. {@link ThrowContext#player()} is the catcher, who carries it from now
         * on; {@link ThrowContext#velocity()} is its velocity at the catch. {@link #onPickup} isn't called for a catch,
         * and {@link #onLand} isn't either: the flight ended in someone's hands. A behavior that removes the mob here
         * (explodes, splits) cancels the catch.
         *
         * @param thrower who threw it, {@code null} if they logged out; can be the catcher (a self-catch)
         */
        default void onCaught(ThrowContext ctx, @Nullable ServerPlayer thrower)
        {
        }

        /** The flight ended (impact, water, timeout) and the mob has its AI back. */
        default void onLand(ThrowContext ctx)
        {
        }
}
