package io.github.safetimamovic.throwablemobs.carry;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarriedFacing;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarrierLogout;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarryConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.DimensionChange;
import io.github.safetimamovic.throwablemobs.physics.ThrowPhysics;
import io.github.safetimamovic.throwablemobs.platform.Services;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Server-side carrying (follow model): a carried mob stays a normal entity, not a passenger. Every tick it is moved
 * above its carrier's head with AI, gravity and collisions off; clients lock it to the carrier for smooth rendering
 * (see {@code CarryClient}). Vanilla can't be used here: the server refuses entities riding players.
 */
public final class CarryManager
{
        private static final Identifier SLOWDOWN_ID = Constants.id("carry_slowdown");

        /** Extra reach allowed for pick-up requests, for movement between the client's check and the server's. */
        private static final double PICK_UP_REACH_TOLERANCE = 3.0;

        /** Carrier UUID to carried mob. Server-side only; mobs that load with a stale {@link CarryState} are restored. */
        private static final Map<UUID, Mob> CARRIED = new HashMap<>();

        private CarryManager()
        {
        }

        public static @Nullable Mob carriedBy(Player player)
        {
                Mob mob = CARRIED.get(player.getUUID());
                return mob != null && !mob.isRemoved() ? mob : null;
        }

        /**
         * Use-entity hook (both sides). Nobody interacts with a carried mob: no trading with the villager on your head.
         * Picking up and putting down go through the key mappings and their payloads, not vanilla interactions.
         */
        public static InteractionResult onUseEntity(Entity target)
        {
                return Services.CARRY_STATE.get(target) != null ? InteractionResult.FAIL : InteractionResult.PASS;
        }

        /** {@link io.github.safetimamovic.throwablemobs.network.DropCarriedPayload} from a client (put-down key). */
        public static void onDropRequest(ServerPlayer player)
        {
                if (!player.isSpectator())
                {
                        drop(player);
                }
        }

        /** {@link io.github.safetimamovic.throwablemobs.network.PickUpPayload} from a client (pick-up key). */
        public static void onPickUpRequest(ServerPlayer player, int entityId)
        {
                if (!CarryRules.canUseHands(player) || carriedBy(player) != null
                        || !(player.level().getEntity(entityId) instanceof Mob mob)
                        || !player.isWithinEntityInteractionRange(mob, PICK_UP_REACH_TOLERANCE))
                {
                        return;
                }
                CarryRules.Denial denial = CarryRules.check(player, mob);
                if (denial != null)
                {
                        // The client only asks when its own check passed, so tell the player why the server disagrees.
                        player.sendOverlayMessage(denial.message());
                        return;
                }
                pickUp(player, mob);
        }

        /** Picks up without checking {@link CarryRules}; callers check first. */
        public static void pickUp(ServerPlayer player, Mob mob)
        {
                hold(player, mob, new CarryState(player.getUUID(), mob.isNoAi(), mob.isNoGravity(),
                        mob.level().getGameTime()));
        }

        /** Starts or resumes a carry with an existing state, keeping the original flags it recorded. */
        private static void hold(ServerPlayer player, Mob mob, CarryState state)
        {
                Services.CARRY_STATE.set(mob, state);
                mob.setNoAi(true);
                mob.setNoGravity(true);
                mob.noPhysics = true;
                mob.getNavigation().stop();
                mob.setTarget(null);
                CARRIED.put(player.getUUID(), mob);
                applySlowdown(player, mob);
                follow(player, mob);
        }

        /** Puts the carried mob down on the ground the player looks at; refuses if that isn't valid ground. */
        public static void drop(ServerPlayer player)
        {
                Mob mob = carriedBy(player);
                if (mob == null)
                {
                        CARRIED.remove(player.getUUID());
                        removeSlowdown(player);
                        return;
                }
                Vec3 at = DropTarget.find(player, mob);
                if (at == null)
                {
                        if (ThrowableMobsConfig.CARRY.dropNeedsGround())
                        {
                                player.sendOverlayMessage(Component.translatable("throwablemobs.carry.need_ground"));
                                return;
                        }
                        at = fallbackDropPosition(player, mob);
                }
                CARRIED.remove(player.getUUID());
                removeSlowdown(player);
                release(mob, at);
        }

