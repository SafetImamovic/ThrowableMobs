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
import io.github.safetimamovic.throwablemobs.network.ProfileSyncPayload;
import io.github.safetimamovic.throwablemobs.network.ThrowPayload;
import io.github.safetimamovic.throwablemobs.platform.NeoForgeCarryStateAccess;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Constants.MOD_ID)
public class ThrowableMobsNeoForge
{
        public ThrowableMobsNeoForge(IEventBus modEventBus, ModContainer container)
        {
                ThrowableMobs.init();
                // SYNCED: NeoForge's synced-to-clients config (called SERVER on Fabric's port); same file names on both.
                container.registerConfig(ModConfig.Type.SYNCED, CreatorConfig.SPEC, CreatorConfig.FILE_NAME);
                container.registerConfig(ModConfig.Type.CLIENT, PlayerConfig.SPEC, PlayerConfig.FILE_NAME);
                NeoForgeCarryStateAccess.ATTACHMENT_TYPES.register(modEventBus);
                // Fires once per registry; RegisterEvent.register ignores keys of the other registries.
                modEventBus.addListener((RegisterEvent event) -> ModRegistries.registerAll(new ModRegistries.Registrar()
                {
                        @Override
                        public <T> void register(ResourceKey<? extends Registry<T>> registry, Identifier id, T value)
                        {
                                event.register(registry, id, () -> value);
                        }
                }));
                modEventBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1")
                        .playToServer(DropCarriedPayload.TYPE, DropCarriedPayload.STREAM_CODEC,
                                (payload, context) -> CarryManager.onDropRequest((ServerPlayer) context.player()))
                        .playToServer(PickUpPayload.TYPE, PickUpPayload.STREAM_CODEC,
                                (payload, context) -> CarryManager.onPickUpRequest((ServerPlayer) context.player(),
                                        payload.entityId()))
                        .playToServer(ChargeThrowPayload.TYPE, ChargeThrowPayload.STREAM_CODEC,
                                (payload, context) -> ThrowManager.onChargeRequest((ServerPlayer) context.player()))
                        .playToServer(ThrowPayload.TYPE, ThrowPayload.STREAM_CODEC,
                                (payload, context) -> ThrowManager.onThrowRequest((ServerPlayer) context.player()))
                        .playToClient(ProfileSyncPayload.TYPE, ProfileSyncPayload.STREAM_CODEC,
                                (payload, context) -> MobProfiles.onSync(payload)));

                IEventBus bus = NeoForge.EVENT_BUS;
                bus.addListener(ThrowableMobsNeoForge::onEntityInteract);
                bus.addListener((RegisterCommandsEvent event) -> ThrowableMobsCommand.register(event.getDispatcher()));
                bus.addListener((AddServerReloadListenersEvent event) -> event
                        .addListener(MobProfileReloadListener.ID, new MobProfileReloadListener()));
                bus.addListener((TagsUpdatedEvent.ServerDataLoad event) -> MobProfiles.onTagsLoaded());
                // On join and after every /reload, once the new tags and profiles are in.
                bus.addListener((OnDatapackSyncEvent event) -> event.getRelevantPlayers()
                        .forEach(player -> PacketDistributor.sendToPlayer(player, MobProfiles.syncPayload())));
                bus.addListener((LivingIncomingDamageEvent event) ->
                {
                        if (!CarryManager.allowDamage(event.getEntity(), event.getSource())
                                || !ThrowManager.allowDamage(event.getEntity(), event.getSource()))
                        {
                                event.setCanceled(true);
                        }
                });
                bus.addListener((ServerTickEvent.Post event) ->
                {
                        CarryManager.tick(event.getServer());
                        ThrowManager.tick(event.getServer());
                });
                bus.addListener((EntityJoinLevelEvent event) ->
                {
                        if (!event.getLevel().isClientSide())
                        {
                                CarryManager.onEntityLoad(event.getEntity());
                                ThrowManager.onEntityLoad(event.getEntity());
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
