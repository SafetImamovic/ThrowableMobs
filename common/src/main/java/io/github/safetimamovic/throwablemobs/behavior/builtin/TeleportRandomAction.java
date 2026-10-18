package io.github.safetimamovic.throwablemobs.behavior.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.advancement.ModTriggers;
import io.github.safetimamovic.throwablemobs.advancement.ThrowEventTrigger.Event;
import io.github.safetimamovic.throwablemobs.api.BehaviorAction;
import io.github.safetimamovic.throwablemobs.api.BehaviorType;
import io.github.safetimamovic.throwablemobs.api.ImpactContext;
import io.github.safetimamovic.throwablemobs.api.ImpactResult;
import io.github.safetimamovic.throwablemobs.api.ThrowContext;
import io.github.safetimamovic.throwablemobs.tags.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * {@code throwablemobs:teleport_random}: the mob teleports to a free spot on the ground up to {@code range} blocks
 * away, like an enderman. On an impact it ends the flight there, and with {@code cancel_damage} takes no impact damage.
 * Mobs in {@code #throwablemobs:no_teleport} (by default {@code #c:teleporting_not_supported}) stay put.
 */
public record TeleportRandomAction(int range, boolean cancelDamage) implements BehaviorAction
{
        private static final int ATTEMPTS = 16;

        public static final MapCodec<TeleportRandomAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(Codec.intRange(1, 64).optionalFieldOf("range", 16).forGetter(TeleportRandomAction::range),
                        Codec.BOOL.optionalFieldOf("cancel_damage", true).forGetter(TeleportRandomAction::cancelDamage))
                .apply(instance, TeleportRandomAction::new));

        public static final BehaviorType<TeleportRandomAction> TYPE = new BehaviorType<>(
                Constants.id("teleport_random"), CODEC);

        @Override
        public BehaviorType<?> type()
        {
                return TYPE;
        }

        @Override
        public void run(ThrowContext ctx)
        {
                teleport(ctx);
        }

        @Override
        public ImpactResult onImpact(ImpactContext ctx, ImpactResult current)
        {
                if (!teleport(ctx))
                {
                        return current;
                }
                return (cancelDamage ? current.withSelfDamage(0.0) : current).land();
        }

        /** {@link #teleport(ServerLevel, Mob)}, and Ender Toss for the player when it worked. */
        private boolean teleport(ThrowContext ctx)
        {
                if (!teleport(ctx.level(), ctx.mob()))
                {
                        return false;
                }
                if (ctx.player() != null)
                {
                        ModTriggers.THROW_EVENT.trigger(ctx.player(), Event.TELEPORT, ctx.mob(), 0.0, 0.0);
                }
                return true;
        }

        private boolean teleport(ServerLevel level, Mob mob)
        {
                if (mob.is(ModTags.NO_TELEPORT))
                {
                        return false;
                }
                RandomSource random = mob.getRandom();
                Vec3 from = mob.position();
                for (int i = 0; i < ATTEMPTS; i++)
                {
                        double x = from.x + (random.nextDouble() - 0.5) * 2.0 * range;
                        double z = from.z + (random.nextDouble() - 0.5) * 2.0 * range;
                        Vec3 to = ground(level, x, from.y + range / 2.0, z);
                        if (to == null)
                        {
                                continue;
                        }
                        AABB box = mob.getDimensions(mob.getPose()).makeBoundingBox(to);
                        if (level.noCollision(mob, box) && !level.containsAnyLiquid(box))
                        {
                                mob.teleportTo(to.x, to.y, to.z);
                                level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT,
                                        mob.getSoundSource(), 1.0F, 1.0F);
                                level.playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT,
                                        mob.getSoundSource(), 1.0F, 1.0F);
                                return true;
                        }
                }
                return false;
        }

        /** First spot going down from {@code y} with a solid top face under it, within {@code 2 × range} blocks. */
        private @Nullable Vec3 ground(ServerLevel level, double x, double y, double z)
        {
                BlockPos.MutableBlockPos pos = BlockPos.containing(x, y, z).mutable();
                if (!level.hasChunkAt(pos))
                {
                        return null;
                }
                for (int step = 0; step < range * 2 && pos.getY() > level.getMinY(); step++)
                {
                        BlockPos below = pos.below();
                        if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP))
                        {
                                return new Vec3(x, pos.getY(), z);
                        }
                        pos.move(Direction.DOWN);
                }
                return null;
        }
}