        /** Once per server tick: keep carried mobs on their carriers' heads, drop them when the carry is broken. */
        public static void tick(MinecraftServer server)
        {
                List<Map.Entry<ServerPlayer, Mob>> travelling = new ArrayList<>();
                Iterator<Map.Entry<UUID, Mob>> it = CARRIED.entrySet().iterator();
                while (it.hasNext())
                {
                        Map.Entry<UUID, Mob> entry = it.next();
                        Mob mob = entry.getValue();
                        ServerPlayer carrier = server.getPlayerList().getPlayer(entry.getKey());
                        if (mob.isRemoved())
                        {
                                // Killed, or unloaded with its chunk; a saved CarryState is restored when it loads again.
                                it.remove();
                                removeSlowdown(carrier);
                                continue;
                        }
                        if (carrier == null || !carrier.isAlive() || carrier.isSpectator())
                        {
                                // Logout, death: drop where the mob is, above the carrier's last spot.
                                it.remove();
                                removeSlowdown(carrier);
                                release(mob, mob.position());
                                continue;
                        }
                        if (carrier.level() != mob.level())
                        {
                                it.remove();
                                if (ThrowableMobsConfig.CARRY.onDimensionChange() == DimensionChange.TRAVEL)
                                {
                                        // Teleporting adds entities to the other level, so not while iterating.
                                        travelling.add(Map.entry(carrier, mob));
                                }
                                else
                                {
                                        removeSlowdown(carrier);
                                        release(mob, mob.position());
                                }
                                continue;
                        }
                        follow(carrier, mob);
                }
                for (Map.Entry<ServerPlayer, Mob> entry : travelling)
                {
                        travel(entry.getKey(), entry.getValue());
                }
        }

        /**
         * Moves a carried mob to its carrier's new dimension. Vanilla replaces the entity with a copy there; the copy
         * loads as an orphan and gets released by {@link #onEntityLoad}, so the carry resumes with the state captured
         * here, which still holds the mob's original flags.
         */
        private static void travel(ServerPlayer carrier, Mob mob)
        {
                CarryState state = Services.CARRY_STATE.get(mob);
                Entity arrived = mob.teleport(new TeleportTransition(carrier.level(), carrier.position()
                        .add(0.0, carrier.getBbHeight(), 0.0), Vec3.ZERO, carrier.getYRot(), mob.getXRot(),
                        TeleportTransition.DO_NOTHING));
                if (state != null && arrived instanceof Mob arrivedMob && !arrivedMob.isRemoved())
                {
                        hold(carrier, arrivedMob, state);
                        return;
                }
                removeSlowdown(carrier);
                Mob left = arrived instanceof Mob arrivedMob ? arrivedMob : mob;
                if (!left.isRemoved())
                {
                        release(left, left.position());
                }
        }

        /**
         * A mob loading with a CarryState nobody holds (crash, restart, chunk reload) gets its original state back. With
         * {@link CarrierLogout#KEEP_CARRYING} it waits for its carrier instead, and is carried again if they're online.
         */
        public static void onEntityLoad(Entity entity)
        {
                CarryState state = Services.CARRY_STATE.get(entity);
                if (state == null || !(entity instanceof Mob mob) || CARRIED.get(state.carrier()) == mob)
                {
                        return;
                }
                if (keepsCarryOnLogout() && mob.level() instanceof ServerLevel level)
                {
                        ServerPlayer carrier = level.getServer().getPlayerList().getPlayer(state.carrier());
                        if (carrier == null)
                        {
                                return;
                        }
                        if (carriedBy(carrier) == null)
                        {
                                hold(carrier, mob, state);
                                return;
                        }
                }
                release(mob, mob.position());
        }

        public static void onLogout(ServerPlayer player)
        {
                Mob mob = CARRIED.remove(player.getUUID());
                removeSlowdown(player);
                if (mob != null && !mob.isRemoved() && !keepsCarryOnLogout())
                {
                        release(mob, mob.position());
                }
        }

