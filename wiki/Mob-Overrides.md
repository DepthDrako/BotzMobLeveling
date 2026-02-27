# Mob Overrides

Mob overrides allow you to customize leveling behavior for specific mob types within structure or biome rules. This gives you fine-grained control over individual mobs.

## Location

Mob overrides are nested inside structure or biome rule files:

```json
{
  "structure": "minecraft:stronghold",
  "mob_overrides": {
    "minecraft:zombie": {
      // Override settings for zombies
    },
    "minecraft:skeleton": {
      // Override settings for skeletons
    }
  }
}
```

## Field Reference

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `fixed_level` | Integer | `null` | Exact level for this mob type |
| `level_bonus` | Integer | `null` | Add to calculated level |
| `min_level` | Integer | `null` | Minimum level for this mob |
| `max_level` | Integer | `null` | Maximum level for this mob |
| `level_range` | Object | `null` | Alternative way to set min/max |
| `ignore_level_cap` | Boolean | `false` | Bypass global level cap |
| `can_attack` | Boolean | `null` | Enable combat for passive mobs |
| `attribute_multipliers` | Object | `{}` | Multiply attribute bonuses |
| `attribute_scaling` | Object | `{}` | Custom attribute scaling |

## Basic Usage

### Fixed Level Override

Give zombies in strongholds a fixed level of 200:

```json
{
  "structure": "minecraft:stronghold",
  "mob_overrides": {
    "minecraft:zombie": {
      "fixed_level": 200
    }
  }
}
```

### Level Bonus

Add 50 levels to whatever the rule calculates:

```json
{
  "structure": "minecraft:stronghold",
  "level_mode": "random",
  "level_range": {
    "min": 50,
    "max": 100
  },
  "mob_overrides": {
    "minecraft:skeleton": {
      "level_bonus": 50
    }
  }
}
```

A skeleton that would be level 75 becomes level 125.

### Custom Level Range

Restrict specific mobs to a different level range:

```json
{
  "biome": "minecraft:desert",
  "level_range": {
    "min": 10,
    "max": 50
  },
  "mob_overrides": {
    "minecraft:husk": {
      "level_range": {
        "min": 80,
        "max": 120
      }
    }
  }
}
```

Or use individual fields:

```json
{
  "mob_overrides": {
    "minecraft:husk": {
      "min_level": 80,
      "max_level": 120
    }
  }
}
```

## Bypassing Level Cap

By default, all levels are capped by `globalLevelCap` in the config. Override this per-mob:

```json
{
  "mob_overrides": {
    "minecraft:warden": {
      "fixed_level": 9999,
      "ignore_level_cap": true
    }
  }
}
```

## Passive Mob Combat

Enable passive mobs to fight back when attacked:

```json
{
  "structure": "minecraft:stronghold",
  "mob_overrides": {
    "minecraft:chicken": {
      "fixed_level": 500,
      "can_attack": true
    }
  }
}
```

### Combat Behavior

When `can_attack` is enabled:
1. The passive mob gains melee attack AI
2. When attacked, it will target the attacker
3. Attack damage scales with level
4. The mob remembers who hurt it

### Combat Values

| Value | Behavior |
|-------|----------|
| `true` | Mob can attack when provoked |
| `false` | Mob cannot attack (even if config enables it) |
| `null` (default) | Use global config setting |

**Global config setting:** `leveledPassivesCanAttack`

## Attribute Overrides

### Attribute Multipliers

Multiply the base attribute bonuses for specific mobs:

```json
{
  "mob_overrides": {
    "minecraft:zombie": {
      "attribute_multipliers": {
        "minecraft:generic.max_health": 2.0,
        "minecraft:generic.attack_damage": 1.5
      }
    }
  }
}
```

This **multiplies** the bonuses from the parent rule's `attribute_scaling`.

### Custom Attribute Scaling

Completely override attribute scaling for specific mobs:

