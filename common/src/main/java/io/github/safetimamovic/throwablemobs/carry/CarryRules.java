package io.github.safetimamovic.throwablemobs.carry;

import io.github.safetimamovic.throwablemobs.behavior.MobProfiles;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarryConfig;
import io.github.safetimamovic.throwablemobs.physics.ThrowPhysics;
import io.github.safetimamovic.throwablemobs.platform.Services;
import io.github.safetimamovic.throwablemobs.tags.ModTags;
import java.util.Locale;
//? if <26.1
/*import java.util.UUID;*/
import net.minecraft.network.chat.Component;
//? if >=26.1
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Who may pick up what. Runs on both sides: the client uses it for the on-screen prompt, the server as the authority.
 * Must therefore only read data the client also has (tags, hitbox, owner, leash, vehicle, synced {@link CarryState}).
 */
public final class CarryRules
{
        public enum Denial
        {
                UNTHROWABLE,
                TOO_HEAVY,
                NOT_YOURS,
                LEASHED,
                RIDING,
                ALREADY_CARRIED,
                UNAVAILABLE;

                public Component message()
                {
                        return Component.translatable("throwablemobs.carry.denied." + name().toLowerCase(Locale.ROOT));
                }
        }

        private CarryRules()
        {
        }

        /** Free to pick something up, whatever the input: not a spectator, main hand empty (if required). */
        public static boolean canUseHands(Player player)
        {
                return !player.isSpectator()
                        && (!ThrowableMobsConfig.carry().requireEmptyHand() || player.getMainHandItem().isEmpty());
        }

        /**
         * Free to throw: main hand empty if {@code require_empty_hand} is on, the same rule as picking up. Switching to
         * a full slot while charging cancels the throw.
         */
        public static boolean canThrow(Player player)
        {
                return canUseHands(player);
        }

        /** Why {@code player} can't pick up {@code mob}, or {@code null} if they can. Ignores input and reach. */
        public static @Nullable Denial check(Player player, Mob mob)
        {
                CarryConfig config = ThrowableMobsConfig.carry();
                if (!mob.isAlive())
                {
                        return Denial.UNAVAILABLE;
                }
                if (Services.CARRY_STATE.get(mob) != null)
                {
                        return Denial.ALREADY_CARRIED;
                }
                // Ender dragon: multipart, its parts can't follow a carrier.
                if (mob.getType().builtInRegistryHolder().is(ModTags.UNTHROWABLE) || mob instanceof EnderDragon
                        || (!config.allowVillagers() && mob instanceof AbstractVillager))
                {
                        return Denial.UNTHROWABLE;
                }
                if (isSomeoneElsesPet(player, mob))
                {
                        return Denial.NOT_YOURS;
                }
                if (mob.isPassenger() || mob.isVehicle())
                {
                        return Denial.RIDING;
                }
                if (mob instanceof Leashable leashable && leashable.isLeashed())
                {
                        return Denial.LEASHED;
                }
                if (mass(mob) > config.maxMass())
                {
                        return Denial.TOO_HEAVY;
                }
                return null;
        }

        /**
         * The mob profile's mass, else from the mob's current hitbox, so mods that resize mobs change their weight too.
         */
        public static double mass(Mob mob)
        {
                return MobProfiles.physics(mob).mass().orElseGet(() -> ThrowPhysics.massFromHitbox(mob.getBbWidth(),
                        mob.getBbHeight(), ThrowableMobsConfig.carry().defaultDensity()));
        }

        private static boolean isSomeoneElsesPet(Player player, Mob mob)
        {
                if (!(mob instanceof OwnableEntity ownable))
                {
                        return false;
                }
                boolean ownerOnly = ThrowableMobsConfig.carry().ownerOnlyForTamed()
                        || mob.getType().builtInRegistryHolder().is(ModTags.OWNER_ONLY);
                //? if >=26.1 {
                EntityReference<LivingEntity> owner = ownable.getOwnerReference();
                return ownerOnly && owner != null && !owner.getUUID().equals(player.getUUID());
                //?} else {
                /*UUID owner = ownable.getOwnerUUID();
                return ownerOnly && owner != null && !owner.equals(player.getUUID());
                *///?}
        }
}
