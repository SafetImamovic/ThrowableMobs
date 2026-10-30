# Server config

Gameplay rules live in `config/throwablemobs-server.toml`. The server sends them to every player, so everyone plays by
the same rules. To use different rules for one world, put a copy in that world's `serverconfig/` folder (Fabric) or
`syncedconfig/` folder (NeoForge); it wins over the global file.

On **1.20.1**, **1.21.1** and NeoForge **26.2** the file is per world: `serverconfig/throwablemobs-server.toml` in the world's folder,
made on first load (from `defaultconfigs/throwablemobs-server.toml` if you put one there).

Each player's own settings (prompt, charge bar, trajectory style, flight pose) are in
`config/throwablemobs-client.toml` and can be changed in game with `/throwablemobs config`. With
[YACL](https://modrinth.com/mod/yacl) installed, that screen also shows this file's settings; they can be changed there
in your own world (singleplayer or LAN host), and on 26.3 from the title screen too. On 1.20.1 there is no settings
screen without YACL; edit the files instead.

Per-mob numbers and behaviors don't belong here: set them with [mob profiles](mob-profiles.md) and [tags](tags.md).

Units: masses in game-kg, distances in blocks, speeds in blocks per tick, times in ticks (20 ticks = 1 second) unless a
name says otherwise.

## Turning the mod off (`[general]`)

| Setting | Default | What it does |
|---|---|---|
| `enabled` | `true` | Off: nobody can pick up, throw or catch mobs. Carried mobs are put down and thrown mobs fall, right away. Mob profiles and the other settings are kept. |

Each player can also turn the mod off for themselves, with `enabled` in their own settings: their keys are plain Sneak
and Use again, they see no prompt, don't catch thrown mobs, and anything they carry is put down.

## When a carrier leaves the server

What happens to a carried mob when its carrier logs out, or when the server stops while they carry it.

```toml
[carry]
on_carrier_logout = "PUT_DOWN"
carrier_logout_seconds = 60
```

| `on_carrier_logout` | The mob... | If the carrier comes back |
|---|---|---|
| `PUT_DOWN` (default) | is put down at the carrier's feet and is free right away. | Nothing happens. |
| `RESUME` | is put down and free, like `PUT_DOWN`. | Within `carrier_logout_seconds`, with the mob alive and within 16 blocks: it's back on their head. |
| `KEEP_CARRYING` | waits at the carrier's feet, frozen (no AI, no gravity). No one else can pick it up. | It's back on their head if it's within 16 blocks. After `carrier_logout_seconds` it's put down. |
| `TAKE_ALONG` | leaves the world with the carrier, saved in their player data. Nothing is left behind. | Any time later: it's back on their head, wherever they log in. |

`carrier_logout_seconds` (default `60`, `0` = forever) is how long `RESUME` remembers the carrier and how long
`KEEP_CARRYING` waits for them. `PUT_DOWN` and `TAKE_ALONG` don't use it.

Good to know:

- If the mob doesn't fit at the carrier's feet (a tall mob in a low tunnel), it's put down where it was carried and
  falls.
- When the carrier **dies**, the mob is always put down at their feet, whatever the setting.
- **Server restarts:** `RESUME` forgets who left what, so after a restart it works like `PUT_DOWN`. `KEEP_CARRYING`
  mobs keep waiting, and the time limit still counts. `TAKE_ALONG` mobs stay with their player.
- `TAKE_ALONG` saves the mob in the player's data file. A player who never comes back takes the mob with them for good.
  Update Minecraft with care: a mob saved under an older version may not come back after an update.
- Changing the setting later doesn't strand anyone. A returning carrier always gets back the mob they took along, left
  behind or kept waiting.

## Pick up (`[pick_up]`)

Which mobs can never be carried is set with the tag `#throwablemobs:unthrowable`; pets only their owner can carry with
`#throwablemobs:owner_only`. See [Tags](tags.md).

| Setting | Default | What it does |
|---|---|---|
| `require_empty_hand` | `true` | Picking up needs an empty main hand. |
| `max_mass` | `1500.0` | Heaviest mob a player can lift. Mass is the hitbox volume times `default_density`: a cow is about 450, an iron golem about 2100. |
| `default_density` | `400.0` | Density used to get a mob's mass from its hitbox, game-kg per block³. |
| `allow_villagers` | `true` | Villagers and wandering traders can be picked up. |
| `owner_only_for_tamed` | `true` | Tamed mobs can only be picked up by their owner. |
| `creative_carries_anything` | `false` | Players in creative mode can pick up any mob, however heavy: `#throwablemobs:unthrowable`, `allow_villagers` and `max_mass` don't apply to them. Other players' pets, the ender dragon and players stay off limits. |
| `pick_up_cooldown_ticks` | `0` | Ticks after putting down or throwing a mob before the same player can pick one up again (20 = one second). 0 = none. Each player can set a longer one in their own settings (`[cooldown]`, 6 ticks by default). |

## Carry (`[carry]`)

| Setting | Default | What it does |
|---|---|---|
| `slowdown_factor` | `0.5` | How much a mob at `max_mass` slows its carrier. Slowdown = mass / `max_mass` × this, so 0.5 makes a cow about 15 % slower. |
| `max_slowdown` | `0.6` | Cap on the slowdown (0.6 = at most 60 % slower). |
| `protect_from_suffocation` | `true` | Carried mobs take no suffocation damage (their head is in the ceiling in 2-high tunnels). |
| `protect_from_fall` | `true` | Carried mobs take no fall damage when their carrier falls. |
| `protect_from_carrier` | `true` | Carried mobs can't be hurt by their carrier. |
| `carried_facing` | `CARRIER` | `CARRIER`: carried mobs face where the carrier looks. `KEEP`: the way they faced when picked up. |
| `on_dimension_change` | `TRAVEL` | `TRAVEL`: the mob comes along through portals and `/tp`, still carried. `PUT_DOWN`: it's put down where the carrier left. |
| `on_carrier_logout` | `PUT_DOWN` | See [When a carrier leaves the server](#when-a-carrier-leaves-the-server). |
| `carrier_logout_seconds` | `60` | See above. `0` = forever. |

## Hunger costs (`[hunger]`)

These server settings add vanilla food exhaustion, so saturation drains before visible hunger. Four exhaustion
drains one saturation point, or one food point (half a drumstick) once saturation is empty. Each cost can be set to
`0` to disable it; set all four to `0` to disable all extra hunger costs. Changes apply immediately, and the settings
also appear under **Hunger Costs** in the server config screen.

| Setting | Default | What it does |
|---|---|---|
| `pick_up_exhaustion` | `0.1` | Exhaustion per successful pickup. Failed requests and catches cost nothing. |
| `walk_exhaustion_per_block` | `0.01` | Extra exhaustion per horizontal block walked or sneaked on the ground while carrying. |
| `sprint_exhaustion_per_block` | `0.05` | Extra exhaustion per horizontal block sprinted on the ground while carrying, instead of the walking cost. Adds to vanilla sprint exhaustion. |
| `throw_exhaustion` | `0.2` | Exhaustion per successful throw, regardless of charge. Cancelled charges cost nothing. |

At the defaults, walking 400 blocks while carrying adds 4 exhaustion; sprinting 80 blocks adds the same, on top of
vanilla costs. Costs are the same for every mob. Standing still, putting down, swimming, climbing, riding and flying
add no carrying exhaustion. Creative and spectator players pay no extra costs. Hunger never blocks putting a mob down.

## Put down (`[put_down]`)

| Setting | Default | What it does |
|---|---|---|
| `put_down_needs_ground` | `true` | The carrier must look at valid ground within reach, so mobs can't be dropped off a skyblock platform. Off: put down in front of the carrier, or at their feet. |
| `ground_filter` | `DENY_LIST` | Which blocks count as ground (they also need something to stand on). `DENY_LIST`: any block except `#throwablemobs:put_down_ground_denied`. `ALLOW_LIST`: only `#throwablemobs:put_down_ground_allowed`. |
| `put_down_cooldown_ticks` | `0` | Ticks after picking up or catching a mob before it can be put down (20 = one second). 0 = none. Each player can set a longer one in their own settings (`[cooldown]`, 6 ticks by default). |

## Throw (`[throw]`)

Hold the Throw key to charge, let go to throw.

| Setting | Default | What it does |
|---|---|---|
| `throw_impulse` | `150.0` | Strength of a full-charge throw. Launch speed = impulse × charge / mass, so light mobs fly fast and heavy ones barely leave your hands (150 throws a cow at about 0.33 blocks/tick). |
| `strength_effect_bonus` | `0.25` | Extra impulse per level of Strength: 0.25 makes Strength II throw 50 % harder. 0 = no help. |
| `max_throw_speed` | `1.0` | Launch speed cap, so light mobs don't fly forever (a full-charge arrow is about 3; 1.0 throws a chicken about 19 blocks). |
| `max_charge_ticks` | `20` | Ticks to reach full charge, same as a bow. |
| `min_charge_ticks` | `3` | Letting go sooner than this cancels the throw. |
| `inherit_velocity` | `1.0` | Share of the thrower's own movement added to the throw: 1 = all of it (sprint-throws go further), 0 = none, 2 = double. |
| `thrower_grace_ticks` | `5` | Ticks after the throw before the mob can hit its thrower. |
| `allow_trajectory` | `true` | Players may see the trajectory preview (each one can still hide it). `false` turns it off for everyone. |

## Flight (`[flight]`)

| Setting | Default | What it does |
|---|---|---|
| `gravity` | `0.05` | Downward acceleration in blocks/tick² (arrows use 0.05). |
| `drag` | `0.99` | Share of the speed kept each tick (arrows use 0.99; lower = more air drag). |
| `max_flight_ticks` | `200` | Flights end after this many ticks; the mob then falls normally. |
| `ai_in_flight` | `false` | Thrown mobs get their AI back at launch instead of when they land (chickens flap). Their own movement can then bend the flight a little. |
| `block_restitution` | `0.0` | Bounciness off blocks: 0 = the flight ends at the first block, 1 = keeps all its speed. Mob profiles can override it. |
| `max_bounces` | `3` | Bounces off blocks before the flight ends. Mob profiles can override it. |
| `min_bounce_speed` | `0.15` | A bounce slower than this ends the flight, so mobs stop instead of skipping along forever. |
| `bounce_impacts` | `EVERY` | Whether bounces after the first impact deal damage too: `EVERY` or `FIRST_ONLY`. Mob profiles can override it. |

## Impact (`[impact]`)

Damage comes from the collision energy, ½ × reduced mass × speed², in game-kg × (blocks/tick)².

| Setting | Default | What it does |
|---|---|---|
| `damage_per_energy` | `0.1` | Damage per unit of collision energy above `min_impact_energy`. |
| `min_impact_energy` | `10.0` | Collisions below this energy do no damage. |
| `max_impact_damage` | `20.0` | Damage cap for one impact, before it's split between the two bodies (the lighter one takes more). |
| `restitution` | `0.3` | Bounciness between bodies: 0 = they stick together, 1 = perfectly elastic. Sets the knockback. |
| `vanilla_fall_damage` | `false` | Thrown mobs also take vanilla fall damage when they land, on top of impact damage. Off: impact damage replaces it during the flight. |
| `self_damage` | `RESPECT_TAG` | Whether thrown mobs take damage from their own impacts: `ALL`, `NONE`, or `RESPECT_TAG` (all but `#throwablemobs:no_self_damage`, by default mobs immune to fall damage). |
| `pvp` | `true` | Thrown mobs can hurt and knock back players. Off: they can't, and players can still catch them. Teams without friendly fire are always safe from their teammates' throws. |

## Catch (`[catch]`)

Catching needs an empty main hand, not carrying anything, looking at the mob, and being allowed to pick it up.

| Setting | Default | What it does |
|---|---|---|
| `catching_enabled` | `true` | Off: thrown mobs can't be caught and hit players as usual. |
| `catch_radius` | `1.5` | How close the mob's path must pass the catcher's eyes, in blocks. |
| `catch_cone_degrees` | `60.0` | The catcher must look within this many degrees of the mob. |
| `self_catch` | `AFTER_BOUNCE` | When the thrower can catch their own mob: `NEVER`, `AFTER_BOUNCE` (it bounced off a block), `AFTER_DELAY`, or `AFTER_BOUNCE_OR_DELAY`. |
| `self_catch_delay_ticks` | `10` | Ticks before the thrower can catch their own mob, with `AFTER_DELAY` or `AFTER_BOUNCE_OR_DELAY`. |
| `latency_compensation` | `1.0` | Players with ping see the mob late, so the radius grows by speed × ping in ticks × this. 0 = off. |
| `max_latency_ticks` | `6` | Ping above this many ticks counts as this much. |
| `catch_recoil` | `0.002` | Knockback on the catcher per unit of the mob's momentum (mass × speed). |
| `catch_damage` | `false` | Hard catches hurt the catcher. |
| `catch_absorb_momentum` | `200.0` | Momentum a catcher takes without damage. |
| `catch_damage_factor` | `0.02` | Damage per unit of momentum above `catch_absorb_momentum`. |

## Effects (`[effects]`)

Sounds and particles on throws, bounces, impacts and catches, for everyone nearby. The flight trail is a player
setting.

| Setting | Default | What it does |
|---|---|---|
| `sounds` | `true` | Play our sounds. Resource packs can replace them one by one ([sound events](advancements.md)). |
| `particles` | `true` | A puff on throws and catches, block dust on impacts. |
| `effect_min_energy` | `2.0` | Bounces and impacts below this collision energy are silent, so a mob rolling along doesn't keep thudding. |
| `effect_full_energy` | `40.0` | Collision energy for full volume and the most particles. A hard creeper throw reaches about 46. |

## Behaviors (`[behaviors]`)

What mobs do when picked up, thrown and when they hit something. Set per mob with [mob profiles](mob-profiles.md); see
[Behaviors](behaviors.md) for every action.

| Setting | Default | What it does |
|---|---|---|
| `behaviors_enabled` | `true` | Off: no mob behaviors run. Mob profile physics (mass, gravity, bounces) still applies. |
| `auto_detect_behaviors` | `true` | Guess behaviors for modded mobs from what they are (see [Modded mobs](modded-mobs.md)). A mob profile overrides the guess. |
| `disabled_behaviors` | `[]` | Behavior types that never run, e.g. `["throwablemobs:explode"]`. |
| `egg_chance` | `0.25` | Chance a thrown chicken lays an egg when it lands. `0` = never, `1` = every throw. Rolled once per throw. |
