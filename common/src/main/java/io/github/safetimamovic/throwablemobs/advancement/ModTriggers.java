package io.github.safetimamovic.throwablemobs.advancement;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.ModRegistries;
import net.minecraft.core.registries.Registries;

/** Our advancement criteria. */
public final class ModTriggers
{
        public static final ThrowEventTrigger THROW_EVENT = new ThrowEventTrigger();

        private ModTriggers()
        {
        }

        public static void register(ModRegistries.Registrar registrar)
        {
                registrar.register(Registries.TRIGGER_TYPE, Constants.id("throw_event"), THROW_EVENT);
        }
}
