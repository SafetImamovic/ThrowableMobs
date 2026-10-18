# Advancements and sounds

## Our advancements

The mod adds its own tab, "Throwable Mobs". Files: `data/throwablemobs/advancement/`. A datapack can replace any of
them by putting a file with the same id in its own copy of that folder.

| Advancement | How |
|---|---|
| Throwable Mobs | Pick up a mob |
| Yeet | Throw a mob at full charge |
| Long Distance | Throw a mob that lands 50 blocks from where you threw it |
| Kaboom | Kill a mob with a thrown creeper's explosion |
| Ender Toss | A mob you threw teleports away (`teleport_random`) |
| Nice Catch | Catch a mob in the air |
| Hot Potato | Catch a lit creeper |
| Return to Sender | Catch a mob thrown at you, throw it back and hit the thrower (hidden) |

## The `throwablemobs:throw_event` criterion

All but Kaboom use one criterion, which your own advancements can use too:

```json
"criteria": {
    "lob": {
        "trigger": "throwablemobs:throw_event",
        "conditions": {
            "event": "throw",
            "min_charge": 0.5,
            "entity": {
                "type": "minecraft:entity_properties",
                "entity": "this",
                "predicate": { "minecraft:entity_type": "minecraft:chicken" }
            }
        }
    }
}
```

| Field | Type | Meaning |
|---|---|---|
| `event` | required | `pickup`, `throw`, `catch`, `land`, `teleport`, `return_hit` (below). |
| `entity` | loot condition | Checks the mob, like vanilla's `tame_animal`. `"entity": "this"` is the mob. |
| `min_charge` | 0 – 1 | `throw` only: how far the throw was charged. Full charge is exactly 1. |
| `min_distance` | blocks | `land` only: straight-line distance from the launch point to where the flight ended. |
| `ignited` | boolean | The mob is (or isn't) a lit or hissing creeper. Other mobs count as not ignited. |
| `player` | loot condition | Checks the player, as in every vanilla criterion. |

| Event | Who gets it | When |
|---|---|---|
| `pickup` | The player | Picks a mob up (not a catch). |
| `throw` | The thrower | Lets go of a throw. |
| `catch` | The catcher | Catches a mob in the air. |
| `land` | The thrower | The flight ends: lands, hits something, goes into water or runs out of time. Not a catch. |
| `teleport` | The behavior's player | `throwablemobs:teleport_random` moved the mob. |
| `return_hit` | The thrower | The mob hit the player who threw it to them before they caught it. |

Thrown-mob explosions count as the player's (see [`explode`](behaviors.md#throwablemobsexplode)), so vanilla's
`player_killed_entity` works for them: Kaboom checks a killing blow tagged `#minecraft:is_explosion` with a creeper as
the direct entity.

## Sounds

| Sound event | Plays | Default |
|---|---|---|
| `throwablemobs:entity.mob.throw` | On a throw, louder and higher at full charge | `entity.player.attack.sweep` |
| `throwablemobs:entity.mob.bounce` | Bouncing off a block | `entity.generic.small_fall` |
| `throwablemobs:entity.mob.impact` | Landing hard, or hitting an entity | `entity.player.attack.knockback` |
| `throwablemobs:entity.mob.catch` | A catch | `item.armor.equip_leather` |

A resource pack swaps one by putting an entry with the same name in its `assets/throwablemobs/sounds.json` with
`"replace": true`. Bounces and impacts below `effect_min_energy` (config `[effects]`) are silent; the mob's own hurt
sound plays as usual when it takes damage.
