# Biome Rules

Biome rules define mob levels based on which biome they spawn in. They support both specific biome IDs and biome tags for broad matching.

## Location

```
data/botzmobleveling/mob_levels/biomes/<rulename>.json
```

## Basic Structure

```json
{
  "biome": "minecraft:desert",
  "biome_tags": [],
  "priority": 50,
  "enabled": true,
  "level_range": {
    "min": 1,
    "max": 100
  },
  "level_mode": "distance",
  "fixed_level": null,
  "ignore_distance_scaling": false,
  "distance_multiplier": 1.0,
  "mob_overrides": {},
  "attribute_scaling": {}
}
```

## Field Reference

### Targeting Fields

| Field | Type | Description |
|-------|------|-------------|
| `biome` | String | Specific biome ID (e.g., `minecraft:desert`) |
| `biome_tags` | Array | List of biome tags for broad matching |

**Note:** You can use `biome`, `biome_tags`, or both. If both are specified, the rule matches if either condition is met.

### Optional Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `priority` | Integer | `50` | Higher values = higher priority |
| `enabled` | Boolean | `true` | Toggle this rule on/off |
| `level_range` | Object | `{min: 1, max: 100}` | Level range for random mode |
| `level_mode` | String | `"distance"` | One of: `fixed`, `random`, `distance` |
| `fixed_level` | Integer | `null` | Exact level (when mode is `fixed`) |
| `ignore_distance_scaling` | Boolean | `false` | Ignore distance from spawn |
| `distance_multiplier` | Double | `1.0` | Multiplier for distance-based levels |
| `mob_overrides` | Object | `{}` | Per-mob level overrides |
| `attribute_scaling` | Object | `{}` | Custom attribute bonuses |

## Biome Matching

### Specific Biome

Match a single, specific biome:

```json
{
  "biome": "minecraft:desert",
  "level_mode": "fixed",
  "fixed_level": 30
}
```

### Biome Tags

Match multiple biomes using tags. Supports vanilla and modded biome tags:

```json
{
  "biome_tags": [
    "minecraft:is_nether"
  ],
  "level_mode": "fixed",
  "fixed_level": 75
}
```

### Combined Matching

Match both a specific biome AND biomes with certain tags:

```json
{
  "biome": "minecraft:badlands",
  "biome_tags": [
    "minecraft:is_badlands"
  ],
  "level_range": {
    "min": 40,
    "max": 80
  }
}
```

## Common Vanilla Biome IDs

### Overworld

| Biome | ID |
|-------|-----|
| Plains | `minecraft:plains` |
| Desert | `minecraft:desert` |
| Forest | `minecraft:forest` |
| Taiga | `minecraft:taiga` |
| Swamp | `minecraft:swamp` |
| Jungle | `minecraft:jungle` |
| Beach | `minecraft:beach` |
| Mountains | `minecraft:stony_peaks` |
| Ocean | `minecraft:ocean` |
| Deep Dark | `minecraft:deep_dark` |

### Nether

| Biome | ID |
|-------|-----|
| Nether Wastes | `minecraft:nether_wastes` |
| Crimson Forest | `minecraft:crimson_forest` |
| Warped Forest | `minecraft:warped_forest` |
| Soul Sand Valley | `minecraft:soul_sand_valley` |
| Basalt Deltas | `minecraft:basalt_deltas` |

### End

| Biome | ID |
|-------|-----|
| The End | `minecraft:the_end` |
| End Highlands | `minecraft:end_highlands` |
| End Midlands | `minecraft:end_midlands` |
| End Barrens | `minecraft:end_barrens` |
| Small End Islands | `minecraft:small_end_islands` |

## Common Biome Tags

| Tag | Description |
|-----|-------------|
| `minecraft:is_overworld` | All overworld biomes |
| `minecraft:is_nether` | All nether biomes |
| `minecraft:is_end` | All end biomes |
| `minecraft:is_ocean` | Ocean biomes |
| `minecraft:is_deep_ocean` | Deep ocean biomes |
| `minecraft:is_forest` | Forest biomes |
| `minecraft:is_jungle` | Jungle biomes |
| `minecraft:is_badlands` | Badlands biomes |
| `minecraft:is_taiga` | Taiga biomes |
| `minecraft:is_hill` | Hilly biomes |
| `minecraft:is_mountain` | Mountain biomes |
| `minecraft:has_structure/village_plains` | Biomes that can have plains villages |

## Complete Examples

### Nether-Wide Rule

All mobs in the Nether get increased levels:

```json
{
  "biome_tags": ["minecraft:is_nether"],
  "priority": 60,
  "level_mode": "distance",
  "level_range": {
    "min": 50,
    "max": 150
  },
  "distance_multiplier": 2.0,
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 20.0,
      "per_level": 1.0,
      "operation": "addition"
    }
  }
}
```

### Desert Difficulty Spike

High-level skeletons in deserts:

```json
{
  "biome": "minecraft:desert",
  "priority": 75,
  "level_mode": "fixed",
  "fixed_level": 1000,
  "ignore_distance_scaling": true,
  "mob_overrides": {
    "minecraft:skeleton": {
      "fixed_level": 1000,
      "attribute_scaling": {
        "minecraft:generic.max_health": {
          "base_bonus": 100.0,
          "per_level": 1.0,
          "operation": "addition"
        },
        "minecraft:generic.attack_damage": {
          "base_bonus": 0.0,
          "per_level": 0.1,
          "operation": "multiply_base"
        }
      }
    }
  }
}
```

### Ocean Progressive Difficulty

Deeper oceans = higher level mobs:

```json
{
  "biome_tags": ["minecraft:is_ocean"],
  "priority": 50,
  "level_mode": "random",
  "level_range": {
    "min": 10,
    "max": 40
  }
}
```

```json
{
  "biome_tags": ["minecraft:is_deep_ocean"],
  "priority": 55,
  "level_mode": "random",
  "level_range": {
    "min": 40,
    "max": 80
  }
}
```

## Distance Scaling

By default, biome rules use distance scaling. This means:

1. Mobs near spawn get levels closer to `level_range.min`
2. Mobs far from spawn get levels closer to `level_range.max`
3. The `distance_multiplier` adjusts how fast levels increase

### Disabling Distance Scaling

For fixed difficulty zones:

```json
{
  "biome": "minecraft:deep_dark",
  "ignore_distance_scaling": true,
  "level_mode": "fixed",
  "fixed_level": 100
}
```

## Priority with Structure Rules

Remember: Structure rules have higher default priority (100) than biome rules (50).

- A mob in a stronghold in a desert will use the **stronghold rule**
- To make biome rules take precedence, increase their priority above 100

```json
{
  "biome": "minecraft:the_end",
  "priority": 150,
  "level_mode": "fixed",
  "fixed_level": 200
}
```

## Tips

1. **Use biome tags** for broad rules (all nether, all oceans)
2. **Use specific biomes** for unique difficulty spikes
3. **Distance scaling works well** for gradual difficulty
4. **Consider structure interactions** when setting priorities
5. **Test with the Nether** - it's isolated from the overworld
