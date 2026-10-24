package io.github.safetimamovic.throwablemobs.client;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.platform.Services;
import java.util.function.UnaryOperator;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/**
 * The config screen: YACL's when it's installed (see {@link YaclConfigScreen}), else the loader's stock one. YACL is
 * optional, so its classes are only touched once it's known to be there.
 */
public final class ConfigScreens
{
        private static final String YACL_ID = "yet_another_config_lib_v3";

        private ConfigScreens()
        {
        }

        /** Each loader passes its stock screen; the result goes to Mod Menu / the mod list and the client command. */
        public static UnaryOperator<@Nullable Screen> withYacl(UnaryOperator<@Nullable Screen> stock)
        {
                if (!Services.PLATFORM.isModLoaded(YACL_ID))
                {
                        return stock;
                }
                return parent ->
                {
                        try
                        {
                                return YaclConfigScreen.create(parent);
                        }
                        catch (LinkageError e)
                        {
                                // A YACL version whose API changed: still better to have a screen.
                                Constants.LOG.warn("Couldn't build the YACL config screen, opening the stock one", e);
                                return stock.apply(parent);
                        }
                };
        }
}
