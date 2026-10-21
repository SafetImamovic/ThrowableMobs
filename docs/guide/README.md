# Throwable Mobs guide

For pack devs, server admins and mod devs: how to change what mobs do when they're picked up, thrown and caught. It's
written next to the code, so it matches the current version. Read it on the
[docs site](https://safetimamovic.github.io/ThrowableMobs/guide/) or here on GitHub.

| Page | For | What's in it |
|---|---|---|
| [Server config](server-config.md) | Server admins | Every setting in `throwablemobs-server.toml`, and what happens to carried mobs when players leave |
| [Mob profiles](mob-profiles.md) | Pack devs | The JSON format, where files go, how profiles merge, `/throwablemobs profile` |
| [Behaviors](behaviors.md) | Pack devs | Triggers, conditions, every built-in action with its fields |
| [Tags](tags.md) | Pack devs | Which mobs can be carried, who takes impact damage, who can teleport |
| [Modded mobs](modded-mobs.md) | Pack devs, admins | What the mod guesses for mobs without a profile, and how to override it |
| [Advancements and sounds](advancements.md) | Pack devs | Our advancements, the `throw_event` criterion, sound events |
| [Java API](java-api.md) | Mod devs | Behaviors, behavior types and profiles from code |
| [Cookbook](cookbook.md) | Everyone | Ready-made examples, including Twilight Forest and Alex's Mobs |

## Three ways to change how mobs act

From no code to full code:

1. **Config** (`config/throwablemobs-server.toml`): global numbers and switches, such as throw strength, bounciness,
   `self_damage`, `disabled_behaviors`. Applies to every mob.
2. **Datapacks**: tags and mob profiles. Per mob, reloads with `/reload`, and wins over everything a mod sets in code.
3. **Java**: other mods register behaviors, behavior types and profiles through `ThrowableMobsApi`.

Our own vanilla behaviors (exploding creepers, teleporting endermen, splitting slimes) are plain mob profiles in the mod's
datapack, `data/throwablemobs/throwablemobs/mob_profile/`. Anything they do, your pack can do too.
