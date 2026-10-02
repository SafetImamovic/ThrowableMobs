# M1 — Pick up & drop

Plan for milestone M1 from [THROWABLE_MOBS.md §18](../../THROWABLE_MOBS.md#18-roadmap).
Branch: `feature/carry` (from `develop`).

## Goal / definition of done

On a **dedicated server**, on both **Fabric and NeoForge**, a player can:

1. Look at a mob within reach and see an on-screen prompt: **"Sneak + [Use] to pick up"** — or **"Can't pick this one up"** with the reason (too heavy, unthrowable, not your pet, …).
2. Sneak + right-click a mob with an empty hand → mob is held above the player's head, AI paused, player slowed by the mob's mass. Prompt switches to **"Sneak + [Use] to drop"**.
3. Sneak + right-click again → mob is placed in front of the player, original state restored exactly.
4. Never lose or permanently freeze a mob — carrier logs out, dies, changes dimension, or the server stops.

## Decisions

| Topic | Decision |
|---|---|
| Config | Hardcoded values in a global `CarryConfig` record (`ThrowableMobsConfig.CARRY`). Real config file comes in M2. |
| Carry model | **Follow model** (decided after spike research, see below). Mob stays a normal entity, *not* a passenger. Server moves it above the carrier's head every tick; client mod locks it to the carrier for smooth rendering. No mixins. |
| Carry position | **On the player's head** (feet at carrier's current hitbox top, so it follows crouching). Configurable offset / "in arms" later. |
| What can be picked up | `Mob` subclasses only. No players, armor stands, boats. Players come later behind `allow_players` (off by default). |
| Input | Sneak + right-click (use key) with empty main hand. Same as Carry On; clash accepted. Dedicated keybind later. |
| Rules evaluation | `CarryRules` runs on **both sides**: client for the on-screen prompt, server as the authority. Rules may only read data that is synced to the client (tags, hitbox, tamed owner, leash, passengers, synced `CarryState`). |

## Why the follow model (spike findings, MC 26.3 source)

| Finding | Where |
|---|---|
| Server refuses any entity riding a player: `startRiding` returns false when the vehicle's type can't be saved, and `PLAYER` is `.noSave()`. Even `force = true` doesn't skip it. | `Entity.startRiding`, `EntityTypes.PLAYER` |
| Passenger changes are sent to *tracking* players only, never to the vehicle's own client. The carrier wouldn't see its own passenger. | `ServerEntity` → `sendToTrackingPlayers` |
| Passengers aren't saved with chunks (`shouldBeSaved()` → false when `isPassenger()`), so a crash mid-carry would delete the mob. | `Entity.shouldBeSaved` |

Passenger model = 2 mixins + crash data loss. Follow model = no mixins, mob is always saved (crash → restored on load), costs client-side positioning code.

## Risks — spike first

