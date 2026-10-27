package io.github.safetimamovic.throwablemobs;

import io.github.safetimamovic.throwablemobs.behavior.MobProfileReloadListener;
import io.github.safetimamovic.throwablemobs.behavior.MobProfiles;
import io.github.safetimamovic.throwablemobs.carry.CarryManager;
import io.github.safetimamovic.throwablemobs.carry.ThrowManager;
import io.github.safetimamovic.throwablemobs.config.CreatorConfig;
import io.github.safetimamovic.throwablemobs.config.PlayerConfig;
import io.github.safetimamovic.throwablemobs.network.ChargeThrowPayload;
import io.github.safetimamovic.throwablemobs.network.DropCarriedPayload;
import io.github.safetimamovic.throwablemobs.network.PickUpPayload;
import io.github.safetimamovic.throwablemobs.network.PlayerEnabledPayload;
import io.github.safetimamovic.throwablemobs.network.ProfileSyncPayload;
import io.github.safetimamovic.throwablemobs.network.RadarStatusPayload;
import io.github.safetimamovic.throwablemobs.network.ThrowPayload;
import io.github.safetimamovic.throwablemobs.platform.ForgeNetwork;
import io.github.safetimamovic.throwablemobs.platform.legacy.EntityStateSyncPayload;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;

/** MinecraftForge 1.20.1 (and NeoForge 1.20.1, which runs Forge mods); newer versions are the NeoForge build. */
@Mod(Constants.MOD_ID)
public class ThrowableMobsForge
{
        public ThrowableMobsForge()
        {
                IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
                ThrowableMobs.init();
                // Per world, in serverconfig/, and synced to clients; same file names as on the other loaders.
                ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, CreatorConfig.SPEC, CreatorConfig.FILE_NAME);
                ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, PlayerConfig.SPEC, PlayerConfig.FILE_NAME);
                // Fires once per registry; RegisterEvent.register ignores keys of the other registries.
                modEventBus.addListener((RegisterEvent event) -> ModRegistries.registerAll(new ModRegistries.Registrar()
                {
                        @Override
                        public <T> void register(ResourceKey<? extends Registry<T>> registry, Identifier id, T value)
                        {
                                event.register(registry, id, () -> value);
                        }
                }));

                ForgeNetwork.toServer(DropCarriedPayload.TYPE, DropCarriedPayload.STREAM_CODEC,
                        (payload, player) -> CarryManager.onDropRequest(player));
                ForgeNetwork.toServer(PickUpPayload.TYPE, PickUpPayload.STREAM_CODEC,
                        (payload, player) -> CarryManager.onPickUpRequest(player, payload.entityId()));
                ForgeNetwork.toServer(ChargeThrowPayload.TYPE, ChargeThrowPayload.STREAM_CODEC,
                        (payload, player) -> ThrowManager.onChargeRequest(player));
                ForgeNetwork.toServer(PlayerEnabledPayload.TYPE, PlayerEnabledPayload.STREAM_CODEC,
                        (payload, player) -> CarryManager.onPlayerEnabled(player, payload.enabled()));
                ForgeNetwork.toServer(ThrowPayload.TYPE, ThrowPayload.STREAM_CODEC,
                        (payload, player) -> ThrowManager.onThrowRequest(player));
                ForgeNetwork.toClient(ProfileSyncPayload.TYPE, ProfileSyncPayload.STREAM_CODEC);
                ForgeNetwork.toClient(RadarStatusPayload.TYPE, RadarStatusPayload.STREAM_CODEC);
                // No synced attachments on 1.20.1: the mod syncs carry and throw state itself.
                ForgeNetwork.toClient(EntityStateSyncPayload.TYPE, EntityStateSyncPayload.STREAM_CODEC);
                if (FMLEnvironment.dist.isClient())
                {
                        ThrowableMobsForgeClient.init(modEventBus);
                }

                IEventBus bus = MinecraftForge.EVENT_BUS;
                bus.addListener(ThrowableMobsForge::onEntityInteract);
                bus.addListener((RegisterCommandsEvent event) -> ThrowableMobsCommand.register(event.getDispatcher()));
                bus.addListener((AddReloadListenerEvent event) -> event.addListener(new MobProfileReloadListener()));
                bus.addListener((TagsUpdatedEvent event) ->
                {
                        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD)
                        {
                                MobProfiles.onTagsLoaded();
                        }
                });
                // On join and after every /reload, once the new tags and profiles are in.
                bus.addListener((OnDatapackSyncEvent event) ->
                {
                        List<ServerPlayer> players = event.getPlayer() != null ? List.of(event.getPlayer())
                                : event.getPlayerList().getPlayers();
                        players.forEach(player -> ForgeNetwork.sendToPlayer(player, MobProfiles.syncPayload()));
                });
                bus.addListener((LivingAttackEvent event) ->
                {
                        if (!CarryManager.allowDamage(event.getEntity(), event.getSource())
                                || !ThrowManager.allowDamage(event.getEntity(), event.getSource()))
                        {
                                event.setCanceled(true);
                        }
                });
                bus.addListener((TickEvent.ServerTickEvent event) ->
                {
                        if (event.phase == TickEvent.Phase.END)
                        {
                                CarryManager.tick(event.getServer());
                                ThrowManager.tick(event.getServer());
                        }
                });
                bus.addListener((EntityJoinLevelEvent event) ->
                {
                        if (!event.getLevel().isClientSide())
                        {
                                CarryManager.onEntityLoad(event.getEntity());
                                ThrowManager.onEntityLoad(event.getEntity());
                        }
                });
                bus.addListener((PlayerEvent.StartTracking event) ->
                {
                        if (event.getEntity() instanceof ServerPlayer player)
                        {
                                EntityStateSyncPayload.onStartTracking(event.getTarget(), player);
                        }
                });
                bus.addListener((PlayerEvent.PlayerLoggedInEvent event) ->
                {
                        if (event.getEntity() instanceof ServerPlayer player)
                        {
                                CarryManager.onLogin(player);
                        }
                });
                bus.addListener((PlayerEvent.PlayerLoggedOutEvent event) ->
                {
                        if (event.getEntity() instanceof ServerPlayer player)
                        {
                                CarryManager.onLogout(player);
                                ThrowManager.onLogout(player);
                        }
                });
                bus.addListener((ServerStoppingEvent event) ->
                {
                        CarryManager.dropAll(event.getServer());
                        ThrowManager.clear();
                });
        }

        private static void onEntityInteract(PlayerInteractEvent.EntityInteract event)
        {
                InteractionResult result = CarryManager.onUseEntity(event.getTarget());
                if (result != InteractionResult.PASS)
                {
                        event.setCancellationResult(result);
                        event.setCanceled(true);
                }
        }
}
