//? if >=1.20.5 {
// Before 1.20.5 Services uses the shared platform.legacy implementation instead.
package io.github.safetimamovic.throwablemobs.platform;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.platform.services.ICarryStashAccess;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public final class FabricCarryStashAccess implements ICarryStashAccess
{
        // Registered when Services loads this class during mod init. Server-side only, so not synced.
        private static final AttachmentType<CompoundTag> CARRY_STASH = AttachmentRegistry.create(
                Constants.id("carry_stash"),
                builder -> builder.persistent(CompoundTag.CODEC));

        @Override
        public @Nullable CompoundTag get(Player player)
        {
                return player.getAttached(CARRY_STASH);
        }

        @Override
        public void set(Player player, CompoundTag mob)
        {
                player.setAttached(CARRY_STASH, mob);
        }

        @Override
        public void remove(Player player)
        {
                player.removeAttached(CARRY_STASH);
        }
}
//?}
