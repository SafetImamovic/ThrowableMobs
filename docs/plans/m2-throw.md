# M2 — Throw

Plan for milestone M2 from [THROWABLE_MOBS.md §18](../../THROWABLE_MOBS.md#18-roadmap).
Branch: `feature/throw` (from `develop`, after `feature/config` is merged).
The config file part of M2 is already done (`docs/plans/config-files.md`).

## Goal / definition of done

On a **dedicated server**, on both **Fabric and NeoForge**, a player who carries a mob can:

1. Hold the **Throw** key (default right-click, no modifier) to charge. A charge bar fills like a bow, and a dotted **trajectory preview** shows where the mob will fly and land at the current charge.
2. Release → the mob flies along the look direction. Light mobs fly far and fast, heavy mobs barely leave your hands (impulse model, §8.2). Sprinting adds to the throw.
3. The mob flies with gravity and drag (§8.3), hits blocks and entities, and deals damage from collision energy (§8.4) to the target and to itself. Knockback transfers momentum.
4. Kills credit the thrower (loot, XP, death message "X was hit by a Cow thrown by Y").
5. When the flight ends (landed, in water, timed out), the mob gets its AI back with its original state, exactly like a put-down.
6. Never lose or permanently freeze a mob: chunk unload, server stop, thrower logout mid-flight.

## Decisions

| Topic | Decision |
|---|---|
| Input | New key mapping **Throw** (default right-click) in the Throwable Mobs category. Only acts while carrying and **without** the Modifier held (Modifier + right-click stays Put down). Vanilla use is swallowed while carrying, same as M1. |
| Charge | Server measures it: `StartChargePayload` on press, `ThrowPayload` on release (C2S, no charge value sent). Charge time clamped to `max_charge_ticks`; release before `min_charge_ticks` cancels (no throw). `c = ease(t / max)` with the bow curve `(x² + 2x) / 3`. |
| Launch | `ThrowPhysics.launchSpeed` (exists) × look direction + carrier velocity × `inherit_velocity`. Starts at the carried position (on the head). |
| Flight state | `ThrowState` attachment on the mob: thrower UUID, launch tick, launch position, original flags (moved over from `CarryState`). **Persistent** (so a mob loaded mid-flight is restored), not synced yet (clients just see server movement). |
| Flight sim | Own integration (§8.3, linear drag only in M2) so the trajectory preview can use the same code from `physics/`. Each tick: `mob.move(MoverType.SELF, v)`, check collisions, then `v = ThrowPhysics.stepVelocity(v)`. The preview repeats exactly this order. |
| Damage model | `energy` only. `momentum` / `force` modes and `damage_model` key come later; key names for them reserved, not added. |
| Damage split | Target takes `dmg × m_self / (m_self + m_target)`, thrown mob the rest (chicken loses vs cow). Block hit → thrown mob takes full `dmg`. Respect `#minecraft:fall_damage_immune`-style immunity via the mob's own fall-damage logic. |
| Damage type | `throwablemobs:thrown_mob` in data (thrower = attacker, mob = direct entity), with death messages in `en_us.json`. PvP gamerule applies for free because the attacker is a player. |
| AI in flight | Creator key `ai_in_flight` (default `false`): AI stays off until the flight ends. `true` gives the AI back at launch (chickens flap, skeletons turn); vanilla `travel` then moves the mob and we only overwrite its velocity each tick, so AI input can bend the path slightly off the preview. |
| Thrower | Excluded from entity hits for `thrower_grace_ticks`. |
| Fall damage | Fall distance reset at launch and at flight end; vanilla fall damage blocked during flight (impact damage replaces it). |
| Trajectory preview | Client-only, while charging. Runs the same `ThrowPhysics` step as the server tick by tick (not the drag-free oblique-throw formula, which lands short with drag), from the carried position with the client's own charge, look direction and velocity. Mass from hitbox × synced `default_density`. Each step clips against blocks; stops at the first block or entity hit and draws a landing marker there. Player toggle `show_trajectory`; creator toggle `allow_trajectory` (synced) lets servers turn it off. |
| Deferred | Block surface tags (`soft_landing`, `breakable_on_impact`) → M5. Behaviors (`onFlightTick`, `onImpact`) → M3; leave a single call site where they'll hook in. Catching → M4. Throw strength attribute + Strength effect bonus → M3. |

## Risks — spike first

| Risk | Detail |
|---|---|
| Moving the mob | Two options. **(a)** our velocity + `entity.move(MoverType.SELF, v)` each tick: free AABB block collision, but vanilla `travel` / air friction (`0.91`, `0.98`) also run on a mob even with `NoAi` and must be stopped or cancelled. **(b)** `noPhysics = true`, own position update + own AABB sweep against blocks: full control, more code. Spike (a) first; fall back to (b). |
| Client smoothness | Fast mobs (chicken at 2 b/t) may stutter with normal entity tracking. Check the update interval / velocity sync; may need to send velocity so clients extrapolate. |
| Entity hits | Sweep along `v` with the mob's AABB inflated, `ProjectileUtil`-style. Fast mobs must not tunnel through thin targets. |
| Landing | The flight ends at the first impact (block or entity), in water, or at `max_flight_ticks`; then vanilla takes over with the leftover motion. No bounces in M2, so no `land_speed`. |

Spike = throwaway code, Fabric only, fixed throw speed, `runServer` + 2 clients. Gate: chicken thrown at full speed flies smooth on both clients, lands, no wall clipping.

## Spike findings (2026-10-04)

Gate passed: smooth on both clients, stops at walls, AI back on landing.

| Finding | Consequence |
|---|---|
| Vanilla skips `travel` for NoAI mobs (`Mob#isEffectiveAi` returns false), so a NoAI mob with a velocity just hangs in the air. Option (a) as written doesn't work. | We call `mob.move(MoverType.SELF, v)` ourselves each tick. Still vanilla block collision, no own sweep needed (option (b) not needed). |
| `onGround` / `horizontalCollision` are stale until the mob has moved once (left over from carrying). | Check collisions only right after our own `move`, never before the first one. |
| Default entity tracking is smooth enough for a chicken at 2 b/t. | No velocity sync / extrapolation work. |
| Distances with impulse 150, max speed 2.0, drag 0.99: cow ~5 blocks, skeleton ~7, chicken ~43. | Chicken too far; tune `throw_impulse` / `max_throw_speed` in the manual pass. |

## Steps

| # | Step | Contents |
|---|---|---|
| 0 | **Physics** | `ThrowPhysics`: `chargeFraction(ticks, max)`, `stepLinear(pos, vel, g, drag)` (shared by the server flight and the preview), `impactDamage(energy, k, eMin, max)`, `damageShares(m1, m2)`, `knockback(m1, m2, e, vRel)`. Unit tests. |
| 1 | **Spike** | See above. |
| 2 | **Config** | Creator file, new `[throw]` and `[impact]` sections (table below). Player file: `show_charge_bar`, `show_trajectory`. Lang keys + docs. |
| 3 | **`ThrowState` + damage type** | Attachment on both loaders (persistent). Damage type JSON, damage type tag hooks, death messages. |
| 4 | **Input** | Throw key mapping, charge tracking on the client for the bar, `StartChargePayload` / `ThrowPayload`. Server tracks charge start per player; cancelled by put down, carried mob gone, or switching to a non-empty hand if `require_empty_hand`. |
| 5 | **`ThrowManager`** (server) | `throwMob`: end carry (without the put-down placement), keep AI off, set `ThrowState`, launch velocity. `tick`: integrate, move, block / entity collision, impact → damage + knockback, end flight. `endFlight`: restore flags, remove state, zero fall distance. Invariants each tick like `CarryManager.tick`. |
| 6 | **Impact** | `ImpactResolver`: reduced mass, relative velocity, energy → damage split → `hurtServer` with `thrown_mob` source, knockback scaled by knockback resistance. |
| 7 | **Charge bar** (client) | Small bar under the prompt (same anchor/offsets). Prompt while carrying shows "[Throw] to throw" next to the put-down line. |
| 7b | **Trajectory preview** (client) | While charging: simulate up to `max_flight_ticks` steps, draw dots along the path (world render event per loader) and a marker at the landing block or hit entity. Hidden when `show_trajectory` or `allow_trajectory` is off. |
| 8 | **Loader events** | Reuse M1 wiring; add fall-damage cancel during flight and entity-load orphan fix for `ThrowState`. |
| 9 | **Tests** | Unit tests for step 0. Manual matrix below. |

## Status (2026-10-04)

| Step | State |
|---|---|
| 0 physics | Done. 14 unit tests pass. |
| 1 spike | Done, findings above. |
| 2–6, 8 | Done on both loaders: config, `ThrowState`, `thrown_mob` damage type (in `#minecraft:no_knockback`, our own push replaces vanilla knockback), Throw key + `ChargeThrowPayload` / `ThrowPayload`, `ThrowManager` with impacts inside it (no separate `ImpactResolver`). Tested in game on Fabric. |
| 7 charge bar | Done. |
| 7b trajectory preview | Next. |

Implementation notes:
- Block impacts use only the blocked part of the velocity (`v − actual movement`), so skimming the ground hurts less than hitting a wall head-on.
- The thrown mob's own damage uses vanilla `fly_into_wall`; the hit entity gets `thrown_mob` with the thrower as attacker. Both set the thrower as last player attacker, so kills give XP and player-only loot.
- With the modifier unbound, Put down and Throw share the same input and Put down wins; Throw then needs its own key.

## Config (creator file)

Starting values, tuned in the manual pass. Units: blocks, ticks, blocks/tick, "game-kg".

| Section | Key | Default | Note |
|---|---|---|---|
| throw | `throw_impulse` | `150.0` | Chicken (45) hits the cap, creeper (245) ≈ 0.6 b/t, cow (454) ≈ 0.33 b/t. |
| throw | `max_throw_speed` | `2.0` | b/t. Bow arrow at full charge ≈ 3. |
| throw | `max_charge_ticks` | `20` | Same as a bow. |
| throw | `min_charge_ticks` | `3` | Shorter release cancels. |
| throw | `inherit_velocity` | `1.0` | Share of the carrier's velocity added. |
| throw | `thrower_grace_ticks` | `5` | |
| flight | `ai_in_flight` | `false` | AI back at launch instead of on landing. |
| flight | `gravity` | `0.05` | Arrow value. |
| flight | `drag` | `0.99` | Per tick, linear. |
| flight | `max_flight_ticks` | `200` | Then flight ends wherever the mob is. |
| impact | `damage_per_energy` | `0.1` | `k_E`. |
| impact | `min_impact_energy` | `10.0` | `E_min`, below this no damage. |
| impact | `max_impact_damage` | `20.0` | |
| impact | `restitution` | `0.3` | Bounce + knockback. |
| throw | `allow_trajectory` | `true` | Synced. Off → no preview for anyone. |

## Manual test matrix (dedicated server, 2 clients, both loaders)

- [ ] 1 Charge bar fills while holding Throw, empties on release; short tap does nothing.
- [ ] 2 Chicken flies far, creeper medium, cow barely leaves the hands.
- [ ] 3 Sprint-throw goes further than a standing throw.
- [ ] 4 Flight looks smooth on the thrower's and the other client.
- [ ] 5 Mob hits a wall → takes damage, stops, gets its AI back.
- [ ] 6 Mob hits another mob → both take damage by mass, target knocked back.
- [ ] 7 Kill by throw → loot drops, XP, death message names the thrower.
- [ ] 8 Thrown mob doesn't hit the thrower right after the throw.
- [ ] 9 Throw into water → flight ends, mob swims.
- [ ] 10 Throw off a cliff → no extra vanilla fall damage on top of impact damage.
- [ ] 11 Thrower logs out mid-flight → flight still ends normally.
- [ ] 12 Server stop mid-flight → mob restored with AI on next start.
- [ ] 13 PvP off → throwing a mob at a player does no damage to them.
- [ ] 14 Config values change the throw live (impulse, gravity, max speed).
- [ ] 15 Trajectory preview grows with charge and the mob lands on the marker (open field, against a wall, off a cliff).
- [ ] 16 `show_trajectory = false` hides it for that player; server `allow_trajectory = false` hides it for everyone.
