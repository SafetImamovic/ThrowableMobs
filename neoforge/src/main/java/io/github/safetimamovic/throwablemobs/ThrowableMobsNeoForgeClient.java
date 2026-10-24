package io.github.safetimamovic.throwablemobs;

import io.github.safetimamovic.throwablemobs.client.CarryClient;
import io.github.safetimamovic.throwablemobs.client.CarryKeys;
import io.github.safetimamovic.throwablemobs.client.ConfigScreens;
import io.github.safetimamovic.throwablemobs.client.ThrowableMobsClientCommand;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen.Context;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen.Element;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
//? if >=26.1 {
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//?} else {
/*import io.github.safetimamovic.throwablemobs.client.ShapeCanvas;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
*///?}
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class ThrowableMobsNeoForgeClient
{
        public ThrowableMobsNeoForgeClient(IEventBus modEventBus, ModContainer container)
        {
                // Servers without the mod don't know the payloads.
                CarryClient.init(payload ->
                {
                        ClientPacketListener connection = Minecraft.getInstance().getConnection();
                        if (connection != null && connection.hasChannel(payload))
                        {
                                //? if >=26.1 {
                                ClientPacketDistributor.sendToServer(payload);
                                //?} else
                                /*PacketDistributor.sendToServer(payload);*/
                        }
                });
                UnaryOperator<@Nullable Screen> configScreen = ConfigScreens.withYacl(
                        parent -> new ConfigurationScreen(container, parent, ThrowableMobsNeoForgeClient::filter));
                container.registerExtensionPoint(IConfigScreenFactory.class,
                        (IConfigScreenFactory) (mod, parent) -> configScreen.apply(parent));
                ThrowableMobsClientCommand.init(configScreen);
                NeoForge.EVENT_BUS.addListener(
                        (RegisterClientCommandsEvent event) -> ThrowableMobsClientCommand.register(event.getDispatcher()));
                modEventBus.addListener((RegisterKeyMappingsEvent event) ->
                {
                        //? if >=26.1
                        event.registerCategory(CarryKeys.CATEGORY);
                        event.register(CarryKeys.MODIFIER);
                        event.register(CarryKeys.PICK_UP);
                        event.register(CarryKeys.PUT_DOWN);
                        event.register(CarryKeys.THROW);
                        event.register(CarryKeys.CANCEL_THROW);
                });
                NeoForge.EVENT_BUS
                        .addListener((ClientTickEvent.Pre event) -> CarryClient.tickInput(Minecraft.getInstance()));
                NeoForge.EVENT_BUS
                        .addListener((ClientTickEvent.Post event) -> CarryClient.tick(Minecraft.getInstance()));
                modEventBus
                        .addListener((RegisterGuiLayersEvent event) -> event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                                Constants.id("carry_prompt"), CarryClient::renderPrompt));
                //? if <26.1 {
                /*// The trajectory preview, drawn by vanilla's gizmos from 26.1 on.
                NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent event) ->
                {
                        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
                        {
                                ShapeCanvas.render();
                        }
                });
                *///?}
        }

        /** The stock screen greys out the player's {@code enabled} while the server has the mod off. */
        private static Element filter(Context context, String key, Element original)
        {
                if (!ConfigScreens.lockedByServer(context.modSpec(), context.keylist(), key))
                {
                        return original;
                }
                AbstractWidget widget = original.getWidget(Minecraft.getInstance().options);
                ConfigScreens.showLockedByServer(widget);
                return new Element(original.name(), original.tooltip(), widget, false);
        }
}
