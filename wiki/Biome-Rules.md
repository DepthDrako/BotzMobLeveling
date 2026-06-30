# Biome Rules

Rules for mobs based on the biome they spawn in. Checked after structure rules and
before dimension rules.

## Location

```
data/botzmobleveling/mob_levels/biomes/<rulename>.json
```

## Fields

Biome rules use the [common rule fields](Datapack-Structure#common-rule-fields).
The targeting field is:

| Field | Type | Description |
|-------|------|-------------|
| `biome` | String | Biome id, full (`minecraft:desert`) or path (`desert`). |

> **One biome per rule.** Biome **tags** and biome **arrays** are not supported in
> 1.21.1 — create one rule per biome (or use a [Dimension Rule](Dimension-Rules) for
> a whole-dimension baseline).

## Examples

### Fixed biome difficulty
```json
{ "biome": "minecraft:deep_dark", "mode": "fixed", "level": 100 }
```

### Random range
```json
{ "biome": "minecraft:desert", "mode": "random", "min_level": 20, "max_level": 50, "priority": 50 }
```

### Hostiles only, with scaling
```json
{
  "biome": "minecraft:crimson_forest",
  "hostile_only": true,
  "mode": "random",
  "min_level": 60,
  "max_level": 120,
  "attribute_scaling": {
    "max_health": 1.5,
    "armor": { "operation": "add_value", "value_per_level": 0.25 }
  }
}
```

## Common Biome IDs

| Biome | ID |
|-------|-----|
| Plains | `minecraft:plains` |
| Desert | `minecraft:desert` |
| Deep Dark | `minecraft:deep_dark` |
| Nether Wastes | `minecraft:nether_wastes` |
| Crimson Forest | `minecraft:crimson_forest` |
| Warped Forest | `minecraft:warped_forest` |
| Soul Sand Valley | `minecraft:soul_sand_valley` |
| The End | `minecraft:the_end` |
| End Highlands | `minecraft:end_highlands` |

## Priority vs Structure Rules

Structure rules are resolved **before** biome rules regardless of `priority`. A mob
in a stronghold that sits in a desert uses the stronghold rule. Use a dedicated
[Structure Rule](Structure-Rules) if you need structure-specific behavior; biome
`priority` only orders rules **within** the biome category.

## Tips

- Pair a low-priority [Dimension Rule](Dimension-Rules) baseline with biome rules for spikes.
- Use `entity` to make one biome dangerous only for a specific mob.
