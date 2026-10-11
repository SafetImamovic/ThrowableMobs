# Java API

For mod devs who want their mobs to behave well when thrown, or to add new behavior types pack devs can use. Everything
is in `io.github.safetimamovic.throwablemobs.api` and works the same on Fabric and NeoForge; call it from your common
init. Datapack profiles merge on top of anything registered here, so pack devs keep the final say.

> **Dependency:** a separate `-api` artifact on Maven comes with the 1.0 release (M6). Until then, depend on the mod
> jar `compileOnly` and treat the API as subject to change.

## Three entry points

| Call | Use it for |
|---|---|
| `ThrowableMobsApi.registerProfile(EntityType, MobProfile)` | Mass, gravity, bounciness, damage multipliers for your mob |
| `ThrowableMobsApi.registerBehavior(EntityType, ThrowBehavior)` | Custom code that runs on pickup, throw, flight, impact, landing |
| `ThrowableMobsApi.registerBehaviorType(BehaviorType)` | A new action pack devs can use in JSON, like `"type": "examplemod:lightning"` |

Or implement `ThrowableMob` on your entity class instead of registering anything.

## Profiles from code

```java
ThrowableMobsApi.registerProfile(ExampleEntities.FIRE_DRAKE, MobProfile.builder()
        .mass(800.0)
        .gravityMultiplier(0.6)
        .selfMultiplier(0.0)
        .build());
```

Every builder field is optional, like in JSON. Registering again for the same type merges on top.

## A behavior in code

`ThrowBehavior` has a default for every hook, so override only what you need. All hooks run on the server thread. A
behavior that throws is logged and skipped; it can't crash the server or leave a mob stuck in the air.

```java
public final class FireDrakeBehavior implements ThrowBehavior
{
        @Override
        public void onFlightTick(ThrowContext ctx)
        {
                Mob mob = ctx.mob();
                ctx.level().sendParticles(ParticleTypes.FLAME, mob.getX(), mob.getY(), mob.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
        }

        @Override
        public ImpactResult onImpact(ImpactContext ctx, ImpactResult current)
        {
                if (ctx.target() != null)
                {
                        ctx.target().igniteForSeconds(5.0F);
                        // Twice the damage to what it hits, none to itself.
                        return current.withTargetDamage(current.targetDamage() * 2.0).withSelfDamage(0.0);
                }
                return current;
        }
}

ThrowableMobsApi.registerBehavior(ExampleEntities.FIRE_DRAKE, new FireDrakeBehavior());
```

| Hook | When |
|---|---|
| `onPickup(ctx)` | Picked up |
| `onCarryTick(ctx)` | Every tick while carried |
| `onThrow(ctx)` | Thrown; `ctx.velocity()` is the launch velocity |
| `onFlightTick(ctx)` | Every tick in the air, before moving |
| `onImpact(ctx, current)` | Hit a block or entity; return a changed `ImpactResult` |
| `onLand(ctx)` | The flight ended and the mob has its AI back |

`ThrowContext` gives the mob, the server level, the player (`null` in flight if the thrower logged out), the velocity
and the ticks since the throw. `ImpactContext` adds the hit entity (`null` for a block), the relative velocity, the
collision energy and which impact of the flight it is (0 = first, then bounces).

`ImpactResult` is immutable: `withTargetDamage`, `withSelfDamage`, `withKnockbackMultiplier`, `bounce(velocity)` to keep
flying after a block hit, `land()` to end the flight. Behaviors run in order, each on the result of the one before. If
your behavior removes the mob (`discard()`), the flight ends there; the hit entity still takes its damage.

## A behavior type for JSON

A `BehaviorAction` is the building block pack devs list in profiles. The profile decides the trigger and conditions;
the action only does the work. Its codec reads the fields next to `"type"`.

```java
public record LightningAction(boolean onlyInRain) implements BehaviorAction
{
        public static final MapCodec<LightningAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("only_in_rain", false).forGetter(LightningAction::onlyInRain))
                .apply(instance, LightningAction::new));

        public static final BehaviorType<LightningAction> TYPE = new BehaviorType<>(
                Identifier.fromNamespaceAndPath("examplemod", "lightning"), CODEC);

        @Override
        public BehaviorType<?> type()
        {
                return TYPE;
        }

        @Override
        public void run(ThrowContext ctx)
        {
                if (onlyInRain && !ctx.level().isRaining())
                {
                        return;
                }
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(ctx.level(), EntitySpawnReason.TRIGGERED);
                if (bolt != null)
                {
                        bolt.snapTo(ctx.mob().position());
                        ctx.level().addFreshEntity(bolt);
                }
        }
}

// In common init, before datapacks load:
ThrowableMobsApi.registerBehaviorType(LightningAction.TYPE);
```

Pack devs can now write:

```json
{ "type": "examplemod:lightning", "trigger": "impact", "only_in_rain": true, "chance": 0.5 }
```

Override `onImpact(ctx, current)` too if the action should change the impact (cancel damage, end the flight); the
default runs `run(ctx)` and leaves the impact alone. Register types during init: profiles that use an unknown type fail
to load.

## `ThrowableMob`

Implement it on your entity to describe itself without registering anything:

```java
public class FireDrake extends Monster implements ThrowableMob
{
        @Override
        public @Nullable ThrowBehavior getThrowBehavior()
        {
                return new FireDrakeBehavior();
        }

        @Override
        public @Nullable MobProfile getThrowProfile()
        {
                return MobProfile.builder().mass(800.0).build();
        }
}
```

It's a layer below Java registration and datapacks, and above the guesses for modded mobs. Both methods are called on
the client too (for the trajectory preview), so keep them cheap and side-safe.

## Order of layers

Lowest first: defaults, guesses for modded mobs, `ThrowableMob`, `registerProfile` / `registerBehavior`, datapack files
by priority. See [Merging](mob-profiles.md#merging).

## Coming later

Cancellable events (`PickupEvent`, `ThrowEvent`, `ImpactEvent`) for claim and protection mods, a
`throwablemobs:throw_strength` attribute for gear, and catching hooks (`onCaught`).
