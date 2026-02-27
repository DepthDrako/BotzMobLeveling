# Attribute Scaling

Attribute scaling determines how mob stats increase with level. This system allows precise control over health, damage, speed, and other attributes.

## Location

Attribute scaling is defined inside structure, biome, or mob override blocks:

```json
{
  "structure": "minecraft:stronghold",
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 10.0,
      "per_level": 0.5,
      "operation": "addition"
    }
  }
}
```

## Field Reference

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `base_bonus` | Double | `0.0` | Flat bonus applied at level 1 |
| `per_level` | Double | `0.0` | Bonus added per level |
| `operation` | String | `"addition"` | How the bonus is applied |
| `max_bonus` | Double | `null` | Maximum total bonus (optional) |

## Operations

### Addition

Adds the calculated bonus directly to the attribute:

```
final_value = base_value + bonus
```

**Example:** A zombie with 20 HP at level 50:
```json
{
  "minecraft:generic.max_health": {
    "base_bonus": 10.0,
    "per_level": 0.5,
    "operation": "addition"
  }
}
```
Bonus = 10 + (0.5 × 50) = 35
Final HP = 20 + 35 = **55 HP**

### Multiply Base

Multiplies the base attribute value:

```
final_value = base_value × (1 + bonus)
```

**Example:** A zombie with 3 attack damage at level 50:
```json
{
  "minecraft:generic.attack_damage": {
    "base_bonus": 0.0,
    "per_level": 0.02,
    "operation": "multiply_base"
  }
}
```
Bonus = 0 + (0.02 × 50) = 1.0
Final damage = 3 × (1 + 1.0) = **6 damage**

### Multiply Total

Multiplies after all other modifiers:

```
final_value = (base_value + additions) × (1 + bonus)
```

Useful for percentage-based scaling that stacks with other effects.

## Supported Attributes

### Vanilla Attributes

| Attribute ID | Description | Base Value (Zombie) |
|--------------|-------------|---------------------|
| `minecraft:generic.max_health` | Maximum HP | 20.0 |
| `minecraft:generic.attack_damage` | Melee damage | 3.0 |
| `minecraft:generic.armor` | Damage reduction | 0.0 |
| `minecraft:generic.armor_toughness` | Armor effectiveness | 0.0 |
| `minecraft:generic.knockback_resistance` | Knockback reduction (0-1) | 0.0 |
| `minecraft:generic.movement_speed` | Movement speed | 0.23 |
| `minecraft:generic.follow_range` | Detection range | 35.0 |
| `minecraft:generic.attack_knockback` | Knockback dealt | 0.0 |
| `minecraft:generic.attack_speed` | Attack cooldown | 4.0 |

### Modded Attributes

You can use any modded attribute if it's registered properly:

```json
{
  "some_mod:custom_attribute": {
    "base_bonus": 10.0,
    "per_level": 1.0,
    "operation": "addition"
  }
}
```

## Calculation Examples

### Health Scaling

**Goal:** Double health every 100 levels

```json
{
  "minecraft:generic.max_health": {
    "base_bonus": 0.0,
    "per_level": 0.01,
    "operation": "multiply_base"
  }
}
```

| Level | Multiplier | Zombie HP |
|-------|------------|-----------|
| 1 | 1.01 | 20.2 |
| 50 | 1.50 | 30.0 |
| 100 | 2.00 | 40.0 |
| 200 | 3.00 | 60.0 |

### Flat Health Bonus

**Goal:** +5 HP per level

```json
{
  "minecraft:generic.max_health": {
    "base_bonus": 0.0,
    "per_level": 5.0,
    "operation": "addition"
  }
}
```

| Level | Bonus | Zombie HP |
|-------|-------|-----------|
| 1 | 5 | 25 |
| 10 | 50 | 70 |
| 50 | 250 | 270 |
| 100 | 500 | 520 |

### Capped Scaling

**Goal:** Scale damage but cap at +20

```json
{
  "minecraft:generic.attack_damage": {
    "base_bonus": 2.0,
    "per_level": 0.5,
    "operation": "addition",
    "max_bonus": 20.0
  }
}
```

