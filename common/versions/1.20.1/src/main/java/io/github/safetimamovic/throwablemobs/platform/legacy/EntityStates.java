package io.github.safetimamovic.throwablemobs.platform.legacy;

import io.github.safetimamovic.throwablemobs.carry.CarryState;
import io.github.safetimamovic.throwablemobs.carry.ThrowState;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

/**
 * The mod's state on an entity, for 1.20.1: there the loaders have no synced data attachments, so a mixin
 * ({@code EntityStateMixin}) adds these to every entity and saves them with it, on Fabric and Forge alike.
 */
public interface EntityStates
{
        @Nullable CarryState throwablemobs$carryState();

        void throwablemobs$setCarryState(@Nullable CarryState state);

        @Nullable ThrowState throwablemobs$throwState();

        void throwablemobs$setThrowState(@Nullable ThrowState state);

        @Nullable CompoundTag throwablemobs$carryStash();

        void throwablemobs$setCarryStash(@Nullable CompoundTag mob);
}