        /**
         * With {@link CarrierLogout#KEEP_CARRYING}: pick the waiting mob back up. It hangs where the carrier logged out,
         * so it's near their login position; mobs loading later are handled by {@link #onEntityLoad}.
         */
        public static void onLogin(ServerPlayer player)
        {
                if (!keepsCarryOnLogout() || carriedBy(player) != null)
                {
                        return;
                }
                for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16.0)))
                {
                        CarryState state = Services.CARRY_STATE.get(mob);
                        if (state != null && state.carrier().equals(player.getUUID()))
                        {
                                hold(player, mob, state);
                                return;
                        }
                }
        }

        /** Server stop. With {@link CarrierLogout#KEEP_CARRYING} the mobs are saved still carried, to resume later. */
        public static void dropAll()
        {
                if (!keepsCarryOnLogout())
                {
                        for (Mob mob : CARRIED.values())
                        {
                                if (!mob.isRemoved())
                                {
                                        release(mob, mob.position());
                                }
                        }
                }
                CARRIED.clear();
        }

        private static boolean keepsCarryOnLogout()
        {
                return ThrowableMobsConfig.CARRY.onCarrierLogout() == CarrierLogout.KEEP_CARRYING;
        }

        /**
         * Damage hook (server). Depending on the config, a carried mob takes no suffocation damage (it sits in the
         * ceiling in 2-high tunnels), no fall damage, and no damage from its carrier.
         */
        public static boolean allowDamage(LivingEntity entity, DamageSource source)
        {
                CarryState state = Services.CARRY_STATE.get(entity);
                if (state == null)
                {
                        return true;
                }
                CarryConfig config = ThrowableMobsConfig.CARRY;
                boolean fromCarrier = source.getEntity() != null
                        && source.getEntity().getUUID().equals(state.carrier());
                return !(config.protectFromSuffocation() && source.is(DamageTypes.IN_WALL)
                        || config.protectFromFall() && source.is(DamageTypeTags.IS_FALL)
                        || config.protectFromCarrier() && fromCarrier);
        }

        private static void follow(Player carrier, Mob mob)
        {
                mob.setPos(carrier.getX(), carrier.getY() + carrier.getBbHeight(), carrier.getZ());
                mob.setDeltaMovement(Vec3.ZERO);
                mob.resetFallDistance();
                if (ThrowableMobsConfig.CARRY.carriedFacing() == CarriedFacing.CARRIER)
                {
                        mob.setYRot(carrier.getYRot());
                        mob.setYHeadRot(carrier.getYRot());
                        mob.setYBodyRot(carrier.getYRot());
                }
                // The mob sits inside the portal frame with its carrier; without a cooldown it sometimes travels on
                // its own, ending the carry. Only the carrier's dimension change decides what happens to it.
                mob.setPortalCooldown();
                // NoAI stops the swell goal, but Creeper#tick keeps applying the last swell direction, so a creeper
                // picked up mid-hiss would still explode. Ignited creepers (flint and steel) keep burning on purpose.
                if (mob instanceof Creeper creeper && !creeper.isIgnited())
                {
                        creeper.setSwellDir(-1);
                }
        }

        private static void release(Mob mob, Vec3 at)
        {
                CarryState state = Services.CARRY_STATE.get(mob);
                Services.CARRY_STATE.remove(mob);
                mob.noPhysics = false;
                if (state != null)
                {
                        mob.setNoAi(state.originalNoAi());
                        mob.setNoGravity(state.originalNoGravity());
                        if (mob.level() instanceof ServerLevel level)
                        {
                                // Covers carriers still online when the mob is restored on load.
                                removeSlowdown(level.getServer().getPlayerList().getPlayer(state.carrier()));
                        }
                }
                mob.setPos(at);
                mob.setDeltaMovement(Vec3.ZERO);
                mob.resetFallDistance();
        }

        /**
         * Movement multiplier from the carry slowdown (1 = not slowed). Read from the synced attribute modifier, so it
         * works on the carrier's client too, where player movement is simulated.
         */
        public static double speedFactor(LivingEntity entity)
        {
                AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
                AttributeModifier slowdown = speed != null ? speed.getModifier(SLOWDOWN_ID) : null;
                return slowdown != null ? 1.0 + slowdown.amount() : 1.0;
        }

        /** Transient modifier: not saved, so a crash can't leave a player permanently slowed. */
        private static void applySlowdown(ServerPlayer player, Mob mob)
        {
                CarryConfig config = ThrowableMobsConfig.CARRY;
                double slowdown = ThrowPhysics.carrySlowdown(CarryRules.mass(mob), config.maxMass(),
                        config.slowdownFactor(), config.maxSlowdown());
                AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null && slowdown > 0.0)
                {
                        speed.addOrUpdateTransientModifier(new AttributeModifier(SLOWDOWN_ID, -slowdown,
                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
        }

        private static void removeSlowdown(@Nullable ServerPlayer player)
        {
                AttributeInstance speed = player != null ? player.getAttribute(Attributes.MOVEMENT_SPEED) : null;
                if (speed != null)
                {
                        speed.removeModifier(SLOWDOWN_ID);
                }
        }

        /**
         * Without a ground target and with {@code dropNeedsGround} off: one step in front of the player if there's room
         * and nothing in between, else at the player's feet.
         */
        private static Vec3 fallbackDropPosition(ServerPlayer player, Mob mob)
        {
                Vec3 look = player.getLookAngle();
                Vec3 flat = new Vec3(look.x, 0.0, look.z);
                if (flat.lengthSqr() > 1.0E-4)
                {
                        double distance = (player.getBbWidth() + mob.getBbWidth()) / 2.0 + 0.5;
                        Vec3 front = player.position().add(flat.normalize().scale(distance));
                        Vec3 from = player.position().add(0.0, 0.5, 0.0);
                        boolean clearPath = player.level()
                                .clip(new ClipContext(from, front.add(0.0, 0.5, 0.0), ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE, player))
                                .getType() == HitResult.Type.MISS;
                        if (clearPath && player.level().noCollision(mob,
                                mob.getDimensions(mob.getPose()).makeBoundingBox(front)))
                        {
                                return front;
                        }
                }
                return player.position();
        }
}
