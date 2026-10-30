package io.github.safetimamovic.throwablemobs.carry;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.advancement.ModTriggers;
import io.github.safetimamovic.throwablemobs.advancement.ThrowEventTrigger.Event;
import io.github.safetimamovic.throwablemobs.api.BounceImpacts;
import io.github.safetimamovic.throwablemobs.api.ImpactResult;
import io.github.safetimamovic.throwablemobs.api.MobProfile;
import io.github.safetimamovic.throwablemobs.behavior.Hooks;
import io.github.safetimamovic.throwablemobs.behavior.MobProfiles;
import io.github.safetimamovic.throwablemobs.behavior.ProfilePhysics;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CatchConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.ThrowConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.effect.ThrowEffects;
import io.github.safetimamovic.throwablemobs.physics.ThrowPhysics;
import io.github.safetimamovic.throwablemobs.platform.Services;
import io.github.safetimamovic.throwablemobs.tags.ModTags;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
//? if >=26.3
import net.minecraft.world.item.component.SwingAnimation;
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

        public static final ResourceKey<DamageType> CAUGHT_MOB = ResourceKey.create(Registries.DAMAGE_TYPE,
                Constants.id("caught_mob"));

        /** How long a throw counts as a player kill (XP, player-only loot), same as a vanilla player attack. */
        private static final int KILL_CREDIT_TICKS = 100;

        /** Player UUID to the game time the Throw key went down. */
        private static final Map<UUID, Long> CHARGING = new HashMap<>();

        /** Mobs in the air. Server-side only; mobs that load with a stale {@link ThrowState} are restored. */
        private static final Map<Mob, Flight> FLYING = new HashMap<>();

        /** Catcher UUID to who threw the mob they caught, until they throw it on (Return to Sender). */
        private static final Map<UUID, UUID> CAUGHT_FROM = new HashMap<>();

        private ThrowManager()
        {
        }

        /** {@link io.github.safetimamovic.throwablemobs.network.ChargeThrowPayload} from a client. */
        public static void onChargeRequest(ServerPlayer player)
        {
                if (CarryManager.enabledFor(player) && CarryRules.canThrow(player)
                        && CarryManager.carriedBy(player) != null)
                {
                        CHARGING.put(player.getUUID(), player.level().getGameTime());
                }
        }

        /** {@link io.github.safetimamovic.throwablemobs.network.ThrowPayload} from a client. */
        public static void onThrowRequest(ServerPlayer player)
        {
                Long start = CHARGING.remove(player.getUUID());
                Mob mob = CarryManager.carriedBy(player);
                if (start == null || mob == null || !CarryManager.enabledFor(player) || !CarryRules.canThrow(player))
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
                        mob.level().getGameTime(), false);
                Services.THROW_STATE.set(mob, state);
                mob.setNoAi(!config.aiInFlight() || state.originalNoAi());
                mob.setNoGravity(true);
                double speed = ThrowPhysics.launchSpeed(impulse(player, config), charge, CarryRules.mass(mob),
                        config.maxThrowSpeed());
                Vec3 velocity = player.getLookAngle().scale(speed)
                        .add(Versioned.knownMovement(player).scale(config.inheritVelocity()));
                mob.setDeltaMovement(velocity);
                FLYING.put(mob, new Flight(player.getUUID(), velocity, mob.position(), 0, 0, mob.position(),
                        CAUGHT_FROM.remove(player.getUUID())));
                ThrowEffects.thrown((ServerLevel) mob.level(), mob, charge);
                CarryHunger.thrown(player);
                ModTriggers.THROW_EVENT.trigger(player, Event.THROW, mob, charge, 0.0);
                Hooks.thrown(mob, player, velocity);
        }

        /**
         * How hard {@code player} throws: {@code throw_impulse}, plus {@code strength_effect_bonus} per level of the
         * Strength effect. Shared with the client's trajectory preview; a player's own effects are synced to them.
         */
        public static double impulse(Player player, ThrowConfig config)
        {
                MobEffectInstance strength = player.getEffect(MobEffects.STRENGTH);
                int level = strength != null ? strength.getAmplifier() + 1 : 0;
                return config.throwImpulse() * (1.0 + config.strengthEffectBonus() * level);
        }

        /** Once per server tick, after entities ticked: move every mob in the air one step. */
        public static void tick(MinecraftServer server)
        {
                if (!ThrowableMobsConfig.enabled())
                {
                        // Turned off: flights end and the mobs fall normally.
                        FLYING.keySet().forEach(mob -> land(mob, Vec3.ZERO));
                        FLYING.clear();
                        CHARGING.clear();
                        return;
                }
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
                ServerPlayer thrower = thrower(level, flight);

                Hooks.flightTick(mob, thrower, velocity, ticks);
                if (mob.isRemoved())
                {
                        return null;
                }
                ServerPlayer catcher = findCatcher(level, mob, flight, ticks);
                if (catcher != null)
                {
                        catchMob(level, mob, catcher, flight, ticks);
                        return null;
                }
                Entity target = findTarget(mob, flight, ticks, config);
                if (target != null)
                {
                        hitEntity(level, mob, target, flight, ticks, config);
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
                        return flight.next(velocity, flight.position(), ticks, flight.bounces());
                }

                int bounces = flight.bounces();
                boolean bounced = false;
                if (mob.horizontalCollision || mob.verticalCollision)
                {
                        Vec3 bounce = hitBlock(level, mob, flight, moved, ticks, config);
                        if (bounce == null)
                        {
                                return null;
                        }
                        velocity = bounce;
                        if (bounces == 0)
                        {
                                // Clients need it too, for the catch highlight's self-catch rule.
                                ThrowState state = Services.THROW_STATE.get(mob);
                                if (state != null)
                                {
                                        Services.THROW_STATE.set(mob, state.withBounced());
                                }
                        }
                        bounces++;
                        bounced = true;
                }
                if (mob.isInWater() || ticks >= config.maxFlightTicks())
                {
                        endFlight(mob, flight, thrower, bounced ? velocity : moved, ticks);
                        return null;
                }

                ProfilePhysics physics = MobProfiles.physics(mob);
                ThrowPhysics.Vector next = ThrowPhysics.stepVelocity(
                        new ThrowPhysics.Vector(velocity.x, velocity.y, velocity.z),
                        physics.gravity(config), physics.drag(config));
                velocity = new Vec3(next.x(), next.y(), next.z());
                mob.setDeltaMovement(velocity);
                if (!config.vanillaFallDamage())
                {
                        mob.resetFallDistance();
                }
                return flight.next(velocity, mob.position(), ticks, bounces);
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
                if (entity.getUUID().equals(thrower))
                {
                        return ticks > config.throwerGraceTicks();
                }
                return !(entity instanceof Player player) || canHurtPlayer(player, thrower, config);
        }

        /**
         * Whether a mob thrown by {@code thrower} may hurt {@code player}: {@code pvp} is on and the thrower could hit
         * them (scoreboard teams, the server's own pvp setting). A thrower who logged out counts as allowed. Players a
         * mob can't hurt aren't in its way: it flies past them.
         */
        public static boolean canHurtPlayer(Player player, UUID thrower, ThrowConfig config)
        {
                if (!config.pvp())
                {
                        return false;
                }
                Player throwing = player.level().getPlayerByUUID(thrower);
                return throwing == null || throwing == player || throwing.canHarmPlayer(player);
        }

        /** The player catching the mob this tick, if any: the first one along its path who can. */
        private static @Nullable ServerPlayer findCatcher(ServerLevel level, Mob mob, Flight flight, int ticks)
        {
                CatchConfig config = ThrowableMobsConfig.catching();
                if (!config.enabled())
                {
                        return null;
                }
                // From where the mob was after the last flight tick, also when vanilla moved it already (ai_in_flight).
                Vec3 start = flight.position().add(0.0, mob.getBbHeight() / 2.0, 0.0);
                ServerPlayer first = null;
                double firstFraction = Double.MAX_VALUE;
                for (ServerPlayer player : level.players())
                {
                        if (!canCatch(player, mob, flight.thrower(), ticks, flight.bounces() > 0, config))
                        {
                                continue;
                        }
                        OptionalDouble fraction = catchFraction(player, start, flight.velocity(),
                                Versioned.latency(player) / 50.0, config);
                        if (fraction.isPresent() && fraction.getAsDouble() < firstFraction)
                        {
                                first = player;
                                firstFraction = fraction.getAsDouble();
                        }
                }
                return first;
        }

        /**
         * Who may catch {@code mob}, wherever it is: same rules as picking it up (empty hand, not carrying, weight,
         * tags, pets), not riding anything, and the thrower only as {@code self_catch} allows.
         */
        private static boolean canCatch(ServerPlayer player, Mob mob, UUID thrower, int ticks, boolean bounced,
                CatchConfig config)
        {
                if (!player.isAlive() || player.isPassenger() || !CarryManager.enabledFor(player)
                        || !CarryRules.canUseHands(player) || CarryManager.carriedBy(player) != null)
                {
                        return false;
                }
                if (player.getUUID().equals(thrower)
                        && !config.selfCatch().allows(ticks, bounced, config.selfCatchDelayTicks()))
                {
                        return false;
                }
                return CarryRules.check(player, mob) == null;
        }

        /**
         * Whether a mob moving from {@code start} (its centre) by {@code velocity} this tick passes close enough in
         * front of {@code player} to be caught: coming towards them, within {@code catch_radius} of their eyes, grown
         * by {@code pingTicks}, and with the mob inside their view cone. Returns how far along this tick's path it gets
         * caught (0 to 1), empty if it doesn't. Geometry only; shared with the client's catch highlight.
         */
        public static OptionalDouble catchFraction(Player player, Vec3 start, Vec3 velocity, double pingTicks,
                CatchConfig config)
        {
                ThrowPhysics.Vector eye = vector(player.getEyePosition());
                ThrowPhysics.Vector from = vector(start);
                ThrowPhysics.Vector to = vector(start.add(velocity));
                if (!ThrowPhysics.approaching(eye, from, vector(velocity)))
                {
                        return OptionalDouble.empty();
                }
                double radius = ThrowPhysics.catchRadius(config.radius(), velocity.length(), pingTicks,
                        config.maxLatencyTicks(), config.latencyCompensation());
                if (ThrowPhysics.segmentDistance(eye, from, to) > radius)
                {
                        return OptionalDouble.empty();
                }
                // Facing where the mob is now, not the closest point of its path: that one sits right next to the eyes, at
                // a steep angle, so a long look-ahead path (the client's highlight) would always miss the cone.
                if (!ThrowPhysics.inViewCone(eye, vector(player.getLookAngle()), from, config.coneDegrees()))
                {
                        return OptionalDouble.empty();
                }
                return OptionalDouble.of(ThrowPhysics.segmentFraction(eye, from, to));
        }

        /**
         * The flight ends in {@code catcher}'s hands: behaviors' {@code onCaught}, then the carry starts with the flags
         * the mob had before it was first picked up, then the recoil pushes the catcher back along the mob's path.
         */
        private static void catchMob(ServerLevel level, Mob mob, ServerPlayer catcher, Flight flight, int ticks)
        {
                CatchConfig config = ThrowableMobsConfig.catching();
                ServerPlayer thrower = thrower(level, flight);
                Hooks.caught(mob, catcher, thrower, flight.velocity(), ticks);
                if (mob.isRemoved())
                {
                        // A behavior blew it up or split it: nothing left to catch.
                        return;
                }
                ThrowState state = Services.THROW_STATE.get(mob);
                if (state == null)
                {
                        land(mob, flight.velocity());
                        return;
                }
                Services.THROW_STATE.remove(mob);
                CarryManager.catchMob(catcher, mob, state);

                Vec3 relative = flight.velocity().subtract(Versioned.knownMovement(catcher));
                double momentum = CarryRules.mass(mob) * relative.length();
                double resistance = catcher.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                Vec3 recoil = relative.normalize().scale(momentum * config.recoil() * Math.max(0.0, 1.0 - resistance));
                catcher.push(recoil.x, recoil.y, recoil.z);
                // Players move on their own client; without this the push never reaches it.
                catcher.syncVelocity = true;
                if (config.damage() && canHurtPlayer(catcher, flight.thrower(), ThrowableMobsConfig.throwing()))
                {
                        hurt(level, catcher, mobSource(level, CAUGHT_MOB, mob, thrower),
                                ThrowPhysics.catchDamage(momentum, config.absorbMomentum(), config.damageFactor()),
                                thrower);
                }
                ThrowEffects.caught(level, mob, momentum);
                //? if >=26.3 {
                catcher.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
                //?} else
                /*catcher.swing(InteractionHand.MAIN_HAND, true);*/
                if (!catcher.getUUID().equals(flight.thrower()))
                {
                        CAUGHT_FROM.put(catcher.getUUID(), flight.thrower());
                }
                ModTriggers.THROW_EVENT.trigger(catcher, Event.CATCH, mob, 0.0, 0.0);
        }

        /**
         * Mob-vs-entity: collision energy from the relative speed and the reduced mass, damage split so the lighter body
         * takes more, knockback by momentum transfer (§8.4).
         */
        private static void hitEntity(ServerLevel level, Mob mob, Entity target, Flight flight, int ticks,
                ThrowConfig config)
        {
                double m1 = CarryRules.mass(mob);
                double m2 = mass(target);
                Vec3 relative = flight.velocity().subtract(Versioned.knownMovement(target));
                double energy = ThrowPhysics.collisionEnergy(m1, m2, relative.length());
                double damage = ThrowPhysics.impactDamage(energy, config.damagePerEnergy(), config.minImpactEnergy(),
                        config.maxImpactDamage());
                double targetShare = ThrowPhysics.targetDamageShare(m1, m2);
                ServerPlayer thrower = thrower(level, flight);
                MobProfile profile = MobProfiles.resolve(mob);
                ImpactResult result = Hooks.impact(mob, thrower, flight.velocity(), ticks, target, relative, energy,
                        flight.bounces(),
                        new ImpactResult(damage * targetShare * multiplier(profile.targetMultiplier()),
                                damage * (1.0 - targetShare) * selfDamageFactor(mob, profile, config), 1.0, null));

                double resistance = target instanceof LivingEntity living
                        ? living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)
                        : 0.0;
                Vec3 push = relative.scale(ThrowPhysics.targetKnockback(m1, m2, config.restitution())
                        * Math.max(0.0, 1.0 - resistance) * result.knockbackMultiplier());
                Vec3 bounce = flight.velocity()
                        .add(relative.scale(ThrowPhysics.thrownKnockback(m1, m2, config.restitution())));

                ThrowEffects.hitEntity(level, mob, target, energy);
                if (thrower != null && target.getUUID().equals(flight.returnTo()))
                {
                        ModTriggers.THROW_EVENT.trigger(thrower, Event.RETURN_HIT, mob, 0.0, 0.0);
                }
                // A behavior may have removed the mob (exploded, split); the target still takes its hit.
                if (!mob.isRemoved())
                {
                        endFlight(mob, flight, thrower, bounce, ticks);
                }
                // The damage type is in #minecraft:no_knockback, so this push is the only knockback.
                hurt(level, target, mobSource(level, THROWN_MOB, mob, thrower), result.targetDamage(), thrower);
                target.push(push.x, push.y, push.z);
                target.needsSync = true;
                // Players move on their own client; without this the knockback never reaches it.
                target.syncVelocity = true;
                if (!mob.isRemoved())
                {
                        hurt(level, mob, level.damageSources().flyIntoWall(), result.selfDamage(), thrower);
                }
        }

        /**
         * Mob-vs-block: the block doesn't move (infinite mass), so the mob takes all the damage. Only the speed into
         * the block counts: skimming along the ground hurts less than hitting a wall head-on. Then the mob either
         * bounces off ({@link #bounceVelocity}) or lands, keeping its speed along the free axes. Bounces after the
         * first impact only hurt with {@code bounce_impacts = EVERY}.
         *
         * @return the velocity to keep flying with, or {@code null} when the flight ended
         */
        private static @Nullable Vec3 hitBlock(ServerLevel level, Mob mob, Flight flight, Vec3 moved, int ticks,
                ThrowConfig config)
        {
                ThrowPhysics.Vector wanted = vector(flight.velocity());
                ThrowPhysics.Vector actual = vector(moved);
                ThrowPhysics.Vector into = ThrowPhysics.impactVelocity(wanted, actual);
                double energy = ThrowPhysics.collisionEnergy(CarryRules.mass(mob), Double.POSITIVE_INFINITY,
                        into.length());
                ServerPlayer thrower = thrower(level, flight);
                MobProfile profile = MobProfiles.resolve(mob);
                ImpactResult result = new ImpactResult(0.0, 0.0, 1.0,
                        bounceVelocity(MobProfiles.physics(mob), config, flight.bounces(), flight.velocity(), moved));
                if (flight.bounces() == 0 || bounceImpacts(profile, config) == BounceImpacts.EVERY)
                {
                        double damage = ThrowPhysics.impactDamage(energy, config.damagePerEnergy(),
                                config.minImpactEnergy(), config.maxImpactDamage()) * selfDamageFactor(mob, profile,
                                        config);
                        result = Hooks.impact(mob, thrower, flight.velocity(), ticks, null,
                                new Vec3(into.x(), into.y(), into.z()), energy, flight.bounces(),
                                result.withSelfDamage(damage));
                }
                if (mob.isRemoved())
                {
                        return null;
                }
                Vec3 intoBlock = new Vec3(into.x(), into.y(), into.z());
                if (result.endsFlight())
                {
                        ThrowEffects.landed(level, mob, intoBlock, energy);
                        ThrowPhysics.Vector left = ThrowPhysics.bounce(wanted, actual, 0.0).velocity();
                        endFlight(mob, flight, thrower, new Vec3(left.x(), left.y(), left.z()), ticks);
                }
                else
                {
                        ThrowEffects.bounced(level, mob, intoBlock, energy);
                }
                hurt(level, mob, level.damageSources().flyIntoWall(), result.selfDamage(), thrower);
                return result.bounceVelocity();
        }

        /**
         * Velocity after bouncing off a block, or {@code null} when the mob doesn't bounce: no bounciness, out of
         * bounces, or too slow away from the block ({@code min_bounce_speed}). Shared with the client's trajectory
         * preview, so it bounces where the real throw does.
         *
         * @param bounces bounces so far in this flight
         * @param wanted  the velocity the mob tried to move by this tick
         * @param moved   how far it really moved
         */
        public static @Nullable Vec3 bounceVelocity(ProfilePhysics physics, ThrowConfig config, int bounces,
                Vec3 wanted, Vec3 moved)
        {
                double restitution = physics.blockRestitution(config);
                if (restitution <= 0.0 || bounces >= physics.maxBounces(config))
                {
                        return null;
                }
                ThrowPhysics.Bounce bounce = ThrowPhysics.bounce(vector(wanted), vector(moved), restitution);
                if (bounce.reboundSpeed() < config.minBounceSpeed())
                {
                        return null;
                }
                return new Vec3(bounce.velocity().x(), bounce.velocity().y(), bounce.velocity().z());
        }

        private static BounceImpacts bounceImpacts(MobProfile profile, ThrowConfig config)
        {
                return profile.bounceImpacts() != null ? profile.bounceImpacts() : config.bounceImpacts();
        }

        /** Share of its impact damage the thrown mob takes: {@code self_damage} mode, then the profile multiplier. */
        private static double selfDamageFactor(Mob mob, MobProfile profile, ThrowConfig config)
        {
                boolean immune = switch (config.selfDamage())
                {
                        case ALL -> false;
                        case RESPECT_TAG -> mob.getType().builtInRegistryHolder().is(ModTags.NO_SELF_DAMAGE);
                        case NONE -> true;
                };
                return immune ? 0.0 : multiplier(profile.selfMultiplier());
        }

        private static double multiplier(@Nullable Double multiplier)
        {
                return multiplier != null ? multiplier : 1.0;
        }

        private static @Nullable ServerPlayer thrower(ServerLevel level, Flight flight)
        {
                return level.getServer().getPlayerList().getPlayer(flight.thrower());
        }

        private static void hurt(ServerLevel level, Entity entity, DamageSource source, double amount,
                @Nullable ServerPlayer thrower)
        {
                if (amount <= 0.0)
                {
                        return;
                }
                //? if >=26.1 {
                if (thrower != null && entity instanceof LivingEntity living && !(entity instanceof Player))
                {
                        living.setLastHurtByPlayer(thrower, KILL_CREDIT_TICKS);
                }
                entity.hurtServer(level, source, (float) amount);
                //?} else {
                /*// The thrower is the source's entity, so hurt gives them the kill credit.
                entity.hurt(source, (float) amount);
                *///?}
        }

        /**
         * Damage from a thrown mob ("X was hit by a mob thrown by Y"): the mob is the direct entity, the thrower gets
         * the kill.
         */
        private static DamageSource mobSource(ServerLevel level, ResourceKey<DamageType> type, Mob mob,
                @Nullable ServerPlayer thrower)
        {
                return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(type), mob, thrower);
        }

        private static ThrowPhysics.Vector vector(Vec3 v)
        {
                return new ThrowPhysics.Vector(v.x, v.y, v.z);
        }

        private static double mass(Entity entity)
        {
                return entity instanceof Mob mob
                        ? CarryRules.mass(mob)
                        : ThrowPhysics.massFromHitbox(entity.getBbWidth(), entity.getBbHeight(),
                                ThrowableMobsConfig.carry().defaultDensity());
        }

        /** {@link #land} plus the behaviors' {@code onLand}: the end of a real flight. */
        private static void endFlight(Mob mob, Flight flight, @Nullable ServerPlayer thrower, Vec3 motion, int ticks)
        {
                land(mob, motion);
                if (thrower != null)
                {
                        ModTriggers.THROW_EVENT.trigger(thrower, Event.LAND, mob, 0.0,
                                mob.position().distanceTo(flight.launch()));
                }
                Hooks.land(mob, thrower, motion, ticks);
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
                if (mob instanceof Shulker)
                {
                        // Free in flight (ShulkerMixin); snaps back onto the block grid now.
                        mob.setPos(mob.position());
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
                CAUGHT_FROM.remove(player.getUUID());
        }

        /** The player turned the mod off for themselves mid-charge. */
        static void forgetCharge(ServerPlayer player)
        {
                CHARGING.remove(player.getUUID());
        }

        /** A normal pick-up: whatever {@code player} caught before is no longer theirs to return. */
        static void forgetCatch(ServerPlayer player)
        {
                CAUGHT_FROM.remove(player.getUUID());
        }

        /** Server stop: mobs in the air are saved with their ThrowState and restored when they load. */
        public static void clear()
        {
                CHARGING.clear();
                FLYING.clear();
                CAUGHT_FROM.clear();
        }

        /**
         * @param position where the mob was at the end of the last flight tick, to tell how far it really moved
         * @param bounces  bounces off blocks so far
         * @param launch   where it was thrown from, for the landing distance
         * @param returnTo who threw it to the thrower before they caught it; hitting them back is Return to Sender
         */
        private record Flight(UUID thrower, Vec3 velocity, Vec3 position, int ticks, int bounces, Vec3 launch,
                @Nullable UUID returnTo)
        {
                Flight next(Vec3 velocity, Vec3 position, int ticks, int bounces)
                {
                        return new Flight(thrower, velocity, position, ticks, bounces, launch, returnTo);
                }
        }
}
