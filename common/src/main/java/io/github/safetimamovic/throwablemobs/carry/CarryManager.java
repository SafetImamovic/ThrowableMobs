package io.github.safetimamovic.throwablemobs.carry;

import io.github.safetimamovic.throwablemobs.Constants;
import io.github.safetimamovic.throwablemobs.advancement.ModTriggers;
import io.github.safetimamovic.throwablemobs.advancement.ThrowEventTrigger.Event;
import io.github.safetimamovic.throwablemobs.behavior.Hooks;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarriedFacing;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarrierLogout;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.CarryConfig;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig.DimensionChange;
import io.github.safetimamovic.throwablemobs.config.ThrowableMobsConfig;
import io.github.safetimamovic.throwablemobs.physics.ThrowPhysics;
import io.github.safetimamovic.throwablemobs.network.PlayerEnabledPayload;
import io.github.safetimamovic.throwablemobs.platform.Services;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
//? if >=26.1
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
//? if >=26.1 {
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
//?}
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
//? if >=1.21
import net.minecraft.world.level.portal.TeleportTransition;
//? if >=26.1
import net.minecraft.world.level.storage.TagValueOutput;
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
        //? if >=1.21 {
        private static final Identifier SLOWDOWN_ID = Constants.id("carry_slowdown");
        //?} else
        /*private static final UUID SLOWDOWN_ID = UUID.nameUUIDFromBytes("throwablemobs:carry_slowdown".getBytes());*/

        /** Extra reach allowed for pick-up requests, for movement between the client's check and the server's. */
        private static final double PICK_UP_REACH_TOLERANCE = 3.0;

        /** How far a mob may be from its returning carrier to be carried again (RESUME, KEEP_CARRYING). */
        private static final double RESUME_RANGE = 16.0;

        /** Carrier UUID to carried mob. Server-side only; mobs that load with a stale {@link CarryState} are restored. */
        private static final Map<UUID, Mob> CARRIED = new HashMap<>();

        /** Players whose own {@code enabled} is off ({@link PlayerEnabledPayload}), until they log out. */
        private static final Set<UUID> DISABLED_PLAYERS = new HashSet<>();

        /** {@link CarrierLogout#RESUME}: carrier UUID to the mob they left behind. In memory, so a restart forgets it. */
        private static final Map<UUID, LeftBehind> LEFT_BEHIND = new HashMap<>();

        /** {@link CarrierLogout#KEEP_CARRYING}: loaded mobs waiting for their carrier, checked for the time limit. */
        private static final Map<UUID, ResourceKey<Level>> WAITING = new HashMap<>();

        /** Carrier UUID to the game time their last carry ended (put down or thrown), for the pick-up cooldown. */
        private static final Map<UUID, Long> LET_GO_AT = new HashMap<>();

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
                // Put-down still works with the mod off: nothing can be carried by then anyway.
                if (!player.isSpectator() && !putDownCoolingDown(player))
                {
                        drop(player);
                }
        }

        /** {@link io.github.safetimamovic.throwablemobs.network.PickUpPayload} from a client (pick-up key). */
        public static void onPickUpRequest(ServerPlayer player, int entityId)
        {
                if (!enabledFor(player) || !CarryRules.canUseHands(player) || carriedBy(player) != null
                        || pickUpCoolingDown(player) || !(player.level().getEntity(entityId) instanceof Mob mob)
                        //? if >=26.1 {
                        || !player.isWithinEntityInteractionRange(mob, PICK_UP_REACH_TOLERANCE))
                        //?} else if >=1.20.5 {
                        /*|| !player.canInteractWithEntity(mob, PICK_UP_REACH_TOLERANCE))
                        *///?} else
                        /*|| !Versioned.withinReach(player, mob.getBoundingBox(), PICK_UP_REACH_TOLERANCE))*/
                {
                        return;
                }
                CarryRules.Denial denial = CarryRules.check(player, mob);
                if (denial != null)
                {
                        // The client only asks when its own check passed, so tell the player why the server disagrees.
                        //? if >=26.1 {
                        player.sendOverlayMessage(denial.message());
                        //?} else
                        /*player.displayClientMessage(denial.message(), true);*/
                        return;
                }
                pickUp(player, mob);
        }

        /** Picks up without checking {@link CarryRules}; callers check first. */
        public static void pickUp(ServerPlayer player, Mob mob)
        {
                hold(player, mob, new CarryState(player.getUUID(), mob.isNoAi(), mob.isNoGravity(),
                        mob.level().getGameTime()));
                ThrowManager.forgetCatch(player);
                CarryHunger.pickUp(player);
                ModTriggers.THROW_EVENT.trigger(player, Event.PICKUP, mob, 0.0, 0.0);
                Hooks.pickup(mob, player);
        }

        /**
         * Starts a carry from a catch, with the flags the mob had before it was first picked up, so they come back
         * right after any number of passes. No {@code onPickup}: the catch ran {@code onCaught} instead.
         */
        static void catchMob(ServerPlayer player, Mob mob, ThrowState thrown)
        {
                hold(player, mob, new CarryState(player.getUUID(), thrown.originalNoAi(), thrown.originalNoGravity(),
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
                        if (ThrowableMobsConfig.carry().putDownNeedsGround())
                        {
                                //? if >=26.1 {
                                player.sendOverlayMessage(Component.translatable("throwablemobs.carry.need_ground"));
                                //?} else
                                /*player.displayClientMessage(Component.translatable("throwablemobs.carry.need_ground"), true);*/
                                return;
                        }
                        at = fallbackDropPosition(player, mob);
                }
                CARRIED.remove(player.getUUID());
                removeSlowdown(player);
                LET_GO_AT.put(player.getUUID(), player.level().getGameTime());
                release(mob, at);
        }

        private static boolean pickUpCoolingDown(ServerPlayer player)
        {
                Long letGoAt = LET_GO_AT.get(player.getUUID());
                return letGoAt != null
                        && player.level().getGameTime() - letGoAt < ThrowableMobsConfig.carry().pickUpCooldownTicks();
        }

        /** Counts from the pickup or catch; a carry resumed after a logout keeps its first pickup time. */
        private static boolean putDownCoolingDown(ServerPlayer player)
        {
                Mob mob = carriedBy(player);
                CarryState state = mob != null ? Services.CARRY_STATE.get(mob) : null;
                return state != null && mob.level().getGameTime() - state.pickupTick()
                        < ThrowableMobsConfig.carry().putDownCooldownTicks();
        }

        /**
         * {@link PlayerEnabledPayload} from a client. Turned off, the player can't pick up, throw or catch until it's
         * back on, and whatever they carry is put down at their feet.
         */
        public static void onPlayerEnabled(ServerPlayer player, boolean enabled)
        {
                if (enabled)
                {
                        DISABLED_PLAYERS.remove(player.getUUID());
                        return;
                }
                DISABLED_PLAYERS.add(player.getUUID());
                ThrowManager.forgetCharge(player);
                putDownAtFeet(player);
        }

        /** The server's {@code enabled} and the player's own. */
        public static boolean enabledFor(ServerPlayer player)
        {
                return ThrowableMobsConfig.enabled() && !DISABLED_PLAYERS.contains(player.getUUID());
        }

        private static void putDownAtFeet(ServerPlayer player)
        {
                Mob mob = CARRIED.remove(player.getUUID());
                removeSlowdown(player);
                if (mob != null && !mob.isRemoved())
                {
                        release(mob, landing(mob, player.position()));
                }
        }

        /** Once per server tick: keep carried mobs on their carriers' heads, drop them when the carry is broken. */
        public static void tick(MinecraftServer server)
        {
                if (!ThrowableMobsConfig.enabled() && !CARRIED.isEmpty())
                {
                        // Turned off (config edit, reload): every carry ends where its carrier stands.
                        for (UUID carrier : List.copyOf(CARRIED.keySet()))
                        {
                                ServerPlayer player = server.getPlayerList().getPlayer(carrier);
                                Mob mob = CARRIED.remove(carrier);
                                removeSlowdown(player);
                                if (!mob.isRemoved())
                                {
                                        release(mob, player != null ? landing(mob, player.position()) : mob.position());
                                }
                        }
                }
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
                                // Death, spectator mode (logouts go through onLogout first): drop at the carrier's feet.
                                it.remove();
                                removeSlowdown(carrier);
                                release(mob, carrier != null ? landing(mob, carrier.position()) : mob.position());
                                continue;
                        }
                        if (carrier.level() != mob.level())
                        {
                                it.remove();
                                if (ThrowableMobsConfig.carry().onDimensionChange() == DimensionChange.TRAVEL)
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
                        Hooks.carryTick(mob, carrier);
                }
                for (Map.Entry<ServerPlayer, Mob> entry : travelling)
                {
                        travel(entry.getKey(), entry.getValue());
                }
                if (server.getTickCount() % 20 == 0)
                {
                        expireLogouts(server);
                }
        }

        /**
         * Ends the carry without placing the mob: it stays where it is with its original flags back. Returns the state it
         * had, or {@code null} if the player carries nothing.
         */
        static @Nullable CarryState letGo(ServerPlayer player)
        {
                Mob mob = carriedBy(player);
                CARRIED.remove(player.getUUID());
                removeSlowdown(player);
                if (mob == null)
                {
                        return null;
                }
                LET_GO_AT.put(player.getUUID(), player.level().getGameTime());
                CarryState state = Services.CARRY_STATE.get(mob);
                release(mob, mob.position());
                return state;
        }

        /**
         * Moves a carried mob to its carrier's new dimension. Vanilla replaces the entity with a copy there; the copy
         * loads as an orphan and gets released by {@link #onEntityLoad}, so the carry resumes with the state captured
         * here, which still holds the mob's original flags.
         */
        private static void travel(ServerPlayer carrier, Mob mob)
        {
                CarryState state = Services.CARRY_STATE.get(mob);
                //? if >=26.1 {
                Entity arrived = mob.teleport(new TeleportTransition(carrier.level(), carrier.position()
                        .add(0.0, carrier.getBbHeight(), 0.0), Vec3.ZERO, carrier.getYRot(), mob.getXRot(),
                        TeleportTransition.DO_NOTHING));
                //?} else if >=1.21 {
                /*Entity arrived = mob.changeDimension(new TeleportTransition(carrier.serverLevel(), carrier.position()
                        .add(0.0, carrier.getBbHeight(), 0.0), Vec3.ZERO, carrier.getYRot(), mob.getXRot(),
                        TeleportTransition.DO_NOTHING));
                *///?} else {
                /*// Before 1.21 changeDimension always goes through portal placement; do its copy step by hand.
                Entity arrived = mob.getType().create(carrier.serverLevel());
                if (arrived != null)
                {
                        arrived.restoreFrom(mob);
                        arrived.moveTo(carrier.getX(), carrier.getY() + carrier.getBbHeight(), carrier.getZ(),
                                carrier.getYRot(), mob.getXRot());
                        arrived.setDeltaMovement(Vec3.ZERO);
                        mob.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
                        carrier.serverLevel().addDuringTeleport(arrived);
                }
                *///?}
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
         * A mob loading with a CarryState nobody holds. One waiting for its carrier ({@link CarrierLogout#KEEP_CARRYING})
         * keeps waiting, or is carried again if they're online and near, or put down once the time is up. Any other
         * (server crash mid-carry, a copy arriving in another dimension) gets its original state back, or starts
         * waiting with KEEP_CARRYING.
         */
        public static void onEntityLoad(Entity entity)
        {
                CarryState state = Services.CARRY_STATE.get(entity);
                if (state == null || !(entity instanceof Mob mob) || CARRIED.get(state.carrier()) == mob
                        || !(mob.level() instanceof ServerLevel level))
                {
                        return;
                }
                if (!state.isWaiting())
                {
                        if (ThrowableMobsConfig.carry().onCarrierLogout() != CarrierLogout.KEEP_CARRYING)
                        {
                                release(mob, mob.position());
                                return;
                        }
                        state = state.waitingFrom(level.getGameTime());
                        Services.CARRY_STATE.set(mob, state);
                }
                if (waitIsOver(state, level))
                {
                        release(mob, mob.position());
                        return;
                }
                ServerPlayer carrier = level.getServer().getPlayerList().getPlayer(state.carrier());
                if (carrier != null && carriedBy(carrier) == null && carrier.level() == level
                        && mob.distanceTo(carrier) <= RESUME_RANGE)
                {
                        WAITING.remove(mob.getUUID());
                        hold(carrier, mob, state.resumed());
                        return;
                }
                WAITING.put(mob.getUUID(), level.dimension());
        }

        public static void onLogout(ServerPlayer player)
        {
                DISABLED_PLAYERS.remove(player.getUUID());
                LET_GO_AT.remove(player.getUUID());
                Mob mob = CARRIED.remove(player.getUUID());
                removeSlowdown(player);
                if (mob != null && !mob.isRemoved())
                {
                        leave(player, mob);
                }
        }

        /**
         * The carrier logs back in: they get back the mob they took along ({@link CarrierLogout#TAKE_ALONG}), left
         * behind ({@link CarrierLogout#RESUME}), or that waits for them nearby ({@link CarrierLogout#KEEP_CARRYING}),
         * whatever the setting is now. Waiting mobs that load later are handled by {@link #onEntityLoad}.
         */
        public static void onLogin(ServerPlayer player)
        {
                if (carriedBy(player) != null || bringBack(player) || resumeLeftBehind(player))
                {
                        return;
                }
                for (Mob mob : player.level().getEntitiesOfClass(Mob.class,
                        player.getBoundingBox().inflate(RESUME_RANGE)))
                {
                        CarryState state = Services.CARRY_STATE.get(mob);
                        if (state != null && state.isWaiting() && state.carrier().equals(player.getUUID()))
                        {
                                WAITING.remove(mob.getUUID());
                                hold(player, mob, state.resumed());
                                return;
                        }
                }
        }

        /**
         * Server stop, while the carriers are still online: each carried mob is handled as if its carrier logged out,
         * before the players and the world are saved. RESUME is remembered in memory only, so it's a plain put-down here.
         */
        public static void dropAll(MinecraftServer server)
        {
                for (Map.Entry<UUID, Mob> entry : CARRIED.entrySet())
                {
                        Mob mob = entry.getValue();
                        if (mob.isRemoved())
                        {
                                continue;
                        }
                        ServerPlayer carrier = server.getPlayerList().getPlayer(entry.getKey());
                        if (carrier != null)
                        {
                                leave(carrier, mob);
                        }
                        else
                        {
                                release(mob, mob.position());
                        }
                }
                CARRIED.clear();
                LEFT_BEHIND.clear();
                WAITING.clear();
                LET_GO_AT.clear();
        }

        /** What happens to a carried mob when its carrier leaves the server, per {@code on_carrier_logout}. */
        private static void leave(ServerPlayer carrier, Mob mob)
        {
                switch (ThrowableMobsConfig.carry().onCarrierLogout())
                {
                        case PUT_DOWN -> release(mob, landing(mob, carrier.position()));
                        case RESUME ->
                        {
                                release(mob, landing(mob, carrier.position()));
                                MinecraftServer server = carrier.level().getServer();
                                long ticks = logoutTicks();
                                LEFT_BEHIND.put(carrier.getUUID(), new LeftBehind(mob.getUUID(), mob.level().dimension(),
                                        ticks == Long.MAX_VALUE ? Long.MAX_VALUE : server.getTickCount() + ticks));
                        }
                        case KEEP_CARRYING -> waitForCarrier(carrier, mob);
                        case TAKE_ALONG -> takeAlong(carrier, mob);
                }
        }

        /** KEEP_CARRYING: the mob stays frozen at the carrier's feet, still holding its CarryState, now waiting. */
        private static void waitForCarrier(ServerPlayer carrier, Mob mob)
        {
                CarryState state = Services.CARRY_STATE.get(mob);
                if (state == null)
                {
                        release(mob, landing(mob, carrier.position()));
                        return;
                }
                Services.CARRY_STATE.set(mob, state.waitingFrom(mob.level().getGameTime()));
                mob.noPhysics = false;
                mob.setPos(landing(mob, carrier.position()));
                mob.setDeltaMovement(Vec3.ZERO);
                mob.resetFallDistance();
                WAITING.put(mob.getUUID(), mob.level().dimension());
        }

        /**
         * TAKE_ALONG: the mob, back to its original state, is saved into the carrier's player data and removed from the
         * world, passengers included. If it can't be saved it's put down instead.
         */
        private static void takeAlong(ServerPlayer carrier, Mob mob)
        {
                release(mob, carrier.position());
                //? if >=26.1 {
                try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(mob.problemPath(),
                        Constants.LOG))
                {
                        TagValueOutput output = TagValueOutput.createWithContext(reporter, mob.registryAccess());
                        if (!mob.saveAsPassenger(output))
                        {
                                release(mob, landing(mob, carrier.position()));
                                return;
                        }
                        Services.CARRY_STASH.set(carrier, NbtUtils.addCurrentDataVersion(output.buildResult()));
                }
                //?} else {
                /*CompoundTag output = new CompoundTag();
                if (!mob.saveAsPassenger(output))
                {
                        release(mob, landing(mob, carrier.position()));
                        return;
                }
                Services.CARRY_STASH.set(carrier, NbtUtils.addCurrentDataVersion(output));
                *///?}
                // Players riding the mob aren't saved with it, so they stay; everything else leaves with it.
                mob.getSelfAndPassengers().filter(entity -> !(entity instanceof Player)).toList()
                        .forEach(Entity::discard);
        }

        /** TAKE_ALONG: spawns the mob the player took along back on their head. Returns whether one came back. */
        private static boolean bringBack(ServerPlayer player)
        {
                CompoundTag tag = Services.CARRY_STASH.get(player);
                if (tag == null)
                {
                        return false;
                }
                Services.CARRY_STASH.remove(player);
                //? if >=26.1 {
                ServerLevel level = player.level();
                Entity entity = EntityType.loadEntityRecursive(tag, level,
                        new EntitySpawnRequest(EntitySpawnReason.LOAD, true), loaded ->
                        {
                                loaded.snapTo(player.getX(), player.getY() + player.getBbHeight(), player.getZ(),
                                        loaded.getYRot(), loaded.getXRot());
                                return loaded;
                        });
                //?} else {
                /*ServerLevel level = player.serverLevel();
                Entity entity = EntityType.loadEntityRecursive(tag, level, loaded ->
                {
                        loaded.moveTo(player.getX(), player.getY() + player.getBbHeight(), player.getZ(),
                                loaded.getYRot(), loaded.getXRot());
                        return loaded;
                });
                *///?}
                // Refused if an entity with its UUID is already loaded, like the copy in the world from before a crash.
                if (!(entity instanceof Mob mob) || !level.tryAddFreshEntityWithPassengers(entity))
                {
                        Constants.LOG.warn("Couldn't bring back the mob {} took along: {}", player.getScoreboardName(),
                                //? if >=26.1 {
                                tag.getStringOr("id", "unknown"));
                                //?} else
                                /*tag.getString("id"));*/
                        return false;
                }
                hold(player, mob, new CarryState(player.getUUID(), mob.isNoAi(), mob.isNoGravity(),
                        level.getGameTime()));
                return true;
        }

        /** RESUME: carries the mob left behind again, if it's in time, alive, near and free. */
        private static boolean resumeLeftBehind(ServerPlayer player)
        {
                LeftBehind left = LEFT_BEHIND.remove(player.getUUID());
                if (left == null || player.level().getServer().getTickCount() > left.until()
                        || player.level().dimension() != left.level())
                {
                        return false;
                }
                //? if >=26.1 {
                if (player.level().getEntity(left.mob()) instanceof Mob mob && mob.isAlive() && !mob.isPassenger()
                //?} else
                /*if (player.serverLevel().getEntity(left.mob()) instanceof Mob mob && mob.isAlive() && !mob.isPassenger()*/
                        && mob.distanceTo(player) <= RESUME_RANGE && Services.CARRY_STATE.get(mob) == null
                        && Services.THROW_STATE.get(mob) == null)
                {
                        hold(player, mob, new CarryState(player.getUUID(), mob.isNoAi(), mob.isNoGravity(),
                                mob.level().getGameTime()));
                        return true;
                }
                return false;
        }

        /** Once a second: forgets RESUME carriers out of time, puts down KEEP_CARRYING mobs whose wait is over. */
        private static void expireLogouts(MinecraftServer server)
        {
                LEFT_BEHIND.values().removeIf(left -> server.getTickCount() > left.until());
                Iterator<Map.Entry<UUID, ResourceKey<Level>>> it = WAITING.entrySet().iterator();
                while (it.hasNext())
                {
                        Map.Entry<UUID, ResourceKey<Level>> entry = it.next();
                        ServerLevel level = server.getLevel(entry.getValue());
                        Entity entity = level != null ? level.getEntity(entry.getKey()) : null;
                        if (entity == null)
                        {
                                // Unloaded with its chunk; onEntityLoad checks the time when it comes back.
                                it.remove();
                                continue;
                        }
                        CarryState state = Services.CARRY_STATE.get(entity);
                        if (!(entity instanceof Mob mob) || state == null || !state.isWaiting())
                        {
                                it.remove();
                        }
                        else if (waitIsOver(state, level))
                        {
                                it.remove();
                                release(mob, mob.position());
                        }
                }
        }

        private static boolean waitIsOver(CarryState state, ServerLevel level)
        {
                long ticks = logoutTicks();
                return ticks != Long.MAX_VALUE && level.getGameTime() - state.waitingSince() > ticks;
        }

        /** {@code carrier_logout_seconds} in ticks; {@link Long#MAX_VALUE} for 0, which means forever. */
        private static long logoutTicks()
        {
                int seconds = ThrowableMobsConfig.carry().carrierLogoutSeconds();
                return seconds <= 0 ? Long.MAX_VALUE : seconds * 20L;
        }

        /** Where a mob leaving its carrier lands: at the carrier's feet if it fits there, else where it is. */
        private static Vec3 landing(Mob mob, Vec3 feet)
        {
                return mob.level().noCollision(mob, mob.getDimensions(mob.getPose()).makeBoundingBox(feet)) ? feet
                        : mob.position();
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
                CarryConfig config = ThrowableMobsConfig.carry();
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
                if (ThrowableMobsConfig.carry().carriedFacing() == CarriedFacing.CARRIER)
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
                //? if >=1.21 {
                return slowdown != null ? 1.0 + slowdown.amount() : 1.0;
                //?} else
                /*return slowdown != null ? 1.0 + slowdown.getAmount() : 1.0;*/
        }

        /** Transient modifier: not saved, so a crash can't leave a player permanently slowed. */
        private static void applySlowdown(ServerPlayer player, Mob mob)
        {
                CarryConfig config = ThrowableMobsConfig.carry();
                double slowdown = ThrowPhysics.carrySlowdown(CarryRules.mass(mob), config.maxMass(),
                        config.slowdownFactor(), config.maxSlowdown());
                AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null && slowdown > 0.0)
                {
                        //? if >=1.21 {
                        speed.addOrUpdateTransientModifier(new AttributeModifier(SLOWDOWN_ID, -slowdown,
                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                        //?} else
                        /*speed.addTransientModifier(new AttributeModifier(SLOWDOWN_ID, "throwablemobs:carry_slowdown", -slowdown, AttributeModifier.Operation.MULTIPLY_TOTAL));*/
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

        /** A mob left behind with {@link CarrierLogout#RESUME}, until server tick {@code until}. */
        private record LeftBehind(UUID mob, ResourceKey<Level> level, long until)
        {
        }
}
