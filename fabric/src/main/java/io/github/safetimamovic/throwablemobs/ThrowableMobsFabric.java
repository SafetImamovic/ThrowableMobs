package io.github.safetimamovic.throwablemobs;

//? if >=26.1 {
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
//?} else if >=1.20.2 {
/*import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
*///?} else {
/*import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import io.github.safetimamovic.throwablemobs.platform.legacy.EntityStateSyncPayload;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
*///?}
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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//? if >=26.1 {
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
//?} else {
/*import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
*///?}
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.neoforged.fml.config.ModConfig;

public class ThrowableMobsFabric implements ModInitializer
{
        @Override
        public void onInitialize()
        {
                ThrowableMobs.init();
                ModRegistries.registerAll(new ModRegistries.Registrar()
                {
                        @Override
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        public <T> void register(ResourceKey<? extends Registry<T>> registry, Identifier id, T value)
                        {
                                //? if >=26.1 {
                                Registry<T> target = (Registry<T>) BuiltInRegistries.REGISTRY
                                        .getValueOrThrow((ResourceKey) registry);
                                //?} else
                                /*Registry<T> target = (Registry<T>) BuiltInRegistries.REGISTRY.getOrThrow((ResourceKey) registry);*/
                                Registry.register(target, id, value);
                        }
                });
                //? if >=26.1 {
                ConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.SERVER, CreatorConfig.SPEC,
                        CreatorConfig.FILE_NAME);
                ConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.CLIENT, PlayerConfig.SPEC,
                        PlayerConfig.FILE_NAME);
                var serverbound = PayloadTypeRegistry.serverboundPlay();
                var clientbound = PayloadTypeRegistry.clientboundPlay();
                //?} else if >=1.20.2 {
                /*NeoForgeConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.SERVER, CreatorConfig.SPEC,
                        CreatorConfig.FILE_NAME);
                NeoForgeConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.CLIENT, PlayerConfig.SPEC,
                        PlayerConfig.FILE_NAME);
                var serverbound = PayloadTypeRegistry.playC2S();
                var clientbound = PayloadTypeRegistry.playS2C();
                *///?} else {
                /*ForgeConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.SERVER, CreatorConfig.SPEC,
                        CreatorConfig.FILE_NAME);
                ForgeConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.CLIENT, PlayerConfig.SPEC,
                        PlayerConfig.FILE_NAME);
                var serverbound = PayloadTypeRegistry.playC2S();
                var clientbound = PayloadTypeRegistry.playS2C();
                *///?}

                serverbound.register(DropCarriedPayload.TYPE, DropCarriedPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(DropCarriedPayload.TYPE,
                        (payload, context) -> CarryManager.onDropRequest(context.player()));
                serverbound.register(PickUpPayload.TYPE, PickUpPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(PickUpPayload.TYPE,
                        (payload, context) -> CarryManager.onPickUpRequest(context.player(), payload.entityId()));
                serverbound.register(ChargeThrowPayload.TYPE, ChargeThrowPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(ChargeThrowPayload.TYPE,
                        (payload, context) -> ThrowManager.onChargeRequest(context.player()));
                serverbound.register(PlayerEnabledPayload.TYPE, PlayerEnabledPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(PlayerEnabledPayload.TYPE,
                        (payload, context) -> CarryManager.onPlayerEnabled(context.player(), payload.enabled()));
                serverbound.register(ThrowPayload.TYPE, ThrowPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(ThrowPayload.TYPE,
                        (payload, context) -> ThrowManager.onThrowRequest(context.player()));

                clientbound.register(ProfileSyncPayload.TYPE, ProfileSyncPayload.STREAM_CODEC);
                clientbound.register(RadarStatusPayload.TYPE, RadarStatusPayload.STREAM_CODEC);
                //? if <1.20.5 {
                /*// No synced attachments before 1.20.5: the mod syncs carry and throw state itself.
                clientbound.register(EntityStateSyncPayload.TYPE, EntityStateSyncPayload.STREAM_CODEC);
                EntityTrackingEvents.START_TRACKING.register((entity, player) ->
                        EntityStateSyncPayload.onStartTracking(entity, player));
                // The client's channels only arrive after the join's data pack sync below, which therefore can't
                // send the profiles yet: send them once the client says it has the channel.
                S2CPlayChannelEvents.REGISTER.register((handler, sender, server, channels) ->
                {
                        if (channels.contains(ProfileSyncPayload.TYPE.id()))
                        {
                                server.execute(() -> ServerPlayNetworking.send(handler.getPlayer(),
                                        MobProfiles.syncPayload()));
                        }
                });
                *///?}
                //? if >=26.1 {
                ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(MobProfileReloadListener.ID,
                        new MobProfileReloadListener());
                //?} else {
                /*ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new FabricReloadListener());
                *///?}
                CommonLifecycleEvents.TAGS_LOADED.register((registries, client) ->
                {
                        if (!client)
                        {
                                MobProfiles.onTagsLoaded();
                        }
                });
                // On join and after every /reload, once the new tags and profiles are in.
                ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) ->
                {
                        if (ServerPlayNetworking.canSend(player, ProfileSyncPayload.TYPE))
                        {
                                ServerPlayNetworking.send(player, MobProfiles.syncPayload());
                        }
                });

                CommandRegistrationCallback.EVENT.register(
                        (dispatcher, registryAccess, environment) -> ThrowableMobsCommand.register(dispatcher));

                UseEntityCallback.EVENT.register(
                        (player, level, hand, entity, hitResult) -> CarryManager.onUseEntity(entity));
                ServerTickEvents.END_SERVER_TICK.register(CarryManager::tick);
                ServerTickEvents.END_SERVER_TICK.register(ThrowManager::tick);
                ServerEntityEvents.ENTITY_LOAD.register((entity, level) ->
                {
                        CarryManager.onEntityLoad(entity);
                        ThrowManager.onEntityLoad(entity);
                });
                ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                        CarryManager.allowDamage(entity, source) && ThrowManager.allowDamage(entity, source));
                ServerPlayConnectionEvents.JOIN
                        .register((listener, sender, server) -> CarryManager.onLogin(listener.getPlayer()));
                ServerPlayConnectionEvents.DISCONNECT
                        .register((listener, server) ->
                        {
                                CarryManager.onLogout(listener.getPlayer());
                                ThrowManager.onLogout(listener.getPlayer());
                        });
                ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                {
                        CarryManager.dropAll(server);
                        ThrowManager.clear();
                });
        }

        //? if <26.1 {
        /*/^* Fabric API's reload listeners before 26.1 need an ID of their own. *^/
        private static final class FabricReloadListener implements IdentifiableResourceReloadListener
        {
                private final MobProfileReloadListener listener = new MobProfileReloadListener();

                @Override
                public Identifier getFabricId()
                {
                        return MobProfileReloadListener.ID;
                }

                @Override
                public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
                        ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor,
                        Executor gameExecutor)
                {
                        return listener.reload(barrier, manager, preparationsProfiler, reloadProfiler, backgroundExecutor,
                                gameExecutor);
                }
        }
        *///?}
}
