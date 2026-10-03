package io.github.safetimamovic.throwablemobs;

import io.github.safetimamovic.throwablemobs.client.CarryClient;
import io.github.safetimamovic.throwablemobs.client.CarryKeys;
import net.minecraft.client.KeyMapping;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
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
                KeyMapping.Category.register(CarryKeys.CATEGORY.id());
                KeyMappingHelper.registerKeyMapping(CarryKeys.MODIFIER);
                KeyMappingHelper.registerKeyMapping(CarryKeys.PICK_UP);
                KeyMappingHelper.registerKeyMapping(CarryKeys.PUT_DOWN);
                ClientTickEvents.START_CLIENT_TICK.register(CarryClient::tickInput);
                ClientTickEvents.END_CLIENT_TICK.register(CarryClient::tick);
                HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Constants.id("carry_prompt"),
                        CarryClient::renderPrompt);
        }
}
