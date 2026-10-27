package io.github.safetimamovic.throwablemobs.advancement;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.ModRegistries;
import net.minecraft.core.registries.Registries;
//? if <1.20.2
/*import io.github.safetimamovic.throwablemobs.mixin.CriteriaTriggersInvoker;*/

/** Our advancement criteria. */
public final class ModTriggers
{
        public static final ThrowEventTrigger THROW_EVENT = new ThrowEventTrigger();
        //? if <1.20.2
        /*private static boolean registered;*/

        private ModTriggers()
        {
        }

        public static void register(ModRegistries.Registrar registrar)
        {
                //? if >=1.20.2 {
                registrar.register(Registries.TRIGGER_TYPE, Constants.id("throw_event"), THROW_EVENT);
                //?} else {
                /*// No trigger registry before 1.20.2. Forge calls this once per registry, so only the first time.
                if (!registered)
                {
                        registered = true;
                        CriteriaTriggersInvoker.invokeRegister(THROW_EVENT);
                }
                *///?}
        }
}
