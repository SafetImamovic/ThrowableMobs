package io.github.safetimamovic.throwablemobs.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** What a {@link ThrowBehavior} hook gets to work with. Only valid during the call. */
public interface ThrowContext
{
        /** The carried or thrown mob. */
        Mob mob();

        ServerLevel level();

        /**
         * The player carrying or throwing the mob. {@code null} in flight when the thrower logged out; check this
         * before crediting or angering anyone.
         */
        @Nullable ServerPlayer player();

        /** Velocity in blocks/tick: launch velocity on throw, current velocity in flight, zero while carried. */
        Vec3 velocity();

        /** Ticks since the throw; 0 before it. */
        int flightTicks();
}
