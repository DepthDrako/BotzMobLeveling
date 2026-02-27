# Structure Rules

Structure rules define mob levels for specific Minecraft structures like strongholds, nether fortresses, and ocean monuments.

## Location

```
data/botzmobleveling/mob_levels/structures/<rulename>.json
```

## Basic Structure

```json
{
  "structure": "minecraft:stronghold",
  "priority": 100,
  "enabled": true,
  "level_range": {
    "min": 50,
    "max": 100
  },
  "level_mode": "random",
  "fixed_level": null,
  "ignore_distance_scaling": true,
  "distance_multiplier": 1.0,
  "mob_overrides": {},
  "attribute_scaling": {}
}
```

## Field Reference

### Required Fields

| Field | Type | Description |
|-------|------|-------------|
| `structure` | String | The structure ID (e.g., `minecraft:stronghold`) |

### Optional Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `priority` | Integer | `100` | Higher values = higher priority |
| `enabled` | Boolean | `true` | Toggle this rule on/off |
| `level_range` | Object | `{min: 1, max: 100}` | Level range for random mode |
| `level_mode` | String | `"random"` | One of: `fixed`, `random`, `distance` |
| `fixed_level` | Integer | `null` | Exact level (when mode is `fixed`) |
| `ignore_distance_scaling` | Boolean | `true` | Ignore distance from spawn |
| `distance_multiplier` | Double | `1.0` | Multiplier for distance-based levels |
| `mob_overrides` | Object | `{}` | Per-mob level overrides |
| `attribute_scaling` | Object | `{}` | Custom attribute bonuses |

## Level Modes

### Fixed Mode
All mobs in the structure get the exact same level.

```json
{
  "structure": "minecraft:stronghold",
  "level_mode": "fixed",
  "fixed_level": 100
}
```

### Random Mode
Mobs get a random level within the specified range.

```json
{
  "structure": "minecraft:stronghold",
  "level_mode": "random",
  "level_range": {
    "min": 50,
    "max": 150
  }
}
```

### Distance Mode
Level scales based on distance from world spawn, useful for structures that generate at varying distances.

```json
{
  "structure": "minecraft:mineshaft",
  "level_mode": "distance",
  "distance_multiplier": 1.5
}
```

## Vanilla Structure IDs

| Structure | ID |
|-----------|-----|
| Stronghold | `minecraft:stronghold` |
| Nether Fortress | `minecraft:fortress` |
| Bastion Remnant | `minecraft:bastion_remnant` |
| Ocean Monument | `minecraft:monument` |
| Woodland Mansion | `minecraft:mansion` |
| End City | `minecraft:end_city` |
| Desert Pyramid | `minecraft:desert_pyramid` |
| Jungle Pyramid | `minecraft:jungle_pyramid` |
| Pillager Outpost | `minecraft:pillager_outpost` |
| Mineshaft | `minecraft:mineshaft` |
| Village | `minecraft:village_plains`, `minecraft:village_desert`, etc. |
| Ruined Portal | `minecraft:ruined_portal` |
| Ancient City | `minecraft:ancient_city` |
| Trail Ruins | `minecraft:trail_ruins` |

## Complete Example

A comprehensive stronghold rule with mob-specific overrides:

```json
{
  "structure": "minecraft:stronghold",
  "priority": 200,
  "enabled": true,
  "level_range": {
    "min": 100,
    "max": 150
  },
  "level_mode": "random",
  "ignore_distance_scaling": true,
  "mob_overrides": {
    "minecraft:zombie": {
      "fixed_level": 200,
      "ignore_level_cap": true
    },
    "minecraft:skeleton": {
      "level_bonus": 50
    },
    "minecraft:chicken": {
      "fixed_level": 500,
      "can_attack": true
    }
  },
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 50.0,
      "per_level": 0.5,
      "operation": "addition"
    },
    "minecraft:generic.attack_damage": {
      "base_bonus": 0.0,
      "per_level": 0.05,
      "operation": "multiply_base"
    }
  }
}
```

## Special Behaviors

### Passive Mob Leveling

By default, passive mobs don't receive levels. Structure rules can override this behavior:

1. Add the passive mob to `mob_overrides`
2. The mob will receive the specified level
3. Optionally enable combat with `"can_attack": true`

```json
{
  "structure": "minecraft:stronghold",
  "mob_overrides": {
    "minecraft:chicken": {
      "fixed_level": 100,
      "can_attack": true
    }
  }
}
```

**Note:** Only mobs explicitly listed in `mob_overrides` will be leveled. Other passive mobs remain unaffected.

### Boss Mob Leveling

Similar to passive mobs, vanilla boss mobs (Wither, Ender Dragon) can be leveled via structure overrides if `structureOverridesBossFilter` is enabled in config.

### Ignoring Level Cap

Use `ignore_level_cap: true` in mob overrides to bypass the global level cap:

```json
{
  "mob_overrides": {
    "minecraft:zombie": {
      "fixed_level": 9999,
      "ignore_level_cap": true
    }
  }
}
```

## Tips

1. **Use high priorities** (150+) for important structures
2. **Test with `/reload`** - changes apply immediately
3. **Enable debug mode** to see which rules are being applied
4. **Use mob_overrides** for structure-specific mob variants
5. **Combine with boss rules** for epic dungeon encounters
