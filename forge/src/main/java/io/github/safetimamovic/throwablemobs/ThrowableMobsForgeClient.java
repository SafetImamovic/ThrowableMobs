package io.github.safetimamovic.throwablemobs;

import io.github.safetimamovic.throwablemobs.behavior.MobProfiles;
import io.github.safetimamovic.throwablemobs.carry.RadarStatus;
import io.github.safetimamovic.throwablemobs.client.CarryClient;
import io.github.safetimamovic.throwablemobs.client.CarryKeys;
import io.github.safetimamovic.throwablemobs.client.ConfigScreens;
import io.github.safetimamovic.throwablemobs.client.ShapeCanvas;
import io.github.safetimamovic.throwablemobs.client.ThrowableMobsClientCommand;
import io.github.safetimamovic.throwablemobs.network.ProfileSyncPayload;
import io.github.safetimamovic.throwablemobs.network.RadarStatusPayload;
import io.github.safetimamovic.throwablemobs.platform.ForgeNetwork;
import io.github.safetimamovic.throwablemobs.platform.Services;
import io.github.safetimamovic.throwablemobs.platform.legacy.EntityStateClient;
import io.github.safetimamovic.throwablemobs.platform.legacy.EntityStateSyncPayload;
import io.github.safetimamovic.throwablemobs.platform.legacy.PayloadCodecs;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import org.jspecify.annotations.Nullable;

/** Client setup on Forge 1.20.1, called from {@link ThrowableMobsForge} on the client only. */
final class ThrowableMobsForgeClient
{
        private static final String YACL_ID = "yet_another_config_lib_v3";

        private ThrowableMobsForgeClient()
        {
        }

        static void init(IEventBus modEventBus)
        {
                // Servers without the mod don't have the channels.
                CarryClient.init(payload ->
                {
                        ClientPacketListener connection = Minecraft.getInstance().getConnection();
                        if (connection != null && ForgeNetwork.present(payload.type(), connection.getConnection()))
                        {
                                connection.send(new ServerboundCustomPayloadPacket(payload.type().id(),
                                        PayloadCodecs.write(payload)));
                        }
                });
                ForgeNetwork.onClient(ProfileSyncPayload.TYPE, MobProfiles::onSync);
                ForgeNetwork.onClient(RadarStatusPayload.TYPE, payload ->
                {
                        Minecraft minecraft = Minecraft.getInstance();
                        if (minecraft.level != null)
                        {
                                RadarStatus.onStatus(payload, minecraft.level.getGameTime());
                        }
                });
                ForgeNetwork.onClient(EntityStateSyncPayload.TYPE, EntityStateClient::onSync);

                // Forge 1.20.1 has no stock config screen: only YACL's, and the mod list's button only with it.
                UnaryOperator<@Nullable Screen> configScreen = ConfigScreens.withYacl(parent -> null);
                if (Services.PLATFORM.isModLoaded(YACL_ID))
                {
                        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                                () -> new ConfigScreenHandler.ConfigScreenFactory(
                                        (minecraft, parent) -> configScreen.apply(parent)));
                }
                ThrowableMobsClientCommand.init(configScreen);
                MinecraftForge.EVENT_BUS.addListener(
                        (RegisterClientCommandsEvent event) -> ThrowableMobsClientCommand.register(event.getDispatcher()));
                modEventBus.addListener((RegisterKeyMappingsEvent event) ->
                {
                        event.register(CarryKeys.MODIFIER);
                        event.register(CarryKeys.PICK_UP);
                        event.register(CarryKeys.PUT_DOWN);
                        event.register(CarryKeys.THROW);
                        event.register(CarryKeys.CANCEL_THROW);
                });
                MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) ->
                {
                        if (event.phase == TickEvent.Phase.START)
                        {
                                CarryClient.tickInput(Minecraft.getInstance());
                        }
                        else
                        {
                                CarryClient.tick(Minecraft.getInstance());
                        }
                });
                modEventBus.addListener((RegisterGuiOverlaysEvent event) -> event.registerAbove(
                        VanillaGuiOverlay.CROSSHAIR.id(), "carry_prompt",
                        (gui, graphics, partialTick, width, height) -> CarryClient.renderPrompt(graphics, partialTick)));
                // The trajectory preview.
                MinecraftForge.EVENT_BUS.addListener((RenderLevelStageEvent event) ->
                {
                        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
                        {
                                ShapeCanvas.render();
                        }
                });
        }
}
