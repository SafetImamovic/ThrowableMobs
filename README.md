# Throwable Mobs

Pick up mobs and throw them. Creepers explode on impact, endermen blink away, slimes split, and your friends can catch them mid-air.

Minecraft Java **26.3** · **Fabric** + **NeoForge** · MIT

> Early development. Design, physics model, behavior API and roadmap: [THROWABLE_MOBS.md](THROWABLE_MOBS.md).

## Building

Requires JDK 25.

```sh
./gradlew build                     # jars in fabric/build/libs and neoforge/build/libs
./gradlew :physics:test             # unit tests
./gradlew :fabric:runClient         # dev client (Fabric)
./gradlew :neoforge:runClient       # dev client (NeoForge)
./gradlew :fabric:runServer         # dedicated server
./gradlew :fabric:runClient --debug-jvm   # wait for a debugger on port 5005
```

## Layout

| Module | Contents |
|---|---|
| `physics/` | Pure-Java physics math (mass, launch, impact). No Minecraft imports; unit tested. |
| `common/` | Almost all code. Compiles against vanilla only. |
| `fabric/` | Fabric entrypoint and Fabric-specific glue. |
| `neoforge/` | NeoForge entrypoint and NeoForge-specific glue. |
| `build-logic/` | Shared Gradle conventions. |

Loader-specific calls from `common` go through `platform/Services` (Java `ServiceLoader`).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) — git-flow branching, atomic Conventional Commits, PRs against `develop`.

Based on [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template).
