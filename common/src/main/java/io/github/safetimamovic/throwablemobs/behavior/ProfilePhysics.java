package io.github.safetimamovic.throwablemobs.behavior;

import io.github.safetimamovic.throwablemobs.api.MobProfile;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The part of a resolved {@link MobProfile} the client needs too: the carry prompt checks mass, the trajectory
 * preview simulates the flight. Behaviors stay on the server.
 *
 * @param mass empty = hitbox volume × {@code default_density}
 */
public record ProfilePhysics(Optional<Double> mass, double gravityMultiplier, double dragMultiplier)
{
        public static final ProfilePhysics DEFAULT = new ProfilePhysics(Optional.empty(), 1.0, 1.0);

        public static final StreamCodec<RegistryFriendlyByteBuf, ProfilePhysics> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(ByteBufCodecs.DOUBLE), ProfilePhysics::mass,
                ByteBufCodecs.DOUBLE, ProfilePhysics::gravityMultiplier,
                ByteBufCodecs.DOUBLE, ProfilePhysics::dragMultiplier,
                ProfilePhysics::new);

        public static ProfilePhysics of(MobProfile profile)
        {
                return new ProfilePhysics(Optional.ofNullable(profile.mass()),
                        profile.gravityMultiplier() != null ? profile.gravityMultiplier() : 1.0,
                        profile.dragMultiplier() != null ? profile.dragMultiplier() : 1.0);
        }
}
