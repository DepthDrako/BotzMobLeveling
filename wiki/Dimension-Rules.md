# Dimension Rules

Dimension rules let you set mob levels for an entire dimension — the Overworld, Nether, End, or any modded dimension. They sit **below Biome rules and above Base rules** in priority, making them perfect for dimension-wide difficulty baselines.

## Priority Chain

```
Structure  (highest)
   ↓
Biome
   ↓
Dimension  ← this is where dimension rules apply
   ↓
Base       (lowest)
```

**Example:** A zombie in a Nether biome called `nether_wastes` with both a biome rule and a dimension rule will use the **biome rule** because it is more specific. The dimension rule acts as the fallback for biomes with no dedicated rule.

## Location

```
data/botzmobleveling/mob_levels/dimensions/<rulename>.json
```

## Basic Structure

```json
{
  "dimension": "minecraft:the_nether",
  "priority": 30,
  "enabled": true,
  "level_range": {
    "min": 50,
    "max": 150
  },
  "level_mode": "distance",
  "fixed_level": null,
  "ignore_distance_scaling": false,
  "distance_multiplier": 1.5,
  "mob_overrides": {},
  "attribute_scaling": {}
}
```

## Field Reference

### Targeting Fields

| Field | Type | Description |
|-------|------|-------------|
| `dimension` | String | Single dimension ID (e.g., `minecraft:the_nether`) |
| `dimensions` | Array | List of dimension IDs — rule applies to all of them |

You must provide at least one of `dimension` or `dimensions`. If you provide both, the rule applies to all listed dimensions.

### Optional Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `priority` | Integer | `30` | Higher values = higher priority when multiple dimension rules match |
| `enabled` | Boolean | `true` | Toggle this rule on/off |
| `level_range` | Object | `{min: 1, max: 100}` | Level range for `random` or `distance` mode |
| `level_mode` | String | `"distance"` | One of: `fixed`, `random`, `distance` |
| `fixed_level` | Integer | `null` | Exact level for all mobs (when mode is `fixed`) |
| `ignore_distance_scaling` | Boolean | `false` | Ignore distance from spawn |
| `distance_multiplier` | Double | `1.0` | Scale how fast levels grow with distance |
| `mob_overrides` | Object | `{}` | Per-mob level and attribute overrides |
| `attribute_scaling` | Object | `{}` | Custom attribute bonuses per level |

## Vanilla Dimension IDs

| Dimension | ID |
|-----------|----|
| Overworld | `minecraft:overworld` |
| The Nether | `minecraft:the_nether` |
| The End | `minecraft:the_end` |

## Modded Dimension Examples

Most mods follow the pattern `modid:dimensionname`:

| Mod | Dimension | ID |
|-----|-----------|-----|
| Twilight Forest | Twilight Forest | `twilightforest:twilight_forest` |
| Aether | The Aether | `aether:the_aether` |
| Blue Skies | Everbright | `blue_skies:everbright` |
| Undergarden | The Undergarden | `undergarden:undergarden` |

Check your mod's documentation or use the `/execute in` command tab-complete to find exact dimension IDs.

## Examples

### Nether Baseline Difficulty

All mobs in the Nether are at least level 50, scaling up to 150 with distance:

```json
{
  "dimension": "minecraft:the_nether",
  "priority": 30,
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
    },
    "minecraft:generic.attack_damage": {
      "base_bonus": 0.0,
      "per_level": 0.03,
      "operation": "multiply_base"
    }
  }
}
```

### End Fixed Difficulty

Every mob in the End is a fixed level 200, regardless of distance:

```json
{
  "dimension": "minecraft:the_end",
  "priority": 30,
  "level_mode": "fixed",
  "fixed_level": 200,
  "ignore_distance_scaling": true,
  "mob_overrides": {
    "minecraft:enderman": {
      "fixed_level": 250,
      "attribute_scaling": {
        "minecraft:generic.max_health": {
          "base_bonus": 60.0,
          "per_level": 1.0,
          "operation": "addition"
        }
      }
    },
    "minecraft:shulker": {
      "fixed_level": 220
    }
  },
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 50.0,
      "per_level": 1.0,
      "operation": "addition"
    }
  }
}
```

### One Rule Covering Multiple Dimensions

Apply the same rule to both the Nether and a modded dimension:

```json
{
  "dimensions": [
    "minecraft:the_nether",
    "undergarden:undergarden"
  ],
  "priority": 25,
  "level_mode": "random",
  "level_range": {
    "min": 60,
    "max": 120
  },
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 30.0,
      "per_level": 0.8,
      "operation": "addition"
    }
  }
}
```

### Overworld Scaling Boost

The Overworld already uses distance scaling by default, but you can override it here:

```json
{
  "dimension": "minecraft:overworld",
  "priority": 20,
  "level_mode": "distance",
  "level_range": {
    "min": 1,
    "max": 200
  },
  "distance_multiplier": 1.5,
  "mob_overrides": {
    "minecraft:zombie": {
      "level_bonus": 10
    },
    "minecraft:skeleton": {
      "level_bonus": 15
    }
  }
}
```

### Modded Dimension (Twilight Forest)

```json
{
  "dimension": "twilightforest:twilight_forest",
  "priority": 30,
  "level_mode": "random",
  "level_range": {
    "min": 75,
    "max": 175
  },
  "ignore_distance_scaling": true,
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 40.0,
      "per_level": 1.2,
      "operation": "addition"
    },
    "minecraft:generic.attack_damage": {
      "base_bonus": 3.0,
      "per_level": 0.05,
      "operation": "addition"
    }
  }
}
```

## Interaction with Other Rule Types

### Dimension + Biome Rules Together

Dimension rules are great for setting a **floor** while biome rules add spikes:

`dimensions/nether_baseline.json` — Level 50-100 everywhere in the Nether
```json
{
  "dimension": "minecraft:the_nether",
  "level_range": { "min": 50, "max": 100 }
}
```

`biomes/crimson_forest.json` — Level 120-180 specifically in the Crimson Forest (biome rule wins here)
```json
{
  "biome": "minecraft:crimson_forest",
  "priority": 70,
  "level_range": { "min": 120, "max": 180 }
}
```

In the Crimson Forest: biome rule applies (level 120-180).
In the Nether Wastes (no biome rule): dimension rule applies (level 50-100).

### Dimension + Structure Rules Together

Structure rules always win over dimension rules, so you can have a dungeon with its own leveling inside a dimension:

`dimensions/end_baseline.json` — Level 200 in the End
```json
{
  "dimension": "minecraft:the_end",
  "level_mode": "fixed",
  "fixed_level": 200
}
```

`structures/end_city.json` — Level 300-400 inside End Cities (structure rule wins)
```json
{
  "structure": "minecraft:end_city",
  "priority": 140,
  "level_range": { "min": 300, "max": 400 }
}
```

## Config Toggle

Dimension rules can be disabled globally in the config:

```toml
[ruleToggles]
dimensionLevelingEnabled = true
```

## Tips

1. **Use dimension rules as baselines** — let biome rules add fine-grained variation on top
2. **Keep priorities low** (20-40 range) so biome rules naturally override with higher defaults
3. **Use `dimensions` array** when multiple dimensions share the same difficulty profile
4. **Avoid `ignore_distance_scaling`** for the Overworld — distance scaling makes the world feel progressive
5. **Always test `/reload`** after changes — no restart needed
