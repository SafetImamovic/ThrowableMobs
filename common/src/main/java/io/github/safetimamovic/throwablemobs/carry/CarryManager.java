package io.github.safetimamovic.throwablemobs.carry;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarryConfig;
import io.github.safetimamovic.throwablemobs.physics.ThrowPhysics;
import io.github.safetimamovic.throwablemobs.platform.Services;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
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
         * Use-entity hook (both sides). With the carry input: drop if carrying, otherwise pick up when the rules allow.
         * A denied pickup passes, so vanilla interactions (trading, mounting) still work.
         */
        public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity target)
        {
                if (hand != InteractionHand.MAIN_HAND || !CarryRules.isCarryInput(player))
                {
                        return InteractionResult.PASS;
                }
                if (level.isClientSide())
                {
                        // Predict the server's answer so the hand swings and vanilla doesn't also act on the click.
                        if (LocalCarry.get() != null)
                        {
                                return InteractionResult.SUCCESS;
                        }
                        return target instanceof Mob mob && CarryRules.check(player, mob) == null
                                ? InteractionResult.SUCCESS
                                : InteractionResult.PASS;
                }

                ServerPlayer serverPlayer = (ServerPlayer) player;
                if (carriedBy(serverPlayer) != null)
                {
                        dropInFront(serverPlayer);
                        return InteractionResult.SUCCESS;
                }
                if (target instanceof Mob mob && CarryRules.check(serverPlayer, mob) == null)
                {
                        pickUp(serverPlayer, mob);
                        return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
        }

        /** {@link io.github.safetimamovic.throwablemobs.network.DropCarriedPayload} from a client. */
        public static void onDropRequest(ServerPlayer player)
        {
                if (CarryRules.isCarryInput(player))
                {
                        dropInFront(player);
                }
        }

        /** Picks up without checking {@link CarryRules}; callers check first. */
        public static void pickUp(ServerPlayer player, Mob mob)
        {
                Services.CARRY_STATE.set(mob, new CarryState(player.getUUID(), mob.isNoAi(), mob.isNoGravity(),
                        mob.level().getGameTime()));
                mob.setNoAi(true);
                mob.setNoGravity(true);
                mob.noPhysics = true;
                mob.getNavigation().stop();
                mob.setTarget(null);
                CARRIED.put(player.getUUID(), mob);
                applySlowdown(player, mob);
                follow(player, mob);
        }

        public static void dropInFront(ServerPlayer player)
        {
                Mob mob = CARRIED.remove(player.getUUID());
                removeSlowdown(player);
                if (mob != null && !mob.isRemoved())
                {
                        release(mob, dropPosition(player, mob));
                }
        }

        /** Once per server tick: keep carried mobs on their carriers' heads, drop them when the carry is broken. */
        public static void tick(MinecraftServer server)
        {
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
                        if (carrier == null || !carrier.isAlive() || carrier.isSpectator()
                                || carrier.level() != mob.level())
                        {
                                // Logout, death, dimension change: drop where the mob is, above the carrier's last spot.
                                it.remove();
                                removeSlowdown(carrier);
                                release(mob, mob.position());
                                continue;
                        }
                        follow(carrier, mob);
                }
        }

        /** A mob loading with a CarryState nobody holds (crash, restart, chunk reload) gets its original state back. */
        public static void onEntityLoad(Entity entity)
        {
                CarryState state = Services.CARRY_STATE.get(entity);
                if (state != null && entity instanceof Mob mob && CARRIED.get(state.carrier()) != mob)
                {
                        release(mob, mob.position());
                }
        }

        public static void onLogout(ServerPlayer player)
        {
                Mob mob = CARRIED.remove(player.getUUID());
                removeSlowdown(player);
                if (mob != null && !mob.isRemoved())
                {
                        release(mob, mob.position());
                }
        }

        public static void dropAll()
        {
                for (Mob mob : CARRIED.values())
                {
                        if (!mob.isRemoved())
                        {
                                release(mob, mob.position());
                        }
                }
                CARRIED.clear();
        }

        /**
         * Damage hook (server). A carried mob takes no suffocation damage (it sits in the ceiling in 2-high tunnels), no
         * fall damage, and no damage from its carrier.
         */
        public static boolean allowDamage(LivingEntity entity, DamageSource source)
        {
                CarryState state = Services.CARRY_STATE.get(entity);
                if (state == null)
                {
                        return true;
                }
                boolean fromCarrier = source.getEntity() != null
                        && source.getEntity().getUUID().equals(state.carrier());
                return !(source.is(DamageTypes.IN_WALL) || source.is(DamageTypeTags.IS_FALL) || fromCarrier);
        }

        private static void follow(Player carrier, Mob mob)
        {
                mob.setPos(carrier.getX(), carrier.getY() + carrier.getBbHeight(), carrier.getZ());
                mob.setDeltaMovement(Vec3.ZERO);
                mob.resetFallDistance();
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

        /** One step in front of the player if there's room and nothing in between, else at the player's feet. */
        private static Vec3 dropPosition(ServerPlayer player, Mob mob)
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
