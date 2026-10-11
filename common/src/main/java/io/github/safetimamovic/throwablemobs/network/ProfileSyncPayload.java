package io.github.safetimamovic.throwablemobs.network;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.behavior.ProfilePhysics;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EntityType;

/**
 * Server → client, on join and after every datapack reload: the physics of every entity type whose resolved profile
 * differs from the defaults. Replaces what the client had.
 */
public record ProfileSyncPayload(Map<EntityType<?>, ProfilePhysics> physics) implements CustomPacketPayload
{
        public static final CustomPacketPayload.Type<ProfileSyncPayload> TYPE = new CustomPacketPayload.Type<>(
                Constants.id("profile_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ProfileSyncPayload> STREAM_CODEC = ByteBufCodecs
                .<RegistryFriendlyByteBuf, EntityType<?>, ProfilePhysics, Map<EntityType<?>, ProfilePhysics>>map(
                        HashMap::new, ByteBufCodecs.registry(Registries.ENTITY_TYPE), ProfilePhysics.STREAM_CODEC)
                .map(ProfileSyncPayload::new, ProfileSyncPayload::physics);

        @Override
        public CustomPacketPayload.Type<ProfileSyncPayload> type()
        {
                return TYPE;
        }
}
