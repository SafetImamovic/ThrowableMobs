package io.github.safetimamovic.throwablemobs.api;

import io.github.safetimamovic.throwablemobs.behavior.BehaviorRegistry;
import net.minecraft.world.entity.EntityType;

/**
 * Entry point for other mods. Works the same on every loader; call it from your common init. Datapack profiles are
 * applied on top of what's registered here, so pack devs keep the final say.
 */
public final class ThrowableMobsApi
{
        private ThrowableMobsApi()
        {
        }

        /**
         * Makes a behavior action usable from mob profile JSON by its id.
         *
         * @throws IllegalStateException if the id is taken
         */
        public static void registerBehaviorType(BehaviorType<?> type)
        {
                BehaviorRegistry.registerType(type);
        }

        /** Adds a behavior to every mob of {@code entityType}, after behaviors registered before it. */
        public static void registerBehavior(EntityType<?> entityType, ThrowBehavior behavior)
        {
                BehaviorRegistry.registerBehavior(entityType, behavior);
        }

        /**
         * Sets physics and damage for every mob of {@code entityType}. Merges on top of earlier registrations for the
         * same type; a set {@code behaviors} list replaces theirs.
         */
        public static void registerProfile(EntityType<?> entityType, MobProfile profile)
        {
                BehaviorRegistry.registerProfile(entityType, profile);
        }
}
