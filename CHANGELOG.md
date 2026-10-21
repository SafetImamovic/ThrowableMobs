# Changelog

Changes per version that players, pack devs and mod devs will notice. Format:
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow [Semantic Versioning](https://semver.org).

## [Unreleased]

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
