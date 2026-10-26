package io.github.safetimamovic.throwablemobs;

//? if >=26.1 {
import fuzs.forgeconfigapiport.fabric.api.v5.client.ConfigScreenFactoryRegistry;
//?} else
/*import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.client.ConfigScreenFactoryRegistry;*/
import io.github.safetimamovic.throwablemobs.behavior.MobProfiles;
import io.github.safetimamovic.throwablemobs.carry.RadarStatus;
import io.github.safetimamovic.throwablemobs.client.CarryClient;
import io.github.safetimamovic.throwablemobs.client.CarryKeys;
import io.github.safetimamovic.throwablemobs.client.ConfigScreens;
import io.github.safetimamovic.throwablemobs.client.ThrowableMobsClientCommand;
import io.github.safetimamovic.throwablemobs.network.ProfileSyncPayload;
import io.github.safetimamovic.throwablemobs.network.RadarStatusPayload;
//? if >=26.1
import net.minecraft.client.KeyMapping;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
//?} else {
/*import io.github.safetimamovic.throwablemobs.client.ShapeCanvas;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
*///?}
import java.util.function.UnaryOperator;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen.Context;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen.Element;
import org.jspecify.annotations.Nullable;

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
                ClientPlayNetworking.registerGlobalReceiver(RadarStatusPayload.TYPE,
                        (payload, context) -> RadarStatus.onStatus(payload, context.player().level().getGameTime()));
                // Opened from Mod Menu, through Forge Config API Port.
                UnaryOperator<@Nullable Screen> configScreen = ConfigScreens.withYacl(
                        parent -> new ConfigurationScreen(Constants.MOD_ID, parent, ThrowableMobsFabricClient::filter));
                ConfigScreenFactoryRegistry.INSTANCE.register(Constants.MOD_ID,
                        (modId, parent) -> configScreen.apply(parent));
                ThrowableMobsClientCommand.init(configScreen);
                ClientCommandRegistrationCallback.EVENT
                        .register((dispatcher, buildContext) -> ThrowableMobsClientCommand.register(dispatcher));
                //? if >=26.1 {
                KeyMapping.Category.register(CarryKeys.CATEGORY.id());
                KeyMappingHelper.registerKeyMapping(CarryKeys.MODIFIER);
                KeyMappingHelper.registerKeyMapping(CarryKeys.PICK_UP);
                KeyMappingHelper.registerKeyMapping(CarryKeys.PUT_DOWN);
                KeyMappingHelper.registerKeyMapping(CarryKeys.THROW);
                KeyMappingHelper.registerKeyMapping(CarryKeys.CANCEL_THROW);
                //?} else {
                /*KeyBindingHelper.registerKeyBinding(CarryKeys.MODIFIER);
                KeyBindingHelper.registerKeyBinding(CarryKeys.PICK_UP);
                KeyBindingHelper.registerKeyBinding(CarryKeys.PUT_DOWN);
                KeyBindingHelper.registerKeyBinding(CarryKeys.THROW);
                KeyBindingHelper.registerKeyBinding(CarryKeys.CANCEL_THROW);
                *///?}
                ClientTickEvents.START_CLIENT_TICK.register(CarryClient::tickInput);
                ClientTickEvents.END_CLIENT_TICK.register(CarryClient::tick);
                //? if >=26.1 {
                HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Constants.id("carry_prompt"),
                        CarryClient::renderPrompt);
                //?} else {
                /*// No HUD layers before 1.21.6: drawn after the whole vanilla HUD.
                HudRenderCallback.EVENT.register(CarryClient::renderPrompt);
                // The trajectory preview, drawn by vanilla's gizmos from 26.1 on.
                WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> ShapeCanvas.render());
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
