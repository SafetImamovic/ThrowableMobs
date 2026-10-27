package io.github.safetimamovic.throwablemobs.mixin;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.carry.CarryState;
import io.github.safetimamovic.throwablemobs.carry.ThrowState;
import io.github.safetimamovic.throwablemobs.platform.legacy.EntityStates;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Holds and saves {@link EntityStates} (1.20.1 only), each under its attachment's ID from newer versions. */
@Mixin(Entity.class)
public abstract class EntityStateMixin implements EntityStates
{
        @Unique
        private static final String CARRY_STATE = Constants.id("carry_state").toString();
        @Unique
        private static final String THROW_STATE = Constants.id("throw_state").toString();
        @Unique
        private static final String CARRY_STASH = Constants.id("carry_stash").toString();

        @Unique
        private @Nullable CarryState throwablemobs$carryState;
        @Unique
        private @Nullable ThrowState throwablemobs$throwState;
        @Unique
        private @Nullable CompoundTag throwablemobs$carryStash;

        @Override
        public @Nullable CarryState throwablemobs$carryState()
        {
                return throwablemobs$carryState;
        }

        @Override
        public void throwablemobs$setCarryState(@Nullable CarryState state)
        {
                throwablemobs$carryState = state;
        }

        @Override
        public @Nullable ThrowState throwablemobs$throwState()
        {
                return throwablemobs$throwState;
        }

        @Override
        public void throwablemobs$setThrowState(@Nullable ThrowState state)
        {
                throwablemobs$throwState = state;
        }

        @Override
        public @Nullable CompoundTag throwablemobs$carryStash()
        {
                return throwablemobs$carryStash;
        }

        @Override
        public void throwablemobs$setCarryStash(@Nullable CompoundTag mob)
        {
                throwablemobs$carryStash = mob;
        }

        @Inject(method = "saveWithoutId", at = @At("RETURN"))
        private void throwablemobs$save(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir)
        {
                throwablemobs$put(tag, CARRY_STATE, CarryState.CODEC, throwablemobs$carryState);
                throwablemobs$put(tag, THROW_STATE, ThrowState.CODEC, throwablemobs$throwState);
                if (throwablemobs$carryStash != null)
                {
                        tag.put(CARRY_STASH, throwablemobs$carryStash.copy());
                }
        }

        @Inject(method = "load", at = @At("RETURN"))
        private void throwablemobs$load(CompoundTag tag, CallbackInfo ci)
        {
                throwablemobs$carryState = throwablemobs$get(tag, CARRY_STATE, CarryState.CODEC);
                throwablemobs$throwState = throwablemobs$get(tag, THROW_STATE, ThrowState.CODEC);
                throwablemobs$carryStash = tag.contains(CARRY_STASH, CompoundTag.TAG_COMPOUND)
                        ? tag.getCompound(CARRY_STASH).copy() : null;
        }

        @Unique
        private static <T> void throwablemobs$put(CompoundTag tag, String key, Codec<T> codec, @Nullable T value)
        {
                if (value != null)
                {
                        codec.encodeStart(NbtOps.INSTANCE, value).resultOrPartial(Constants.LOG::error)
                                .ifPresent(encoded -> tag.put(key, encoded));
                }
        }

        @Unique
        private static <T> @Nullable T throwablemobs$get(CompoundTag tag, String key, Codec<T> codec)
        {
                if (!tag.contains(key))
                {
                        return null;
                }
                return codec.parse(NbtOps.INSTANCE, tag.get(key)).resultOrPartial(Constants.LOG::error).orElse(null);
        }
}
