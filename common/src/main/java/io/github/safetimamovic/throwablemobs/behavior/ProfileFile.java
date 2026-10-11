package io.github.safetimamovic.throwablemobs.behavior;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.safetimamovic.throwablemobs.api.BounceImpacts;
import io.github.safetimamovic.throwablemobs.api.MobProfile;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * One datapack mob profile, {@code data/<namespace>/throwablemobs/mob_profile/<name>.json}:
 *
 * <pre>{@code
 * {
 *   "schema_version": 1,
 *   "entities": ["minecraft:chicken", "#somemod:birds"],
 *   "priority": 0,
 *   "replace": false,
 *   "physics": { "mass": 45, "gravity_multiplier": 0.4, "drag_multiplier": 1, "restitution": 0.3,
 *                "block_restitution": 0, "max_bounces": 3, "bounce_impacts": "every" },
 *   "damage": { "self_multiplier": 1, "target_multiplier": 1 }
 * }
 * }</pre>
 *
 * Everything but {@code entities} is optional; unset values come from the layers below (see {@link MobProfile}).
 * Unknown ids in {@code entities} are skipped, so a profile for a mod that isn't installed does nothing.
 */
public record ProfileFile(List<Either<TagKey<EntityType<?>>, Identifier>> entities, int priority, MobProfile profile)
{
        public static final int SCHEMA_VERSION = 1;

        private static final Codec<Either<TagKey<EntityType<?>>, Identifier>> ENTITY = Codec
                .either(TagKey.hashedCodec(Registries.ENTITY_TYPE), Identifier.CODEC);

        private static final Codec<Double> MULTIPLIER = Codec.doubleRange(0.0, 1000.0);

        public static final Codec<ProfileFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, SCHEMA_VERSION).optionalFieldOf("schema_version", SCHEMA_VERSION)
                        .forGetter(file -> SCHEMA_VERSION),
                ENTITY.listOf().fieldOf("entities").forGetter(ProfileFile::entities),
                Codec.INT.optionalFieldOf("priority", 0).forGetter(ProfileFile::priority),
                Codec.BOOL.optionalFieldOf("replace", false).forGetter(file -> file.profile().replace()),
                Physics.CODEC.optionalFieldOf("physics").forGetter(file -> Optional.of(Physics.of(file.profile()))),
                Damage.CODEC.optionalFieldOf("damage").forGetter(file -> Optional.of(Damage.of(file.profile()))))
                .apply(instance, (version, entities, priority, replace, physics, damage) -> new ProfileFile(entities,
                        priority, toProfile(replace, physics.orElse(Physics.EMPTY), damage.orElse(Damage.EMPTY)))));

        public boolean matches(EntityType<?> type)
        {
                for (Either<TagKey<EntityType<?>>, Identifier> entity : entities)
                {
                        boolean match = entity.map(tag -> BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(tag),
                                id -> EntityType.getKey(type).equals(id));
                        if (match)
                        {
                                return true;
                        }
                }
                return false;
        }

        private static MobProfile toProfile(boolean replace, Physics physics, Damage damage)
        {
                return new MobProfile(replace, physics.mass.orElse(null), physics.gravityMultiplier.orElse(null),
                        physics.dragMultiplier.orElse(null), physics.restitution.orElse(null),
                        physics.blockRestitution.orElse(null), physics.maxBounces.orElse(null),
                        physics.bounceImpacts.orElse(null), damage.selfMultiplier.orElse(null),
                        damage.targetMultiplier.orElse(null), null);
        }

        private record Physics(Optional<Double> mass, Optional<Double> gravityMultiplier,
                Optional<Double> dragMultiplier, Optional<Double> restitution, Optional<Double> blockRestitution,
                Optional<Integer> maxBounces, Optional<BounceImpacts> bounceImpacts)
        {
                static final Physics EMPTY = of(MobProfile.EMPTY);

                static final Codec<Physics> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.doubleRange(0.001, 1.0E9).optionalFieldOf("mass").forGetter(Physics::mass),
                        MULTIPLIER.optionalFieldOf("gravity_multiplier").forGetter(Physics::gravityMultiplier),
                        MULTIPLIER.optionalFieldOf("drag_multiplier").forGetter(Physics::dragMultiplier),
                        Codec.doubleRange(0.0, 2.0).optionalFieldOf("restitution").forGetter(Physics::restitution),
                        Codec.doubleRange(0.0, 2.0).optionalFieldOf("block_restitution")
                                .forGetter(Physics::blockRestitution),
                        Codec.intRange(0, 100).optionalFieldOf("max_bounces").forGetter(Physics::maxBounces),
                        BounceImpacts.CODEC.optionalFieldOf("bounce_impacts").forGetter(Physics::bounceImpacts))
                        .apply(instance, Physics::new));

                static Physics of(MobProfile profile)
                {
                        return new Physics(Optional.ofNullable(profile.mass()),
                                Optional.ofNullable(profile.gravityMultiplier()),
                                Optional.ofNullable(profile.dragMultiplier()), Optional.ofNullable(profile.restitution()),
                                Optional.ofNullable(profile.blockRestitution()),
                                Optional.ofNullable(profile.maxBounces()), Optional.ofNullable(profile.bounceImpacts()));
                }
        }

        private record Damage(Optional<Double> selfMultiplier, Optional<Double> targetMultiplier)
        {
                static final Damage EMPTY = of(MobProfile.EMPTY);

                static final Codec<Damage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        MULTIPLIER.optionalFieldOf("self_multiplier").forGetter(Damage::selfMultiplier),
                        MULTIPLIER.optionalFieldOf("target_multiplier").forGetter(Damage::targetMultiplier))
                        .apply(instance, Damage::new));

                static Damage of(MobProfile profile)
                {
                        return new Damage(Optional.ofNullable(profile.selfMultiplier()),
                                Optional.ofNullable(profile.targetMultiplier()));
                }
        }
}
