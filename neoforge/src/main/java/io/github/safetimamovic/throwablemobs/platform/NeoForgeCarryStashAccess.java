package io.github.safetimamovic.throwablemobs.platform;

import io.github.safetimamovic.throwablemobs.platform.services.ICarryStashAccess;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jspecify.annotations.Nullable;

public final class NeoForgeCarryStashAccess implements ICarryStashAccess
{
        // Same register as the carry state; no meaningful default, only ever read with getExistingDataOrNull. Server-side
        // only, so not synced.
        private static final Supplier<AttachmentType<CompoundTag>> CARRY_STASH = NeoForgeCarryStateAccess.ATTACHMENT_TYPES
                .register("carry_stash", () -> AttachmentType.<CompoundTag>builder(() -> null)
                        .serialize(CompoundTag.CODEC.fieldOf("mob"))
                        .build());

        @Override
        public @Nullable CompoundTag get(Player player)
        {
                return player.getExistingDataOrNull(CARRY_STASH);
        }

        @Override
        public void set(Player player, CompoundTag mob)
        {
                player.setData(CARRY_STASH, mob);
        }

        @Override
        public void remove(Player player)
        {
                player.removeData(CARRY_STASH);
        }
}