| Risk | Detail |
|---|---|
| Smoothness | Server movement packets are interpolated on clients and lag behind. Client must lock the mob to the carrier each tick (and set `xo/yo/zo` from the carrier's previous position) so render interpolation matches the carrier. Check the vanilla interpolation handler doesn't fight it — may need to cancel it while carried. |
| State sync to clients | Every client tracking the mob needs `CarryState` (who carries it). Check attachment sync on 26.3: Fabric `syncWith(..., AttachmentSyncPredicate.all())`, NeoForge `.sync(...)`. Fallback: own S2C payload. |
| Collisions | Carried mob gets pushed by entities / clips ceilings. Use `noPhysics = true` + zero velocity + `noGravity` while carried; server re-positions every tick anyway. |
| Teleports | Same-level `/tp` and ender pearls: mob just follows (re-positioned next tick). Level change (portal, `/tp` to other dimension): drop at carrier's last position. |

Spike = throwaway code, Fabric only, hardcoded pickup, `runServer` + 2 clients. Gate: no visible lag/jitter on carrier's own client and on the other client while sprinting/jumping/flying.

## Steps

| # | Step | Contents |
|---|---|---|
| 0 | **Split `physics/`** (M0 follow-up) | Own Gradle module, no Minecraft imports. Move `ThrowPhysics` + test. `common` compiles against it; loader jars compile its sources in (same way they consume `common`). Separate `refactor:` commit. |
| 1 | **Spike** | See above. |
| 2 | **Platform plumbing** | `platform/services/`: attachment access, networking (C2S payload send/register), HUD layer registration, event wiring per loader. Fabric `client` entrypoint; NeoForge client setup. |
| 3 | **`CarryState` attachment** (on the mob) | `record CarryState(UUID carrier, boolean originalNoAi, boolean originalNoGravity, long pickupTick)` + codec + stream codec. **Persistent** (mob loading with state but no online carrier → restored) and **synced to tracking clients** (prompt, client lock). |
| 4 | **`CarryRules`** | `check(player, mob) → Allowed / Denied(reason)`. Order: spectator, main hand empty, already carrying; target is `Mob`, alive, not already carried (`CarryState`); not vehicle/passenger (incl. the horse you ride), not leashed; not in `#throwablemobs:unthrowable`; not multipart; tamed or `#throwablemobs:owner_only` → owner only; mass ≤ `max_mass`; within reach. Sneak is *not* a rule — it's part of the input, so the prompt shows before you sneak. |
| 5 | **`CarryManager`** (server) | `pickUp`: store state, `setNoAi(true)`, `setNoGravity(true)`, `noPhysics = true`, add slowdown, position on head. `drop`: place 1 block in front (if free, else at feet), zero velocity + fall distance, restore flags, remove attachment + modifier. `tick`: invariant — carrier online, alive, same level, mob alive; then move mob to `carrier.position() + (0, carrier.getBbHeight(), 0)`, zero velocity. Else drop at carrier's last position. Covers death, portals, logout, other mods. |
| 5b | **Client lock** | Client tick: for each entity with `CarryState`, set position (and previous position) from the carrier → smooth on every client. |
| 6 | **Loader events** | Event table below. Logic stays in `common`. Only handle `MAIN_HAND` (no double fire). |
| 7 | **Drop input** | Empty-hand right-click on air sends **no packet**. Client tick: carrying + sneak + use key pressed (edge) → `DropCarriedPayload` (C2S). Server validates. Right-clicking the carried mob itself (looking up) also drops. |
| 8 | **On-screen prompt** (client) | HUD layer under the crosshair. Looking at a mob (`Minecraft.hitResult` is an `EntityHitResult`, which already respects entity interaction range), not carrying, empty main hand → run `CarryRules`: allowed → "Sneak + [Use] to pick up" (key names from the current bindings, so rebinding updates the text); denied → "Can't pick this one up" + reason line. While carrying → "Sneak + [Use] to drop". Hidden with F1 / when a screen is open. Server still shows a denial in the action bar if client and server disagree. |
| 9 | **Carried protection** | Cancel `in_wall` (head clips ceiling in 2-high tunnels), fall damage, and damage from the carrier for carried mobs. |
| 10 | **Slowdown** | Movement-speed modifier `throwablemobs:carry_slowdown`, `ADD_MULTIPLIED_TOTAL`, `-clamp(mass / max_mass × 0.5, 0, 0.6)`. Mass from `ThrowPhysics.massFromHitbox` using **current** hitbox. Math in `physics/` + unit test. |
| 11 | **Data** | `tags/entity_type/unthrowable.json`: `#c:bosses` + `#c:capturing_not_supported` (`"required": false`) + warden, wither, ender dragon explicit. `owner_only.json`. `assets/throwablemobs/lang/en_us.json`: prompt + denial reasons. |
| 12 | **Tests** | JUnit: slowdown math, drop placement math. GameTests (registration API changed in 1.21.5+ — check Fabric/NeoForge support on 26.3): pickup works / unthrowable blocked / drop restores `noAi` / pre-existing `NoAI` preserved. If GameTest setup is expensive → defer to M6. |
| 13 | **Manual test pass** | Matrix below, both loaders. |

## Status (2026-10-02)

| Step | State |
|---|---|
| 0 physics module | Done. 7 unit tests pass. |
| 1 spike | Code done (both loaders). **In-game smoothness check pending.** |
| 2–11 | Code done, builds on both loaders: attachment, `CarryRules`, `CarryManager`, client lock, drop payload, HUD prompt, damage protection, slowdown, tags, lang. |
| 12 tests | Unit tests for slowdown done. GameTests deferred to M6 (26.x GameTest registration needs separate research). |
| 13 manual pass | Pending (matrix below). |

## Hardcoded config (`CarryConfig`)

| Key | Default |
|---|---|
| `require_sneak` | `true` |
| `require_empty_hand` | `true` |
| `max_mass` | `1500.0` |
| `default_density` | `400.0` |
| `allow_villagers` | `true` |
| `owner_only_for_tamed` | `true` |
| `max_slowdown` | `0.6` |
| `carry_offset_y` | `0.0` (above hitbox top) |
| `show_prompt` | `true` (client) |

## Event mapping (verify names on 26.3)

| Need | Fabric | NeoForge |
|---|---|---|
| Right-click entity | `UseEntityCallback` | `PlayerInteractEvent.EntityInteract` |
| Server tick | `ServerTickEvents.END_SERVER_TICK` | `ServerTickEvent.Post` |
| Client tick | `ClientTickEvents.END_CLIENT_TICK` | `ClientTickEvent.Post` |
| HUD layer | `HudElementRegistry` | `RegisterGuiLayersEvent` |
| Cancel damage | `ServerLivingEntityEvents.ALLOW_DAMAGE` | `LivingIncomingDamageEvent` |
| Logout → drop | `ServerPlayConnectionEvents.DISCONNECT` | `PlayerEvent.PlayerLoggedOutEvent` |
| Stopping → drop all (tidy, not required: mob is saved anyway) | `ServerLifecycleEvents.SERVER_STOPPING` | `ServerStoppingEvent` |
| Entity load → orphan fix | `ServerEntityEvents.ENTITY_LOAD` | `EntityJoinLevelEvent` |
| Attachment (persistent + synced) | `AttachmentRegistry.create(..., b -> b.persistent(CODEC).syncWith(STREAM_CODEC, AttachmentSyncPredicate.all()))` | `AttachmentType.builder(...).serialize(...).sync(...)` + `DeferredRegister` |
| C2S payload | `PayloadTypeRegistry.playC2S()` + `ServerPlayNetworking` | `RegisterPayloadHandlersEvent` |

## Manual test matrix (dedicated server, 2 clients)

- [ ] Prompt: shows "pick up" on a chicken in reach; "can't" + reason on iron golem (too heavy), warden (unthrowable), someone else's wolf (not yours); hides out of reach, with item in hand, with F1.
- [ ] Prompt key names follow rebinding (rebind Use → text updates).
- [ ] Pick up + drop chicken, cow, creeper, villager. Creeper doesn't explode while carried; villager trades afterwards.
- [ ] Mob with `NoAI` set by command keeps `NoAI` after carry.
- [ ] Other client sees mob on carrier's head, no jitter while sprinting / jumping.
- [ ] 2-high tunnel: no suffocation. Carrier falls 10 blocks: mob unhurt.
- [ ] Carrier dies / takes Nether portal / `/tp` to another dimension / logs out → mob dropped at carrier position, still exists.
- [ ] `/stop` while carrying, restart → mob present, AI works.
- [ ] Kill the server process while carrying (simulated crash), restart → mob restored near carrier's last position with AI.
- [ ] Same-level `/tp` and ender pearl while carrying → mob follows.
- [ ] Drop in 1-wide corridor / facing a wall → mob not stuck in a block.
- [ ] Mod on client only / server only → no crash (graceful refusal).
