package io.github.safetimamovic.throwablemobs.platform.services;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * The mob a player took along when they logged out ({@code on_carrier_logout = TAKE_ALONG}), saved as entity NBT with
 * the player until they log back in. Each loader backs this with its own data attachment API; persisted with the player,
 * not synced.
 */
public interface ICarryStashAccess
{
        @Nullable
        CompoundTag get(Player player);

        void set(Player player, CompoundTag mob);

        void remove(Player player);
}
