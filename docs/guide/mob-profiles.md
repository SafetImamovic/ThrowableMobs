# Mob profiles

A mob profile sets how a kind of mob flies, bounces, takes impact damage and what it does when thrown. Profiles are
JSON files in a datapack and reload with `/reload`.

## Where files go

```
<datapack>/data/<namespace>/throwablemobs/mob_profile/<name>.json
```

- `<datapack>` is any datapack: a folder or zip in a world's `datapacks/` folder, a mod's built-in data, or a pack
  loaded by a pack-loading mod.
- `<namespace>` is yours (your pack's name), not `throwablemobs`, unless you mean to replace one of our files.
- `<name>` is free. The file id (`<namespace>:<name>`) is used for ordering and shown by `/throwablemobs profile`.

After adding or changing a file, run `/reload` (needs op). The server log says `Loaded N mob profile(s)`. A file that
doesn't parse is logged with the reason and skipped; the others still load.

## Format

```json
{
    "schema_version": 1,
    "entities": ["minecraft:slime", "#mypack:bouncy"],
    "priority": 0,
    "replace": false,
    "physics": {
        "mass": 120.0,
        "gravity_multiplier": 1.0,
        "drag_multiplier": 1.0,
        "restitution": 0.3,
        "block_restitution": 0.8,
        "max_bounces": 5,
        "bounce_impacts": "every"
    },
    "damage": {
        "self_multiplier": 1.0,
        "target_multiplier": 1.0
    },
    "behaviors": [
        { "type": "throwablemobs:split", "trigger": "impact", "min_impact_energy": 15.0 }
    ]
}
```

Only `entities` is required. Everything else is optional: a profile sets only what it changes, the rest comes from the
layers below (see [Merging](#merging)) or from the config.

| Key | Type, range | Default | Meaning |
|---|---|---|---|
| `schema_version` | `1` | `1` | Format version. A file for a newer version fails to load, and the log names it. |
| `entities` | list of ids and `#tags` | required | Mobs this profile applies to. Ids of mods that aren't installed are skipped, so one file can cover optional mods. |
| `priority` | integer | `0` | Higher wins. Equal priorities apply in file id order: by name, then namespace (`a:x` before `b:x`, so `b:x` wins). |
| `replace` | boolean | `false` | Drop every layer below this one instead of merging. |

### `physics`

| Key | Range | Unset means | Meaning |
|---|---|---|---|
| `mass` | 0.001 – 10⁹ | hitbox volume × `default_density` | Weight in game-kg. Light mobs fly fast and far, heavy ones barely leave your hands. Over `max_mass`, the mob can't be picked up. |
| `gravity_multiplier` | 0 – 1000 | 1 | Scales the config `gravity`. `0.4` = floaty, `0` = flies straight. |
| `drag_multiplier` | 0 – 1000 | 1 | Exponent on the config `drag` per tick. `2` loses speed twice as fast, `0` = no air drag. |
| `restitution` | 0 – 2 | config `restitution` | Bounciness against entities: how much knockback the hit mob gets. |
| `block_restitution` | 0 – 2 | config `block_restitution` (0) | Bounciness against blocks. `0` = the flight ends at the first block. |
| `max_bounces` | 0 – 100 | config `max_bounces` (3) | Bounces off blocks before the flight ends. |
| `bounce_impacts` | `every`, `first_only` | config `bounce_impacts` | Whether later bounces deal damage and fire impact behaviors. |

The trajectory preview uses mass, gravity, drag and bounces too, so it stays exact for profiled mobs.

### `damage`

| Key | Range | Unset means | Meaning |
|---|---|---|---|
| `self_multiplier` | 0 – 1000 | 1 | Scales impact damage to this mob. `0` = never hurt by its own impacts. Applied after the `self_damage` config. |
| `target_multiplier` | 0 – 1000 | 1 | Scales impact damage this mob deals to what it hits. |

### `behaviors`

A list of things the mob does when picked up, thrown, in flight or on impact. See [Behaviors](behaviors.md). A set list
**replaces** the list from lower layers as a whole; `"behaviors": []` removes all of them.

## Merging

Several profiles can apply to one mob. They stack in layers, lowest first:

1. **Defaults**: hitbox mass, config values, no behaviors.
2. **Guesses** for modded mobs without a profile ([Modded mobs](modded-mobs.md)).
3. **`ThrowableMob`**: what a mod's mob class says about itself.
4. **Java registration**: profiles and behaviors other mods register in code.
5. **Datapack files**, lowest `priority` first.

Each layer merges on top of the one below:

- A field a layer sets wins over the same field below. Fields it doesn't set come from below.
- Lists (`entities`, `behaviors`) aren't merged: a set list replaces the one below.
- `"replace": true` drops every layer below.

Example: we ship a creeper profile with an `explode` behavior. Your pack adds:

```json
{ "entities": ["minecraft:creeper"], "physics": { "mass": 400 } }
```

The creeper now weighs 400 and still explodes. With `"behaviors": []` added, it stops exploding. With
`"replace": true`, it drops our profile completely: mass 400, no behaviors, everything else from the config.

## `/throwablemobs profile`

Shows the resolved profile of a mob and where every value came from:

```
/throwablemobs profile @e[type=chicken,limit=1,sort=nearest]

Mob profile of minecraft:chicken, layers (low to high): mypack:chicken < throwablemobs:chicken
  mass: 44.8 from hitbox  (default)
  gravity_multiplier: 0.4  (throwablemobs:chicken)
  block_restitution: 0.0 from config  (default)
  behaviors: [throwablemobs:drop_item on impact, throwablemobs:radar on carry_tick]  (throwablemobs:chicken)
```

Needs op level 2. Use it when a profile doesn't seem to apply: it shows whether the file matched the mob at all and
which layer won.

## Sizes to start from

Default density is 400 game-kg per block³; `throw_impulse` is 150 and `max_throw_speed` 1.0 b/t.

| Mob | Mass | Full-charge speed | Collision energy hitting a wall |
|---|---|---|---|
| Chicken | 45 | 1.0 (capped) | 22 |
| Creeper | 245 | 0.61 | 46 |
| Cow | 454 | 0.33 | 25 |

Without a `mass`, a mob weighs its hitbox volume × `default_density`, as if the box were solid. That's far off for mobs
whose hitbox is mostly air, so ours set it by hand: spider 150 (its box alone gives 706), cave spider 30, phantom 60,
and horses, donkeys and mules 600 (about 1250 from the box).

Collision energy is `½ × mass × speed²` against a block (against a mob, the reduced mass of the two is used). Use it
to pick `min_impact_energy` for behaviors: a hard creeper throw reaches about 46, a short lob stays under 10.
