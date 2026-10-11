# Modded mobs

Every mob can be picked up and thrown unless a tag or its mass says otherwise, modded ones included. Without a profile,
a modded mob gets its mass from its hitbox and, if the mod can tell what it is, a guessed set of behaviors.

## Guesses

With `auto_detect_behaviors = true` (default), a modded mob without its own behaviors gets them from the vanilla mob it's
built on:

| The mob is a... | It gets |
|---|---|
| Creeper (extends `Creeper`) | `explode` on impacts from energy 20 |
| Slime or magma cube (extends `AbstractCubeMob`) | `block_restitution 0.8`, `max_bounces 5`, `split` on impacts from energy 15 |
| Enderman (extends `Enderman`) | `teleport_random` and `anger_thrower` on impact |
| Fire immune (`fireImmune()`) | `ignite` what it hits, 4 seconds |

A mob can match more than one row and gets all of them, like a fire-immune slime.

Guesses are the lowest layer above the defaults, so any profile for the mob merges on top: a profile that only sets
`mass` keeps the guessed behaviors; one with `"behaviors": []` removes them. Vanilla mobs never get guesses; our shipped
profiles cover them.

Check what a mob got with `/throwablemobs profile <mob>`: guessed values show `(heuristics)` as their source.

## Turning guesses off

- For everything: `auto_detect_behaviors = false` in `config/throwablemobs-server.toml`.
- For one mod or mob: a profile with `"replace": true` drops the guess and everything else below it.

```json
// data/mypack/throwablemobs/mob_profile/twilight_slimes.json
{
    "entities": ["twilightforest:maze_slime"],
    "replace": true
}
```

## Checklist for a mob mod

1. Pick one up and throw it. Too heavy? Its hitbox is big; set `mass` in a profile or raise `max_mass`.
2. Does it do something silly or break? Look at `/throwablemobs profile` for guessed behaviors and remove them.
3. Bosses: check that the mod puts them in `#c:bosses`. If not, add them to `#throwablemobs:unthrowable`.
4. Multipart mobs (several hitboxes, like the ender dragon) don't follow a carrier well. Make them unthrowable.

The [cookbook](cookbook.md) has worked examples for Twilight Forest and Alex's Mobs.
