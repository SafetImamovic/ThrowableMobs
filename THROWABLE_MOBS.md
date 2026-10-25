# Throwable Mobs — Central Design & Setup Doc

> Pick up a mob. Throw it. The creeper explodes on impact, the enderman blinks away, the slime splits, your friend catches the chicken mid-air.
>
> This file covers: the 2026 modding ecosystem, multi-loader/multi-version strategy, Neovim setup, mod architecture, the physics model, the behavior API for modpack devs, config design, multiplayer catching, compat, publishing, a Paper plugin port, public documentation, and a roadmap.
>
> Written 2026-10-02. Modding moves fast — anything marked **(verify)** should be checked before you rely on it.

---

## Table of contents

1. [State of Minecraft modding (late 2026)](#1-state-of-minecraft-modding-late-2026)
2. [Loaders: who to target](#2-loaders-who-to-target)
3. [Multi-loader & multi-version strategy](#3-multi-loader--multi-version-strategy)
4. [Distribution: Modrinth vs CurseForge](#4-distribution-modrinth-vs-curseforge)
5. [Dev environment: JDK, Gradle, Neovim](#5-dev-environment-jdk-gradle-neovim)
6. [Mod architecture](#6-mod-architecture)
7. [Core mechanic: pick up, carry, throw](#7-core-mechanic-pick-up-carry-throw)
8. [Physics model](#8-physics-model)
9. [Mob behaviors (the fun part)](#9-mob-behaviors-the-fun-part)
10. [Behavior API for mod & modpack devs](#10-behavior-api-for-mod--modpack-devs)
11. [Configuration](#11-configuration)
12. [Multiplayer: catching & passing](#12-multiplayer-catching--passing)
13. [Compatibility checklist](#13-compatibility-checklist)
14. [Testing](#14-testing)
15. [Publishing & CI](#15-publishing--ci)
16. [Paper plugin port](#16-paper-plugin-port)
17. [Public documentation](#17-public-documentation)
18. [Roadmap](#18-roadmap)
19. [Decisions](#19-decisions)
20. [Links](#20-links)

---

## 1. State of Minecraft modding (late 2026)

What changed since you last modded (assuming you left somewhere around 1.16–1.20):

| Change | What it means for you |
|---|---|
| **Version numbering switched to year-based.** After 1.21.11 (Dec 2025) came **26.1** (Mar 2026, "Tiny Takeover"), **26.2** (Jun 2026, "Chaos Cubed"), **26.3** (15 Sep 2026). **26.4** is planned for Q4 2026. | "Latest" = 26.3 right now. Expect ~4 drops/year, each a small breaking update for mods. |
| **Minecraft Java is unobfuscated since 26.1.** Mojang ships the game with real class/method/field *and parameter* names. | The mappings war (MCP vs Yarn vs Mojmap vs Parchment) is basically over. Everyone uses Mojang's names. No remapping at build time on Fabric; NeoForge dropped Parchment. Code from tutorials/other mods now reads the same everywhere. |
| **Java 25 required** for 26.x (both runtime and Gradle JVM). | Install JDK 25. 1.20.5–1.21.11 used Java 21. |
| **Forge split (2023).** Most of the Forge team forked into **NeoForge**. | NeoForge is now the "big modpack" loader. Old MinecraftForge still ships builds but momentum is gone; many mods dropped it for 26.x. |
| **Fabric API updated to Mojang names** for 26.1; new Loom plugin id `net.fabricmc.fabric-loom` that no longer remaps. Needs Loom ≥ 1.15, Gradle ≥ 9.4. | Old Fabric templates/tutorials (Yarn names like `ServerPlayerEntity`) are now outdated. Use Mojang names (`ServerPlayer`). |
| **Common (`c:`) tags** shared between Fabric & NeoForge (since 1.21). Before: Fabric `c:`, Forge `forge:`, different names. Now one namespace, folder-style names (`c:ingots/iron`). Filled by the loaders (Fabric API `fabric-convention-tags-v2`, NeoForge built-in) and by other mods (`data/c/tags/...`, `"replace": false`). | Reference them from `common` with your own `TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", "bosses"))` — never import Fabric's `ConventionalEntityTypeTags` / NeoForge's `Tags`. Relevant: `c:bosses`, `c:capturing_not_supported`, `c:teleporting_not_supported`. **Empty on vanilla/Paper** (no loader fills them). Diff the two loaders' tag lists on 26.3 **(verify)**. |
| **Networking = typed payloads** (`CustomPacketPayload` + `StreamCodec`, since 1.20.5) on both loaders. | Network code is nearly identical across loaders now. |
| **Data components replaced item NBT** (1.20.5+). Entities still use NBT-ish save data (now via `ValueInput`/`ValueOutput` in recent versions **(verify)**). | Relevant if you store "carried mob" on an item. Prefer data attachments on the entity/player. |
| **Data attachments** exist on both loaders (Fabric Data Attachment API, NeoForge `AttachmentType`). | Clean way to store "this mob is in flight / being carried" without mixins into entity save code. |

**Prior art worth looking at before you start:**
- **Carry On** — pick up blocks/entities and carry them. Closest existing mod. Study its input handling, rendering, and blacklist approach. You'll also need to be compatible with it (both use sneak+right-click).
- Various "yeet"/throw mods exist; none (to my knowledge) do per-mob characteristics + physics + catching. Search Modrinth for "throw mob" / "yeet" first to avoid a name clash.

---

## 2. Loaders: who to target

| Loader | Status (late 2026) | Audience | Target? |
|---|---|---|---|
| **Fabric** | Very active, first to update to new versions. | Lightweight/vanilla+, performance packs, latest-version players. | ✅ Yes |
| **NeoForge** | Very active, dominant for large modpacks. | Big kitchen-sink modpacks (ATM, etc.). | ✅ Yes |
| **MinecraftForge** | Still releases (26.1 builds exist), but many mods skipped it for 26.2/26.3. | Legacy packs, older versions. | ⚠️ Optional. Only if you backport to ≤1.20.1, where Forge still rules. |
| **Quilt** | Fabric-compatible, tiny share. | Runs most Fabric mods. | Free via Fabric build — don't target explicitly. |
| **Paper** (server plugins) | Plugins, not mods. Hard-forked from Spigot (announced Dec 2024), API diverging. | Public servers & minigame networks with **vanilla clients** — the biggest audience for this mechanic. | ✅ **Later (M10)**, as a separate plugin sharing the `physics` module. See §16. |
| **Spigot / Bukkit** | Legacy, falling behind Paper. | Older servers. | ❌ Target Paper API only. |

**Recommendation:** Fabric + NeoForge, latest version (26.3) first. Paper plugin after 1.0. Consider a backport to **1.21.1** later — that's where a huge chunk of NeoForge modpacks still live (verify on CurseForge's version stats when you decide).

> Note: 26.3 is ~2 weeks old. Check that Fabric API and NeoForge both have stable 26.3 builds before pinning. If not, start on 26.2 and bump — the diff between drops is usually small.

---

## 3. Multi-loader & multi-version strategy

Three tools, different jobs:

| Tool | Job | Notes |
|---|---|---|
| **MultiLoader-Template** (jaredlll08) | Multi-*loader*. Gradle layout `common/`, `fabric/`, `neoforge/`. `common` compiles against vanilla only; loader-specific glue via `ServiceLoader` "platform services". | **No runtime dependency.** Most popular style for serious multi-loader mods. Recommended. |
| **Architectury** (API + Loom plugin) | Multi-*loader*, with a runtime API that abstracts events/registries. | Convenient, but your users must install Architectury API. Check its 26.x status **(verify)**. |
| **Stonecutter** | Multi-*version*. Preprocessor comments (`//? if >=26.1 {`) in a single codebase, builds every MC version. | Combine with a multi-loader layout. **Stonecraft** is a Gradle plugin/template bundling Stonecutter + loaders. |

**Recommended plan:**
1. Start from **MultiLoader-Template** (single version: 26.3). Get the mod working.
2. When you want a 1.21.1 backport, add **Stonecutter** (or switch to the Stonecraft template). Don't start with multi-version — it's a lot of build complexity while you're still finding the design.

Keep **95% of code in `common`**. Loader modules only do: entrypoint, event hookup, registration, networking registration, config file location, keybind registration.

**Extra module: `physics/`** — pure Java, **zero Minecraft imports**. `common` depends on it, and so will the Paper plugin later (§16). Split it out early: it's cheap now and painful once `common` grows. The other shared contract between mod and plugin is the **mob profile JSON schema** (§10).

---

## 4. Distribution: Modrinth vs CurseForge

| | **Modrinth** | **CurseForge** |
|---|---|---|
| Owner | Independent, open-source (Rocket Science/community) | Overwolf |
| Launcher | Modrinth App; works with Prism, ATLauncher, etc. | CurseForge App (Overwolf); Prism can import |
| Monetization | Ad revenue share to creators (typically considered generous) | "Authors Rewards" points program |
| Review | Manual review, usually fast | Automated + manual, can be slow |
| Modpack format | `.mrpack` | CurseForge zip / manifest |
| Audience | Growing fast, Fabric-heavy, techy | Largest, NeoForge/Forge modpack-heavy |
| API/publish | Simple REST API, Minotaur Gradle plugin | API + CurseGradle; API key needed |

**Do both.** Big modpacks (where your "interface for modpack devs" matters) are mostly on CurseForge; Modrinth has the more engaged Fabric crowd. Use **`mc-publish`** (GitHub Action by Kir-Antipov) to push to Modrinth, CurseForge, and GitHub Releases in one go.

Also:
- **Prism Launcher** — best launcher for testing your builds in real modpacks (multiple instances, easy mod swapping).
- **License**: MIT or LGPL-3.0 are common. If you want modpack devs to bundle it freely, say so explicitly on the page (CurseForge has a "allow in modpacks" toggle; Modrinth relies on license).
- **Version naming**: `throwablemobs-1.0.0+26.3-fabric.jar` / `...-neoforge.jar`.

---

## 5. Dev environment: JDK, Gradle, Neovim

You're on Windows 11.

### 5.1 JDK

```powershell
winget install EclipseAdoptium.Temurin.25.JDK
# optional, for 1.21.x backports later:
winget install EclipseAdoptium.Temurin.21.JDK
```

Set `JAVA_HOME` to JDK 25. Gradle toolchains (`java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }`) pick the right JDK per project anyway.

Optional: **JetBrains Runtime (JBR)** with enhanced class redefinition for much better hotswap during dev (change method bodies *and* add methods without restarting the game).

### 5.2 Gradle basics you'll use

```powershell
./gradlew build                      # builds all loader jars → */versions/<mc>/build/libs
./gradlew :fabric:26.3:runClient          # launch dev client (Fabric)
./gradlew :neoforge:26.3:runClient        # launch dev client (NeoForge)
./gradlew :fabric:26.3:runServer          # dedicated server — ALWAYS test multiplayer here
./gradlew :fabric:26.3:runClient --debug-jvm   # waits for debugger on port 5005
./gradlew :fabric:26.3:genSources         # Fabric: decompile MC sources for go-to-definition
```

NeoForge's ModDevGradle (`net.neoforged.moddev`) attaches decompiled sources automatically.

### 5.3 Neovim

Honest take first: **IntelliJ IDEA + "Minecraft Development" plugin** is the path of least resistance (Mixin target validation, access-widener/transformer support, run configs). Neovim works well for Java via **jdtls** (the Eclipse language server — same engine as VS Code's Java extension), but you lose Mixin-aware inspections. A common combo: Neovim for daily coding, IntelliJ open occasionally for Mixin-heavy work. You'll write few mixins for this mod, so Neovim is fine.

**Plugins (lazy.nvim):**

| Plugin | Purpose |
|---|---|
| `mason.nvim` | Installs `jdtls`, `java-debug-adapter`, `java-test`, `google-java-format` |
| `mfussenegger/nvim-jdtls` | Proper jdtls client (extensions: organize imports, extract method, test runner, DAP hookup) |
| `mfussenegger/nvim-dap` + `rcarriga/nvim-dap-ui` | Debugger (attach to running Minecraft) |
| `nvim-treesitter` (java, json, toml, groovy/kotlin for Gradle) | Highlighting |
| `blink.cmp` or `nvim-cmp` | Completion |
| `stevearc/conform.nvim` | Formatting |
| Alternative all-in-one: `nvim-java/nvim-java` | Wraps jdtls+dap+test setup. Less control, less config. |

`:MasonInstall jdtls java-debug-adapter java-test`

Note: jdtls itself needs Java 21+ to run; your JDK 25 covers that. On Windows the Mason `jdtls` shim needs **Python** on PATH.

**Installed config:** `%LOCALAPPDATA%\nvim\lua\plugins\java.lua` (lazy.nvim spec; auto-starts jdtls on Java files, keymaps `<leader>j*` / `<leader>d*`). Simplified standalone version of the same idea, as `ftplugin/java.lua`:

```lua
local root = vim.fs.root(0, { "gradlew", "settings.gradle", "settings.gradle.kts", ".git" })
if not root then return end

local mason = vim.fn.stdpath("data") .. "/mason/packages"
-- one workspace per project; jdtls caches the index here
local workspace = vim.fn.stdpath("data") .. "/jdtls-workspace/" .. vim.fn.fnamemodify(root, ":t")

local bundles = {
  vim.fn.glob(mason .. "/java-debug-adapter/extension/server/com.microsoft.java.debug.plugin-*.jar", true),
}
for _, jar in ipairs(vim.split(vim.fn.glob(mason .. "/java-test/extension/server/*.jar", true), "\n")) do
  -- these two jars break jdtls if loaded as bundles
  if not jar:match("runner%-jar%-with%-dependencies") and not jar:match("jacocoagent") then
    table.insert(bundles, jar)
  end
end

local jdtls = require("jdtls")
jdtls.start_or_attach({
  cmd = { "jdtls", "-data", workspace, "--jvm-arg=-Xmx4g" }, -- MC projects are big; give it RAM
  root_dir = root,
  settings = {
    java = {
      configuration = {
        runtimes = {
          { name = "JavaSE-25", path = "C:/Program Files/Eclipse Adoptium/jdk-25", default = true }, -- adjust path
        },
      },
      import = { gradle = { enabled = true, wrapper = { enabled = true } } },
      eclipse = { downloadSources = true },
      maven = { downloadSources = true },
      signatureHelp = { enabled = true },
      contentProvider = { preferred = "fernflower" }, -- decompile when no sources
      completion = {
        favoriteStaticMembers = { "org.junit.jupiter.api.Assertions.*" },
      },
    },
  },
  init_options = { bundles = bundles },
  on_attach = function(_, bufnr)
    jdtls.setup_dap({ hotcodereplace = "auto" })
    local map = function(k, f, d) vim.keymap.set("n", k, f, { buffer = bufnr, desc = d }) end
    map("<leader>jo", jdtls.organize_imports, "Organize imports")
    map("<leader>jt", jdtls.test_nearest_method, "Test method")
    map("<leader>jT", jdtls.test_class, "Test class")
  end,
})
```

**DAP attach config (e.g. in your dap setup):**

```lua
require("dap").configurations.java = {
  {
    type = "java",
    request = "attach",
    name = "Attach to Minecraft (5005)",
    hostName = "127.0.0.1",
    port = 5005,
  },
}
```

Workflow: `./gradlew :fabric:26.3:runClient --debug-jvm` in a terminal → in Neovim `:lua require'dap'.continue()` → pick "Attach". Breakpoints + hot code replace work.

**Gotchas:**
- First import of a Minecraft project takes minutes (it indexes the whole game). Run `./gradlew build` once from the shell first so Gradle has everything downloaded.
- If go-to-definition on Minecraft classes shows decompiled-without-names, run `genSources` (Fabric) and `:JdtUpdateConfig` / restart jdtls.
- Multi-project builds: open Neovim at the repo root so jdtls sees `settings.gradle`.
- If jdtls gets confused after changing Gradle files: `:JdtWipeDataAndRestart`.

---

## 6. Mod architecture

```
ThrowableMobs/
├─ physics/                        # pure Java, NO Minecraft imports — shared with paper/
│  └─ src/main/java/.../physics/   # impulse, drag, impact energy/momentum/force, catch geometry
├─ common/                         # 95% of the mod code, vanilla-only, depends on physics/
│  └─ src/main/java/dev/<you>/throwablemobs/
│     ├─ ThrowableMobs.java        # constants, mod id, init
│     ├─ api/                      # PUBLIC API (stable, documented, semver'd)
│     │  ├─ ThrowableMobsApi.java
│     │  ├─ ThrowBehavior.java
│     │  ├─ ThrowContext.java
│     │  ├─ ImpactContext.java / ImpactResult.java
│     │  ├─ MobPhysicsProfile.java
│     │  └─ event/                 # cancellable pickup/throw/catch/impact events
│     ├─ core/
│     │  ├─ CarryManager.java      # who carries what
│     │  ├─ FlightTracker.java     # in-flight mobs, per-tick sim & collision
│     │  ├─ ImpactResolver.java    # physics → damage/knockback (calls physics/ module)
│     │  └─ CatchHandler.java
│     ├─ tags/
│     │  └─ ModTags.java           # own tags + c: TagKeys built by hand (no loader imports)
│     ├─ behavior/
│     │  ├─ builtin/               # explode, teleport, split, ignite, apply_effect...
│     │  ├─ BehaviorRegistry.java
│     │  └─ HeuristicResolver.java # guesses behaviors for unknown modded mobs
│     ├─ data/                     # datapack JSON loader (codecs) for mob profiles
│     ├─ config/
│     ├─ network/                  # payloads: ThrowRequest, CarrySync, CatchAttempt...
│     ├─ client/                   # charge HUD, trajectory preview, carry rendering
│     └─ platform/                 # Services interface (ServiceLoader) for loader-specific stuff
├─ fabric/                         # entrypoints, Fabric events, Fabric networking reg
├─ neoforge/                       # @Mod class, NeoForge events, payload reg
├─ paper/                          # LATER (M10): Paper plugin, Bukkit API glue, depends on physics/
├─ docs/                           # public docs site source (§17)
│  ├─ generated/                   # config/defaults/schema reference, written by Gradle — don't hand-edit
│  └─ example-datapack/
└─ THROWABLE_MOBS.md               # this file
```

**Design rules:**
- **The thrown mob stays the real entity.** Don't replace it with a projectile entity and re-spawn it later. Keeping the original entity preserves all modded data (NBT, attachments, capabilities, names, equipment, AI state) — which is the single biggest compat win. The mob gets an "in-flight" attachment and the `FlightTracker` drives its motion and collision each tick, the same way `AbstractArrow` does internally.
- **Server-authoritative.** Clients send *intent* (start charge, release, catch attempt); server validates and simulates. Clients only render and predict.
- **Physics in pure Java.** The `physics/` module takes doubles/vectors, returns numbers. Trivial to unit test and to tune, and reusable by the Paper plugin.
- **Everything goes through the API**, including your built-in behaviors. If vanilla creepers use the same `ThrowBehavior` interface third parties use, the API is guaranteed to be good enough.

---

## 7. Core mechanic: pick up, carry, throw

### Controls (default, all rebindable / configurable)

| Action | Input |
|---|---|
| Pick up | Modifier + Pick up (default Left Shift + right-click) on a mob, empty main hand |
| Charge throw | Hold right-click while carrying (charge bar like a bow) |
| Throw | Release right-click |
| Put down | Modifier + Put down (default Left Shift + right-click) while carrying, looking at ground |
| Catch | Empty hand + look at incoming mob (+ optionally hold right-click) — see §12 |

Three key mappings in Controls → Throwable Mobs: **Modifier** (default Left Shift; unbind it to need no modifier), **Pick up** and **Put down** (both default right-click, so the same key toggles). The defaults share keys with Sneak and Use; when one of ours acts, vanilla's use is swallowed for that press, so no trading with the villager you just picked up. Same input as Carry On (clash accepted).

### Carrying
- **Follow model** (not a passenger): vanilla refuses entities riding players on the server (`PLAYER` is `.noSave()`), and passengers aren't saved with chunks. The mob stays a normal, always-saved entity; the server moves it above the carrier's head each tick (AI, gravity and physics off), and the client mod locks it to the carrier for smooth rendering. A carry-state attachment (persistent + synced) records who carries it.
- While carried: mob AI paused (`setNoAi` temporarily), invulnerable to suffocation, can't attack carrier. Restore original state on throw/drop — store the original flags in the attachment.
- **Carry slowdown** scales with mass (attribute modifier on movement speed). Carrying a cow should feel heavy.
- Behaviors get `onPickup` / `onCarryTick` hooks (creeper: fuse starts ticking while carried if config `creeper_fuse_on_pickup = true` — hot potato!).
- Max carry mass per player configurable. Code checks **only** `#throwablemobs:unthrowable`; its default contents include `#c:bosses` and `#c:capturing_not_supported` (see §10 Layer 1). This keeps cross-loader conventions on by default while letting pack devs override everything.

### Throwing
- Charge time `t` (ticks), clamped to `max_charge_ticks`. Charge fraction `c = ease(t / max)` (bow-style ease curve).
- Server measures charge time itself (between start-charge and release packets) — never trust a client-reported charge.
- Initial velocity from the impulse model (§8). Dismount, set velocity, attach `InFlight` state (thrower UUID, launch tick, launch position, mass, behavior list).
- Client: charge HUD + optional dotted **trajectory preview** using the same physics code (shared from `common`).

### Flight & collision
Per server tick for each in-flight mob:
1. Behaviors' `onFlightTick` (chicken flaps → reduced gravity; blaze leaves fire particles; ghast… shouldn't be throwable).
2. Integrate velocity (§8).
3. Sweep collision from `pos` to `pos + v` against blocks (`level.clip`) and entities (`ProjectileUtil.getEntityHitResult` style, inflated AABB, excluding thrower for the first N ticks).
4. On hit → `ImpactResolver` → behaviors' `onImpact` → apply results.
5. End flight when: impact without bounce, velocity below threshold on ground, in water (behavior-dependent), max flight ticks reached.

---

## 8. Physics model

Minecraft units: **1 block = 1 m**, **1 tick = 0.05 s**, velocities are **blocks/tick** (1 b/t = 20 m/s). Vanilla arrows use gravity `0.05 b/t²` and drag `0.99`/tick. Use the same scheme so throws feel "Minecraft".

### 8.1 Mass

Default from hitbox volume × density, overridable per mob:

```
m = width² × height × ρ × massMultiplier
```

| Mob | Hitbox (w × h) | Volume (m³) | m (ρ = 400 "game-kg/m³") |
|---|---|---|---|
| Chicken | 0.4 × 0.7 | 0.112 | ~45 |
| Creeper | 0.6 × 1.7 | 0.612 | ~245 |
| Cow | 0.9 × 1.4 | 1.134 | ~454 |
| Iron golem | 1.4 × 2.7 | 5.292 | ~2117 → over default `max_mass`, unthrowable |

Values are "game-kg", not real kg; only ratios matter. Densities can be overridden per mob (slimes are dense and bouncy, phantoms are light, magma cubes heavy).

### 8.2 Launch — impulse model

The player delivers a fixed **impulse** `J` (strength). Light mobs fly fast, heavy mobs barely leave your hands:

```
v₀ = min( J × c / m , v_max ) × look_direction  +  player_velocity × inherit_factor
```

- `J` configurable (`throw_impulse`), modified by an optional **Strength effect** bonus and a custom attribute `throwablemobs:throw_strength` (lets other mods/gear buff throwing).
- `c` = charge fraction.
- `v_max` cap so chickens don't become orbital.
- Inheriting player velocity = sprint-throws go further.

### 8.3 Flight integration

Per tick (semi-implicit Euler, same as vanilla projectiles):

```
v.y -= g × gravityMultiplier          (g = 0.05 b/t² default)
v   *= drag                           (linear mode: drag = 0.99)
   or v -= k × |v| × v × A / m        (quadratic mode: A = frontal area w×h, heavier = less affected)
pos += v
```

`physics.drag_model = "linear" | "quadratic"` in config. Quadratic is more "real" (a chicken slows fast, a creeper flies through), linear is more predictable and vanilla-like.

Track per flight: distance travelled, peak speed, previous-tick velocity (needed for impact deceleration), airtime.

### 8.4 Impact

You asked for impact strength from speed, distance, and acceleration. The physically sound way, with all three available as config modes:

**Relative velocity & reduced mass** (handles mob-vs-mob and mob-vs-wall in one formula):

```
v_rel = v_thrown − v_target            (target = block → v_target = 0)
μ     = (m₁ × m₂) / (m₁ + m₂)          (target = block → μ = m₁, i.e. infinite mass)
```

**Collision energy** (energy available to cause damage — the kinetic energy in the centre-of-mass frame):

```
E = ½ × μ × |v_rel|²
```

**Deceleration / force** (your "acceleration" term). The mob goes from `v_rel` to `e × v_rel` (e = restitution, bounciness) over a collision time `Δt` (1 tick by default; softer mobs/blocks = longer):

```
a = (1 + e) × |v_rel| / Δt
F = m₁ × a
```

**Damage modes** (`impact.damage_model`):

| Mode | Formula | Feel |
|---|---|---|
| `energy` (default) | `dmg = k_E × max(0, E − E_min)` | Speed matters a lot (quadratic). Most "physical". |
| `momentum` | `dmg = k_p × max(0, μ|v_rel| − p_min)` | Linear in speed; heavy slow mobs hurt more. Easier to balance. |
| `force` | `dmg = k_F × max(0, F − F_min)` | Uses deceleration; soft landings (hay, slime blocks, water, powder snow) reduce damage via longer `Δt`. |

All capped by `max_impact_damage`, then optionally shaped by a curve (`linear | sqrt | log`).

**Distance:** physics already accounts for distance (drag/gravity change the speed at impact). Additionally offer `impact.distance_falloff` (e.g. `"none" | "linear:32"`) for gameplay tuning, and use distance for advancements/stats ("threw a creeper 100 blocks").

**Damage split:** target takes `dmg × target_share`, thrown mob takes `dmg × self_share` (default: proportional to the *other* body's mass — the chicken loses when it hits a cow). Thrown mob hitting a block takes the full self share (like fall damage). Respect `#minecraft:fall_damage_immune`-style tags / mob's own `causeFallDamage` logic for compat.

**Knockback** (momentum transfer):

```
Δv_target = (1 + e) × m₁ / (m₁ + m₂) × v_rel
Δv_thrown = −(1 + e) × m₂ / (m₁ + m₂) × v_rel
```

Multiplied by the target's knockback resistance attribute.

**Block surface modifiers** (data-driven via block tags): `#throwablemobs:soft_landing` (hay, wool, slime block → bigger Δt, e higher for slime), `#throwablemobs:breakable_on_impact` (glass panes, leaves if config allows → breaks when `E > block_break_energy`). Respect claim protection & `mobGriefing` gamerule.

**Damage source:** custom damage type `throwablemobs:thrown_mob` with the thrower as attacker (so kills credit the player, loot works, and death messages read "Steve was hit by a Cow thrown by Alex"). Ship the damage type in data so other mods/datapacks can tag it.

---

## 9. Mob behaviors (the fun part)

Behaviors are composable "atoms" (see §10). Default vanilla roster:

| Mob | Behavior on impact / in flight | Notes |
|---|---|---|
| **Creeper** | Explodes on impact (power scales with impact energy, configurable). Charged creeper = bigger. Fuse keeps burning while carried/thrown → **hot potato**. | `explode` atom |
| **TNT minecart** | not a mob, ignore | — |
| **Enderman** | Teleports randomly on impact instead of taking damage; angry at thrower. Thrown into water: takes damage. | `teleport_random` (all teleport atoms skip entities in `#c:teleporting_not_supported`) |
| **Endermite / Shulker** | Endermite: teleports *thrower* to impact point (ender pearl!). Shulker: levitation cloud on impact. | Pearl behavior configurable |
| **Slime / Magma cube** | Bounces (restitution 0.8); on hard impact splits into smaller slimes. Magma cube ignites target. | `bounce`, `split`, `ignite` |
| **Blaze** | Leaves fire trail, ignites hit entity/sets fire at impact. | `ignite`, particles |
| **Chicken** | Flaps → low gravity, no fall damage; lays an egg on impact (chance). | `gravity_multiplier: 0.4` |
| **Bee** | Stings what it hits (poison), angry swarm if hive nearby. | `apply_effect` |
| **Pufferfish** | Poison + nausea cloud. Must be in water bucket… or allow picking up when out of water. | `apply_effect` area |
| **Witch** | Shatters a random splash potion at impact point. | `splash_potion` |
| **Skeleton / Stray / Bogged** | Fires an arrow mid-flight at nearest target. Stray: slowness. Bogged: poison. | `ranged_attack_in_flight` |
| **Snow golem** | Snowball burst; leaves snow layer at impact. | `place_block` |
| **Sheep** | Drops wool on hard impact (shear-like, no damage to wool if already sheared). | `drop_item` |
| **Squid / Glow squid** | Ink blindness cloud / glowing effect. | `apply_effect` area |
| **Cat** | Always lands on its feet: no self damage. | `self_damage_multiplier: 0` |
| **Goat** | Rams on impact: extra knockback. Screaming goat: louder. | `knockback_multiplier: 2.5` |
| **Llama** | Spits at the target on impact. | `spit` |
| **Phantom / Bat / Allay / Vex** | Fly away mid-flight (escape after N ticks). | `escape_after` |
| **Zombie / Husk / Drowned** | Grab target on impact (brief slowness) — husks apply hunger. | `apply_effect` |
| **Wither skeleton** | Wither effect on hit target. | `apply_effect` |
| **Breeze** | Wind burst on impact (launches entities). | `wind_burst` |
| **Armadillo** | Rolls up: no damage, bounces. | `bounce`, `self_damage_multiplier: 0` |
| **Axolotl** | Plays dead in water ;) | cosmetic |
| **Villager** | Allowed by default? Config flag. Reputation penalty with village. | Gossip system — be nice. |
| **Pets (wolf, cat, parrot)** | Only the owner can pick up. Wolves land, don't take damage under config threshold. | `owner_only` |
| **Iron golem, Ravager, Warden, Wither, Ender Dragon, Elder Guardian** | Unthrowable by default (mass or `#throwablemobs:unthrowable`, which includes `#c:bosses`). | Config / tag override for chaos |
| **Players** | Off by default. `allow_throwing_players` for PvP servers. | Fun with friends |

### Heuristic resolver (unknown modded mobs)

> **As built (M3, 2026-10-11):** guesses cover creeper, cube mobs (slime / magma cube: bounce + split), enderman and fire-immune mobs, and only apply to non-vanilla mobs; the flying, ranged and escape rows come with those atoms in M7. They're the lowest profile layer, so any profile merges on top. Reference: `docs/guide/modded-mobs.md`.
When a mob has no profile, guess from its class hierarchy and properties (toggle `behaviors.auto_detect = true`):

| Detected | Behavior |
|---|---|
| `extends Creeper` | `explode` |
| `extends Slime` | `bounce` + `split` |
| `extends EnderMan` | `teleport_random` |
| `implements RangedAttackMob` | `ranged_attack_in_flight` |
| `fireImmune()` | `ignite` |
| `FlyingMob` / `FlyingAnimal` / no-gravity mobs | `escape_after` + low gravity |
| In `#throwablemobs:unthrowable` (incl. `#c:bosses`, `#c:capturing_not_supported`) | blocked |
| In `#c:teleporting_not_supported` | no teleport atoms |
| anything else | mass from hitbox, default damage |

Since 26.x is unobfuscated, `instanceof` checks against vanilla classes are reliable on both loaders, and modded mobs almost always extend vanilla bases.

---

## 10. Behavior API for mod & modpack devs

Three layers, from no-code to full Java:

### Layer 1 — Tags (modpack devs, zero code)

```
data/throwablemobs/tags/entity_type/unthrowable.json
data/throwablemobs/tags/entity_type/owner_only.json
data/throwablemobs/tags/entity_type/no_self_damage.json
data/throwablemobs/tags/block/soft_landing.json
data/throwablemobs/tags/block/breakable_on_impact.json
```

Our own tags **include `c:` tags by default** instead of the code checking `c:` tags directly. Mod is convention-friendly out of the box, and pack devs keep the final say (`"replace": true` to drop e.g. `#c:bosses` and throw the Wither):

```json
// data/throwablemobs/tags/entity_type/unthrowable.json (shipped default)
{
  "replace": false,
  "values": [
    "#c:bosses",
    "#c:capturing_not_supported",
    "minecraft:warden",
    "minecraft:iron_golem"
  ]
}
```

Same idea for blocks: `breakable_on_impact` includes `#c:glass_panes` so modded glass panes work too.

Rules for referencing tags we don't own:
- Mark them optional so a missing tag never fails loading: `{ "id": "#c:something", "required": false }`.
- Don't invent `c:` tags (there is no `#c:creepers`). Make our own (`#throwablemobs:creeper_like`) instead.
- On Paper, `c:` tags are empty (no loader fills them) — the plugin ships explicit default lists (§16).

### Layer 2 — Datapack mob profiles (modpack devs, JSON)

> **As built (M3, 2026-10-11):** the format below is the original sketch. What the code reads is documented in `docs/guide/mob-profiles.md` and `docs/guide/behaviors.md`. Differences: profiles **merge** per field across layers (lists replace, `"replace": true` drops lower layers); `physics` also has `block_restitution`, `max_bounces`, `bounce_impacts`; action fields sit flat next to `type` (`"base"`, `"per_energy"`, `"max"` for `explode`, block damage always follows `mobGriefing`); `trigger` defaults to `impact`. Implemented atoms: `explode`, `teleport_random`, `anger_thrower`, `split`, `ignite`, `drop_item`, `particles`. Conditions: `min_impact_energy`, `chance`, `in_water`, `is_charged`, `first_impact_only`. `carry`, `collision_time_ticks`, `fuse`, the `caught` trigger and the other conditions are reserved for later.

`data/<namespace>/throwablemobs/mob_profile/<name>.json`, loaded with codecs and **reloaded with `/reload`**. Synced to clients (for trajectory preview mass/gravity).

```json
{
  "$schema": "https://<docs-site>/schema/mob_profile.v1.json",
  "schema_version": 1,
  "entities": ["minecraft:creeper", "#throwablemobs:creeper_like", "somemod:nuclear_creeper"],
  "priority": 0,
  "physics": {
    "mass": 245.0,
    "gravity_multiplier": 1.0,
    "drag_multiplier": 1.0,
    "restitution": 0.1,
    "collision_time_ticks": 1.0
  },
  "carry": {
    "allowed": true,
    "owner_only": false,
    "slowdown": 0.15
  },
  "damage": {
    "self_multiplier": 1.0,
    "target_multiplier": 1.0
  },
  "behaviors": [
    {
      "type": "throwablemobs:explode",
      "trigger": "impact",
      "min_impact_energy": 50.0,
      "power": { "base": 2.0, "per_energy": 0.002, "max": 5.0 },
      "charged_multiplier": 2.0,
      "fire": false,
      "respect_mob_griefing": true
    },
    {
      "type": "throwablemobs:fuse",
      "trigger": "pickup",
      "ticks": 60,
      "on_expire": "explode"
    }
  ]
}
```

**Built-in atoms** (each a registered `BehaviorType` with its own codec):
`explode`, `teleport_random`, `teleport_thrower`, `bounce`, `split`, `ignite`, `apply_effect` (single target or area cloud), `splash_potion`, `spawn_entity`, `drop_item` / `loot_table`, `place_block`, `play_sound`, `particles`, `lightning`, `wind_burst`, `ranged_attack_in_flight`, `escape_after`, `knockback_multiplier`, `fuse`, **`run_function`** (runs an `.mcfunction` as the mob at the impact point — this makes the system infinitely extensible for datapack authors).

**Triggers:** `pickup`, `carry_tick`, `throw`, `flight_tick`, `impact`, `impact_entity`, `impact_block`, `caught`, `land`.

**Conditions** (optional on any behavior): `min_impact_energy`, `min_speed`, `min_distance`, `chance`, `target_type` (tag), `is_charged`, `in_water`, `dimension`, plus vanilla **predicates** (`"predicate": "mypack:is_night"`) — reuse vanilla's loot-condition system so authors already know it.

### Layer 3 — Java API (mod devs)

> **As built (M3, 2026-10-11):** `onImpact(ImpactContext ctx, ImpactResult current)` takes the result so far (behaviors chain); `registerBehaviorType(BehaviorType)` takes a record of id + `MapCodec`; `registerProfile` merges with earlier registrations; `onCaught` and the events come later (M4 / M7); no `-api` artifact before M8. Reference: `docs/guide/java-api.md`.

Published as a separate `-api` artifact (Maven) so other mods can `compileOnly` against it without a hard dependency.

```java
public interface ThrowBehavior {
    default void onPickup(ThrowContext ctx) {}
    default void onCarryTick(ThrowContext ctx) {}
    default void onThrow(ThrowContext ctx) {}
    default void onFlightTick(ThrowContext ctx) {}
    /** Return a modified result (e.g. cancel damage, force bounce, end flight). */
    default ImpactResult onImpact(ImpactContext ctx) { return ctx.defaultResult(); }
    default void onCaught(ThrowContext ctx, Player catcher) {}
}

// registration (common code, works on every loader)
ThrowableMobsApi.registerBehavior(MyEntities.FIRE_DRAKE, new FireDrakeThrowBehavior());
ThrowableMobsApi.registerBehaviorType(id("lava_splash"), LavaSplashBehavior.CODEC); // usable from JSON
ThrowableMobsApi.registerPhysicsProfile(MyEntities.FIRE_DRAKE, MobPhysicsProfile.builder().mass(800).build());

// or implement on the entity itself — auto-detected
public class FireDrake extends Monster implements ThrowableMob {
    @Override public ThrowBehavior getThrowBehavior() { ... }
}
```

**Events** (cancellable, exposed per loader in their native event system *and* through the API's own simple listener list):
`PickupEvent`, `ThrowEvent` (modify velocity), `ImpactEvent` (modify damage/knockback), `CatchEvent`. Claim/protection mods hook these.

**Resolution order:** Java registration on the entity class → `ThrowableMob` interface → datapack profile (highest priority) → tags → heuristics → defaults. Datapacks can override Java defaults (`"replace": true`) so pack devs keep final say.

> **As built (M3):** layers from lowest: defaults → heuristics → `ThrowableMob` → Java registration → datapack files by priority. Each merges on top of the one below; tags aren't a profile layer but separate rules (`unthrowable`, `no_self_damage`, `no_teleport`).

**KubeJS:** optional integration later — expose the events to KubeJS scripts. Big modpacks love this.

Document all of this on the public docs site (§17); ship an **example datapack** zip with the release.

---

## 11. Configuration

Split into what belongs where:

| Kind | Where | Synced? | Example |
|---|---|---|---|
| **Creator file** (pack devs, admins) | `config/throwablemobs-server.toml`; a copy in the world's `syncedconfig/` (NeoForge) or `serverconfig/` (Fabric) overrides it per world | Server → client on join | mass limit, slowdown, put-down rules, later impulse, damage model, catching rules |
| **Player file** | `config/throwablemobs-client.toml`, editable in game (mod list on NeoForge, Mod Menu on Fabric), applies right away | No | prompt position, later HUD, trajectory preview |
| **Key mappings** | Controls → Throwable Mobs | No | Modifier, Pick up, Put down |
| **Per-mob** | Datapacks (§10) | Yes (datapack sync) | creeper behavior |
| **Per-world toggles** | **Gamerules** | Yes | `throwableMobsEnabled`, `throwableMobsPvp`, `throwableMobsGriefing` |

**Cross-loader config library: Forge Config API Port** (Fuzss, decided 2026-10-03). NeoForge's config API on Fabric too, so both specs live in `common/config` (`CreatorConfig`, `PlayerConfig`): TOML with comments and ranges, server sync, and NeoForge's `ConfigurationScreen`, which the port also ships, so both loaders get the same in-game screen. Fabric players need the port installed; NeoForge has it built in. NeoForge 26.x calls the synced type `SYNCED`, the port still `SERVER`; file names are set explicitly so they match. Code reads settings only through `ThrowableMobsConfig`.

Sketch of the server config:

```toml
[pickup]
require_empty_hand = true
max_mass = 1500.0
allow_players = false
allow_villagers = true
owner_only_for_tamed = true
blocked = ["minecraft:warden"]        # in addition to tags

[throw]
impulse = 3000.0                      # J, game-kg·b/t
max_charge_ticks = 20
charge_curve = "bow"                  # linear | bow | quadratic
max_speed = 3.0                       # b/t  (= 60 m/s)
inherit_player_velocity = 0.5
thrower_immunity_ticks = 5

[physics]
gravity = 0.05
drag_model = "linear"                 # linear | quadratic
drag = 0.99
quadratic_k = 0.02
default_density = 400.0
max_flight_ticks = 200

[impact]
damage_model = "energy"               # energy | momentum | force
energy_coefficient = 0.004
min_energy = 100.0
max_damage = 40.0
curve = "linear"                      # linear | sqrt | log
distance_falloff = "none"             # none | linear:<blocks>
self_damage = true
break_blocks = true                   # respects mobGriefing + claims
block_break_energy = 800.0

[catch]
enabled = true
radius = 1.5
cone_degrees = 35
require_use_key = false
self_catch = "AFTER_BOUNCE"
self_catch_delay_ticks = 10
recoil = true                         # catching fast heavy mobs knocks you back
recoil_damage = false
latency_compensation = true

[behaviors]
enabled = true
auto_detect_modded = true
creeper_fuse_on_pickup = true
disabled = []                         # e.g. ["throwablemobs:split"]
```

Every number in §8 is in here. Validate ranges, and document each key in a comment.

**Naming: one spelling per concept.** Vanilla's `server.properties` is the cautionary tale: `white-list` (turns the list on) sits next to `enforce-whitelist` (kicks players not on it). `white-list` is the old key, kept so existing servers don't break; `enforce-whitelist` came later with the other spelling, and now admins have to remember both. Our config keys, tags and translation keys use `snake_case` with one term per concept (e.g. `put_down_ground_allowed` / `put_down_ground_denied`, never mixing allow/white). Pick names before 1.0: renaming a key later means keeping the old one forever.

**Config screen: YACL, optional (0.4.0: the screen and the HUD preview; the 3D scene is still planned).** With [YACL](https://modrinth.com/mod/yacl) (YetAnotherConfigLib) installed, Mod Menu, the NeoForge config button and `/throwablemobs config` open a YACL screen; without it, the stock NeoForge `ConfigurationScreen` as now. YACL only draws the screen: each option is bound to its `ModConfigSpec` value, so storage stays as described above. Server options are greyed out on multiplayer servers. YACL's description pane takes a custom renderer in place of its image: it previews the prompt and charge bar, and a 3D mini scene for trajectory, flight pose, trail and catch highlight. Replaces the planned Configurator library (dropped 2026-10-24: YACL already does all of it, on both versions and loaders). Starts after M5.

---

## 12. Multiplayer: catching & passing

**Rules:**
- Any player with an **empty main hand** who **isn't carrying** can catch.
- Catch check runs on the **server** each tick for in-flight mobs: player within `radius` of the mob's swept path this tick (segment–AABB distance, not just endpoint — fast mobs skip past otherwise), and the mob is inside the player's view cone (`dot(look, toMob) ≥ cos(cone)`), and (optional) holding use key.
- **Thrower self-catch** per `self_catch`: by default only after a bounce (boomerang off a wall). `AFTER_DELAY` allows it after `self_catch_delay_ticks` (throw up, catch yourself), but then a thrower walking after a slow throw catches it again.
- On catch: flight ends, mob becomes carried by catcher. Behaviors get `onCaught`. **Fuse/timers keep running** → creeper hot potato across a team. (M4: only **ignited** creepers keep hissing; carrying calms a creeper that isn't, as since M1.)
- **Recoil:** catcher receives momentum `m × v` (scaled) as knockback; optional damage when the impulse exceeds what a player can "absorb". Catching a cow at full speed should push you back a few blocks.
- **Teams/PvP:** if `pvp` is false, thrown mobs don't damage players at all but can still be caught. Respect scoreboard teams (`allowFriendlyFire`). (M4: a mob that can't hurt a player flies past them instead of hitting them; the thrower's own mob can always hit them after `thrower_grace_ticks`.)

**Latency:** a player with 150 ms ping sees the mob ~3 ticks late. Options:
- `latency_compensation`: expand catch radius by `speed × ping_ticks` (cheap, good enough).
- Or client sends `CatchAttempt(entityId, clientTick)`; server rewinds that mob's position history (store last ~10 positions) and checks there. More accurate, more code. Start with radius expansion.

**Client prediction** (moved to M7 polish, 2026-10-18): clients already receive entity motion; for smoothness, run the same physics integration client-side between server updates (shared `physics/` code) and render the incoming-catchable highlight (outline glow when you're in catch range).

**Game-mode ideas** (later, optional): "Hot Potato" mini-game (creeper fuse passed around, last holder explodes), throw-distance leaderboard via stats, "Yeet" / "Nice Catch" / "Return to Sender" advancements.

---

## 13. Compatibility checklist

- [ ] **Never delete/respawn the entity.** Keep it as the real entity (preserves modded data/capabilities/attachments).
- [ ] Restore AI/noAi/invulnerability/flags exactly as they were (store originals).
- [ ] Multipart entities (Ender Dragon-style), vehicles, entities with passengers → block or dismount first.
- [ ] Leashed mobs: break leash or block (config).
- [ ] Tamed mobs: owner-only.
- [ ] Named / persistent mobs: don't let them despawn mid-flight.
- [ ] Chunk unload during flight: persist `InFlight` attachment; resume or land safely on reload.
- [ ] Dimension change / player logout while carrying: drop the mob safely at player position.
- [ ] Player death while carrying: drop mob.
- [ ] **Claim/protection mods** (FTB Chunks, Open Parties and Claims, etc.): fire cancellable events; use the **Fabric Permissions API** / **NeoForge PermissionAPI** for `throwablemobs.pickup`, `throwablemobs.throw.<mobid>`.
- [ ] Respect `mobGriefing` for explosions/block breaking.
- [ ] Fall damage: mob shouldn't *also* take vanilla fall damage on top of impact damage → reset `fallDistance` during flight.
- [ ] **Carry On** compat: detect it, avoid double-handling the same click (config: let Carry On handle carry, we handle throw?).
- [ ] **Common tags**: `#c:bosses` + `#c:capturing_not_supported` in default `unthrowable`; teleport atoms respect `#c:teleporting_not_supported`; `#c:glass_panes` in `breakable_on_impact`. No hardcoded `c:` checks in code.
- [ ] Mods that change mob size (Pehkui-style scaling): compute mass from **current** bounding box.
- [ ] Shaders/render mods: carried mob is a normal entity positioned by the client lock, rendered by vanilla = safe.
- [ ] Dedicated server: no client classes in common/server paths (test with `runServer`!).
- [ ] Mod works **server-optional?** Not really (needs client input + HUD). Document: "required on both sides".
- [ ] Mixins: minimize. Use loader events where possible. If you mixin, use **MixinExtras** (`@WrapOperation`, `@ModifyExpressionValue`) — far more compatible than `@Redirect`/`@Overwrite`.

---

## 14. Testing

| Kind | Tool | What |
|---|---|---|
| Unit | JUnit 5 in `common` | Physics math: launch speed vs mass, energy/momentum/force formulas, drag integration, catch geometry (segment–AABB). |
| In-game automated | **GameTest** (vanilla framework; both loaders support it) | "Creeper thrown at wall explodes", "slime splits above energy threshold", "unthrowable tag respected", "catch within radius succeeds". Run in CI headless. |
| Manual | `runClient` + `runServer` with 2 clients | Catching, latency (use clumsy / a latency proxy on Windows to fake ping), hot potato. |
| Real modpacks | Prism Launcher instances | Drop the jar into a big NeoForge pack + a Fabric pack; throw modded mobs; check log for errors. |

---

## 15. Publishing & CI

- **GitHub repo** + GitHub Actions:
  - On push/PR: `./gradlew build` + unit tests + GameTests.
  - On tag `v*`: build, then **mc-publish** → Modrinth + CurseForge + GitHub Release (both loader jars, changelog from tag).
- Publish the API jar to a Maven repo (GitHub Packages, or Modrinth Maven for consumers: `maven.modrinth.com`).
- **Dependencies to declare** on both sites: Fabric API (Fabric), Forge Config API Port (if used), Mod Menu (optional).
- Page content: GIFs. This mod lives or dies on a 10-second clip of a creeper being thrown into a friend's base.
- Semver for the API; bump major when breaking `ThrowBehavior`/JSON schema. Add `"schema_version"` to datapack JSON from day one.
- On tag also: build + deploy the docs site (§17) for that version.
- Paper plugin (later): publish to **Hangar** (PaperMC's repo), **Modrinth** (supports plugins), and **SpigotMC** resources (where many admins still look). mc-publish doesn't cover Hangar — use Hangar's Gradle plugin/API **(verify)**.

---

## 16. Paper plugin port

**Why:** public servers and minigame networks run Paper with **vanilla clients**. Creeper hot potato, chicken dodgeball, "yeet the sheep" CTF — no client install needed. Biggest possible audience for the mechanic.

**Target:** Paper API only (not Spigot — Paper hard-forked, API diverging). Version: whatever Paper supports for the current drop **(verify Paper 26.3 status)**.

### What carries over

| Part | Why |
|---|---|
| Physics (§8) | `physics/` module is pure Java. Shared as-is. |
| Flight sim, collision, impact, catching | Already server-authoritative. |
| Carry as passenger | Vanilla clients render passengers fine. |
| Behaviors | Re-implemented as thin glue on the Bukkit API (explode, teleport, ignite, effects, spawn…). |
| Mob profile JSON schema | Same schema, loaded from `plugins/ThrowableMobs/profiles/` instead of datapacks. |

### What changes for vanilla clients

| Mod feature | Problem | Paper solution |
|---|---|---|
| **Hold right-click to charge** | Empty-hand right-click on air **sends no packet** — server can't see it. | **A:** "throw" item with the `consumable` component (bow-style use animation, any item, since 1.21.2) → server sees use start + release (`PlayerStopUsingItemEvent`). **B:** hold sneak to charge, left-click (arm swing) to throw. Configurable. |
| Keybinds | None on vanilla. | Sneak, swing, swap-hands (`F` → `PlayerSwapHandItemsEvent`), drop key. |
| Charge HUD | No custom rendering. | Action bar, boss bar, or XP bar as meter. |
| Trajectory preview | Same. | Particles sent **only to the thrower** along the predicted arc. |
| Catch-glow highlight | Glowing is global. | Per-player glow via packets (packetevents) or scoreboard teams. |
| Client prediction | None. | Server movement only. Fine at normal ping. |
| `c:` tags | Empty — no loader fills them. | Ship explicit default lists in plugin config. |
| Custom damage type | Needs registry entry. | Paper registry/bootstrap API **(verify for 26.3)**; fallback: generic damage with custom death message. |
| Gamerules | No custom gamerules. | Per-world config sections. |

### Plugin-specific design
- **Folia:** big networks run region-threaded servers. In-flight mobs cross regions → tick via the entity's scheduler, not a global task. Support from the start or declare "Folia: no".
- **Avoid NMS / paperweight-userdev.** Would allow sharing more of `common`, but breaks every version and lags drops.
- **Minigame API** is the real value: `MobPickupEvent`, `MobThrowEvent`, `MobCatchEvent`, `MobImpactEvent` (cancellable, modifiable), per-world/per-arena enable, programmatic throw (`ThrowableMobs.throwMob(entity, velocity)`), stats hooks for leaderboards.
- **Permissions:** `throwablemobs.pickup`, `throwablemobs.throw.<mob>`, `throwablemobs.catch`, `throwablemobs.admin` (LuckPerms-friendly).

### Hybrid (later)
Paper plugin + **optional** client mod (Fabric/NeoForge), talking over a plugin-messaging channel. Vanilla players get the action-bar version; modded players get real charge HUD, trajectory preview, keybinds. Server never forces the mod.

---

## 17. Public documentation

Audiences: **players**, **server admins**, **modpack devs**, **mod devs**, **plugin devs**. Each gets a clear entry point. Docs are a feature — a mod with a great API nobody can find is a mod with no API.

### Tooling
- **VitePress** site in `docs/`, deployed to **GitHub Pages** by GitHub Actions on every release tag (Fabric's own docs use VitePress). Versioned: one docs version per mod major.
- Linked from Modrinth, CurseForge, Hangar pages, the in-game config screen ("Docs" button), and the mod's `fabric.mod.json`/`neoforge.mods.toml` homepage field.

### Site structure

```
Getting started        — install (Fabric / NeoForge / Paper), controls, first throw (GIFs)
Players                — controls, catching, hot potato, per-mob behaviors table
Server admins          — config reference, gamerules, permissions, claims, performance, Paper/Folia notes
Modpack devs           — tags, mob profiles, behavior atoms, triggers, conditions, recipes/cookbook, presets ("chaos")
Mod devs               — Java API, Javadoc, events per loader, ThrowableMob interface, Maven coordinates
Plugin devs            — Paper events & API, minigame examples
Reference              — GENERATED: config, default mob profiles, tags, JSON schema, damage formulas
Changelog & migration  — per release; migration guide per schema_version / API major
```

### Generate references from code — single source of truth
Hand-written reference tables rot. Generate them:

| Reference | Generated from | How |
|---|---|---|
| **Config reference** (every key, type, default, range, comment) | Config spec in `common` | Gradle task `generateDocs` walks the spec → `docs/generated/config.md` |
| **Default mob roster** (mass, gravity, restitution, behaviors per vanilla mob) | Shipped default datapack JSON | Same task reads `data/throwablemobs/throwablemobs/mob_profile/*.json` → table |
| **Default tag contents** | Shipped tag JSON | → table |
| **Behavior atoms** (fields, types, defaults) | Each `BehaviorType`'s codec + a short description string registered with it | → one page per atom |
| **JSON Schema** for mob profiles | Codecs (or hand-written + validated against all shipped/example JSON in tests) | Published at `/schema/mob_profile.v1.json`; profiles reference it via `"$schema"` → autocompletion + validation in VS Code/Neovim |
| **Java API** | Javadoc on `api/` package | Published with the site + to the Maven `-javadoc` jar |
| **Physics formulas** | §8 of this doc | Hand-written, but with a worked example per damage model whose numbers come from a unit test |

**CI check:** `generateDocs` runs in CI; build fails if `docs/generated/` differs from what's committed. Docs can never drift from defaults.

### Rules
- Every config key has a comment in the config file itself **and** a docs entry (both generated from the same spec).
- Every behavior atom page has: what it does, all fields with defaults, a minimal JSON example, a GIF if visual.
- **Cookbook** page with copy-paste recipes: "make zombies unthrowable", "make chickens explode", "throw the Wither", "hot potato server preset", "add my modded mob".
- Example datapack in `docs/example-datapack/`, zipped into every release, tested by GameTests (load it, assert no errors).
- API stability labels: `@ApiStatus.Stable` / `@ApiStatus.Experimental` / `@ApiStatus.Internal` (JetBrains annotations), shown in Javadoc.
- Changelog written for humans, grouped by audience ("Admins: new config key `catch.radius`").

---

## 18. Roadmap

| Milestone | Scope |
|---|---|
| **M0 — Setup** | JDK 25, MultiLoader-Template on 26.3 (or 26.2 if loaders lag), Neovim + jdtls + DAP working, `runClient` on both loaders. **Follow-up:** split `physics/` into its own MC-free Gradle module. |
| **M1 — Pick up & drop** | Carry (follow model, §7), sneak+right-click, on-screen prompt, restore state, `unthrowable` tag (incl. `c:` defaults), dedicated server test. |
| **M2 — Throw** | Charge + charge bar, impulse launch, flight sim, trajectory preview, block/entity collision, energy damage, knockback, damage type. Config files + screens done before M2. |
| **M3 — Behavior system** | `ThrowBehavior` API, registry, 6 vanilla behaviors (creeper, enderman, slime, chicken, blaze, cat), datapack loader, heuristics. |
| **M4 — Catching** | Server catch check, recoil, self-catch, glow highlight, latency compensation. |
| **M5 — Backport 1.21.1** | Stonecutter multi-version build, 1.21.1 on Fabric + NeoForge next to 26.3. **Done in 0.3.0.** Moved up from "Reach" (2026-10-11): most modpacks run 1.21.1 / 1.20.1. |
| **M5b — YACL screen** | Optional YACL config screen with live HUD preview and 3D throw scene; the stock screen stays as the fallback (§11). |
| **M6 — Carry+** | Carry blocks with their contents (chests, furnaces, spawners, modded machines) and throw them; carry and throw players (opt-in); stack mobs (small on big, thrown as a stack); holding animation in first and third person; Carry On compatibility. Goal: the best carry mod, not just the best throw mod. |
| **M7 — Polish** | HUD, sounds/particles, full vanilla roster, advancements, soft-landing / breakable block tags. |
| **M8 — Release 1.0** | Docs site (§17) with generated references + JSON Schema + cookbook, example datapack, GameTests, CI publishing (jars + docs), Modrinth + CurseForge pages. (Done early: `mc-publish` to both since 0.4.1; [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/throwablemobs) approved 2026-10-25, Modrinth page still in review.) |
| **M9 — Reach** | 1.20.1 backport, KubeJS integration. |
| **M10 — Paper plugin** | `paper/` module on shared `physics/`, item-based / sneak+swing controls, action-bar HUD, particle trajectory preview, minigame events API, permissions, Folia decision, docs pages for admins/plugin devs, Hangar + Modrinth + SpigotMC release. |
| **M11 — Hybrid** (optional) | Optional client mod enhances Paper servers via plugin-messaging channel (real HUD, preview, keybinds). |

---

## 19. Decisions

Settled 2026-10-02:

| Topic | Decision |
|---|---|
| Mod id / name | `throwablemobs` / "Throwable Mobs", package `io.github.safetimamovic.throwablemobs` |
| Versions | **26.3 first**, then a **backport** (target: 1.21.1) via Stonecutter once M3 is done — the core needs to be stable before the build gets multi-version complexity. 2026-10-11: 1.21.1 right after catching (M5), 1.20.1 later (M9). |
| Goal | **The best carry mod and the best carry + throw mod** (2026-10-11): match Carry On on carrying (blocks with contents, players, stacking, animation, M6) and stay ahead on throwing. |
| Thrown blocks (M6 sketch) | **Energy-based landing, highly configurable** (2026-10-11). Soft landing: the block is placed where it lands, facing its flight direction, contents and data kept. Hard landing (collision energy over `block_break_energy`): breaks like mining, block item drops and contents spill. Hitting a mob: damage from mass and speed like a thrown mob, then it falls on. Can't be placed (no space, claim, `mobGriefing` off): drops as an item that keeps its contents, never loses data. Valuable blocks in `#throwablemobs:never_break_on_impact` (spawners and similar by default) always place or drop with their data. Mass from the block, plus its contents. Knobs to expect: a global landing mode (energy / always place / always break), `block_break_energy`, whether contents spill, and per-block overrides through block profiles (like mob profiles) and tags. Own thrown-block entity: vanilla's falling block drops the plain block and loses contents when it can't place. |
| Loaders | Fabric + NeoForge. **No MinecraftForge.** |
| Defaults | **Balanced survival.** But every number is configurable and per-mob behavior is data-driven, so goofy packs can crank it (explosion power, impulse, `max_mass`, unthrowable list). Consider shipping a built-in "chaos" datapack/config preset players can enable. |
| Players throwable | Config option, **off by default**. |
| Villagers | **Throwable by default.** |
| Pick-up input | Key mappings **Modifier** (Left Shift) + **Pick up** / **Put down** (right-click), all rebindable in Controls; Modifier can be unbound. Same default as Carry On (clash accepted for now). |
| License | **MIT** |
| Server plugin | **Paper port planned (M10)**, Paper API only (no Spigot). Shares the `physics/` module + mob profile JSON schema. |
| Common tags | Code checks only our own tags; `c:` tags are default *contents* of them (pack devs can remove). No hardcoded `c:` checks. |
| Config | Forge Config API Port: creator file (synced) + player file (in-game screen). Which mobs can be carried stays in tags; per-mob weight comes with mob profiles (M3). |
| Public docs | VitePress site on GitHub Pages, versioned per major. Config/defaults/tags/atoms/JSON Schema **generated from code**, CI fails on drift. |

### Current setup state (M0 done)

- MultiLoader-Template (26.3 branch) layout: `common/`, `fabric/`, `neoforge/`, `build-logic/`.
- Versions pinned in `gradle.properties`: MC 26.3, Fabric Loader 0.19.5, Fabric API 0.164.0+26.3, NeoForge 26.3.0.95 (the first stable 26.3 build), Loom 1.18.2, ModDevGradle 2.0.148, Gradle 9.7.1, Java 25.
- `./gradlew build` produces `throwablemobs-{fabric,neoforge}-26.3-0.1.0.jar`; JUnit tests run on the `physics/` module (`ThrowPhysics` + test), split out of `common` during M1 (§3).
- Both loaders verified loading the mod on a dev server.
- Neovim: `~/AppData/Local/nvim/lua/plugins/java.lua` (nvim-jdtls + nvim-dap + dap-ui, Mason auto-installs jdtls/java-debug-adapter/java-test); treesitter java/groovy/properties parsers.

### Backport notes (for later)

1.21.1 differs from 26.x in ways Stonecutter handles with `//? if` blocks: Java 21 instead of 25, obfuscated game (Loom needs `mappings loom.officialMojangMappings()`; Fabric uses the old remapping `fabric-loom` plugin), `ResourceLocation` instead of `Identifier`, older entity save API, older Fabric API names in places. Keep version-sensitive calls behind small helper methods in `common` so the `//? if` blocks stay few.

---

## 20. Links

- Minecraft versions: https://minecraft.wiki/w/Java_Edition_26.1 · https://minecraft.wiki/w/Java_Edition_26.2 · https://mcreference.com/latest
- Fabric 26.1 announcement: https://fabricmc.net/2026/03/14/261.html
- Fabric API porting to 26.1: https://docs.fabricmc.net/develop/porting/fabric-api
- Fabric docs: https://docs.fabricmc.net
- NeoForge 26.1 release: https://neoforged.net/news/26.1release/
- NeoForge docs: https://docs.neoforged.net
- MultiLoader-Template: https://github.com/jaredlll08/MultiLoader-Template
- Stonecutter: https://stonecutter.kikugie.dev
- Stonecraft: https://stonecraft.meza.gg/docs
- Architectury: https://docs.architectury.dev
- mc-publish: https://github.com/Kir-Antipov/mc-publish
- nvim-jdtls: https://github.com/mfussenegger/nvim-jdtls
- nvim-java: https://github.com/nvim-java/nvim-java
- Modrinth: https://modrinth.com · CurseForge: https://www.curseforge.com/minecraft
- Prism Launcher: https://prismlauncher.org
- Fabric convention tags: https://docs.fabricmc.net/develop/data-generation/tags · NeoForge tags: https://docs.neoforged.net/docs/resources/server/tags
- Paper docs: https://docs.papermc.io · Hangar: https://hangar.papermc.io · Folia: https://papermc.io/software/folia
- packetevents: https://github.com/retrooper/packetevents
- VitePress: https://vitepress.dev · JSON Schema: https://json-schema.org
