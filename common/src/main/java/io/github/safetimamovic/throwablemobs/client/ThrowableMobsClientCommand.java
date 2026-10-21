package io.github.safetimamovic.throwablemobs.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.safetimamovic.throwablemobs.Constants;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Client side of {@code /throwablemobs}: {@code help}, {@code config} and {@code keybinds}, for every player. They share
 * the root with the server's {@code profile}; both loaders send a subcommand the client doesn't know on to the server.
 * Each loader registers this with its own command source, which these commands don't use.
 */
public final class ThrowableMobsClientCommand
{
        /** Subcommands in help order; {@code profile} is the server's, for game masters. */
        private static final List<String> SUBCOMMANDS = List.of("help", "config", "keybinds", "profile");

        private static UnaryOperator<@Nullable Screen> configScreen = parent -> parent;

        private ThrowableMobsClientCommand()
        {
        }

        /** Called by each loader's client entrypoint with its config screen, which takes the screen to go back to. */
        public static void init(UnaryOperator<@Nullable Screen> configScreenFactory)
        {
                configScreen = configScreenFactory;
        }

        public static <S> void register(CommandDispatcher<S> dispatcher)
        {
                dispatcher.register(LiteralArgumentBuilder.<S>literal(Constants.MOD_ID)
                        .executes(context -> help())
                        .then(LiteralArgumentBuilder.<S>literal("help").executes(context -> help()))
                        .then(LiteralArgumentBuilder.<S>literal("config")
                                .executes(context -> open(() -> configScreen.apply(null))))
                        .then(LiteralArgumentBuilder.<S>literal("keybinds")
                                .executes(context -> open(
                                        () -> new KeyBindsScreen(null, Minecraft.getInstance().options)))));
        }

        /** "/throwablemobs keybinds" and the like; clicking it puts it in the chat box. */
        public static Component link(String subcommand)
        {
                String command = "/" + Constants.MOD_ID + " " + subcommand;
                return Component.literal(command).withStyle(style -> style.withColor(ChatFormatting.AQUA)
                        .withClickEvent(new ClickEvent.SuggestCommand(command)));
        }

        private static int help()
        {
                chat(Component.translatable("throwablemobs.command.help.header").withStyle(ChatFormatting.GOLD));
                for (String subcommand : SUBCOMMANDS)
                {
                        chat(Component.literal("  ").append(link(subcommand)).append(" ")
                                .append(Component.translatable("throwablemobs.command.help." + subcommand)
                                        .withStyle(ChatFormatting.GRAY)));
                }
                return SUBCOMMANDS.size();
        }

        /** Next tick: the chat screen the command was typed in closes after running it, and would close this too. */
        private static int open(Supplier<@Nullable Screen> screen)
        {
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.schedule(() -> minecraft.gui.setScreen(screen.get()));
                return 1;
        }

        private static void chat(Component message)
        {
                Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(message);
        }
}
