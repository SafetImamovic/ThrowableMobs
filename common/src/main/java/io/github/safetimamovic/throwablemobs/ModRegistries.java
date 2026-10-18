package io.github.safetimamovic.throwablemobs;

import io.github.safetimamovic.throwablemobs.advancement.ModTriggers;
import io.github.safetimamovic.throwablemobs.effect.ModSounds;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Everything the mod adds to vanilla registries. The objects are created once in common code; each loader passes its
 * own way of registering them (Fabric right away, NeoForge from its register event).
 */
public final class ModRegistries
{
        private ModRegistries()
        {
        }

        /** Registers one value. NeoForge calls {@link #registerAll} once per registry and skips the other keys. */
        public interface Registrar
        {
                <T> void register(ResourceKey<? extends Registry<T>> registry, Identifier id, T value);
        }

        public static void registerAll(Registrar registrar)
        {
                ModSounds.register(registrar);
                ModTriggers.register(registrar);
        }
}
