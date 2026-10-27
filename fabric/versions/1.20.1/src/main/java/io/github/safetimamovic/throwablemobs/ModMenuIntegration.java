package io.github.safetimamovic.throwablemobs;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.safetimamovic.throwablemobs.client.ConfigScreens;
import java.util.function.UnaryOperator;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/**
 * Mod Menu's config button (1.20.1 only): Forge Config API Port registers screens with Mod Menu from 1.20.2 on. There
 * is no stock screen before that, so without YACL there's no button.
 */
public class ModMenuIntegration implements ModMenuApi
{
        static final UnaryOperator<@Nullable Screen> CONFIG_SCREEN = ConfigScreens.withYacl(parent -> null);

        @Override
        public ConfigScreenFactory<?> getModConfigScreenFactory()
        {
                return CONFIG_SCREEN::apply;
        }
}
