package io.github.safetimamovic.throwablemobs.behavior;

import io.github.safetimamovic.throwablemobs.api.MobProfile;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.ThrowConfig;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The part of a resolved {@link MobProfile} the client needs too: the carry prompt checks mass, the trajectory
 * preview simulates the flight with its bounces. Behaviors and damage stay on the server.
 *
 * @param mass             empty = hitbox volume × {@code default_density}
 * @param blockRestitution empty = the config's {@code block_restitution}
 * @param maxBounces       empty = the config's {@code max_bounces}
 */
public record ProfilePhysics(Optional<Double> mass, double gravityMultiplier, double dragMultiplier,
        Optional<Double> blockRestitution, Optional<Integer> maxBounces)
{
        public static final ProfilePhysics DEFAULT = new ProfilePhysics(Optional.empty(), 1.0, 1.0, Optional.empty(),
                Optional.empty());

        public static final StreamCodec<RegistryFriendlyByteBuf, ProfilePhysics> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(ByteBufCodecs.DOUBLE), ProfilePhysics::mass,
                ByteBufCodecs.DOUBLE, ProfilePhysics::gravityMultiplier,
                ByteBufCodecs.DOUBLE, ProfilePhysics::dragMultiplier,
                ByteBufCodecs.optional(ByteBufCodecs.DOUBLE), ProfilePhysics::blockRestitution,
                ByteBufCodecs.optional(ByteBufCodecs.VAR_INT), ProfilePhysics::maxBounces,
                ProfilePhysics::new);

        public static ProfilePhysics of(MobProfile profile)
        {
                return new ProfilePhysics(Optional.ofNullable(profile.mass()),
                        profile.gravityMultiplier() != null ? profile.gravityMultiplier() : 1.0,
                        profile.dragMultiplier() != null ? profile.dragMultiplier() : 1.0,
                        Optional.ofNullable(profile.blockRestitution()), Optional.ofNullable(profile.maxBounces()));
        }

        public double blockRestitution(ThrowConfig config)
        {
                return blockRestitution.orElse(config.blockRestitution());
        }

        public int maxBounces(ThrowConfig config)
        {
                return maxBounces.orElse(config.maxBounces());
        }
}
