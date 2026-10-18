# M4 — Catching

Plan for milestone M4 from [THROWABLE_MOBS.md §18](../../THROWABLE_MOBS.md#18-roadmap).
Branch: `feature/catching` (from `develop`, after M3 is merged).
Spec: §12 (multiplayer: catching & passing), §13 (compatibility checklist).

## Goal / definition of done

On a **dedicated server** with **two clients**, on both **Fabric and NeoForge**:

1. A player with an empty main hand who isn't carrying catches a mob flying at them: the flight ends and they carry it, same as after a pick-up.
2. The thrower can catch their own mob after `self_catch_delay_ticks` (throw it up, catch it again).
3. Catching a fast, heavy mob pushes the catcher back (recoil). Optional damage above an absorb threshold, off by default.
4. Catching works at ~150 ms ping without feeling worse than at 0 ms.
5. A mob the catcher couldn't pick up (too heavy, unthrowable, someone else's pet) isn't caught: it hits them as in M3.
6. Behaviors get `onCaught` and the `caught` trigger. An ignited creeper's fuse keeps running while it's passed around (hot potato).
7. `pvp` off: thrown mobs never damage players but can still be caught. Scoreboard teams with `allowFriendlyFire` off are respected.

## Decisions

| Topic | Decision |
|---|---|
| Input | **Automatic.** No key: empty hand + not carrying + in range + in view cone = catch. |
| Who can catch | Same rules as picking up: `CarryRules.canUseHands` and `CarryRules.check(player, mob)`. A denial means no catch, and the mob hits the player as before. Spectators and players in vehicles never catch. |
| Geometry | Server, every flight tick, **before** `findTarget`. The mob must be **coming towards** the catcher (playtest 2026-10-18: a thrower walking after a slow cow caught it again right after the self-catch delay). Then distance from the catcher's eye to the mob's swept segment this tick (start → end, not just the end point), within `catch_radius`. View cone: `dot(look, toMob) ≥ cos(catch_cone_degrees)`, towards where the mob is now, not the closest point of its path (that one sits beside the eyes at a steep angle; playtest 2026-10-18: the highlight never showed). Several catchers in range: the nearest along the path wins. Math in `physics/` (`ThrowPhysics.segmentDistance`), unit tested. |
| Self-catch | The thrower can catch once `ticks > self_catch_delay_ticks`. Separate from `thrower_grace_ticks` (which only stops the mob hitting its thrower). |
| Latency | **Radius expansion**: radius + `speed × ping_ticks × latency_compensation`, ping from the player's connection latency, capped by `max_latency_ticks`. Position rewind waits until playtests show it's needed. |
| Recoil | Knockback along the mob's velocity, strength `mass × speed × catch_recoil`. Damage only when `catch_damage` is on and the momentum is over `catch_absorb_momentum`: damage = excess × `catch_damage_factor`, damage type `throwablemobs:caught_mob`. |
| Hand-off | Catching calls the pick-up path (`CarryManager.hold`) with the mob's **original** flags from `ThrowState`, so `NoAI` / `NoGravity` still restore correctly after a chain of passes. The flight's `Flight` entry and `ThrowState` attachment go away without calling `onLand`. |
| Hooks | `ThrowBehavior.onCaught(ctx, thrower)`: `ctx.player()` is the catcher, `thrower` is nullable and can be the catcher. One call site in `ThrowManager.catchMob`. Trigger `caught` for data behaviors. Order: `onCaught` → then the carry starts (`onPickup` and `onLand` are **not** called). A behavior that removes the mob cancels the catch. |
| Fuses | A caught creeper keeps its swell. Check in the spike whether carried mobs (no AI) tick their fuse; if not, the carry tick advances it for mobs whose `Hooks` say so. |
| PvP | Creator key `impact.pvp` (default `true`). A mob that can't hurt a player **flies past** them (not in `canHit`), so no damage, no knockback, no impact behaviors; catching still works. Can't hurt = `pvp` off, or the thrower's `Player.canHarmPlayer` says no (teams without friendly fire, the server's own pvp setting). The thrower is always hittable after `thrower_grace_ticks`. Catch damage follows the same rule. |
| Highlight | Client-only white outline on a flying mob the local player would catch within the next 10 ticks at its current speed (same rules and `ThrowManager.catchFraction` as the server, no ping). Player config toggle `catch.catch_highlight`, default on. Set through `outlineColor` in the existing `LivingEntityRendererMixin`. |
| Prediction | **Not in M4.** Vanilla entity motion sync; client-side flight prediction goes to M7 if jitter shows. |
| Feedback | Catch sound + arm swing on the catcher. Fancy particles wait for M7. |

## Risks — spike first

