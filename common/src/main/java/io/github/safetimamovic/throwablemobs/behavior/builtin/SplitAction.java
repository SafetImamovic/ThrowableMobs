package io.github.safetimamovic.throwablemobs.behavior.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.api.BehaviorAction;
import io.github.safetimamovic.throwablemobs.api.BehaviorType;
import io.github.safetimamovic.throwablemobs.api.ImpactContext;
import io.github.safetimamovic.throwablemobs.api.ImpactResult;
import io.github.safetimamovic.throwablemobs.api.ThrowContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
//? if >=26.1 {
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
//?} else
/*import net.minecraft.world.entity.monster.Slime;*/

/**
 * {@code throwablemobs:split}: a slime or magma cube bigger than the smallest size splits into {@code min_pieces} to
 * {@code max_pieces} cubes of half its size. The thrown mob shrinks and stays one of them, so its own data survives;
 * the others are new and get its name and persistence. On an impact it ends the flight. Other mobs are ignored.
 */
public record SplitAction(int minPieces, int maxPieces) implements BehaviorAction
{
        public static final MapCodec<SplitAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.intRange(2, 16).optionalFieldOf("min_pieces", 2).forGetter(SplitAction::minPieces),
                Codec.intRange(2, 16).optionalFieldOf("max_pieces", 4).forGetter(SplitAction::maxPieces))
                .apply(instance, SplitAction::new));

        public static final BehaviorType<SplitAction> TYPE = new BehaviorType<>(Constants.id("split"), CODEC);

        @Override
        public BehaviorType<?> type()
        {
                return TYPE;
        }

        @Override
        public void run(ThrowContext ctx)
        {
                split(ctx.level(), ctx);
        }

        @Override
        public ImpactResult onImpact(ImpactContext ctx, ImpactResult current)
        {
                return split(ctx.level(), ctx) ? current.land() : current;
        }

        private boolean split(ServerLevel level, ThrowContext ctx)
        {
                //? if >=26.1 {
                if (!(ctx.mob() instanceof AbstractCubeMob cube) || cube.getSize() <= 1)
                //?} else
                /*if (!(ctx.mob() instanceof Slime cube) || cube.getSize() <= 1)*/
                {
                        return false;
                }
                RandomSource random = cube.getRandom();
                int size = cube.getSize() / 2;
                int pieces = minPieces + random.nextInt(Math.max(1, maxPieces - minPieces + 1));
                cube.setSize(size, true);
                for (int i = 1; i < pieces; i++)
                {
                        //? if >=26.1 {
                        AbstractCubeMob piece = cube.getType().create(level, EntitySpawnReason.TRIGGERED);
                        //?} else
                        /*Slime piece = cube.getType().create(level);*/
                        if (piece == null)
                        {
                                continue;
                        }
                        piece.setSize(size, true);
                        piece.setCustomName(cube.getCustomName());
                        piece.setCustomNameVisible(cube.isCustomNameVisible());
                        if (cube.isPersistenceRequired())
                        {
                                piece.setPersistenceRequired();
                        }
                        double spread = cube.getBbWidth() / 2.0;
                        double x = cube.getX() + (random.nextDouble() - 0.5) * spread;
                        double z = cube.getZ() + (random.nextDouble() - 0.5) * spread;
                        //? if >=26.1 {
                        piece.snapTo(x, cube.getY() + 0.5, z, random.nextFloat() * 360.0F, 0.0F);
                        //?} else
                        /*piece.moveTo(x, cube.getY() + 0.5, z, random.nextFloat() * 360.0F, 0.0F);*/
                        level.addFreshEntity(piece);
                }
                return true;
        }
}
