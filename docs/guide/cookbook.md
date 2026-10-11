# Cookbook

Ready-made examples. Each file goes in a datapack (see [Mob profiles](mob-profiles.md#where-files-go)) and applies with
`/reload`. Check the result with `/throwablemobs profile <mob>`.

Third-party entity ids below were taken from the mods' source code (Twilight Forest `latest` branch, Alex's Mobs 1.20
branch). Ids can change between versions; if a profile doesn't apply, `/throwablemobs profile` shows whether it matched.

## Datapack only

### Floaty birds, no fall damage

```json
// data/mypack/throwablemobs/mob_profile/birds.json
{
    "entities": ["minecraft:parrot", "#minecraft:fall_damage_immune"],
    "physics": { "gravity_multiplier": 0.5 }
}
```

Tags work in `entities`; here every fall-immune mob floats. Mobs that already have a profile (the chicken) keep their
own `gravity_multiplier` if theirs has a higher priority or a later file id; give yours `"priority": 10` to win.

### A creeper that survives its own explosion

```json
// data/mypack/throwablemobs/mob_profile/reusable_creeper.json
{
    "entities": ["minecraft:creeper"],
    "priority": 10,
    "behaviors": [
        { "type": "throwablemobs:explode", "min_impact_energy": 20.0, "consume_mob": false, "max": 2.0 }
    ]
}
```

The list replaces our creeper's list, so the whole `explode` entry is written out again with the changes.

### Bouncy cows

```json
// data/mypack/throwablemobs/mob_profile/bouncy_cows.json
{
    "entities": ["minecraft:cow", "minecraft:mooshroom"],
    "physics": { "block_restitution": 0.6, "max_bounces": 4 },
    "damage": { "self_multiplier": 0.0 }
}
```

### Endermites as ender pearls (almost)

```json
// data/mypack/throwablemobs/mob_profile/endermite.json
{
    "entities": ["minecraft:endermite"],
    "behaviors": [
        { "type": "throwablemobs:teleport_random", "range": 8 },
        { "type": "throwablemobs:particles", "trigger": "flight_tick", "particle": { "type": "minecraft:portal" }, "count": 3 }
    ]
}
```

### Egg-laying chickens every time

```json
// data/mypack/throwablemobs/mob_profile/chicken_eggs.json
{
    "entities": ["minecraft:chicken"],
    "priority": 10,
    "behaviors": [
        { "type": "throwablemobs:drop_item", "item": "minecraft:egg", "count": 2 }
    ]
}
```

## A chaos server

Config (`config/throwablemobs-server.toml`) does most of it:

```toml
[throw]
    throw_impulse = 400.0
    max_throw_speed = 2.5
    strength_effect_bonus = 1.0

[flight]
    block_restitution = 0.5
    max_bounces = 6

[pick_up]
    max_mass = 5000.0
```

Then let players throw the Warden and the Wither by replacing our unthrowable list (see [Tags](tags.md#examples)), and
make every creeper a big one:

```json
// data/chaos/throwablemobs/mob_profile/creeper.json
{
    "entities": ["minecraft:creeper"],
    "priority": 100,
    "behaviors": [
        { "type": "throwablemobs:explode", "base": 3.0, "per_energy": 0.05, "max": 8.0, "fire": true }
    ]
}
```

## Twilight Forest

Facts from the source, namespace `twilightforest`:

- Its bosses are in `#twilightforest:bosses`, which Twilight Forest adds to `#c:bosses`, so our `unthrowable` tag already
  blocks the Naga, Lich, Hydra and the rest.
- The maze slime extends the vanilla slime, so it gets the slime guess: bouncy, splits on hard impacts.

Birds and small critters that float like chickens:

```json
// data/mypack/throwablemobs/mob_profile/twilight_birds.json
{
    "entities": ["twilightforest:raven", "twilightforest:tiny_bird", "twilightforest:penguin"],
    "physics": { "gravity_multiplier": 0.5 },
    "damage": { "self_multiplier": 0.0 }
}
```

A redcap sapper that blows up:

```json
// data/mypack/throwablemobs/mob_profile/redcap_sapper.json
{
    "entities": ["twilightforest:redcap_sapper"],
    "behaviors": [
        { "type": "throwablemobs:explode", "min_impact_energy": 15.0, "base": 1.0, "max": 2.5, "consume_mob": false }
    ]
}
```

Maze slimes that don't split (they're placed in mazes on purpose):

```json
// data/mypack/throwablemobs/mob_profile/maze_slime.json
{
    "entities": ["twilightforest:maze_slime"],
    "behaviors": []
}
```

Throwing Twilight bosses on a chaos server: replace `unthrowable` without `#c:bosses`, raise `max_mass`, and before
freeing a boss check that it's a single entity. Bosses made of several parts don't follow a carrier.

## Alex's Mobs

Namespace `alexsmobs`. The official mod targets 1.20.1; a community continuation carries it to newer versions, so check
the ids against the version you run.

Birds:

```json
// data/mypack/throwablemobs/mob_profile/alexs_birds.json
{
    "entities": [
        "alexsmobs:crow", "alexsmobs:hummingbird", "alexsmobs:toucan", "alexsmobs:blue_jay",
        "alexsmobs:seagull", "alexsmobs:roadrunner", "alexsmobs:bald_eagle"
    ],
    "physics": { "gravity_multiplier": 0.5 },
    "damage": { "self_multiplier": 0.0 }
}
```

The void worm is made of parts (`alexsmobs:void_worm_part`), so keep it out of players' hands:

```json
// data/throwablemobs/tags/entity_type/unthrowable.json
{
    "replace": false,
    "values": [
        { "id": "alexsmobs:void_worm", "required": false },
        { "id": "alexsmobs:void_worm_part", "required": false }
    ]
}
```

`"required": false` keeps the tag loading when Alex's Mobs isn't installed.

Big animals like `alexsmobs:elephant` or `alexsmobs:cachalot_whale` are probably over `max_mass` already; their mass
shows in `/throwablemobs profile`. To make one throwable anyway, give it a smaller `mass` in a profile.

## In code

Mod devs: see [Java API](java-api.md) for a complete example mod setup (a fire drake with its own behavior, and a
`lightning` action pack devs can use from JSON).
