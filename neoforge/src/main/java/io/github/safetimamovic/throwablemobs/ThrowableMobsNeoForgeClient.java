package io.github.safetimamovic.throwablemobs;

import io.github.safetimamovic.throwablemobs.client.CarryClient;
import io.github.safetimamovic.throwablemobs.client.CarryKeys;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class ThrowableMobsNeoForgeClient
{
        public ThrowableMobsNeoForgeClient(IEventBus modEventBus)
        {
                CarryClient.init(ClientPacketDistributor::sendToServer);
                modEventBus.addListener((RegisterKeyMappingsEvent event) ->
                {
                        event.registerCategory(CarryKeys.CATEGORY);
                        event.register(CarryKeys.MODIFIER);
                        event.register(CarryKeys.PICK_UP);
                        event.register(CarryKeys.PUT_DOWN);
                });
                NeoForge.EVENT_BUS
                        .addListener((ClientTickEvent.Pre event) -> CarryClient.tickInput(Minecraft.getInstance()));
                NeoForge.EVENT_BUS
                        .addListener((ClientTickEvent.Post event) -> CarryClient.tick(Minecraft.getInstance()));
                modEventBus
                        .addListener((RegisterGuiLayersEvent event) -> event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                                Constants.id("carry_prompt"), CarryClient::renderPrompt));
        }
}
