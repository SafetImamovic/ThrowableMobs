package io.github.safetimamovic.throwablemobs.behavior;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.api.MobProfile;
import io.github.safetimamovic.throwablemobs.network.ProfileSyncPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * Resolved mob profiles. The server stacks the layers (Java registration, then datapack files by priority) per entity
 * type and caches the result; clients only get the physics part through {@link ProfileSyncPayload}.
 *
 * <p>Datapack files arrive from a reload listener before tags are bound, so they wait in {@code pending} until
 * {@link #onTagsLoaded} swaps them in; resolving earlier would match {@code #tag} entries against the old tags.
 */
public final class MobProfiles
{
        /** Read by the reload thread, swapped in on the server thread. */
        private static volatile @Nullable List<ProfileFile> pending;

        /** Datapack files, lowest priority first. Server thread only, like the cache. */
        private static List<ProfileFile> files = List.of();

        private static final Map<EntityType<?>, MobProfile> CACHE = new HashMap<>();

        /** What the server sent; read on the client thread. */
        private static volatile Map<EntityType<?>, ProfilePhysics> clientPhysics = Map.of();

        private MobProfiles()
        {
        }

        /** Reload listener: new datapack files, applied once tags are loaded. */
        static void prepared(Map<Identifier, ProfileFile> loaded)
        {
                List<Map.Entry<Identifier, ProfileFile>> entries = new ArrayList<>(loaded.entrySet());
                // Lowest priority first; on a tie, a stable order by file id so every reload resolves the same way.
                entries.sort(Comparator.comparingInt((Map.Entry<Identifier, ProfileFile> entry) -> entry.getValue()
                        .priority()).thenComparing(Map.Entry::getKey));
                pending = entries.stream().map(Map.Entry::getValue).toList();
                Constants.LOG.info("Loaded {} mob profile(s)", loaded.size());
        }

        /** Server tags loaded (startup or {@code /reload}): new files take effect, every profile resolves again. */
        public static void onTagsLoaded()
        {
                List<ProfileFile> loaded = pending;
                if (loaded != null)
                {
                        files = loaded;
                        pending = null;
                }
                CACHE.clear();
        }

        /** Resolved profile for {@code type}. Server thread only. */
        public static MobProfile resolve(EntityType<?> type)
        {
                return CACHE.computeIfAbsent(type, MobProfiles::stack);
        }

        private static MobProfile stack(EntityType<?> type)
        {
                MobProfile profile = MobProfile.EMPTY;
                MobProfile java = BehaviorRegistry.javaProfile(type);
                if (java != null)
                {
                        profile = java.over(profile);
                }
                for (ProfileFile file : files)
                {
                        if (file.matches(type))
                        {
                                profile = file.profile().over(profile);
                        }
                }
                return profile;
        }

        /** Physics for {@code mob} on either side: resolved on the server, synced on the client. */
        public static ProfilePhysics physics(Mob mob)
        {
                if (mob.level().isClientSide())
                {
                        return clientPhysics.getOrDefault(mob.getType(), ProfilePhysics.DEFAULT);
                }
                return ProfilePhysics.of(resolve(mob.getType()));
        }

        /** Everything that differs from the defaults, for one client. */
        public static ProfileSyncPayload syncPayload()
        {
                Map<EntityType<?>, ProfilePhysics> physics = new HashMap<>();
                for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE)
                {
                        ProfilePhysics resolved = ProfilePhysics.of(resolve(type));
                        if (!resolved.equals(ProfilePhysics.DEFAULT))
                        {
                                physics.put(type, resolved);
                        }
                }
                return new ProfileSyncPayload(physics);
        }

        /** Client: profiles from the server. */
        public static void onSync(ProfileSyncPayload payload)
        {
                clientPhysics = Map.copyOf(payload.physics());
        }
}
