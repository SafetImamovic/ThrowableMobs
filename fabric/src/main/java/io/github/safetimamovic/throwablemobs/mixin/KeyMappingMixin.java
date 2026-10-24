//? if <26.1 {
/*package io.github.safetimamovic.throwablemobs.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/^*
 * In 1.21.1 vanilla sent a key's presses to only one of the mappings on it, the last one registered: our defaults
 * (Left Shift, Right Button) took them from Sneak and Use. This hands them to the others on the key too, like 26.x
 * vanilla does. NeoForge already does it with its own key lookup, so this is Fabric only.
 ^/
@Mixin(KeyMapping.class)
abstract class KeyMappingMixin
{
        @Shadow
        @Final
        private static Map<String, KeyMapping> ALL;
        @Shadow
        @Final
        private static Map<InputConstants.Key, KeyMapping> MAP;
        @Shadow
        private InputConstants.Key key;
        @Shadow
        private int clickCount;

        @Inject(method = "click", at = @At("TAIL"))
        private static void throwablemobs$clickShared(InputConstants.Key key, CallbackInfo ci)
        {
                KeyMapping handled = MAP.get(key);
                for (KeyMapping mapping : ALL.values())
                {
                        KeyMappingMixin shared = (KeyMappingMixin) (Object) mapping;
                        if (mapping != handled && shared.key.equals(key))
                        {
                                shared.clickCount++;
                        }
                }
        }

        @Inject(method = "set", at = @At("TAIL"))
        private static void throwablemobs$setShared(InputConstants.Key key, boolean down, CallbackInfo ci)
        {
                KeyMapping handled = MAP.get(key);
                for (KeyMapping mapping : ALL.values())
                {
                        if (mapping != handled && ((KeyMappingMixin) (Object) mapping).key.equals(key))
                        {
                                mapping.setDown(down);
                        }
                }
        }
}
*///?}
