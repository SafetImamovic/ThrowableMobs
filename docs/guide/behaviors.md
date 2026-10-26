# Behaviors

Behaviors are what a mob does around a throw: a creeper explodes on impact, an enderman teleports away. They go in the
`behaviors` list of a [mob profile](mob-profiles.md). Each entry is one JSON object with three parts:

```json
{
    "type": "throwablemobs:explode",
    "trigger": "impact",
    "min_impact_energy": 20.0,
    "base": 1.5
}
```

- `type`: which action, see [Actions](#actions). The action's own fields sit next to it (`base` here).
- `trigger`: when it runs. Optional, defaults to `impact`.
- Conditions: optional extra checks (`min_impact_energy` here).

Behaviors run in list order. On an impact, each one sees what the ones before it did (one may cancel the damage, the next
may still set the target on fire).

## Triggers

| Trigger | Runs |
|---|---|
| `pickup` | When a player picks the mob up |
| `carry_tick` | Every tick while carried |
| `throw` | When thrown |
| `flight_tick` | Every tick in the air, before the mob moves |
| `impact` | When it hits a block or an entity, including bounces (see `bounce_impacts`) |
| `impact_entity` | Only when it hits an entity |
| `impact_block` | Only when it hits a block |
| `land` | When the flight ends: landed, in water, or out of time |
| `caught` | When a player catches the mob in the air. The action's player is the catcher. `pickup` and `land` don't run for a catch |

## Conditions

All are optional; every set condition must hold.

| Condition | Type | Meaning |
|---|---|---|
| `min_impact_energy` | number | Only impacts with at least this collision energy. Ignored outside impacts. A hard creeper throw reaches about 46, a short lob stays under 10. |
| `chance` | 0 – 1 | Rolled every time the trigger fires. `0.25` = one in four. |
| `chance_setting` | name | Take `chance` from a server setting instead, so admins can change it in game. Only `"egg_chance"` so far (our chicken uses it). |
| `in_water` | boolean | The mob must (or must not) be in water. |
| `is_charged` | boolean | The mob must (or must not) be a charged creeper. Other mobs count as not charged. |
| `first_impact_only` | boolean | Only the first impact of a flight, not later bounces. |

## Actions

Defaults in brackets. Actions that make sense outside impacts also work with the other triggers; the notes say what they
do there.

### `throwablemobs:explode`

An explosion at the mob. Block damage follows the `mobGriefing` gamerule, like a creeper's.

| Field | Default | Meaning |
|---|---|---|
| `base` | 1.5 | Power at zero energy. A vanilla creeper is 3, TNT 4. |
| `per_energy` | 0.01 | Extra power per unit of collision energy. |
| `max` | 4.0 | Power cap. |
| `charged_multiplier` | 2.0 | Power factor for a charged creeper. |
| `fire` | false | Leave fire. |
| `consume_mob` | true | The mob is gone afterwards, like a creeper that blew up. `false` = it survives (a reusable bomb). |

On an impact it ends the flight. The blast counts as the player's (the thrower, or the catcher with `caught`), like TNT
they lit: they get the kill, and teammates without friendly fire take no damage.

### `throwablemobs:teleport_random`

Teleports the mob to a free spot on the ground, like an enderman. Mobs in `#throwablemobs:no_teleport` stay put.

| Field | Default | Meaning |
|---|---|---|
| `range` | 16 | Up to this many blocks away (1 – 64). |
| `cancel_damage` | true | On an impact, the mob takes no impact damage itself. |

On an impact it ends the flight. If no free spot is found, nothing happens.

### `throwablemobs:anger_thrower`

The mob goes after its thrower; an enderman stays angry like one you looked at. Not for players in creative or
spectator. No fields.

### `throwablemobs:split`

A slime or magma cube splits into smaller ones of half its size. The thrown mob shrinks and stays one of the pieces, so its
own data survives; the others are new and get its name and persistence. Smallest cubes and other mobs are ignored.

| Field | Default | Meaning |
|---|---|---|
| `min_pieces` | 2 | Fewest pieces, counting the thrown mob (2 – 16). |
| `max_pieces` | 4 | Most pieces. |

On an impact it ends the flight, so a split mob doesn't bounce on.

### `throwablemobs:ignite`

Sets the hit entity on fire. Outside impacts it sets the mob itself on fire.

| Field | Default | Meaning |
|---|---|---|
| `seconds` | 4.0 | Burn time. |
| `fire_on_block` | false | On a block hit, light a fire where the mob landed. Only with `mobGriefing`. |

### `throwablemobs:drop_item`

Drops items at the mob.

| Field | Default | Meaning |
|---|---|---|
| `item` | required | Item id, like `"minecraft:egg"`. |
| `count` | 1 | 1 – 64. |

### `throwablemobs:particles`

Particles around the middle of the mob. With `"trigger": "flight_tick"` it leaves a trail.

| Field | Default | Meaning |
|---|---|---|
| `particle` | required | Particle options in vanilla's format: `{ "type": "minecraft:flame" }`. |
| `count` | 4 | Particles each time (1 – 1000). |
| `spread` | 0.2 | Blocks around the middle. |
| `speed` | 0.0 | Particle speed. |

### `throwablemobs:radar`

The mob panics when a monster comes near: it screams, faster and higher the closer the nearest monster is, like a
Geiger counter. Use it with `"trigger": "carry_tick"` and carry the mob as an early warning. Carried chickens do this by
default. Monsters are mobs the game counts as enemies (zombies, creepers, slimes, phantoms...), modded ones included.

| Field | Default | Meaning |
|---|---|---|
| `range` | 16.0 | Blocks (1 – 64). No monster in range, no sound. |
| `slowest_interval_ticks` | 40 | Ticks between screams with the monster at the edge of the range (4 – 1200). |
| `fastest_interval_ticks` | 8 | Ticks between screams with the monster right next to the mob (4 – 1200). |
| `sound` | the mob's hurt sound | Sound event id, like `"minecraft:block.note_block.bell"`. |
| `volume` | 1.0 | 0 – 10. Above 1 it carries further, not louder. |
| `line_of_sight` | false | `true` = only monsters the mob can see. Off, it senses them through walls. |

## Turning behaviors off

In `config/throwablemobs-server.toml`, section `[behaviors]`:

| Key | Default | Meaning |
|---|---|---|
| `behaviors_enabled` | `true` | `false` = no behaviors run. Profile physics (mass, gravity, bounces) still apply. |
| `disabled_behaviors` | `[]` | Action types that never run, for example `["throwablemobs:explode"]`. |
| `auto_detect_behaviors` | `true` | Guesses for modded mobs, see [Modded mobs](modded-mobs.md). |

Changes apply while the server runs.

## Coming later

Reserved for later versions, not read yet: `dimension` and vanilla predicate
conditions, and more actions (`apply_effect`, `spawn_entity`, `run_function`, `wind_burst`, `escape_after`...).
