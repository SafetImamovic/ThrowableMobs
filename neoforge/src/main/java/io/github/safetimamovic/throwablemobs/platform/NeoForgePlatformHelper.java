package io.github.safetimamovic.throwablemobs.platform;

import io.github.safetimamovic.throwablemobs.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgePlatformHelper implements IPlatformHelper
{

        @Override
        public String getPlatformName()
        {

                return "NeoForge";
        }

        @Override
        public boolean isModLoaded(String modId)
        {

                return ModList.get().isLoaded(modId);
        }

        @Override
        public boolean isDevelopmentEnvironment()
        {

                //? if >=26.1 {
                return !FMLLoader.getCurrent().isProduction();
                //?} else
                /*return !FMLLoader.isProduction();*/
        }

        @Override
        public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload)
        {
                PacketDistributor.sendToPlayer(player, payload);
        }
}
