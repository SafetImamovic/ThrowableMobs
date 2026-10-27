package io.github.safetimamovic.throwablemobs.platform;

import io.github.safetimamovic.throwablemobs.platform.legacy.PayloadCodecs;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.event.EventNetworkChannel;

/**
 * The payloads on Forge 1.20.1: one raw channel per payload type, named after it, so the bytes match what the Fabric
 * build sends. Optional on both ends, so either side can be without the mod.
 */
public final class ForgeNetwork
{
        private static final String VERSION = "1";
        private static final Map<Identifier, EventNetworkChannel> CHANNELS = new ConcurrentHashMap<>();

        private ForgeNetwork()
        {
        }

        /** Client → server; {@code handler} runs on the server thread. */
        @SuppressWarnings("unchecked")
        public static <T extends CustomPacketPayload> void toServer(CustomPacketPayload.Type<T> type,
                StreamCodec<? super FriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler)
        {
                channel(type, codec).addListener((NetworkEvent event) ->
                {
                        NetworkEvent.Context context = event.getSource().get();
                        if (event.getPayload() == null || context.getDirection().getReceptionSide() != LogicalSide.SERVER)
                        {
                                return;
                        }
                        T payload = (T) PayloadCodecs.read(type.id(), event.getPayload());
                        ServerPlayer sender = context.getSender();
                        context.enqueueWork(() ->
                        {
                                if (sender != null)
                                {
                                        handler.accept(payload, sender);
                                }
                        });
                        context.setPacketHandled(true);
                });
        }

        /** Server → client. Registered on both sides, so each sees whether the other has the mod. */
        public static <T extends CustomPacketPayload> void toClient(CustomPacketPayload.Type<T> type,
                StreamCodec<? super FriendlyByteBuf, T> codec)
        {
                channel(type, codec);
        }

        /** On the client, for a payload registered with {@link #toClient}: {@code handler} runs on the client thread. */
        @SuppressWarnings("unchecked")
        public static <T extends CustomPacketPayload> void onClient(CustomPacketPayload.Type<T> type,
                Consumer<T> handler)
        {
                CHANNELS.get(type.id()).addListener((NetworkEvent event) ->
                {
                        NetworkEvent.Context context = event.getSource().get();
                        if (event.getPayload() == null || context.getDirection().getReceptionSide() != LogicalSide.CLIENT)
                        {
                                return;
                        }
                        T payload = (T) PayloadCodecs.read(type.id(), event.getPayload());
                        context.enqueueWork(() -> handler.accept(payload));
                        context.setPacketHandled(true);
                });
        }

        /** Whether the other end of {@code connection} has the mod (registered {@code type}'s channel). */
        public static boolean present(CustomPacketPayload.Type<?> type, Connection connection)
        {
                EventNetworkChannel channel = CHANNELS.get(type.id());
                return channel != null && channel.isRemotePresent(connection);
        }

        public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload)
        {
                if (present(payload.type(), player.connection.connection))
                {
                        player.connection.send(new ClientboundCustomPayloadPacket(payload.type().id(),
                                PayloadCodecs.write(payload)));
                }
        }

        /** To every player that sees {@code entity}. Clients without the mod drop the unknown payload. */
        public static void sendToTracking(Entity entity, CustomPacketPayload payload)
        {
                PacketDistributor.TRACKING_ENTITY.with(() -> entity)
                        .send(new ClientboundCustomPayloadPacket(payload.type().id(), PayloadCodecs.write(payload)));
        }

        private static <T extends CustomPacketPayload> EventNetworkChannel channel(CustomPacketPayload.Type<T> type,
                StreamCodec<? super FriendlyByteBuf, T> codec)
        {
                PayloadCodecs.register(type, codec);
                return CHANNELS.computeIfAbsent(type.id(), id -> NetworkRegistry.ChannelBuilder.named(type.id())
                        .networkProtocolVersion(() -> VERSION)
                        .clientAcceptedVersions(NetworkRegistry.acceptMissingOr(VERSION))
                        .serverAcceptedVersions(NetworkRegistry.acceptMissingOr(VERSION))
                        .eventNetworkChannel());
        }
}