```json
{
  "mob_overrides": {
    "minecraft:skeleton": {
      "attribute_scaling": {
        "minecraft:generic.max_health": {
          "base_bonus": 100.0,
          "per_level": 2.0,
          "operation": "addition",
          "max_bonus": 1000.0
        },
        "minecraft:generic.attack_damage": {
          "base_bonus": 5.0,
          "per_level": 0.1,
          "operation": "addition"
        }
      }
    }
  }
}
```

## Complete Examples

### Dungeon Elite Zombies

```json
{
  "structure": "minecraft:stronghold",
  "priority": 100,
  "level_mode": "random",
  "level_range": {
    "min": 50,
    "max": 100
  },
  "mob_overrides": {
    "minecraft:zombie": {
      "fixed_level": 200,
      "ignore_level_cap": true,
      "attribute_scaling": {
        "minecraft:generic.max_health": {
          "base_bonus": 200.0,
          "per_level": 1.0,
          "operation": "addition"
        },
        "minecraft:generic.attack_damage": {
          "base_bonus": 10.0,
          "per_level": 0.2,
          "operation": "addition"
        },
        "minecraft:generic.armor": {
          "base_bonus": 10.0,
          "per_level": 0.1,
          "operation": "addition"
        }
      }
    }
  }
}
```

### Aggressive Farm Animals

```json
{
  "structure": "minecraft:village_plains",
  "priority": 80,
  "mob_overrides": {
    "minecraft:chicken": {
      "fixed_level": 50,
      "can_attack": true,
      "attribute_scaling": {
        "minecraft:generic.attack_damage": {
          "base_bonus": 2.0,
          "per_level": 0.1,
          "operation": "addition"
        }
      }
    },
    "minecraft:cow": {
      "fixed_level": 75,
      "can_attack": true,
      "attribute_scaling": {
        "minecraft:generic.attack_damage": {
          "base_bonus": 4.0,
          "per_level": 0.2,
          "operation": "addition"
        }
      }
    },
    "minecraft:pig": {
      "fixed_level": 60,
      "can_attack": true
    }
  }
}
```

### Tiered Nether Difficulty

```json
{
  "biome_tags": ["minecraft:is_nether"],
  "priority": 60,
  "level_mode": "distance",
  "level_range": {
    "min": 30,
    "max": 100
  },
  "mob_overrides": {
    "minecraft:blaze": {
      "level_bonus": 20,
      "attribute_multipliers": {
        "minecraft:generic.max_health": 1.5
      }
    },
    "minecraft:wither_skeleton": {
      "level_bonus": 30,
      "attribute_multipliers": {
        "minecraft:generic.max_health": 2.0,
        "minecraft:generic.attack_damage": 1.5
      }
    },
    "minecraft:ghast": {
      "fixed_level": 150,
      "ignore_level_cap": true
    }
  }
}
```

## Special Considerations

### Passive Mobs in Structures

By default, passive mobs don't receive levels. To level passive mobs:

1. Add them to `mob_overrides`
2. The presence in `mob_overrides` allows them to be leveled
3. Only mobs **explicitly listed** will be affected

```json
{
  "structure": "minecraft:stronghold",
  "mob_overrides": {
    "minecraft:chicken": {
      "fixed_level": 100
    }
  }
}
```

**Important:** Without an explicit `mob_overrides` entry, passive mobs remain level 0.

### Override Priority

1. Mob override `fixed_level` takes absolute priority
2. Mob override level range overrides parent range
3. `level_bonus` is added to calculated levels
4. `attribute_scaling` replaces parent scaling
5. `attribute_multipliers` multiply parent scaling

## Tips

1. **Use fixed levels** for signature mobs in dungeons
2. **Use level_bonus** for slight variations
3. **Enable can_attack** sparingly for surprise difficulty
4. **Attribute multipliers** are simpler than full scaling
5. **Test with debug mode** to verify correct application
