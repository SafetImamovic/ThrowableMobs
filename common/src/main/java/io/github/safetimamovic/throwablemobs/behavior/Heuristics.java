package io.github.safetimamovic.throwablemobs.behavior;

import io.github.safetimamovic.throwablemobs.api.MobProfile;
import io.github.safetimamovic.throwablemobs.api.ThrowBehavior;
import io.github.safetimamovic.throwablemobs.api.Trigger;
import io.github.safetimamovic.throwablemobs.behavior.builtin.AngerThrowerAction;
import io.github.safetimamovic.throwablemobs.behavior.builtin.ExplodeAction;
import io.github.safetimamovic.throwablemobs.behavior.builtin.IgniteAction;
import io.github.safetimamovic.throwablemobs.behavior.builtin.SplitAction;
import io.github.safetimamovic.throwablemobs.behavior.builtin.TeleportRandomAction;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enderman;
//? if >=26.1 {
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
//?} else
/*import net.minecraft.world.entity.monster.Slime;*/

/**
 * Guessed behaviors for modded mobs without a profile, from what they extend: a modded creeper explodes, a modded
 * slime bounces and splits. The lowest layer above the defaults, so any profile overrides it. Vanilla mobs never get
 * guesses; their roster is set by our shipped profiles. Class checks only, so client and server guess the same.
 */
public final class Heuristics
{
        private static final Conditions ALWAYS = new Conditions(Optional.empty(), 1.0, Optional.empty(),
                Optional.empty(), false);

        private static final Conditions HARD_IMPACT = new Conditions(Optional.of(15.0), 1.0, Optional.empty(),
                Optional.empty(), false);

        private Heuristics()
        {
        }

        /** The guess for {@code mob}, or {@link MobProfile#EMPTY} for vanilla mobs or with auto-detect off. */
        public static MobProfile guess(Mob mob)
        {
                if (!ThrowableMobsConfig.behaviors().autoDetect()
                        || EntityType.getKey(mob.getType()).getNamespace().equals("minecraft"))
                {
                        return MobProfile.EMPTY;
                }
                MobProfile.Builder profile = MobProfile.builder();
                List<ThrowBehavior> behaviors = new ArrayList<>();
                if (mob instanceof Creeper)
                {
                        behaviors.add(new DataBehavior(Trigger.IMPACT, new Conditions(Optional.of(20.0), 1.0,
                                Optional.empty(), Optional.empty(), false),
                                new ExplodeAction(1.5F, 0.01F, 4.0F, 2.0F, false, true)));
                }
                //? if >=26.1 {
                if (mob instanceof AbstractCubeMob)
                //?} else
                /*if (mob instanceof Slime)*/
                {
                        profile.blockRestitution(0.8).maxBounces(5);
                        behaviors.add(new DataBehavior(Trigger.IMPACT, HARD_IMPACT, new SplitAction(2, 4)));
                }
                if (mob instanceof Enderman)
                {
                        behaviors.add(new DataBehavior(Trigger.IMPACT, ALWAYS, new TeleportRandomAction(16, true)));
                        behaviors.add(new DataBehavior(Trigger.IMPACT, ALWAYS, new AngerThrowerAction()));
                }
                if (mob.fireImmune())
                {
                        behaviors.add(new DataBehavior(Trigger.IMPACT_ENTITY, ALWAYS, new IgniteAction(4.0F, false)));
                }
                if (!behaviors.isEmpty())
                {
                        profile.behaviors(behaviors);
                }
                return profile.build();
        }
}
