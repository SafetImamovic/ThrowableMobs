package io.github.safetimamovic.throwablemobs.behavior.builtin;

import com.mojang.serialization.MapCodec;
import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.api.BehaviorAction;
import io.github.safetimamovic.throwablemobs.api.BehaviorType;
import io.github.safetimamovic.throwablemobs.api.ThrowContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enderman;

/**
 * {@code throwablemobs:anger_thrower}: the mob goes after its thrower (not in creative or spectator). An enderman stays
 * angry like one you looked at.
 */
public record AngerThrowerAction() implements BehaviorAction
{
        public static final MapCodec<AngerThrowerAction> CODEC = MapCodec.unit(AngerThrowerAction::new);

        public static final BehaviorType<AngerThrowerAction> TYPE = new BehaviorType<>(Constants.id("anger_thrower"),
                CODEC);

        @Override
        public BehaviorType<?> type()
        {
                return TYPE;
        }

        @Override
        public void run(ThrowContext ctx)
        {
                ServerPlayer player = ctx.player();
                if (player == null || !player.isAlive() || player.isCreative() || player.isSpectator())
                {
                        return;
                }
                Mob mob = ctx.mob();
                mob.setLastHurtByMob(player);
                mob.setTarget(player);
                if (mob instanceof Enderman enderman)
                {
                        enderman.setPersistentAngerTarget(EntityReference.of(player));
                        enderman.startPersistentAngerTimer();
                }
        }
}
