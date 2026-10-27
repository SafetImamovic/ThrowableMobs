package io.github.safetimamovic.throwablemobs.platform.network;

import io.github.safetimamovic.throwablemobs.network.codec.CustomPacketPayload;
import io.github.safetimamovic.throwablemobs.platform.legacy.PayloadCodecs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The part of Fabric API's {@code ServerPlayNetworking} (1.20.5+) the mod uses, for 1.20.1: payloads go over a
 * channel named after their type, and handlers run on the server thread like they do on 1.20.5+.
 */
public final class ServerPlayNetworking
{
        private ServerPlayNetworking()
        {
        }

        @SuppressWarnings("unchecked")
        public static <T extends CustomPacketPayload> void registerGlobalReceiver(CustomPacketPayload.Type<T> type,
                PlayPayloadHandler<T> handler)
        {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(type.id(),
                        (server, player, listener, buf, sender) ->
                        {
                                T payload = (T) PayloadCodecs.read(type.id(), buf);
                                server.execute(() -> handler.receive(payload, new Context(server, player)));
                        });
        }

        public static boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type)
        {
                return net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, type.id());
        }

        public static void send(ServerPlayer player, CustomPacketPayload payload)
        {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload.type().id(),
                        PayloadCodecs.write(payload));
        }

        @FunctionalInterface
        public interface PlayPayloadHandler<T extends CustomPacketPayload>
        {
                void receive(T payload, Context context);
        }

        public record Context(MinecraftServer server, ServerPlayer player)
        {
        }
}
