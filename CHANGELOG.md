# Changelog

Changes per version that players, pack devs and mod devs will notice. Format:
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow [Semantic Versioning](https://semver.org).

## [Unreleased]

## [0.9.0] - 2026-10-30

### Added

- Minecraft 26.1.2, on Fabric and NeoForge (26.1.2.125 or newer). Same features as 26.3; on NeoForge the server config
  is per world, in the world's `serverconfig/` folder, as on 1.21.1.
- Minecraft 26.2, on Fabric and NeoForge (26.2.0.93 or newer). Same features as 26.3; on NeoForge the server config is
  per world, in the world's `serverconfig/` folder, as on 1.21.1.

## [0.8.0] - 2026-10-29

### Added

- Server-side hunger costs for picking up, walking/sneaking or sprinting while carrying, and throwing mobs. Four
  settings in `[hunger]` control vanilla food exhaustion; set any to 0 to disable that cost. Creative and spectator
  players are exempt. Standing still, catching, putting down and cancelled throws have no extra cost.

### Changed

- Shared key bindings no longer disable picking up mobs or hide the pickup prompt. Removed automatic yielding to
  other mods and the generic key conflict chat notice. When Carry On is installed, a notice explains the shared
  defaults once per world join without disabling pickup.

### Fixed

- Mod list artwork: package the banner, use the supported logo field on older NeoForge, and provide Catalogue with
  the square icon separately from the banner.

## [0.7.0] - 2026-10-27

### Added

- Minecraft 1.20.1, on Fabric (Fabric API and Forge Config API Port 8) and Forge (47.1 or newer). The Forge jar also
  runs on NeoForge 1.20.1. Same features as on the newer versions, except that 1.20.1 has no stock settings screen:
  install [YACL](https://modrinth.com/mod/yacl) to change settings in game, or edit the config files.
- Server setting `creative_carries_anything` (off by default): players in creative mode can pick up any mob, however
  heavy, ignoring `#throwablemobs:unthrowable`, `allow_villagers` and `max_mass`. Other players' pets and the ender
  dragon stay off limits.

### Fixed

- 1.21.1: the advancement tab's hay background was missing.

## [0.6.0] - 2026-10-26

### Added

- Chicken radar: a carried chicken screams when a monster comes near, faster and higher the closer it gets, so you can
  carry one as an early warning. The new `throwablemobs:radar` behavior gives the same to any mob through a mob
  profile, with its own range, rhythm and sound. Servers turn it off with
  `disabled_behaviors = ["throwablemobs:radar"]`.
- Server setting `egg_chance` (0.25 by default): how often a thrown chicken lays an egg, from never to every throw.
  Mob profiles can use it with the new `chance_setting` condition.
- Carry info panel: while carrying, a small panel shows the mob's name, health, weight and traits (Floaty, Bouncy,
  Heavy), and for mobs with a radar, the nearest monster it senses and how far away it is, flashing with each scream.
  Each line can be turned off, and the panel moved to any corner or under the crosshair, in `[carry_info]` of the
  player settings, with a live preview in the config screen.

### Changed

- Spiders, cave spiders, phantoms and horses (with donkeys, mules, skeleton and zombie horses) have hand-set weights.
  Their hitboxes are mostly air, so they used to weigh far too much: a spider weighed more than a cow. They now fly
  further and hit softer. Spider 150 (was 706), cave spider 30 (98), phantom 60 (162), horses 600 (about 1250).

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
