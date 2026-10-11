package io.github.safetimamovic.throwablemobs.behavior;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.api.MobProfile;
import io.github.safetimamovic.throwablemobs.api.ThrowBehavior;
import io.github.safetimamovic.throwablemobs.api.ThrowableMob;
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
 * Resolved mob profiles. Layers, lowest first: defaults, heuristics ({@link Heuristics}), the {@link ThrowableMob}
 * interface, Java registration, datapack files by priority. Each layer merges on top of the one below
 * ({@link MobProfile#over}).
 *
 * <p>The upper two layers are per entity type: the server stacks and caches them, and sends their physics to clients
 * ({@link ProfileSyncPayload}). The lower two depend on the mob's class and run on both sides, so the client stacks the
 * synced physics on top of its own guess and gets the same result as the server.
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

        /** Java and datapack layers per entity type. */
        private static final Map<EntityType<?>, MobProfile> CACHE = new HashMap<>();

        /** Physics of the Java and datapack layers, from the server; read on the client thread. */
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

        /** Resolved profile for {@code mob}, all layers. Server thread only. */
        public static MobProfile resolve(Mob mob)
        {
                return upper(mob.getType()).over(lower(mob));
        }

        /** Java registration and datapack files for {@code type}, cached. */
        private static MobProfile upper(EntityType<?> type)
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

        /** Heuristics, then the {@link ThrowableMob} interface. Works on both sides. */
        private static MobProfile lower(Mob mob)
        {
                MobProfile profile = Heuristics.guess(mob);
                if (mob instanceof ThrowableMob throwable)
                {
                        MobProfile own = throwable.getThrowProfile();
                        ThrowBehavior behavior = throwable.getThrowBehavior();
                        if (behavior != null)
                        {
                                List<ThrowBehavior> behaviors = new ArrayList<>();
                                if (own != null && own.behaviors() != null)
                                {
                                        behaviors.addAll(own.behaviors());
                                }
                                behaviors.add(behavior);
                                own = MobProfile.builder().behaviors(behaviors).build()
                                        .over(own != null ? own : MobProfile.EMPTY);
                        }
                        if (own != null)
                        {
                                profile = own.over(profile);
                        }
                }
                return profile;
        }

        /** Physics for {@code mob} on either side. */
        public static ProfilePhysics physics(Mob mob)
        {
                if (mob.level().isClientSide())
                {
                        ProfilePhysics synced = clientPhysics.getOrDefault(mob.getType(), ProfilePhysics.EMPTY);
                        return synced.over(ProfilePhysics.of(lower(mob)));
                }
                return ProfilePhysics.of(resolve(mob));
        }

        /** The Java and datapack layers' physics wherever they set something, for one client. */
        public static ProfileSyncPayload syncPayload()
        {
                Map<EntityType<?>, ProfilePhysics> physics = new HashMap<>();
                for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE)
                {
                        ProfilePhysics upper = ProfilePhysics.of(upper(type));
                        if (!upper.isEmpty())
                        {
                                physics.put(type, upper);
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
