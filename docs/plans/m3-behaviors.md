# M3 — Behavior system

Plan for milestone M3 from [THROWABLE_MOBS.md §18](../../THROWABLE_MOBS.md#18-roadmap).
Branch: `feature/behaviors` (from `develop`, after M2 is merged).
Spec: §9 (mob behaviors), §10 (behavior API), §8.4 (damage split and fall immunity).

## Goal / definition of done

On a **dedicated server**, on both **Fabric and NeoForge**:

1. Mobs behave differently when picked up, carried, thrown, in flight and on impact, driven by **behaviors**. Our own vanilla behaviors use the same API that other mods do.
2. Six vanilla mobs ship with behaviors: **creeper** (explodes on impact), **enderman** (teleports instead of taking damage), **slime / magma cube** (bounce, split, magma ignites), **chicken** (flaps: low gravity, no self damage, may lay an egg), **blaze** (sets the target on fire), **cat** (lands on its feet: no self damage).
3. Pack devs can change all of this with **datapack mob profiles** (JSON, `/reload` works) and tags, with no code.
4. Modded mobs without a profile get a sensible guess from the **heuristic resolver** (a modded creeper explodes).
5. The trajectory preview still lands on the marker for mobs whose profile changes mass, gravity or drag.

## Decisions

| Topic | Decision |
|---|---|
| API package | `common/.../api/`: `ThrowBehavior`, `ThrowContext`, `ImpactContext`, `ImpactResult`, `MobProfile`, `ThrowableMobsApi`. It stays in `common` for M3; the separate `-api` Maven artifact comes in M6. Treat it as public anyway: javadoc on every type, no Minecraft internals that might change. |
| Hooks | `ThrowBehavior` default methods: `onPickup`, `onCarryTick`, `onThrow`, `onFlightTick`, `onImpact` (returns `ImpactResult`), `onLand`. `onCaught` comes with catching in M4; the method name is reserved. Each hook has **one call site** in `CarryManager` / `ThrowManager`. |
| `ImpactResult` | What a behavior can change about an impact: damage to the target and to the thrown mob (multiplied, or cancelled), knockback multiplier, and `continueFlight(velocity)` for bounces. Several behaviors run in order and each one gets the result of the one before. |
| Behavior types | Atoms such as `explode` and `split` are `BehaviorType`s, each with a `MapCodec`. They're kept in **our own map keyed by `Identifier`**, not a vanilla registry, so both loaders need no registration code and other mods register from common code with `ThrowableMobsApi.registerBehaviorType`. JSON dispatches on `"type"`. |
| Triggers | `pickup`, `carry_tick`, `throw`, `flight_tick`, `impact` (block or entity), `impact_entity`, `impact_block`, `land`. `caught` comes with M4. |
| Conditions | M3 adds `min_impact_energy`, `chance`, `in_water` and `is_charged` (creeper). Vanilla predicates (`"predicate": "pack:is_night"`) and `dimension` come later; the key names are reserved. |
| Mob profiles | `data/<ns>/throwablemobs/mob_profile/<name>.json`, decoded with codecs and loaded by a server reload listener on each loader. Fields in M3: `schema_version`, `entities` (ids and tags), `priority`, `physics` (`mass`, `gravity_multiplier`, `drag_multiplier`, `restitution`), `damage` (`self_multiplier`, `target_multiplier`), `behaviors`. `carry` and `collision_time_ticks` come later; the key names are reserved. |
| Sync | The server sends the **physics part** of resolved profiles to clients (S2C payload on join and after `/reload`), because the client's prompt (too heavy) and trajectory preview need mass, gravity and drag. Behaviors are server-only and never sent. |
| Resolution | Layers, lowest first: defaults (hitbox mass, no behaviors) → heuristics → `ThrowableMob` interface on the entity → Java registration (`registerProfile` / `registerBehavior`) → datapack profiles by ascending `priority` (on a tie, load order). Each layer **merges** on top of the one below: objects merge per field, lists (`entities`, `behaviors`) are replaced, not appended (`"behaviors": []` removes all). `"replace": true` in a profile drops every layer below it, same idea as vanilla tags. Resolved once per entity type and cached; the cache is cleared on reload. |
| Debug command | `/throwablemobs profile <entity>` (op level 2): prints the resolved profile and, per value, the layer or file it came from. Makes merging easy to debug. |
| Mass | `CarryRules.mass` = profile `mass` if set, else hitbox × `default_density` as now (resized mobs still change weight). Same on the client through the synced profile. |
| Flight with a profile | `ThrowPhysics.stepVelocity(v, g × gravity_multiplier, drag ^ drag_multiplier)`; the preview uses the same call. Restitution from the profile replaces the config `restitution` for that mob. |
| Bounces | **Physics for every mob**, no `bounce` atom. On a block hit the blocked axis is reflected and scaled by `block_restitution`; the flight goes on until `max_bounces` is used up or the rebound speed along the blocked axes is below `min_bounce_speed` (so a mob sliding along the floor stops bouncing instead of skipping forever). Math in `ThrowPhysics.bounce`. Creator defaults: `block_restitution 0.0` (no bounce, M2 feel), `max_bounces 3`, `min_bounce_speed 0.15`. Profiles override `physics.block_restitution` and `physics.max_bounces` (slime: `0.8` / `5`). Behaviors can still force a bounce or end the flight through `ImpactResult`. |
| Mob hits | Hitting an entity always **ends** the flight, as in M2 (damage, knockback, rebound). Bounces only happen off blocks. |
| Bounce impacts | Creator key `bounce_impacts` = `EVERY` (default) / `FIRST_ONLY`, profile override `physics.bounce_impacts`. `EVERY`: each bounce deals self-damage (from the blocked speed) and fires `impact` / `impact_block` behaviors. Condition `first_impact_only` on a behavior limits it to the first hit either way. A behavior that ends the flight (split, explode) stops further bounces. |
| Preview | Follows block bounces with the same step and reflection as the server, draws the whole path and puts the marker where the flight ends. An entity in the path still ends the preview there. |
| Self-damage immunity (M2 follow-up) | Creator key `self_damage` = `ALL` / `RESPECT_TAG` (default) / `NONE`. With `RESPECT_TAG`, mobs in `#throwablemobs:no_self_damage` take no impact self-damage. The tag contains `#minecraft:fall_damage_immune` by default (optional entry; in 26.3 it has chicken, cat, blaze, magma cube, bee, parrot, bat, phantom, breeze and others). The profile's `damage.self_multiplier` applies on top. Fixes chickens dying when they hit walls. |
| Throwing needs an empty hand (M2 follow-up) | With `require_empty_hand` on, switching to a non-empty main hand cancels the charge (client clears the bar, server drops the recorded charge start). Same rule as picking up. |
| Throw strength | Attribute `throwablemobs:throw_strength` (base `1.0`, player only), registered on each loader. Impulse = `throw_impulse × throw_strength × (1 + strength_effect_bonus × Strength level)`. Creator key `strength_effect_bonus` (default `0.25`). The client reads its own attribute for the preview (attributes are synced). |
| Heuristics | Creator key `auto_detect_behaviors` (default `true`). The lowest layer above the defaults, so any profile overrides it. `instanceof Creeper` → `explode`, `Slime` → `block_restitution 0.8` + `split`, `EnderMan` → `teleport_random`, `fireImmune()` → `ignite`. Anything else gets the defaults. The flying, ranged and escape atoms come in M5 with the rest of the vanilla roster. Runs on both sides from class checks only, so the client's physics matches. |
| Docs | Drafted in `docs/guide/` during M3, next to the code, so they don't drift; M6 turns them into the VitePress site with generated references. Pages: profile reference, atoms and conditions, merge rules and the debug command, heuristics, Java API guide, and a cookbook. Cookbook examples: a big mob mod (guessed behaviors plus profile overrides), boss / dungeon mods (`unthrowable`, `#c:bosses`, a chaos pack that throws bosses), a made-up Java example mod (own `ThrowBehavior` and behavior type), and a datapack-only pack (profiles, tags, `"replace": true`). Real mod ids and classes are looked up before writing, never guessed. |
| Global switches | Creator key `behaviors_enabled` (default `true`) and `disabled_behaviors` (list of type ids, e.g. `["throwablemobs:explode"]`). |
| Deferred | Events (`PickupEvent`, `ThrowEvent`, `ImpactEvent`) and the loader-native versions → M5, before claim mod support. `run_function`, `apply_effect`, `spawn_entity` and the rest of the atoms → M5. `-api` artifact and JSON Schema → M6. |

## Vanilla behaviors (shipped profiles)

| Mob | Profile | Atoms used |
|---|---|---|
| Creeper | Explodes on impact if `E ≥ 20`: power `1.5 + 0.01 × E`, max `4`; charged ×2. Fire off. Block damage follows `mobGriefing`. Dies in the explosion like a vanilla creeper (no reusable bomb). Fuse stays frozen while carried. | `explode` |
| Enderman | On impact: no self damage, teleports up to 16 blocks away (vanilla `teleport` logic), angry at the thrower. Skips entities in `#c:teleporting_not_supported`. | `teleport_random`, `anger_thrower` |
| Slime / Magma cube | `block_restitution 0.8`, `max_bounces 5`. Hard impact (`E ≥ 40`) on a slime of size > 1 splits it: the **original shrinks and stays one of the pieces** (its data survives), 1–3 new ones get its name and persistence. Ends the flight. Magma cube: sets the hit entity on fire for 4 s. | `split`, `ignite` |
| Chicken | `gravity_multiplier 0.4`; no self damage from the fall-immune tag. Lays an egg on impact, 25 % chance. | `drop_item` |
| Blaze | Hit entity on fire for 5 s; fire block at the impact point on a block hit (if `mobGriefing`). Flame particles in flight. | `ignite`, `particles` |
| Cat | No self damage from the fall-immune tag; no profile needed. | none |

Starting values, tuned in the manual pass.

## Risks — spike first

| Risk | Detail |
|---|---|
| Reload + sync on both loaders | Fabric: `ResourceLoader` server reload listener + `ServerLifecycleEvents.END_DATA_PACK_RELOAD` / join to send. NeoForge: `AddServerReloadListenersEvent` + `OnDatapackSyncEvent`. Check that the client gets profiles **before** it can see the first carry prompt. |
| Explosions and splits inside `ThrowManager.tick` | An explosion can hurt or kill other flying mobs, and a split discards the parent, while we iterate `FLYING`. End the flight (`land`) **before** running destructive behaviors and collect side effects to run after the loop. |
| Bounces with `Entity#move` | After a block hit the mob is touching the block. The bounce velocity has to point away from it, or the next tick collides again at once. Reflect only the blocked axis. The preview must use the exact same reflection, or the marker drifts after the first bounce. |
| Creeper during carry | `CarryManager` currently freezes the creeper fuse while carrying. That stays (hot potato is a later opt-in); `explode` only triggers on impact. |
| Profile merging | Field-level merge over codecs: decode each file to a partial profile (all fields optional), merge, then fill defaults. Keep the source of every field for the debug command. |

Spike = throwaway code, Fabric, one hardcoded profile JSON for chicken (`gravity_multiplier 0.4`) loaded by a reload listener and synced. Gate: `/reload` after editing the JSON changes the preview on a connected client, and the chicken lands on the marker.

## Steps

| # | Step | Contents |
|---|---|---|
| 0 | **API skeleton** | `api/` types with javadoc. `ThrowableMobsApi` with `registerBehaviorType`, `registerBehavior`, `registerProfile`. No behavior yet. |
| 1 | **Spike** | See above. |
| 2 | **Profiles** | Partial `MobProfile` codec, reload listener on both loaders, layered merge with `replace`, resolver + cache, `ProfileSyncPayload` (physics only), `/throwablemobs profile`. |
| 3 | **Physics from profiles** | `CarryRules.mass`, gravity / drag multipliers in `ThrowManager` and `TrajectoryPreview`, per-mob restitution. Block bounces (`block_restitution`, `max_bounces`, `min_bounce_speed`, `bounce_impacts`) in the flight and the preview. Throw strength attribute + Strength bonus. |
| 4 | **Hook call sites** | `onPickup` / `onCarryTick` in `CarryManager`, `onThrow` / `onFlightTick` / `onImpact` / `onLand` in `ThrowManager`. `ImpactResult` applied in `hitEntity` / `hitBlock`. `self_damage` mode. |
| 5 | **Atoms** | `explode`, `teleport_random`, `anger_thrower`, `split`, `ignite`, `drop_item`, `particles`. Conditions: `min_impact_energy`, `chance`, `in_water`, `is_charged`, `first_impact_only`. |
| 6 | **Vanilla profiles** | JSON for the six mobs above in `data/throwablemobs/throwablemobs/mob_profile/`. `no_self_damage` tag. |
| 7 | **Heuristics** | `HeuristicResolver`, `auto_detect_behaviors`, `behaviors_enabled`, `disabled_behaviors`. |
| 8 | **M2 follow-ups** | Charge cancelled on a non-empty hand with `require_empty_hand`. |
| 9 | **Config + docs** | New creator keys, lang keys, config doc comments. `docs/guide/` drafts (see Docs above). Update §9/§10 in THROWABLE_MOBS.md where the code ended up different. |
| 10 | **Tests** | Unit tests in `physics/` for the multipliers and the bounce reflection. Manual matrix below. Profile codec tests need a bootstrapped game, so they wait for GameTests in M6. |

## Status (2026-10-11)

| Step | State |
|---|---|
| 0 API skeleton | Done, compiles on both loaders: `api/` (`ThrowBehavior`, `ThrowContext`, `ImpactContext`, `ImpactResult`, `Trigger`, `BehaviorAction`, `BehaviorType`, `BounceImpacts`, `MobProfile` with `over` merge, `ThrowableMob`, `ThrowableMobsApi`) and `behavior/BehaviorRegistry` (types, dispatch codec, Java profile layer). Nothing calls it yet. |
| Bounce math | Done: `ThrowPhysics.bounce`, `scaledDrag`. 19 unit tests pass. |
| 1 spike | Done, tested on NeoForge (2026-10-11): a chicken profile in a world datapack changes the preview and the throw; editing it + `/reload` changes it live. Built as real code, not throwaway (step 2 physics part + step 3 mass / gravity / drag). |
| Bounces | Done, tested on NeoForge (2026-10-11, slime profile `0.8` / `5`, preview matches): config `block_restitution`, `max_bounces`, `min_bounce_speed`, `bounce_impacts`; profile overrides; `ThrowManager.bounceVelocity` shared by the server flight and the preview (yellow dot per bounce, marker at the final stop). |
| Found while testing | Fixed two M2 bugs in `ThrowManager.hitBlock`: impact damage used the leftover movement instead of the speed into the block, and the mob kept pushing into the block after landing (`ThrowPhysics.impactVelocity`, `bounce` with `e = 0`). Ground landings now draw a filled footprint plus a faint box. |

## Config (creator file, new keys)

| Section | Key | Default | Note |
|---|---|---|---|
| throw | `strength_effect_bonus` | `0.25` | Extra impulse per Strength level. |
| flight | `block_restitution` | `0.0` | Elasticity off blocks for every mob. `0` = no bounce. Profile override. |
| flight | `max_bounces` | `3` | Bounces before the flight ends. Profile override. |
| flight | `min_bounce_speed` | `0.15` | b/t. A slower bounce ends the flight. |
| flight | `bounce_impacts` | `EVERY` | `EVERY` / `FIRST_ONLY`: whether later bounces deal damage and fire impact behaviors. Profile override. |
| impact | `self_damage` | `RESPECT_TAG` | `ALL` / `RESPECT_TAG` / `NONE`. |
| behaviors | `behaviors_enabled` | `true` | Off → every mob behaves like M2. |
| behaviors | `auto_detect_behaviors` | `true` | Heuristics for modded mobs without a profile. |
| behaviors | `disabled_behaviors` | `[]` | Behavior type ids to skip everywhere. |

## Manual test matrix (dedicated server, 2 clients, both loaders)

- [ ] 1 Creeper thrown at a wall explodes; a soft lob (low charge) doesn't. Charged creeper explodes bigger. `mobGriefing false` → no block damage.
- [ ] 2 Creeper explosion mid-flight with other mobs in the air: no crash, the other mobs' flights end normally.
- [ ] 3 Enderman hits a wall → teleports, takes no damage, attacks the thrower.
- [ ] 4 Slime bounces up to `max_bounces` times and the preview shows the bounces; a hard throw splits a big slime (the original is one of the pieces); small slime doesn't split. Split slimes keep a name tag.
- [ ] 4b `block_restitution = 0.5` globally → a cow bounces too; `bounce_impacts = FIRST_ONLY` → later bounces deal no damage.
- [ ] 5 Magma cube sets the hit mob on fire.
- [ ] 6 Chicken flies further than before, lands on the preview marker, survives hitting a wall, sometimes drops an egg.
- [ ] 7 Blaze sets the hit mob on fire and leaves fire on a block hit (only with `mobGriefing`).
- [ ] 8 Cat takes no damage from a wall.
- [ ] 9 A datapack profile with `mass: 2000` makes a chicken too heavy (prompt says so); `/reload` after editing it changes it on a connected client.
- [ ] 10 A datapack profile with only `mass` on the creeper keeps the explosion (merge); `"behaviors": []` stops it; `"replace": true` drops our profile. `/throwablemobs profile minecraft:creeper` shows where each value came from.
- [ ] 11 `disabled_behaviors = ["throwablemobs:explode"]` stops creeper explosions; `behaviors_enabled = false` → everything like M2.
- [ ] 12 A modded creeper (or a spawned `Creeper` subclass from a test mod) without a profile explodes with `auto_detect_behaviors = true`, not with `false`.
- [ ] 13 Strength II potion → throws go visibly further and the preview matches.
- [ ] 14 `require_empty_hand = true` → switching to a sword while charging cancels the charge.
- [ ] 14b `self_damage = ALL` → chicken takes wall damage again; `NONE` → no mob does.
- [ ] 15 Preview lands on the marker for chicken (low gravity) and a profile with `drag_multiplier 2`.

## Review (2026-10-11)

Settled with the user: merge profiles with `"replace": true` and a debug command; bounces as physics for every mob with global and per-mob settings; `self_damage` mode + tag + multiplier; preview follows bounces; mob hits end the flight; `bounce_impacts` configurable (default `EVERY`); creeper fuse frozen while carried and the creeper dies in its explosion; split keeps the original slime; heuristics on by default; Strength bonus `0.25` per level; docs drafted in M3 with third-party examples; events in M5.
