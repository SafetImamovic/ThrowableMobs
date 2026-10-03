package io.github.safetimamovic.throwablemobs;

import io.github.safetimamovic.throwablemobs.carry.CarryManager;
import io.github.safetimamovic.throwablemobs.network.DropCarriedPayload;
import io.github.safetimamovic.throwablemobs.platform.NeoForgeCarryStateAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(Constants.MOD_ID)
public class ThrowableMobsNeoForge
{
        public ThrowableMobsNeoForge(IEventBus modEventBus)
        {
                ThrowableMobs.init();
                NeoForgeCarryStateAccess.ATTACHMENT_TYPES.register(modEventBus);
                modEventBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1")
                        .playToServer(DropCarriedPayload.TYPE, DropCarriedPayload.STREAM_CODEC,
                                (payload, context) -> CarryManager.onDropRequest((ServerPlayer) context.player())));

                IEventBus bus = NeoForge.EVENT_BUS;
                bus.addListener(ThrowableMobsNeoForge::onEntityInteract);
                bus.addListener((PlayerInteractEvent.RightClickBlock event) ->
                {
                        InteractionResult result = CarryManager.onUseBlock(event.getEntity(), event.getLevel());
                        if (result != InteractionResult.PASS)
                        {
                                event.setCancellationResult(result);
                                event.setCanceled(true);
                        }
                });
                bus.addListener((LivingIncomingDamageEvent event) ->
                {
                        if (!CarryManager.allowDamage(event.getEntity(), event.getSource()))
                        {
                                event.setCanceled(true);
                        }
                });
                bus.addListener((ServerTickEvent.Post event) -> CarryManager.tick(event.getServer()));
                bus.addListener((EntityJoinLevelEvent event) ->
                {
                        if (!event.getLevel().isClientSide())
                        {
                                CarryManager.onEntityLoad(event.getEntity());
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
                        }
                });
                bus.addListener((ServerStoppingEvent event) -> CarryManager.dropAll());
        }

        private static void onEntityInteract(PlayerInteractEvent.EntityInteract event)
        {
                InteractionResult result = CarryManager.onUseEntity(event.getEntity(), event.getLevel(),
                        event.getHand(),
                        event.getTarget());
                if (result != InteractionResult.PASS)
                {
                        event.setCancellationResult(result);
                        event.setCanceled(true);
                }
        }
}
