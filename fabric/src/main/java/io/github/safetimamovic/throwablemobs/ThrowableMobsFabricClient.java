package io.github.safetimamovic.throwablemobs;

import io.github.safetimamovic.throwablemobs.client.CarryClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public class ThrowableMobsFabricClient implements ClientModInitializer
{
        @Override
        public void onInitializeClient()
        {
                // canSend: servers without the mod don't know the payload.
                CarryClient.init(payload ->
                {
                        if (ClientPlayNetworking.canSend(payload.type()))
                        {
                                ClientPlayNetworking.send(payload);
                        }
                });
                ClientTickEvents.END_CLIENT_TICK.register(CarryClient::tick);
                HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Constants.id("carry_prompt"),
                        CarryClient::renderPrompt);
        }
}
