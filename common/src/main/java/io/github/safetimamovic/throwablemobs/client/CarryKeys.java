package io.github.safetimamovic.throwablemobs.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.safetimamovic.throwablemobs.Constants;
import net.minecraft.client.KeyMapping;

/**
 * Key mappings, in their own Controls category; each loader registers them. Picking up and putting down need the
 * modifier held, unless it is unbound; throwing is the same key without the modifier. Defaults: Left Shift + Right Button for both, the same input as Carry On; they
 * share keys with Sneak and Use, which still work (see {@code CarryClient#tickInput}).
 */
public final class CarryKeys
{
        public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Constants.id("controls"));

        public static final KeyMapping MODIFIER = new KeyMapping(name("modifier"), InputConstants.KEY_LSHIFT, CATEGORY);
        public static final KeyMapping PICK_UP = rightButton("pick_up");
        public static final KeyMapping PUT_DOWN = rightButton("put_down");
        /** Hold to charge, let go to throw. No modifier: Modifier + the same key is Put down. */
        public static final KeyMapping THROW = rightButton("throw");

        private CarryKeys()
        {
        }

        /** Unbound means no modifier needed. */
        public static boolean modifierHeld()
        {
                return MODIFIER.isUnbound() || MODIFIER.isDown();
        }

        private static KeyMapping rightButton(String name)
        {
                return new KeyMapping(name(name), InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_RIGHT, CATEGORY);
        }

        private static String name(String name)
        {
                return "key." + Constants.MOD_ID + "." + name;
        }
}
