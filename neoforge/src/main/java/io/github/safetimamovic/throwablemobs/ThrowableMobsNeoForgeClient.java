package io.github.safetimamovic.throwablemobs;

import io.github.safetimamovic.throwablemobs.client.CarryClient;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
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
                NeoForge.EVENT_BUS
                        .addListener((ClientTickEvent.Post event) -> CarryClient.tick(Minecraft.getInstance()));
                modEventBus
                        .addListener((RegisterGuiLayersEvent event) -> event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                                Constants.id("carry_prompt"), CarryClient::renderPrompt));
        }
}
