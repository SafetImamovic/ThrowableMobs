# Changelog

Changes per version that players, pack devs and mod devs will notice. Format:
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow [Semantic Versioning](https://semver.org).

## [Unreleased]

## [0.5.0] - 2026-10-25

### Added

- With [Ragdollified](https://modrinth.com/mod/ragdollified) installed (optional, NeoForge), thrown mobs go limp in the
  air and get back up where they land. The ragdoll follows the real flight, so hits and landings don't change, and a
  mob killed by the throw keeps its ragdoll. Player setting `ragdoll_in_flight` (on by default) turns it off. Tested on
  1.21.1; Ragdollified's 26.3 build doesn't load on NeoForge 26.3.0.95 yet (its own bug).
- Cooldowns between picking up and putting down, so a double press no longer picks a mob up and drops it straight
  back. Server settings `pick_up_cooldown_ticks` and `put_down_cooldown_ticks` (off by default); each player has their
  own under `[cooldown]` (6 ticks by default), and the longer one counts.

### Fixed

- Carrying a mob made you faster: on jumps and steps, the mob on your head pushed you forward. Carried mobs no longer
  push anyone.
- Shulkers jittered and teleported away while carried, and didn't fly when thrown. They now sit still on your head,
  fly like other mobs, and stick to a block where they land.

## [0.4.1] - 2026-10-24

### Changed

- Releases are also published on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/throwablemobs).

## [0.4.0] - 2026-10-24

### Added

- With [YACL](https://modrinth.com/mod/yacl) installed (optional), the settings screen is a YACL one, with every
  player and server setting. Server settings can only be changed in your own world. Without YACL, the screen stays as
  before.
- An `enabled` setting on both sides. Server: off, nobody can pick up, throw or catch, carried mobs are put down and
  thrown ones fall. Player: off, your keys are plain Sneak and Use again, no prompt, and you don't catch thrown mobs.
  While the server has it off, your own setting is greyed out with a note saying so.
- The prompt and charge bar settings in the YACL screen show a live preview: a small screen with the prompt and a
  filling charge bar where they'd be, updated while you change the values.

## [0.3.0] - 2026-10-24

### Added

- Minecraft 1.21.1, on Fabric (Fabric API 0.109.0 or newer) and NeoForge (21.1.197 or newer), with the same features
  as 26.3. Its server config is per world, in the world's `serverconfig/` folder.

### Changed

- The catch outline uses vanilla's glowing outline, so a mob on a team glows in its team color.

## [0.2.0] - 2026-10-23

### Added

- `on_carrier_logout` has two new options: `RESUME` (the mob is put down, but carried again if its carrier rejoins in
  time) and `TAKE_ALONG` (the mob leaves the world with its carrier and comes back with them). The new
  `carrier_logout_seconds` sets how long `RESUME` and `KEEP_CARRYING` wait.
- Docs: a [Server config](https://safetimamovic.github.io/ThrowableMobs/guide/server-config) page with every setting.

### Changed

- `on_carrier_logout` now defaults to `PUT_DOWN`: when a carrier leaves, the mob is put down at their feet. Existing
  config files keep their value.
- With `KEEP_CARRYING`, the mob waits on the ground at the carrier's feet instead of frozen in mid-air, and is put down
  after `carrier_logout_seconds`.
- When a carrier dies, the mob is put down at their feet instead of falling from above their head.
- Built against NeoForge 26.3.0.95 (the first stable 26.3 build) and Fabric API 0.164.0.

## [0.1.0] - 2026-10-21

First beta, for Minecraft 26.3 on Fabric and NeoForge.

### Added

- Pick up mobs and carry them on your head; put them down where you look. Heavy mobs slow you down; bosses and other
  players' pets can't be picked up.
- Charged throws with a charge bar and a trajectory preview that shows where the mob lands, bounces included. Mass,
  sprinting and Strength change how far it goes.
- Impact damage and knockback from collision energy, split by mass. Kills credit the thrower.
- Mob behaviors: creepers explode, endermen teleport and turn on the thrower, slimes and magma cubes bounce and split,
  fire-immune mobs set things on fire, chickens float and lay eggs. Modded mobs get the behaviors of the vanilla mob
  they're built on.
- Catch thrown mobs mid-air, with an outline on mobs you can catch.
- Sounds, particles, a flight trail and 8 advancements in their own tab.
- Server and client config, with a settings screen.
- Datapack mob profiles and tags for every number and behavior; `/throwablemobs profile <mob>` shows where each value
  comes from. Java API for other mods' behaviors.
- Commands: `/throwablemobs help`, `config`, `keybinds` (everyone) and `profile` (operators).
- When another mod (like Carry On) has a key on the one that starts a pick-up, picking up is left to that mod and a chat
  message explains how to rebind.
