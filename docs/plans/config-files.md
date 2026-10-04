# Config files

Replaces the hardcoded `ThrowableMobsConfig.CARRY` record from M1 with real config files.
Branch: `feature/config` (from `develop`). Design: [THROWABLE_MOBS.md §11](../../THROWABLE_MOBS.md#11-configuration).

## Decisions

| Topic | Decision |
|---|---|
| Library | **Forge Config API Port** (Fuzss). NeoForge's config API on Fabric too; NeoForge has it built in. Fabric players must install the port. |
| Files | **Creator file** `config/throwablemobs-server.toml` (pack devs, admins): synced from the server to clients, per-world override in `syncedconfig/` (NeoForge) or `serverconfig/` (Fabric). **Player file** `config/throwablemobs-client.toml`: personal, not synced. |
| In-game editing | NeoForge's `ConfigurationScreen` on both loaders (the port ships it): mod list → Config on NeoForge, Mod Menu on Fabric. Settings are read every time, so changes apply right away. |
| Synced type | NeoForge 26.x calls it `SYNCED`, the port still `SERVER`. File names set explicitly so both loaders match. |
| Not in config | Which mobs can be carried: tags `#throwablemobs:unthrowable` / `owner_only`. Per-mob weight: mob profiles (M3). Key mappings: Controls. |
| Naming | Public names follow the one-name-per-concept rule: `put_down_ground_*` tags (were `drop_ground_*`), enum value `PUT_DOWN` (was `DROP`). |

## Settings

| File | Section | Keys |
|---|---|---|
| creator | `pick_up` | `require_empty_hand`, `max_mass`, `default_density`, `allow_villagers`, `owner_only_for_tamed` |
| creator | `carry` | `slowdown_factor`, `max_slowdown`, `protect_from_suffocation`, `protect_from_fall`, `protect_from_carrier`, `carried_facing`, `on_dimension_change`, `on_carrier_logout` |
| creator | `put_down` | `put_down_needs_ground`, `ground_filter` |
| player | `prompt` | `show_prompt`, `prompt_anchor`, `prompt_offset_x`, `prompt_offset_y` |

## Status (2026-10-03)

| Step | State |
|---|---|
| Dependency (port + Mod Menu for dev runs) | Done |
| Rename put-down tags | Done |
| Creator + player specs, registration on both loaders | Done. Fabric dev server writes both files with comments, defaults, ranges. |
| In-game screen + labels | Done, tested in game (NeoForge + Fabric) |
| Docs | Done |
| Manual tests | Done (2026-10-04). 11 and 12 skipped. |

## Manual tests

Run one server at a time (check `netstat -ano | findstr :25565` first), and start the client only after the server says "Done".

**Files**
- [x] 1  NeoForge and Fabric: both files appear in `runs/<side>/config/` with comments; values match the defaults.
- [x] 2  Edit a value by hand (e.g. `prompt_offset_y = -40`) while the game runs → applies without restart.
- [x] 3  Put an invalid value in a file (e.g. `max_slowdown = 5`) → corrected to the default / clamped, warning in the log, no crash.

**In-game screen**
- [x] 4  NeoForge: mod list → Throwable Mobs → Config opens the screen; labels readable, comments shown as tooltips.
- [x] 5  Fabric: Mod Menu → Throwable Mobs → config button opens the same screen.
- [x] 6  Player file: change prompt anchor / offsets in the screen → prompt moves right away, value saved to `throwablemobs-client.toml`.
- [x] 7  Player file: turn `show_prompt` off → prompt gone; on → back.
- [x] 8  Singleplayer: creator settings editable from the screen (e.g. `max_mass = 100` → cow shows "Too heavy").
- [x] 9  Connected to a dedicated server: creator settings greyed out / not editable.

**Sync (dedicated server)**
- [x] 10 Server `max_mass = 100`, restart server, join → looking at a cow shows "Too heavy" on the client; picking up is refused.
- [ ] 11 Server `put_down_needs_ground = false` → client prompt no longer says "Look at the ground"; putting down in the air works.
- [ ] 12 Leave the server, open the screen from the title screen → creator settings show defaults again, no crash.
- [x] 13 Per-world override: copy the server file into the world's `syncedconfig/` (NeoForge) or `serverconfig/` (Fabric) with a different `max_mass` → that world uses it.

**Behavior still works with config values**
- [x] 14 `on_carrier_logout = PUT_DOWN` → logout puts the mob down; `KEEP_CARRYING` → it waits.
- [x] 15 `on_dimension_change = PUT_DOWN` → portal puts the mob down; `TRAVEL` → it comes along.
- [x] 16 `slowdown_factor = 2.0` → carrying a cow is clearly slower, also when sprint-jumping.
- [x] 17 `ground_filter = ALLOW_LIST` with `#throwablemobs:put_down_ground_allowed` empty → can't put down anywhere; add `minecraft:grass_block` via datapack → only on grass.

Still open from M1 ([m1-carry.md](m1-carry.md)): rows 2 (prompt follows rebinding), 4 (pre-existing `NoAI` kept), 12 (mod on one side only), 17 (sprint-jump feel), 18 (prompt position — now covered by 6 here).
