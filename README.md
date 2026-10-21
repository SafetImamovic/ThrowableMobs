# Throwable Mobs

Pick up mobs and throw them. Creepers explode on impact, endermen blink away, slimes bounce and split, chickens float.
Catching your friends' throws mid-air is coming next.

Minecraft Java **26.3** · **Fabric** + **NeoForge** · MIT

Requires [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) on Fabric (NeoForge needs nothing extra).

> Early development. Design, physics model, behavior API and roadmap: [THROWABLE_MOBS.md](THROWABLE_MOBS.md).

## What it does

- **Carry:** pick up a mob, carry it on your head, put it down where you look. Heavy mobs slow you down; bosses and pets
  that aren't yours stay put.
- **Throw:** hold Throw to charge (charge bar like a bow) with a trajectory preview that shows exactly where the mob will
  land, bounces included. Light mobs fly far, heavy ones barely leave your hands. Sprinting adds to the throw, Strength
  makes it harder.
- **Impacts:** damage from collision energy, split by mass between the mob and what it hits, with knockback. Kills
  credit the thrower.
- **Mob behaviors:** creepers explode, endermen teleport and come for you, slimes and magma cubes bounce and split,
  blazes and magma cubes set things on fire, chickens float and lay eggs. Modded mobs get sensible guesses.
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

- `config/throwablemobs-server.toml`: gameplay, synced from the server (throw strength, bounciness, damage, behaviors).
- `config/throwablemobs-client.toml`: personal (prompt, charge bar, trajectory style, flight pose), editable in game.
- Datapacks: tags and mob profiles per mob, reload with `/reload`. Check a mob with `/throwablemobs profile <mob>`.

Guide for pack devs and mod devs (draft): [docs/guide](docs/guide/README.md).

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

See [CONTRIBUTING.md](CONTRIBUTING.md) — git-flow branching, atomic Conventional Commits, PRs against `develop`.

Based on [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template).
