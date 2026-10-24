package io.github.safetimamovic.throwablemobs.client;

import io.github.safetimamovic.throwablemobs.carry.CarryRules;
import io.github.safetimamovic.throwablemobs.carry.LocalCarry;
import io.github.safetimamovic.throwablemobs.carry.ThrowManager;
import io.github.safetimamovic.throwablemobs.carry.ThrowState;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CatchConfig;
import io.github.safetimamovic.throwablemobs.platform.Services;
import java.util.OptionalDouble;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Outline on the thrown mob the local player is about to catch, with player config {@code catch_highlight}. Same rules
 * and geometry as the server's check, looking a few ticks ahead so it shows while the mob is still on its way. The
 * server also grows the radius for ping, so it can catch a mob that never glowed, but not the other way round.
 */
public final class CatchHighlight
{
        /** How many ticks of the mob's path ahead count, at its current speed. */
        private static final int LOOKAHEAD_TICKS = 10;

        private static @Nullable Entity highlighted;

        private CatchHighlight()
        {
        }

        /** Once per client tick, after {@link LocalCarry} is up to date. */
        public static void tick(ClientLevel level, LocalPlayer player)
        {
                Entity previous = highlighted;
                highlighted = null;
                CatchConfig config = ThrowableMobsConfig.catching();
                if (!ThrowableMobsConfig.hud().catchHighlight() || !config.enabled() || LocalCarry.get() != null
                        || player.isPassenger() || !CarryRules.canUseHands(player))
                {
                        return;
                }
                double first = Double.MAX_VALUE;
                for (Entity entity : level.entitiesForRendering())
                {
                        ThrowState state = Services.THROW_STATE.get(entity);
                        if (state == null || !(entity instanceof Mob mob) || !canCatch(level, player, mob, state, config))
                        {
                                continue;
                        }
                        Vec3 velocity = mob.position().subtract(mob.xo, mob.yo, mob.zo);
                        if (velocity.lengthSqr() == 0.0 && mob == previous)
                        {
                                // Position updates arrive in steps, so some ticks it doesn't move here: keep the outline.
                                highlighted = mob;
                                first = 0.0;
                                continue;
                        }
                        OptionalDouble fraction = ThrowManager.catchFraction(player, mob.getBoundingBox().getCenter(),
                                velocity.scale(LOOKAHEAD_TICKS), 0.0, config);
                        if (fraction.isPresent() && fraction.getAsDouble() < first)
                        {
                                highlighted = mob;
                                first = fraction.getAsDouble();
                        }
                }
        }

        /** Whether {@code entity} gets the outline, asked by vanilla's glowing check (see {@code MinecraftMixin}). */
        public static boolean isHighlighted(Entity entity)
        {
                return entity == highlighted;
        }

        private static boolean canCatch(ClientLevel level, LocalPlayer player, Mob mob, ThrowState state,
                CatchConfig config)
        {
                if (state.thrower().equals(player.getUUID()) && !config.selfCatch().allows(
                        level.getGameTime() - state.launchTick(), state.bounced(), config.selfCatchDelayTicks()))
                {
                        return false;
                }
                return CarryRules.check(player, mob) == null;
        }
}
