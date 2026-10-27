package io.github.safetimamovic.throwablemobs.platform;

import io.github.safetimamovic.throwablemobs.platform.services.IPlatformHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

public class ForgePlatformHelper implements IPlatformHelper
{

        @Override
        public String getPlatformName()
        {

                return "Forge";
        }

        @Override
        public boolean isModLoaded(String modId)
        {

                return ModList.get().isLoaded(modId);
        }

        @Override
        public boolean isDevelopmentEnvironment()
        {

                return !FMLLoader.isProduction();
        }

        @Override
        public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload)
        {
                ForgeNetwork.sendToPlayer(player, payload);
        }

        @Override
        public void sendToTracking(Entity entity, CustomPacketPayload payload)
        {
                ForgeNetwork.sendToTracking(entity, payload);
        }
}
