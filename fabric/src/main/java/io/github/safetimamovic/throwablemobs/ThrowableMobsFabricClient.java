package io.github.safetimamovic.throwablemobs;

import fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry;
import io.github.safetimamovic.throwablemobs.behavior.MobProfiles;
import io.github.safetimamovic.throwablemobs.client.CarryClient;
import io.github.safetimamovic.throwablemobs.client.CarryKeys;
import io.github.safetimamovic.throwablemobs.network.ProfileSyncPayload;
import net.minecraft.client.KeyMapping;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

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
                ClientPlayNetworking.registerGlobalReceiver(ProfileSyncPayload.TYPE,
                        (payload, context) -> MobProfiles.onSync(payload));
                // Opened from Mod Menu, through Forge Config API Port.
                ConfigScreenFactoryRegistry.INSTANCE.register(Constants.MOD_ID, ConfigurationScreen::new);
                KeyMapping.Category.register(CarryKeys.CATEGORY.id());
                KeyMappingHelper.registerKeyMapping(CarryKeys.MODIFIER);
                KeyMappingHelper.registerKeyMapping(CarryKeys.PICK_UP);
                KeyMappingHelper.registerKeyMapping(CarryKeys.PUT_DOWN);
                KeyMappingHelper.registerKeyMapping(CarryKeys.THROW);
                KeyMappingHelper.registerKeyMapping(CarryKeys.CANCEL_THROW);
                ClientTickEvents.START_CLIENT_TICK.register(CarryClient::tickInput);
                ClientTickEvents.END_CLIENT_TICK.register(CarryClient::tick);
                HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Constants.id("carry_prompt"),
                        CarryClient::renderPrompt);
        }
}
