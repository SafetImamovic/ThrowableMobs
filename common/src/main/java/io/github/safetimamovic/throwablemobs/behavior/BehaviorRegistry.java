package io.github.safetimamovic.throwablemobs.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import io.github.safetimamovic.throwablemobs.api.BehaviorAction;
import io.github.safetimamovic.throwablemobs.api.BehaviorType;
import io.github.safetimamovic.throwablemobs.api.MobProfile;
import io.github.safetimamovic.throwablemobs.api.ThrowBehavior;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

/**
 * Behavior types and the Java layer of mob profiles, filled through
 * {@link io.github.safetimamovic.throwablemobs.api.ThrowableMobsApi}. Our own map instead of a vanilla registry, so
 * neither loader needs registration code. Concurrent maps because mods may register from parallel init.
 */
public final class BehaviorRegistry
{
        private static final Map<Identifier, BehaviorType<?>> TYPES = new ConcurrentHashMap<>();

        /** Java layer of profiles, per entity type. */
        private static final Map<EntityType<?>, MobProfile> JAVA_PROFILES = new ConcurrentHashMap<>();

        private static final Codec<BehaviorType<?>> TYPE_CODEC = Identifier.CODEC
                .comapFlatMap(BehaviorRegistry::typeResult, BehaviorType::id);

        /** Reads a behavior action by its {@code "type"} field, from the same object as the other fields. */
        public static final MapCodec<BehaviorAction> ACTION_MAP_CODEC = TYPE_CODEC
                //? if >=1.20.5 {
                .dispatchMap("type", BehaviorAction::type, BehaviorType::codec);
                //?} else
                /*.dispatchMap("type", BehaviorAction::type, type -> type.codec().codec());*/

        private BehaviorRegistry()
        {
        }

        public static void registerType(BehaviorType<?> type)
        {
                if (TYPES.putIfAbsent(type.id(), type) != null)
                {
                        throw new IllegalStateException("Behavior type " + type.id() + " is already registered");
                }
        }

        public static @Nullable BehaviorType<?> type(Identifier id)
        {
                return TYPES.get(id);
        }

        private static DataResult<BehaviorType<?>> typeResult(Identifier id)
        {
                BehaviorType<?> type = TYPES.get(id);
                return type != null
                        ? DataResult.success(type)
                        : DataResult.error(() -> "Unknown behavior type " + id);
        }

        /** Merges {@code profile} on top of what's already registered for {@code entityType}. */
        public static void registerProfile(EntityType<?> entityType, MobProfile profile)
        {
                JAVA_PROFILES.merge(entityType, profile, (old, added) -> added.over(old));
        }

        /** Adds {@code behavior} after the ones already registered for {@code entityType}. */
        public static void registerBehavior(EntityType<?> entityType, ThrowBehavior behavior)
        {
                JAVA_PROFILES.compute(entityType, (type, old) ->
                {
                        MobProfile base = old != null ? old : MobProfile.EMPTY;
                        List<ThrowBehavior> behaviors = new ArrayList<>(
                                base.behaviors() != null ? base.behaviors() : List.of());
                        behaviors.add(behavior);
                        return MobProfile.builder().behaviors(behaviors).build().over(base);
                });
        }

        /** Java layer for {@code entityType}, or {@code null} if no mod registered one. */
        public static @Nullable MobProfile javaProfile(EntityType<?> entityType)
        {
                return JAVA_PROFILES.get(entityType);
        }
}
