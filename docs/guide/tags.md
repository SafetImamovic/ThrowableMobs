# Tags

Tags are the no-code way to change which mobs the rules apply to. Put a file with the same path in your datapack:
`"replace": false` adds to our list, `"replace": true` replaces it. Changes apply with `/reload`.

The mod only ever checks its own tags. Convention tags (`#c:...`) that other mods fill are part of our tags' default
contents, so you can take them out.

## Entity type tags

`data/throwablemobs/tags/entity_type/<name>.json`

| Tag | Default contents | Effect |
|---|---|---|
| `unthrowable` | `#c:bosses`, `#c:capturing_not_supported`, ender dragon, wither, warden, elder guardian | Can never be picked up. |
| `owner_only` | wolf, cat, parrot | Only the owner can pick these up, even with `owner_only_for_tamed = false`. |
| `no_self_damage` | `#minecraft:fall_damage_immune` (chicken, cat, blaze, bee, parrot, bat, phantom, breeze, golems...) | Take no impact damage themselves, with the default `self_damage = RESPECT_TAG`. |
| `no_teleport` | `#c:teleporting_not_supported` | The `teleport_random` behavior leaves them where they are. |

Mass also limits pickup: anything heavier than `max_mass` (1500 by default; an iron golem is about 2100) can't be
carried, tag or not.

## Block tags

`data/throwablemobs/tags/block/<name>.json`

| Tag | Default contents | Effect |
|---|---|---|
| `put_down_ground_denied` | empty | Never ground to put a mob down on, with the default `ground_filter = DENY_LIST`. |
| `put_down_ground_allowed` | empty | The only ground to put a mob down on, with `ground_filter = ALLOW_LIST`. |

## Examples

Throw the Warden, but keep everything else on our list. Replace the list and leave the Warden out:

```json
// data/throwablemobs/tags/entity_type/unthrowable.json
{
    "replace": true,
    "values": [
        { "id": "#c:bosses", "required": false },
        { "id": "#c:capturing_not_supported", "required": false },
        "minecraft:ender_dragon",
        "minecraft:wither",
        "minecraft:elder_guardian"
    ]
}
```

A mob can be in our tag through one of the `c:` tags, too: other mods (and the loaders) fill `#c:bosses`. To free such a
mob, drop the `c:` tag and list by id the ones you still want blocked. `/throwablemobs profile` doesn't show tags; try
picking the mob up instead, the prompt says why it can't be carried.

Make chickens take impact damage again:

```json
// data/throwablemobs/tags/entity_type/no_self_damage.json
{
    "replace": true,
    "values": [
        "minecraft:cat",
        "minecraft:ocelot"
    ]
}
```

Or for every mob at once, set `self_damage = ALL` in the config.

Rules for referencing tags you don't own:

- Mark them optional, so a missing tag never breaks loading: `{ "id": "#c:something", "required": false }`.
- Don't invent `c:` tags. Make your own (`#mypack:bouncy`) and reference that.
