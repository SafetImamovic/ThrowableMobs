package io.github.safetimamovic.throwablemobs;

import io.github.safetimamovic.throwablemobs.carry.CarryManager;
import io.github.safetimamovic.throwablemobs.network.DropCarriedPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ThrowableMobsFabric implements ModInitializer
{
        @Override
        public void onInitialize()
        {
                ThrowableMobs.init();

                PayloadTypeRegistry.serverboundPlay().register(DropCarriedPayload.TYPE,
                        DropCarriedPayload.STREAM_CODEC);
                ServerPlayNetworking.registerGlobalReceiver(DropCarriedPayload.TYPE,
                        (payload, context) -> CarryManager.onDropRequest(context.player()));

                UseEntityCallback.EVENT.register(
                        (player, level, hand, entity, hitResult) -> CarryManager.onUseEntity(player, level, hand,
                                entity));
                UseBlockCallback.EVENT.register(
                        (player, level, hand, hitResult) -> CarryManager.onUseBlock(player, level));
                ServerTickEvents.END_SERVER_TICK.register(CarryManager::tick);
                ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> CarryManager.onEntityLoad(entity));
                ServerLivingEntityEvents.ALLOW_DAMAGE.register(
                        (entity, source, amount) -> CarryManager.allowDamage(entity, source));
                ServerPlayConnectionEvents.DISCONNECT
                        .register((listener, server) -> CarryManager.onLogout(listener.getPlayer()));
                ServerLifecycleEvents.SERVER_STOPPING.register(server -> CarryManager.dropAll());
        }
}
