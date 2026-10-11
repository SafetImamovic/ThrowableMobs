package io.github.safetimamovic.throwablemobs.behavior;

import io.github.safetimamovic.throwablemobs.api.MobProfile;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.ThrowConfig;
import io.github.safetimamovic.throwablemobs.physics.ThrowPhysics;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The part of a {@link MobProfile} the client needs too: the carry prompt checks mass, the trajectory preview simulates
 * the flight with its bounces. Behaviors and damage stay on the server. Every field is optional like in the profile,
 * so the client can stack the synced layers on top of the ones it works out itself (heuristics, {@code ThrowableMob}).
 *
 * @param replace drop the layers below, like {@link MobProfile#replace()}
 */
public record ProfilePhysics(boolean replace, Optional<Double> mass, Optional<Double> gravityMultiplier,
        Optional<Double> dragMultiplier, Optional<Double> blockRestitution, Optional<Integer> maxBounces)
{
        public static final ProfilePhysics EMPTY = new ProfilePhysics(false, Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());

        public static final StreamCodec<RegistryFriendlyByteBuf, ProfilePhysics> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, ProfilePhysics::replace,
                ByteBufCodecs.optional(ByteBufCodecs.DOUBLE), ProfilePhysics::mass,
                ByteBufCodecs.optional(ByteBufCodecs.DOUBLE), ProfilePhysics::gravityMultiplier,
                ByteBufCodecs.optional(ByteBufCodecs.DOUBLE), ProfilePhysics::dragMultiplier,
                ByteBufCodecs.optional(ByteBufCodecs.DOUBLE), ProfilePhysics::blockRestitution,
                ByteBufCodecs.optional(ByteBufCodecs.VAR_INT), ProfilePhysics::maxBounces,
                ProfilePhysics::new);

        public static ProfilePhysics of(MobProfile profile)
        {
                return new ProfilePhysics(profile.replace(), Optional.ofNullable(profile.mass()),
                        Optional.ofNullable(profile.gravityMultiplier()), Optional.ofNullable(profile.dragMultiplier()),
                        Optional.ofNullable(profile.blockRestitution()), Optional.ofNullable(profile.maxBounces()));
        }

        /** This on top of {@code below}, the same rule as {@link MobProfile#over}. */
        public ProfilePhysics over(ProfilePhysics below)
        {
                if (replace)
                {
                        return this;
                }
                return new ProfilePhysics(below.replace, mass.or(below::mass),
                        gravityMultiplier.or(below::gravityMultiplier), dragMultiplier.or(below::dragMultiplier),
                        blockRestitution.or(below::blockRestitution), maxBounces.or(below::maxBounces));
        }

        public double gravity(ThrowConfig config)
        {
                return config.gravity() * gravityMultiplier.orElse(1.0);
        }

        public double drag(ThrowConfig config)
        {
                return ThrowPhysics.scaledDrag(config.drag(), dragMultiplier.orElse(1.0));
        }

        public double blockRestitution(ThrowConfig config)
        {
                return blockRestitution.orElse(config.blockRestitution());
        }

        public int maxBounces(ThrowConfig config)
        {
                return maxBounces.orElse(config.maxBounces());
        }

        public boolean isEmpty()
        {
                return equals(EMPTY);
        }
}
