# Dimension Rules

Whole-dimension baselines. Checked after biome rules and before base rules — ideal
for a dimension-wide difficulty floor that biome/structure rules build on top of.

## Priority Chain

```
boss → structure → biome → DIMENSION → base → fallback
```

A mob in a biome that has its own biome rule uses that biome rule; the dimension
rule covers everything else in the dimension.

## Location

```
data/botzmobleveling/mob_levels/dimensions/<rulename>.json
```

## Fields

Dimension rules use the [common rule fields](Datapack-Structure#common-rule-fields).
The targeting field is:

| Field | Type | Description |
|-------|------|-------------|
| `dimension` | String | Dimension id, full (`minecraft:the_nether`) or path (`the_nether`). |

> **One dimension per rule.** The 1.20.1 `dimensions` array is not supported — make
> one file per dimension.

## Vanilla Dimension IDs

| Dimension | ID |
|-----------|----|
| Overworld | `minecraft:overworld` |
| The Nether | `minecraft:the_nether` |
| The End | `minecraft:the_end` |

Modded dimensions follow `modid:dimension` (e.g. `twilightforest:twilight_forest`).

## Examples

### Nether baseline (random)
```json
{ "dimension": "minecraft:the_nether", "mode": "random", "min_level": 50, "max_level": 150, "priority": 30 }
```

### Flat End difficulty with an override
```json
{
  "dimension": "minecraft:the_end",
  "mode": "fixed",
  "level": 200,
  "mob_overrides": {
    "minecraft:enderman": { "level": 250 },
    "minecraft:shulker":  { "level_bonus": 20 }
  },
  "attribute_scaling": {
    "max_health": { "operation": "add_value", "value_per_level": 1.0 }
  }
}
```

### Distance scaling in the Overworld
```json
{ "dimension": "minecraft:overworld", "mode": "distance", "min_level": 1, "max_level": 200, "distance_scale": 0.02 }
```

`distance` mode uses the configured origin and global scale factor — see
[Configuration → Distance scaling](Configuration#distance-scaling).

## Tips

- Keep dimension `priority` low so biome rules naturally take precedence where they exist.
- Use a dimension rule as the floor and biome/structure rules for hot spots.