| Level | Calculated | Capped | Zombie Damage |
|-------|------------|--------|---------------|
| 1 | 2.5 | 2.5 | 5.5 |
| 20 | 12.0 | 12.0 | 15.0 |
| 36 | 20.0 | 20.0 | 23.0 |
| 100 | 52.0 | 20.0 | 23.0 |

## Complete Examples

### Balanced Dungeon Scaling

Moderate scaling across all stats:

```json
{
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 20.0,
      "per_level": 1.0,
      "operation": "addition",
      "max_bonus": 500.0
    },
    "minecraft:generic.attack_damage": {
      "base_bonus": 0.0,
      "per_level": 0.02,
      "operation": "multiply_base",
      "max_bonus": 5.0
    },
    "minecraft:generic.armor": {
      "base_bonus": 2.0,
      "per_level": 0.1,
      "operation": "addition",
      "max_bonus": 20.0
    },
    "minecraft:generic.movement_speed": {
      "base_bonus": 0.0,
      "per_level": 0.001,
      "operation": "multiply_base",
      "max_bonus": 0.5
    }
  }
}
```

### Tank Mobs

High health, low damage, slow movement:

```json
{
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 100.0,
      "per_level": 5.0,
      "operation": "addition"
    },
    "minecraft:generic.attack_damage": {
      "base_bonus": 0.0,
      "per_level": 0.01,
      "operation": "multiply_base"
    },
    "minecraft:generic.armor": {
      "base_bonus": 10.0,
      "per_level": 0.5,
      "operation": "addition"
    },
    "minecraft:generic.knockback_resistance": {
      "base_bonus": 0.5,
      "per_level": 0.005,
      "operation": "addition",
      "max_bonus": 1.0
    },
    "minecraft:generic.movement_speed": {
      "base_bonus": -0.05,
      "per_level": 0.0,
      "operation": "addition"
    }
  }
}
```

### Glass Cannon Mobs

High damage, low health:

```json
{
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 0.0,
      "per_level": 0.1,
      "operation": "addition"
    },
    "minecraft:generic.attack_damage": {
      "base_bonus": 5.0,
      "per_level": 0.1,
      "operation": "multiply_base"
    },
    "minecraft:generic.movement_speed": {
      "base_bonus": 0.0,
      "per_level": 0.005,
      "operation": "multiply_base",
      "max_bonus": 1.0
    },
    "minecraft:generic.attack_speed": {
      "base_bonus": 0.0,
      "per_level": 0.02,
      "operation": "multiply_base"
    }
  }
}
```

### Speedster Mobs

Very fast, moderate stats:

```json
{
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 0.0,
      "per_level": 0.5,
      "operation": "addition"
    },
    "minecraft:generic.attack_damage": {
      "base_bonus": 0.0,
      "per_level": 0.03,
      "operation": "multiply_base"
    },
    "minecraft:generic.movement_speed": {
      "base_bonus": 0.1,
      "per_level": 0.01,
      "operation": "multiply_base",
      "max_bonus": 2.0
    },
    "minecraft:generic.follow_range": {
      "base_bonus": 10.0,
      "per_level": 0.5,
      "operation": "addition",
      "max_bonus": 100.0
    }
  }
}
```

## Config Whitelist

The mod only modifies attributes listed in the config whitelist:

```toml
[attributes]
allowedAttributes = [
    "minecraft:generic.max_health",
    "minecraft:generic.attack_damage",
    "minecraft:generic.armor",
    "minecraft:generic.armor_toughness",
    "minecraft:generic.knockback_resistance",
    "minecraft:generic.movement_speed",
    "minecraft:generic.follow_range",
    "minecraft:generic.attack_knockback",
    "minecraft:generic.attack_speed"
]
```

Add modded attributes here to enable scaling.

## Tips

1. **Use `max_bonus`** to prevent scaling from getting out of control
2. **Test at different levels** - what works at level 10 may break at level 500
3. **Balance health and damage** - too much of either makes combat unfun
4. **Consider knockback resistance** - high-level mobs should be harder to cheese
5. **Movement speed caps** - values above 2.0 can cause issues
6. **Start conservative** - it's easier to buff than to nerf
