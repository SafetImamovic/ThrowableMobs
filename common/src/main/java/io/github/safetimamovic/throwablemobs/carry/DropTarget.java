package io.github.safetimamovic.throwablemobs.carry;

import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.GroundFilter;
import io.github.safetimamovic.throwablemobs.tags.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Where a carried mob is put down: on the ground block the carrier looks at. Runs on both sides like
 * {@link CarryRules}: the client uses it for the prompt, the server to place the mob. Only reads synced data (blocks,
 * block tags, the carrier's view).
 */
public final class DropTarget
{
        private DropTarget()
        {
        }

        /**
         * Feet position for {@code mob} on the ground {@code player} looks at within block reach, or {@code null} if they
         * aren't looking at valid ground or the mob doesn't fit there. Looking at the top of a block puts the mob on it;
         * looking at the side of a block puts it on the ground in front of that side.
         */
        public static @Nullable Vec3 find(Player player, Mob mob)
        {
                Level level = player.level();
                Vec3 eye = player.getEyePosition();
                Vec3 reach = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
                BlockHitResult hit = level.clip(new ClipContext(eye, reach, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, player));
                if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() == Direction.DOWN)
                {
                        return null;
                }

                if (hit.getDirection() == Direction.UP)
                {
                        BlockPos ground = hit.getBlockPos();
                        if (!isValidGround(level, ground))
                        {
                                return null;
                        }
                        double y = top(level, ground);
                        // Exactly where the player looks, or centred on the block if the mob doesn't fit there.
                        Vec3 looked = new Vec3(hit.getLocation().x, y, hit.getLocation().z);
                        return fits(mob, looked) ? looked : fitsOrNull(mob, centeredOn(ground, y));
                }

                BlockPos ground = hit.getBlockPos().relative(hit.getDirection()).below();
                if (!isValidGround(level, ground))
                {
                        return null;
                }
                return fitsOrNull(mob, centeredOn(ground, top(level, ground)));
        }

        /** Has something to stand on and passes the configured deny / allow list. */
        public static boolean isValidGround(Level level, BlockPos pos)
        {
                BlockState state = level.getBlockState(pos);
                if (state.getCollisionShape(level, pos).isEmpty())
                {
                        return false;
                }
                return ThrowableMobsConfig.CARRY.dropGroundFilter() == GroundFilter.ALLOW_LIST
                        ? state.is(ModTags.DROP_GROUND_ALLOWED)
                        : !state.is(ModTags.DROP_GROUND_DENIED);
        }

        /** Top of the block's collision shape, so slabs, carpets and snow layers work. */
        private static double top(Level level, BlockPos pos)
        {
                VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
                return pos.getY() + shape.max(Direction.Axis.Y);
        }

        private static Vec3 centeredOn(BlockPos pos, double y)
        {
                return new Vec3(pos.getX() + 0.5, y, pos.getZ() + 0.5);
        }

        private static boolean fits(Mob mob, Vec3 feet)
        {
                return mob.level().noCollision(mob, mob.getDimensions(mob.getPose()).makeBoundingBox(feet));
        }

        private static @Nullable Vec3 fitsOrNull(Mob mob, Vec3 feet)
        {
                return fits(mob, feet) ? feet : null;
        }
}
