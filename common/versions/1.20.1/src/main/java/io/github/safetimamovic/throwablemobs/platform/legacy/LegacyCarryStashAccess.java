package io.github.safetimamovic.throwablemobs.platform.legacy;

import io.github.safetimamovic.throwablemobs.platform.services.ICarryStashAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/** {@link ICarryStashAccess} for 1.20.1, on both loaders; see {@link EntityStates}. Saved, not synced. */
public final class LegacyCarryStashAccess implements ICarryStashAccess
{
        @Override
        public @Nullable CompoundTag get(Player player)
        {
                return ((EntityStates) player).throwablemobs$carryStash();
        }

        @Override
        public void set(Player player, CompoundTag mob)
        {
                ((EntityStates) player).throwablemobs$setCarryStash(mob);
        }

        @Override
        public void remove(Player player)
        {
                ((EntityStates) player).throwablemobs$setCarryStash(null);
        }
}
