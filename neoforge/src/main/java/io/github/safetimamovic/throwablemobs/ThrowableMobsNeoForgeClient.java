package io.github.safetimamovic.throwablemobs;

import io.github.safetimamovic.throwablemobs.client.CarryClient;
import io.github.safetimamovic.throwablemobs.client.CarryKeys;
import io.github.safetimamovic.throwablemobs.client.ThrowableMobsClientCommand;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
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

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class ThrowableMobsNeoForgeClient
{
        public ThrowableMobsNeoForgeClient(IEventBus modEventBus, ModContainer container)
        {
                //? if >=26.1 {
                CarryClient.init(ClientPacketDistributor::sendToServer);
                //?} else
                /*CarryClient.init(payload -> PacketDistributor.sendToServer(payload));*/
                container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
                ThrowableMobsClientCommand.init(parent -> new ConfigurationScreen(container, parent));
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
}
