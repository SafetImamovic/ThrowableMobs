package io.github.safetimamovic.throwablemobs.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.safetimamovic.throwablemobs.Constants;
//? if <26.1
/*import java.util.Set;*/
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
//? if >=26.1
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Key mappings, in their own Controls category; each loader registers them. Picking up and putting down need the
 * modifier held, unless it is unbound; throwing is the same key without the modifier. Defaults: Left Shift + Right
 * Button for both, the same input as Carry On, which gets it first (see {@link #foreignConflict}); they share keys with
 * Sneak and Use, which still work (see {@code CarryClient#tickInput}).
 */
public final class CarryKeys
{
        //? if >=26.1 {
        public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Constants.id("controls"));
        //?} else {
        /*// Before key categories were registered, a category was its translation key.
        public static final String CATEGORY = "key.category." + Constants.MOD_ID + ".controls";
        private static final Set<String> VANILLA_CATEGORIES = Set.of(KeyMapping.CATEGORY_MOVEMENT,
                KeyMapping.CATEGORY_MISC, KeyMapping.CATEGORY_MULTIPLAYER, KeyMapping.CATEGORY_GAMEPLAY,
                KeyMapping.CATEGORY_INVENTORY, KeyMapping.CATEGORY_INTERFACE, KeyMapping.CATEGORY_CREATIVE);
        *///?}

        public static final KeyMapping MODIFIER = new KeyMapping(name("modifier"), InputConstants.KEY_LSHIFT, CATEGORY);
        public static final KeyMapping PICK_UP = rightButton("pick_up");
        public static final KeyMapping PUT_DOWN = rightButton("put_down");
        /** Hold to charge, let go to throw. No modifier: Modifier + the same key is Put down. */
        public static final KeyMapping THROW = rightButton("throw");
        /** While charging: drop the charge without throwing. Shares Left Button with Attack, which it swallows. */
        public static final KeyMapping CANCEL_THROW = new KeyMapping(name("cancel_throw"), InputConstants.Type.MOUSE,
                InputConstants.MOUSE_BUTTON_LEFT, CATEGORY);

        private CarryKeys()
        {
        }

        /** Unbound means no modifier needed. */
        public static boolean modifierHeld()
        {
                return MODIFIER.isUnbound() || MODIFIER.isDown();
        }

        /**
         * Another mod's key mapping on the key that starts a pick-up (Modifier, or Pick up when Modifier is unbound),
         * like Carry On's carry key on Left Shift; {@code null} if none. Picking up is left to that mod until the player
         * rebinds one of the two. Mappings in vanilla categories don't count: sharing with Sneak and Use is on purpose.
         */
        public static @Nullable KeyMapping foreignConflict(Options options)
        {
                KeyMapping start = MODIFIER.isUnbound() ? PICK_UP : MODIFIER;
                if (start.isUnbound())
                {
                        return null;
                }
                for (KeyMapping key : options.keyMappings)
                {
                        //? if >=26.1 {
                        String namespace = key.getCategory().id().getNamespace();
                        if (!namespace.equals(Identifier.DEFAULT_NAMESPACE) && !namespace.equals(Constants.MOD_ID)
                                && key.same(start))
                        //?} else {
                        /*String category = key.getCategory();
                        if (!VANILLA_CATEGORIES.contains(category) && !category.equals(CATEGORY) && key.same(start))
                        *///?}
                        {
                                return key;
                        }
                }
                return null;
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
