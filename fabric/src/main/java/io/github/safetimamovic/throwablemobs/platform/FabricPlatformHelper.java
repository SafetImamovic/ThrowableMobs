package io.github.safetimamovic.throwablemobs.platform;

import io.github.safetimamovic.throwablemobs.platform.services.IPlatformHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class FabricPlatformHelper implements IPlatformHelper
{

        @Override
        public String getPlatformName()
        {
                return "Fabric";
        }

        @Override
        public boolean isModLoaded(String modId)
        {

                return FabricLoader.getInstance().isModLoaded(modId);
        }

        @Override
        public boolean isDevelopmentEnvironment()
        {

                return FabricLoader.getInstance().isDevelopmentEnvironment();
        }

        @Override
        public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload)
        {
                // Fabric lets vanilla clients join; they don't know the payload.
                if (ServerPlayNetworking.canSend(player, payload.type()))
                {
                        ServerPlayNetworking.send(player, payload);
                }
        }
}