1. **Fuse while carried.** ✅ Checked in the 26.3 source (2026-10-18): `Creeper.tick` applies the swell direction every tick, with or without AI, and `NoAI` freezes `SwellGoal`. But `CarryManager.follow` sets `swellDir -1` on every carry tick for creepers that aren't ignited (M1: a hissing creeper you pick up calms down). So the hot potato works with **ignited** creepers (flint and steel), which is what we want. With `ai_in_flight` on, `SwellGoal` also calms a non-ignited creeper in the air.
2. **Client vs server catch check.** The highlight uses client positions, the server decides. With radius expansion the server is more generous than the client, so the glow can under-promise but not over-promise. Good enough; note it in the docs.
3. **Hand-off in one tick.** ✅ `CarryManager.hold` → `follow` already sets the position and zeroes the motion. The catch removes the `ThrowState` (no `onLand`) and calls `hold` with a `CarryState` built from the `ThrowState`'s original flags. Still watch for a visible snap in the manual tests.

## Steps

| # | Step | Contents |
|---|---|---|
| 0 | **Spike** | Risks 1 and 3. Done from the source, see above. |
| 1 | **Geometry** | `ThrowPhysics.segmentDistance` + view-cone check in `physics/`, with unit tests (fast mob passing the player between two ticks, behind the player, at the edge of the cone). |
| 2 | **Server catch** | `ThrowManager.findCatcher` before `findTarget`, rules from `CarryRules`, self-catch delay, latency radius, hand-off into `CarryManager`. |
| 3 | **Recoil** | Knockback + optional damage, `caught_mob` damage type + lang + death message. |
| 4 | **Hooks** | `onCaught` in the API with javadoc, `caught` trigger, call site. Update `docs/guide/behaviors.md`. |
| 5 | **PvP / teams** | `pvp` key, team check in `hitEntity`. |
| 6 | **Highlight** | Client check + glow, player config `catch_highlight`. |
| 7 | **Config + docs** | New creator keys, lang keys, config comments. Update §12 in THROWABLE_MOBS.md where the code ended up different. |
| 8 | **Tests** | Unit tests for step 1. Manual matrix below. |

## Status (2026-10-18)

Steps 0–7 in code on `feature/catching`, `./gradlew build` green on both loaders, 26 physics tests pass. Not playtested yet: the manual matrix below is next, then tuning the defaults.

Also fixed on the way: knockback from a thrown mob never reached a hit **player's** client (M2). `push` only sets `needsSync`, but players move on their own client and need `syncVelocity`. Same fix for the catch recoil.

## Config (creator file, new keys)

Section `catch`:

| Key | Default | Meaning |
|---|---|---|
| `catching_enabled` | `true` | Master switch. |
| `catch_radius` | `1.5` | Blocks from the catcher's eye to the mob's path. |
| `catch_cone_degrees` | `60` | Half-angle of the view cone. |
| `self_catch_delay_ticks` | `10` | Ticks before the thrower can catch their own mob. |
| `latency_compensation` | `1.0` | Multiplier on `speed × ping_ticks` added to the radius. `0` turns it off. |
| `max_latency_ticks` | `6` | Ping above this is treated as this. |
| `catch_recoil` | `0.002` | Knockback per unit of momentum. |
| `catch_damage` | `false` | Hard catches hurt. |
| `catch_absorb_momentum` | `200` | Momentum a player takes without damage. |
| `catch_damage_factor` | `0.02` | Damage per unit of momentum over the threshold. |

Section `impact`: `pvp` (`true`).

Player file: `catch_highlight` (`true`).

Defaults are first guesses; tune them in the manual tests.

## Manual test matrix (dedicated server, 2 clients, both loaders)

| # | Case | Expect |
|---|---|---|
| 1 | Throw a pig at the other player, they look at it, empty hand | Caught, they carry it, glow showed before. |
| 2 | Same, they look away | Not caught, pig hits them. |
| 3 | Same, they hold a sword | Not caught, hit. |
| 4 | Same, they already carry a mob | Not caught, hit. |
| 5 | Throw a heavy mob over their `max_mass` | Not caught, hit. |
| 6 | Throw straight up, catch it yourself | Works after the delay, not before. |
| 7 | Full-speed cow catch | Pushed back a few blocks; with `catch_damage` on, some damage. |
| 8 | Ignited creeper (flint and steel) hot potato: pass three times | Fuse keeps running, explodes on whoever holds it last. |
| 9 | Fake 150 ms ping (clumsy) | Catching still works at full speed. |
| 10 | `pvp false` | No damage to players, catching works. |
| 11 | Both players on a team, `friendlyFire false` | No damage, catching works. |
| 12 | Caught mob, then put down | `NoAI` / `NoGravity` back to what they were before the first pick-up. |
| 13 | Catcher logs out / dies right after catching | Same as carrying (M1 rules). |
