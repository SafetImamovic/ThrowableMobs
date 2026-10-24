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
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BaseFireBlock;
//? if >=26.1 {
import net.minecraft.world.level.gamerules.GameRules;
//?} else
/*import net.minecraft.world.level.GameRules;*/

/**
 * {@code throwablemobs:ignite}: sets the hit entity on fire for {@code seconds}. On a block hit with
 * {@code fire_on_block}, lights a fire where the mob landed, only with the {@code mobGriefing} gamerule. Outside
 * impacts it sets the mob itself on fire.
 */
public record IgniteAction(float seconds, boolean fireOnBlock) implements BehaviorAction
{
        public static final MapCodec<IgniteAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.floatRange(0.0F, 600.0F).optionalFieldOf("seconds", 4.0F).forGetter(IgniteAction::seconds),
                Codec.BOOL.optionalFieldOf("fire_on_block", false).forGetter(IgniteAction::fireOnBlock))
                .apply(instance, IgniteAction::new));

        public static final BehaviorType<IgniteAction> TYPE = new BehaviorType<>(Constants.id("ignite"), CODEC);

        @Override
        public BehaviorType<?> type()
        {
                return TYPE;
        }

        @Override
        public void run(ThrowContext ctx)
        {
                ctx.mob().igniteForSeconds(seconds);
        }

        @Override
        public ImpactResult onImpact(ImpactContext ctx, ImpactResult current)
        {
                Entity target = ctx.target();
                if (target != null)
                {
                        target.igniteForSeconds(seconds);
                }
                else if (fireOnBlock)
                {
                        lightFire(ctx.level(), ctx.mob().blockPosition());
                }
                return current;
        }

        private static void lightFire(ServerLevel level, BlockPos pos)
        {
                //? if >=26.1 {
                if (level.getGameRules().get(GameRules.MOB_GRIEFING) && level.getBlockState(pos).isAir())
                //?} else
                /*if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && level.getBlockState(pos).isAir())*/
                {
                        level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
                }
        }
}
