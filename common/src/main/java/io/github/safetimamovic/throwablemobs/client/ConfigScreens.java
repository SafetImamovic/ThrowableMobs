package io.github.safetimamovic.throwablemobs.client;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.config.PlayerConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.platform.Services;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jspecify.annotations.Nullable;

/**
 * The config screen: YACL's when it's installed (see {@link YaclConfigScreen}), else the loader's stock one. YACL is
 * optional, so its classes are only touched once it's known to be there.
 */
public final class ConfigScreens
{
        private static final String YACL_ID = "yet_another_config_lib_v3";

        /** Why the player's {@code enabled} is greyed out, see {@link #lockedByServer}. */
        static final Component SERVER_OFF = Component.translatable("throwablemobs.config.server_off");

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

        /**
         * The player's own {@code enabled}, in a world whose server has the mod turned off: it does nothing there, so
         * both screens grey it out and say why.
         */
        public static boolean lockedByServer(ModConfigSpec spec, List<String> section, String key)
        {
                return spec == PlayerConfig.SPEC && section.equals(List.of("general")) && key.equals("enabled")
                        && Minecraft.getInstance().level != null && !ThrowableMobsConfig.enabled();
        }

        /** For the stock screen's filter: greys out the widget of a setting {@link #lockedByServer}. */
        public static void showLockedByServer(AbstractWidget widget)
        {
                widget.active = false;
                widget.setMessage(Component.translatable("throwablemobs.config.off_on_server"));
                widget.setTooltip(Tooltip.create(SERVER_OFF));
        }
}
