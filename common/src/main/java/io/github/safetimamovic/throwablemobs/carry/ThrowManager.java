package io.github.safetimamovic.throwablemobs.carry;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.ThrowConfig;
import io.github.safetimamovic.throwablemobs.physics.ThrowPhysics;
import io.github.safetimamovic.throwablemobs.platform.Services;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Server-side throwing. The client reports when the Throw key goes down and up; the charge is the time in between,
 * measured here. A thrown mob keeps its AI off (unless {@code ai_in_flight}) and is moved by us each tick with
 * {@link Entity#move}, which does vanilla block collision: vanilla skips movement for NoAI mobs. The flight step is
 * {@link ThrowPhysics#stepVelocity}, the same one the client's trajectory preview uses.
 */
public final class ThrowManager
{
        public static final ResourceKey<DamageType> THROWN_MOB = ResourceKey.create(Registries.DAMAGE_TYPE,
                Constants.id("thrown_mob"));

        /** How long a throw counts as a player kill (XP, player-only loot), same as a vanilla player attack. */
        private static final int KILL_CREDIT_TICKS = 100;

        /** Player UUID to the game time the Throw key went down. */
        private static final Map<UUID, Long> CHARGING = new HashMap<>();

        /** Mobs in the air. Server-side only; mobs that load with a stale {@link ThrowState} are restored. */
        private static final Map<Mob, Flight> FLYING = new HashMap<>();

        private ThrowManager()
        {
        }

        /** {@link io.github.safetimamovic.throwablemobs.network.ChargeThrowPayload} from a client. */
        public static void onChargeRequest(ServerPlayer player)
        {
                if (!player.isSpectator() && CarryManager.carriedBy(player) != null)
                {
                        CHARGING.put(player.getUUID(), player.level().getGameTime());
                }
        }

        /** {@link io.github.safetimamovic.throwablemobs.network.ThrowPayload} from a client. */
        public static void onThrowRequest(ServerPlayer player)
        {
                Long start = CHARGING.remove(player.getUUID());
                Mob mob = CarryManager.carriedBy(player);
                if (start == null || mob == null || player.isSpectator())
                {
                        return;
                }
                ThrowConfig config = ThrowableMobsConfig.throwing();
                long held = player.level().getGameTime() - start;
                if (held < config.minChargeTicks())
                {
                        return;
                }
                int ticks = (int) Math.min(held, config.maxChargeTicks());
                throwMob(player, mob, ThrowPhysics.chargeFraction(ticks, config.maxChargeTicks()), config);
        }

        private static void throwMob(ServerPlayer player, Mob mob, double charge, ThrowConfig config)
        {
                CarryState carried = CarryManager.letGo(player);
                if (carried == null)
                {
                        return;
                }
                ThrowState state = new ThrowState(player.getUUID(), carried.originalNoAi(), carried.originalNoGravity(),
                        mob.level().getGameTime());
                Services.THROW_STATE.set(mob, state);
                mob.setNoAi(!config.aiInFlight() || state.originalNoAi());
                mob.setNoGravity(true);
                double speed = ThrowPhysics.launchSpeed(config.throwImpulse(), charge, CarryRules.mass(mob),
                        config.maxThrowSpeed());
                Vec3 velocity = player.getLookAngle().scale(speed)
                        .add(player.getKnownMovement().scale(config.inheritVelocity()));
                mob.setDeltaMovement(velocity);
                FLYING.put(mob, new Flight(player.getUUID(), velocity, mob.position(), 0));
        }

        /** Once per server tick, after entities ticked: move every mob in the air one step. */
        public static void tick(MinecraftServer server)
        {
                ThrowConfig config = ThrowableMobsConfig.throwing();
                Iterator<Map.Entry<Mob, Flight>> it = FLYING.entrySet().iterator();
                while (it.hasNext())
                {
                        Map.Entry<Mob, Flight> entry = it.next();
                        Mob mob = entry.getKey();
                        // Killed, or unloaded with its chunk; a saved ThrowState is restored when it loads again.
                        Flight next = mob.isRemoved() ? null : fly(mob, entry.getValue(), config);
                        if (next == null)
                        {
                                it.remove();
                        }
                        else
                        {
                                entry.setValue(next);
                        }
                }
        }

        /** One flight tick; {@code null} when the flight ended. */
        private static @Nullable Flight fly(Mob mob, Flight flight, ThrowConfig config)
        {
                ServerLevel level = (ServerLevel) mob.level();
                int ticks = flight.ticks() + 1;
                Vec3 velocity = flight.velocity();

                Entity target = findTarget(mob, flight, ticks, config);
                if (target != null)
                {
                        hitEntity(level, mob, target, flight, config);
                        return null;
                }

                // With AI in flight vanilla moves the mob by its velocity; otherwise nothing does, so we move it.
                boolean vanillaMoves = mob.isEffectiveAi();
                if (!vanillaMoves)
                {
                        mob.move(MoverType.SELF, velocity);
                }
                Vec3 moved = mob.position().subtract(flight.position());
                if (vanillaMoves && ticks == 1 && moved.lengthSqr() == 0.0)
                {
                        // The throw arrived after the mob ticked: no move yet, so its collision flags are stale.
                        return new Flight(flight.thrower(), velocity, flight.position(), ticks);
                }

                if (mob.horizontalCollision || mob.verticalCollision)
                {
                        hitBlock(level, mob, flight, velocity.subtract(moved), config);
                        return null;
                }
                if (mob.isInWater() || ticks >= config.maxFlightTicks())
                {
                        land(mob, moved);
                        return null;
                }

                ThrowPhysics.Vector next = ThrowPhysics.stepVelocity(
                        new ThrowPhysics.Vector(velocity.x, velocity.y, velocity.z), config.gravity(), config.drag());
                velocity = new Vec3(next.x(), next.y(), next.z());
                mob.setDeltaMovement(velocity);
                if (!config.vanillaFallDamage())
                {
                        mob.resetFallDistance();
                }
                return new Flight(flight.thrower(), velocity, mob.position(), ticks);
        }

        /** Nearest entity in the mob's path this tick. The thrower is safe for the first few ticks. */
        private static @Nullable Entity findTarget(Mob mob, Flight flight, int ticks, ThrowConfig config)
        {
                Entity nearest = null;
                double nearestDistance = Double.MAX_VALUE;
                for (Entity entity : mob.level().getEntities(mob, mob.getBoundingBox().expandTowards(flight.velocity()),
                        entity -> canHit(entity, flight.thrower(), ticks, config)))
                {
                        double distance = entity.distanceToSqr(mob);
                        if (distance < nearestDistance)
                        {
                                nearest = entity;
                                nearestDistance = distance;
                        }
                }
                return nearest;
        }

        /**
         * Whether a mob {@code ticks} into its flight can hit {@code entity}: living, not carried, and not its thrower
         * during the grace ticks. Shared with the client's trajectory preview.
         */
        public static boolean canHit(Entity entity, UUID thrower, int ticks, ThrowConfig config)
        {
                if (!(entity instanceof LivingEntity) || !entity.isAlive() || entity.isSpectator() || !entity.isPickable()
                        || Services.CARRY_STATE.get(entity) != null)
                {
                        return false;
                }
                return ticks > config.throwerGraceTicks() || !entity.getUUID().equals(thrower);
        }

        /**
         * Mob-vs-entity: collision energy from the relative speed and the reduced mass, damage split so the lighter body
         * takes more, knockback by momentum transfer (§8.4).
         */
        private static void hitEntity(ServerLevel level, Mob mob, Entity target, Flight flight, ThrowConfig config)
        {
                double m1 = CarryRules.mass(mob);
                double m2 = mass(target);
                Vec3 relative = flight.velocity().subtract(target.getKnownMovement());
                double damage = ThrowPhysics.impactDamage(ThrowPhysics.collisionEnergy(m1, m2, relative.length()),
                        config.damagePerEnergy(), config.minImpactEnergy(), config.maxImpactDamage());
                double targetShare = ThrowPhysics.targetDamageShare(m1, m2);
                ServerPlayer thrower = level.getServer().getPlayerList().getPlayer(flight.thrower());

                double resistance = target instanceof LivingEntity living
                        ? living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)
                        : 0.0;
                Vec3 push = relative.scale(ThrowPhysics.targetKnockback(m1, m2, config.restitution())
                        * Math.max(0.0, 1.0 - resistance));
                Vec3 bounce = flight.velocity()
                        .add(relative.scale(ThrowPhysics.thrownKnockback(m1, m2, config.restitution())));

                land(mob, bounce);
                // The damage type is in #minecraft:no_knockback, so this push is the only knockback.
                hurt(level, target, thrownMob(level, mob, thrower), damage * targetShare, thrower);
                target.push(push);
                target.needsSync = true;
                hurt(level, mob, level.damageSources().flyIntoWall(), damage * (1.0 - targetShare), thrower);
        }

        /**
         * Mob-vs-block: the block doesn't move (infinite mass), so the mob takes all the damage. Only the blocked part
         * of the velocity counts: skimming along the ground hurts less than hitting a wall head-on.
         */
        private static void hitBlock(ServerLevel level, Mob mob, Flight flight, Vec3 blocked, ThrowConfig config)
        {
                double damage = ThrowPhysics.impactDamage(
                        ThrowPhysics.collisionEnergy(CarryRules.mass(mob), Double.POSITIVE_INFINITY, blocked.length()),
                        config.damagePerEnergy(), config.minImpactEnergy(), config.maxImpactDamage());
                land(mob, flight.velocity().subtract(blocked));
                hurt(level, mob, level.damageSources().flyIntoWall(),
                        damage, level.getServer().getPlayerList().getPlayer(flight.thrower()));
        }

        private static void hurt(ServerLevel level, Entity entity, DamageSource source, double amount,
                @Nullable ServerPlayer thrower)
        {
                if (amount <= 0.0)
                {
                        return;
                }
                if (thrower != null && entity instanceof LivingEntity living && !(entity instanceof Player))
                {
                        living.setLastHurtByPlayer(thrower, KILL_CREDIT_TICKS);
                }
                entity.hurtServer(level, source, (float) amount);
        }

        /** "X was hit by a mob thrown by Y": the thrown mob is the direct entity, the thrower gets the kill. */
        private static DamageSource thrownMob(ServerLevel level, Mob mob, @Nullable ServerPlayer thrower)
        {
                return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(THROWN_MOB), mob, thrower);
        }

        private static double mass(Entity entity)
        {
                return entity instanceof Mob mob
                        ? CarryRules.mass(mob)
                        : ThrowPhysics.massFromHitbox(entity.getBbWidth(), entity.getBbHeight(),
                                ThrowableMobsConfig.carry().defaultDensity());
        }

        /** Ends a flight: original flags back, then vanilla takes over with {@code motion}. */
        private static void land(Mob mob, Vec3 motion)
        {
                ThrowState state = Services.THROW_STATE.get(mob);
                Services.THROW_STATE.remove(mob);
                if (state != null)
                {
                        mob.setNoAi(state.originalNoAi());
                        mob.setNoGravity(state.originalNoGravity());
                }
                mob.setDeltaMovement(motion);
                mob.resetFallDistance();
                mob.needsSync = true;
        }

        /** A mob loading mid-flight (chunk reload, restart) gets its original state back and falls normally. */
        public static void onEntityLoad(Entity entity)
        {
                if (entity instanceof Mob mob && Services.THROW_STATE.get(mob) != null && !FLYING.containsKey(mob))
                {
                        land(mob, Vec3.ZERO);
                }
        }

        /**
         * Damage hook (server): no vanilla fall damage in the air, impact damage replaces it, unless
         * {@code vanilla_fall_damage} stacks the two. Vanilla applies it inside the landing {@link Entity#move}, while
         * the mob still has its ThrowState.
         */
        public static boolean allowDamage(LivingEntity entity, DamageSource source)
        {
                return !(source.is(DamageTypeTags.IS_FALL) && Services.THROW_STATE.get(entity) != null
                        && !ThrowableMobsConfig.throwing().vanillaFallDamage());
        }

        public static void onLogout(ServerPlayer player)
        {
                CHARGING.remove(player.getUUID());
        }

        /** Server stop: mobs in the air are saved with their ThrowState and restored when they load. */
        public static void clear()
        {
                CHARGING.clear();
                FLYING.clear();
        }

        /**
         * @param position where the mob was at the end of the last flight tick, to tell how far it really moved
         */
        private record Flight(UUID thrower, Vec3 velocity, Vec3 position, int ticks)
        {
        }
}
