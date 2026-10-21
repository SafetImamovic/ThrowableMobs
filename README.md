<p align="center">
  <img src="common/src/main/resources/throwablemobs.png" alt="Throwable Mobs icon: a pig flying through the air" width="160">
</p>

<h1 align="center">Throwable Mobs</h1>

<p align="center">
  Pick up mobs and throw them. Creepers explode on impact, endermen blink away, slimes bounce and split, chickens float.
  Catch your friends' throws mid-air.
</p>

<p align="center">
  <a href="https://github.com/SafetImamovic/ThrowableMobs/actions/workflows/build.yml"><img src="https://github.com/SafetImamovic/ThrowableMobs/actions/workflows/build.yml/badge.svg" alt="Build"></a>
  <a href="https://github.com/SafetImamovic/ThrowableMobs/releases"><img src="https://img.shields.io/github/v/release/SafetImamovic/ThrowableMobs?include_prereleases&sort=semver&label=release" alt="Latest release"></a>
  <a href="https://safetimamovic.github.io/ThrowableMobs/"><img src="https://img.shields.io/badge/docs-online-f28c28" alt="Docs"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue" alt="MIT license"></a>
</p>

<p align="center">
  <a href="https://github.com/SafetImamovic/ThrowableMobs/releases"><b>Download</b></a> ·
  <a href="https://safetimamovic.github.io/ThrowableMobs/"><b>Docs</b></a> ·
  <a href="CHANGELOG.md"><b>Changelog</b></a> ·
  <a href="https://github.com/SafetImamovic/ThrowableMobs/issues/new/choose"><b>Report a bug</b></a>
</p>

Minecraft Java **26.3** · **Fabric** + **NeoForge** · MIT · **Beta**

- **Fabric:** needs [Fabric API](https://modrinth.com/mod/fabric-api) and
  [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port).
- **NeoForge:** nothing extra.
- Install on both the client and the server.

Design, physics model, behavior API and roadmap: [THROWABLE_MOBS.md](THROWABLE_MOBS.md).

## What it does

- **Carry:** pick up a mob, carry it on your head, put it down where you look. Heavy mobs slow you down; bosses and pets
  that aren't yours stay put.
- **Throw:** hold Throw to charge (charge bar like a bow) with a trajectory preview that shows exactly where the mob will
  land, bounces included. Light mobs fly far, heavy ones barely leave your hands. Sprinting adds to the throw, Strength
  makes it harder.
- **Impacts:** damage from collision energy, split by mass between the mob and what it hits, with knockback. Kills
  credit the thrower.
- **Catch:** grab a thrown mob out of the air; an outline shows which ones you can catch. Your own throw only after it
  bounces.
- **Mob behaviors:** creepers explode, endermen teleport and come for you, slimes and magma cubes bounce and split,
  blazes and magma cubes set things on fire, chickens float and lay eggs. Modded mobs act like the vanilla mob they're
  built on.
- **Effects:** sounds, particles, a flight trail, and 8 advancements in their own tab.
- **Data-driven:** every behavior and number is set per mob in datapack mob profiles, or globally in the config. Other
  mods can add their own behaviors through a Java API.

## Controls

All rebindable under Controls → Throwable Mobs, or with `/throwablemobs keybinds`.

| Action | Default |
|---|---|
| Pick up / put down | Left Shift + Right Button |
| Throw (hold to charge, let go to throw) | Right Button while carrying |
| Cancel throw | Left Button while charging |

The defaults match Carry On's. If another mod has a key on Left Shift (or on whatever starts a pick-up), picking up is
left to that mod and a chat message says so; rebind either one to use both.

## Commands

| Command | Who | What |
|---|---|---|
| `/throwablemobs help` | Everyone | Lists these commands |
| `/throwablemobs config` | Everyone | Opens the settings screen |
| `/throwablemobs keybinds` | Everyone | Opens Controls to rebind the keys |
| `/throwablemobs profile <mob>` | Operators | Shows a mob's profile and where each value comes from |

## Configuration

- `config/throwablemobs-server.toml`: gameplay, synced from the server (throw strength, bounciness, damage, behaviors,
  what happens to carried mobs when players leave). Every setting:
  [Server config](https://safetimamovic.github.io/ThrowableMobs/guide/server-config).
- `config/throwablemobs-client.toml`: personal (prompt, charge bar, trajectory style, flight pose), editable in game.
- Datapacks: tags and mob profiles per mob, reload with `/reload`. Check a mob with `/throwablemobs profile <mob>`.

Guide for pack devs and mod devs: [safetimamovic.github.io/ThrowableMobs](https://safetimamovic.github.io/ThrowableMobs/)
(source in [docs/guide](docs/guide/README.md)).

## Issues and ideas

- Bugs and crashes: [open a bug report](https://github.com/SafetImamovic/ThrowableMobs/issues/new/choose) with your
  `latest.log` uploaded to [mclo.gs](https://mclo.gs).
- Ideas for features or mob behaviors are welcome too; the issue form asks what the mob should do.
- Changes per version: [CHANGELOG.md](CHANGELOG.md).

## Building

Requires JDK 25.

```sh
./gradlew build                     # jars in fabric/build/libs and neoforge/build/libs
./gradlew :physics:test             # unit tests
./gradlew :fabric:runClient         # dev client (Fabric)
./gradlew :neoforge:runClient       # dev client (NeoForge); :neoforge:runClient2 for a second one
./gradlew :fabric:runServer         # dedicated server
./gradlew :fabric:runClient --debug-jvm   # wait for a debugger on port 5005
```

## Layout

| Module | Contents |
|---|---|
| `physics/` | Pure-Java physics math (mass, launch, flight, bounces, impact). No Minecraft imports; unit tested. |
| `common/` | Almost all code. Compiles against vanilla only. Public API in `api/`. |
| `fabric/` | Fabric entrypoint and Fabric-specific glue. |
| `neoforge/` | NeoForge entrypoint and NeoForge-specific glue. |
| `build-logic/` | Shared Gradle conventions. |

Loader-specific calls from `common` go through `platform/Services` (Java `ServiceLoader`).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md): short branches off `master`, atomic Conventional Commits, PRs against `master`.

Based on [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template).
