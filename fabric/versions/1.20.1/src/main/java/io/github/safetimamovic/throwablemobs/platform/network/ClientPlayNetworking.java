package io.github.safetimamovic.throwablemobs.platform.network;

import io.github.safetimamovic.throwablemobs.network.codec.CustomPacketPayload;
import io.github.safetimamovic.throwablemobs.platform.legacy.PayloadCodecs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Client side of {@link ServerPlayNetworking}: Fabric API's {@code ClientPlayNetworking} (1.20.5+) for 1.20.1. */
public final class ClientPlayNetworking
{
        private ClientPlayNetworking()
        {
        }

        @SuppressWarnings("unchecked")
        public static <T extends CustomPacketPayload> void registerGlobalReceiver(CustomPacketPayload.Type<T> type,
                PlayPayloadHandler<T> handler)
        {
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(type.id(),
                        (client, listener, buf, sender) ->
                        {
                                T payload = (T) PayloadCodecs.read(type.id(), buf);
                                client.execute(() -> handler.receive(payload, new Context(client, client.player)));
                        });
        }

        public static boolean canSend(CustomPacketPayload.Type<?> type)
        {
                return net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(type.id());
        }

        public static void send(CustomPacketPayload payload)
        {
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload.type().id(),
                        PayloadCodecs.write(payload));
        }

        @FunctionalInterface
        public interface PlayPayloadHandler<T extends CustomPacketPayload>
        {
                void receive(T payload, Context context);
        }

        public record Context(Minecraft client, LocalPlayer player)
        {
        }
}
