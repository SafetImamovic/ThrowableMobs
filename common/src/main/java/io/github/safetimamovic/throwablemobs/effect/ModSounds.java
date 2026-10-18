package io.github.safetimamovic.throwablemobs.effect;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.ModRegistries;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/**
 * Our sound events. {@code assets/throwablemobs/sounds.json} points them at vanilla sounds, so a resource pack can swap
 * any of them without touching the others.
 */
public final class ModSounds
{
        public static final SoundEvent THROW = create("entity.mob.throw");
        public static final SoundEvent BOUNCE = create("entity.mob.bounce");
        public static final SoundEvent IMPACT = create("entity.mob.impact");
        public static final SoundEvent CATCH = create("entity.mob.catch");

        private ModSounds()
        {
        }

        private static SoundEvent create(String path)
        {
                return SoundEvent.createVariableRangeEvent(Constants.id(path));
        }

        public static void register(ModRegistries.Registrar registrar)
        {
                for (SoundEvent sound : List.of(THROW, BOUNCE, IMPACT, CATCH))
                {
                        registrar.register(Registries.SOUND_EVENT, sound.location(), sound);
                }
        }
}
