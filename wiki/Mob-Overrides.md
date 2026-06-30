# Mob Overrides

`mob_overrides` apply per-entity tweaks **inside** any rule, so one rule can treat
specific mobs differently without authoring a separate rule.

## Location

Nested in any rule, keyed by entity id (full `minecraft:zombie` or path `zombie`):

```json
{
  "structure": "minecraft:stronghold",
  "mode": "random",
  "min_level": 50,
  "max_level": 100,
  "mob_overrides": {
    "minecraft:zombie":   { "level": 200, "ignore_level_cap": true },
    "minecraft:skeleton": { "level_bonus": 50 }
  }
}
```

## Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `level_bonus` | Integer | `0` | Added on top of the rule's resolved level. |
| `level` | Integer | *(none)* | Fixed level that **replaces** the rule's level for this mob (takes precedence over `level_bonus`). |
| `ignore_level_cap` | Boolean | `false` | This mob may exceed the global cap (persisted across reloads). |
| `attribute_scaling` | Object | `{}` | Per-attribute scaling for this mob — merged **over** the rule's `attribute_scaling` (override wins per attribute). See [Attribute Scaling](Attribute-Scaling). |

> Only these four fields exist in 1.21.1. The 1.20.1 `min_level`/`max_level`/
> `level_range`, `can_attack`, and `attribute_multipliers` override fields are **not**
> supported. (Passive-mob combat is global now — see below.)

## How the Level Is Computed

For a mob that matches an override:

1. Start from the rule's resolved **base** level (range-clamped for `random`/`distance`).
2. If the override sets `level`, use it; otherwise add `level_bonus`.
3. Add the adaptive-difficulty bonus (if enabled).
4. Clamp to the global cap — **unless** the rule or override sets `ignore_level_cap`.

## Examples

### Fixed level for one mob
```json
{ "structure": "minecraft:stronghold", "mode": "fixed", "level": 80,
  "mob_overrides": { "minecraft:enderman": { "level": 300, "ignore_level_cap": true } } }
```

### Bonus on top of a range
```json
{ "biome": "minecraft:nether_wastes", "mode": "random", "min_level": 30, "max_level": 60,
  "mob_overrides": { "minecraft:wither_skeleton": { "level_bonus": 40 } } }
```

### Per-mob attribute scaling
```json
{
  "structure": "minecraft:fortress",
  "mode": "fixed",
  "level": 120,
  "attribute_scaling": { "max_health": 1.0 },
  "mob_overrides": {
    "minecraft:blaze": {
      "level_bonus": 20,
      "attribute_scaling": {
        "max_health": 2.0,
        "attack_damage": { "operation": "multiply_base", "value_per_level": 0.01 }
      }
    }
  }
}
```
Blaze uses its own `max_health` op (2.0/level, overriding the rule's 1.0) and adds
the `attack_damage` op; other mobs use the rule's scaling.

## Leveling Passive Mobs

There is no per-override `can_attack`. Instead:

- Set `levelPassiveMobs = true` in the config to allow passive mobs to be leveled,
  or scope a rule to them with `passive_only` / `entity`.
- Leveled passive mobs automatically gain retaliation combat AI (so they fight back
  when hit), governed by the hunting-AI config.

See [Configuration](Configuration).
