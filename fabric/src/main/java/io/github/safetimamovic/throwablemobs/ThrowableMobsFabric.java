package io.github.safetimamovic.throwablemobs;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
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
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
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
                                Registry<T> target = (Registry<T>) BuiltInRegistries.REGISTRY
                                        .getValueOrThrow((ResourceKey) registry);
                                Registry.register(target, id, value);
                        }
                });
                ConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.SERVER, CreatorConfig.SPEC,
                        CreatorConfig.FILE_NAME);
                ConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.CLIENT, PlayerConfig.SPEC,
                        PlayerConfig.FILE_NAME);

                PayloadTypeRegistry.serverboundPlay().register(DropCarriedPayload.TYPE,
                        DropCarriedPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(DropCarriedPayload.TYPE,
                        (payload, context) -> CarryManager.onDropRequest(context.player()));
                PayloadTypeRegistry.serverboundPlay().register(PickUpPayload.TYPE, PickUpPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(PickUpPayload.TYPE,
                        (payload, context) -> CarryManager.onPickUpRequest(context.player(), payload.entityId()));
                PayloadTypeRegistry.serverboundPlay().register(ChargeThrowPayload.TYPE,
                        ChargeThrowPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(ChargeThrowPayload.TYPE,
                        (payload, context) -> ThrowManager.onChargeRequest(context.player()));
                PayloadTypeRegistry.serverboundPlay().register(ThrowPayload.TYPE, ThrowPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(ThrowPayload.TYPE,
                        (payload, context) -> ThrowManager.onThrowRequest(context.player()));

                PayloadTypeRegistry.clientboundPlay().register(ProfileSyncPayload.TYPE,
                        ProfileSyncPayload.STREAM_CODEC);
                ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(MobProfileReloadListener.ID,
                        new MobProfileReloadListener());
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
                        CarryManager.dropAll();
                        ThrowManager.clear();
                });
        }
}
