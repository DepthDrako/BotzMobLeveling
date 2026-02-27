# Examples

This page provides complete, copy-paste ready examples for common use cases.

## Quick Reference

Jump to:
- [Dungeon Difficulty](#dungeon-difficulty)
- [Progressive Overworld](#progressive-overworld)
- [Dimension Baselines](#dimension-baselines)
- [Nether Scaling](#nether-scaling)
- [End Game Content](#end-game-content)
- [Boss Encounters](#boss-encounters)
- [Hostile Passive Mobs](#hostile-passive-mobs)
- [Modded Content](#modded-content)

---

## Dungeon Difficulty

### Stronghold - High Level Dungeon

`structures/stronghold.json`
```json
{
  "structure": "minecraft:stronghold",
  "priority": 150,
  "enabled": true,
  "level_mode": "fixed",
  "fixed_level": 200,
  "ignore_distance_scaling": true,
  "mob_overrides": {
    "minecraft:silverfish": {
      "fixed_level": 100
    },
    "minecraft:enderman": {
      "fixed_level": 300,
      "ignore_level_cap": true
    }
  },
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 50.0,
      "per_level": 1.0,
      "operation": "addition"
    },
    "minecraft:generic.attack_damage": {
      "base_bonus": 2.0,
      "per_level": 0.05,
      "operation": "addition"
    }
  }
}
```

### Nether Fortress - Fire Themed

`structures/nether_fortress.json`
```json
{
  "structure": "minecraft:fortress",
  "priority": 120,
  "enabled": true,
  "level_mode": "random",
  "level_range": {
    "min": 100,
    "max": 200
  },
  "ignore_distance_scaling": true,
  "mob_overrides": {
    "minecraft:blaze": {
      "level_bonus": 50,
      "attribute_scaling": {
        "minecraft:generic.max_health": {
          "base_bonus": 30.0,
          "per_level": 0.5,
          "operation": "addition"
        }
      }
    },
    "minecraft:wither_skeleton": {
      "level_bonus": 75,
      "ignore_level_cap": true
    }
  },
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 40.0,
      "per_level": 0.8,
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

### Ocean Monument - Aquatic Challenge

`structures/ocean_monument.json`
```json
{
  "structure": "minecraft:monument",
  "priority": 110,
  "enabled": true,
  "level_mode": "random",
  "level_range": {
    "min": 75,
    "max": 150
  },
  "ignore_distance_scaling": true,
  "mob_overrides": {
    "minecraft:elder_guardian": {
      "fixed_level": 500,
      "ignore_level_cap": true,
      "attribute_scaling": {
        "minecraft:generic.max_health": {
          "base_bonus": 200.0,
          "per_level": 2.0,
          "operation": "addition"
        }
      }
    }
  },
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 30.0,
      "per_level": 0.5,
      "operation": "addition"
    }
  }
}
```

---

## Progressive Overworld

### Peaceful Spawn Zone

`biomes/spawn_zone.json`
```json
{
  "biome": "minecraft:plains",
  "priority": 30,
  "enabled": true,
  "level_mode": "distance",
  "level_range": {
    "min": 1,
    "max": 10
  },
  "ignore_distance_scaling": false,
  "distance_multiplier": 0.5,
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 0.0,
      "per_level": 0.5,
      "operation": "addition"
    }
  }
}
```

### Desert - Harsh Environment

`biomes/desert.json`
```json
{
  "biome": "minecraft:desert",
  "priority": 60,
  "enabled": true,
  "level_mode": "distance",
  "level_range": {
    "min": 20,
    "max": 80
  },
  "distance_multiplier": 1.5,
  "mob_overrides": {
    "minecraft:husk": {
      "level_bonus": 20,
      "attribute_scaling": {
        "minecraft:generic.max_health": {
          "base_bonus": 10.0,
          "per_level": 1.0,
          "operation": "addition"
        }
      }
    }
  },
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 10.0,
      "per_level": 0.8,
      "operation": "addition"
    },
    "minecraft:generic.movement_speed": {
      "base_bonus": 0.0,
      "per_level": 0.002,
      "operation": "multiply_base"
    }
  }
}
```

### Deep Dark - Endgame Zone

`biomes/deep_dark.json`
```json
{
  "biome": "minecraft:deep_dark",
  "priority": 90,
  "enabled": true,
  "level_mode": "fixed",
  "fixed_level": 300,
  "ignore_distance_scaling": true,
  "mob_overrides": {
    "minecraft:warden": {
      "fixed_level": 1000,
      "ignore_level_cap": true,
      "attribute_scaling": {
        "minecraft:generic.max_health": {
          "base_bonus": 500.0,
          "per_level": 1.0,
          "operation": "addition"
        },
        "minecraft:generic.attack_damage": {
          "base_bonus": 10.0,
          "per_level": 0.1,
          "operation": "addition"
        }
      }
    }
  },
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 100.0,
      "per_level": 1.5,
      "operation": "addition"
    },
    "minecraft:generic.attack_damage": {
      "base_bonus": 5.0,
      "per_level": 0.1,
      "operation": "addition"
    }
  }
}
```

---

## Dimension Baselines

Dimension rules set a difficulty floor for an **entire dimension**. Biome and structure rules stack on top.

### Simple Nether Baseline

`dimensions/nether.json`
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

### Flat End Difficulty

`dimensions/the_end.json`
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

### One Rule for Multiple Dimensions

Applies the same baseline to both the Nether and a modded dimension:

`dimensions/underworld_realms.json`
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
    "max": 130
  },
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 30.0,
      "per_level": 0.8,
      "operation": "addition"
    },
    "minecraft:generic.armor": {
      "base_bonus": 2.0,
      "per_level": 0.05,
      "operation": "addition"
    }
  }
}
```

### Layered Nether — Baseline + Biome Spike

This pair of files gives the whole Nether a baseline while making the Crimson Forest extra dangerous:

`dimensions/nether_baseline.json` — applies everywhere in the Nether
```json
{
  "dimension": "minecraft:the_nether",
  "priority": 30,
  "level_range": { "min": 50, "max": 100 }
}
```

`biomes/crimson_forest.json` — overrides the dimension rule in this biome only
```json
{
  "biome": "minecraft:crimson_forest",
  "priority": 70,
  "level_range": { "min": 120, "max": 200 }
}
```

| Location | Rule Used | Level Range |
|----------|-----------|-------------|
| Nether Wastes | Dimension | 50 – 100 |
| Soul Sand Valley | Dimension | 50 – 100 |
| Crimson Forest | Biome (overrides) | 120 – 200 |
| Nether Fortress | Structure (overrides) | (your fortress rule) |

---

## Nether Scaling

### All Nether Biomes

`biomes/nether_all.json`
```json
{
  "biome_tags": ["minecraft:is_nether"],
  "priority": 70,
  "enabled": true,
  "level_mode": "distance",
  "level_range": {
    "min": 50,
    "max": 200
  },
  "distance_multiplier": 2.0,
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 30.0,
      "per_level": 1.0,
      "operation": "addition"
    },
    "minecraft:generic.attack_damage": {
      "base_bonus": 0.0,
      "per_level": 0.04,
      "operation": "multiply_base"
    },
    "minecraft:generic.armor": {
      "base_bonus": 2.0,
      "per_level": 0.1,
      "operation": "addition"
    }
  }
}
```

### Soul Sand Valley - Undead Boost

`biomes/soul_sand_valley.json`
```json
{
  "biome": "minecraft:soul_sand_valley",
  "priority": 75,
  "enabled": true,
  "level_mode": "random",
  "level_range": {
    "min": 80,
    "max": 180
  },
  "mob_overrides": {
    "minecraft:ghast": {
      "level_bonus": 30,
      "attribute_scaling": {
        "minecraft:generic.max_health": {
          "base_bonus": 20.0,
          "per_level": 1.0,
          "operation": "addition"
        }
      }
    },
    "minecraft:skeleton": {
      "level_bonus": 40
    }
  }
}
```

---

## End Game Content

### The End - All Biomes

`biomes/end_all.json`
```json
{
  "biome_tags": ["minecraft:is_end"],
  "priority": 80,
  "enabled": true,
  "level_mode": "fixed",
  "fixed_level": 150,
  "ignore_distance_scaling": true,
  "mob_overrides": {
    "minecraft:enderman": {
      "fixed_level": 200,
      "attribute_scaling": {
        "minecraft:generic.max_health": {
          "base_bonus": 40.0,
          "per_level": 1.0,
          "operation": "addition"
        },
        "minecraft:generic.attack_damage": {
          "base_bonus": 3.0,
          "per_level": 0.05,
          "operation": "addition"
        }
      }
    },
    "minecraft:shulker": {
      "fixed_level": 180
    }
  },
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 50.0,
      "per_level": 1.2,
      "operation": "addition"
    }
  }
}
```

### End City Structure

`structures/end_city.json`
```json
{
  "structure": "minecraft:end_city",
  "priority": 140,
  "enabled": true,
  "level_mode": "random",
  "level_range": {
    "min": 250,
    "max": 400
  },
  "ignore_distance_scaling": true,
  "mob_overrides": {
    "minecraft:shulker": {
      "level_bonus": 100,
      "ignore_level_cap": true,
      "attribute_scaling": {
        "minecraft:generic.max_health": {
          "base_bonus": 30.0,
          "per_level": 0.5,
          "operation": "addition"
        }
      }
    }
  },
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 80.0,
      "per_level": 1.5,
      "operation": "addition"
    },
    "minecraft:generic.attack_damage": {
      "base_bonus": 5.0,
      "per_level": 0.1,
      "operation": "addition"
    }
  }
}
```

---

## Boss Encounters

### Stronghold Mini-Boss

`bosses/stronghold_guardian.json`
```json
{
  "enabled": true,
  "target_mobs": ["minecraft:zombie"],
  "spawn_chance": 0.03,
  "structures": ["minecraft:stronghold"],

  "display_name": "§5§lStronghold Guardian",
  "tier": 2,
  "level": 400,
  "ignore_level_cap": true,

  "boss_bar": {
    "color": "purple",
    "style": "notched_6",
    "visible": true
  },

  "size_multiplier": 1.5,
  "glow_effect": true,
  "glow_color": "purple",
  "particle_effect": "minecraft:portal",

  "immunities": ["fire", "fall"],

  "stat_multipliers": {
    "minecraft:generic.max_health": 30.0,
    "minecraft:generic.attack_damage": 5.0,
    "minecraft:generic.armor": 3.0,
    "minecraft:generic.knockback_resistance": 0.8
  },

  "xp_multiplier": 15.0,

  "minions": {
    "type": "minecraft:zombie",
    "count": 2,
    "interval_seconds": 25,
    "health_threshold": 0.6,
    "max_minions": 6
  }
}
```

### Nether Warlord

`bosses/nether_warlord.json`
```json
{
  "enabled": true,
  "target_mobs": ["minecraft:wither_skeleton"],
  "spawn_chance": 0.02,
  "structures": ["minecraft:fortress"],

  "display_name": "§c§l⚔ Nether Warlord ⚔",
  "tier": 3,
  "level": 600,
  "ignore_level_cap": true,

  "boss_bar": {
    "color": "red",
    "style": "notched_10",
    "visible": true
  },

  "size_multiplier": 1.8,
  "glow_effect": true,
  "glow_color": "red",
  "particle_effect": "minecraft:flame",

  "immunities": ["fire", "wither", "explosion"],

  "stat_multipliers": {
    "minecraft:generic.max_health": 50.0,
    "minecraft:generic.attack_damage": 8.0,
    "minecraft:generic.armor": 5.0,
    "minecraft:generic.movement_speed": 1.2,
    "minecraft:generic.knockback_resistance": 1.0
  },

  "xp_multiplier": 25.0,

  "minions": {
    "type": "minecraft:blaze",
    "count": 3,
    "interval_seconds": 20,
    "health_threshold": 0.5,
    "max_minions": 9
  }
}
```

### World Boss - Rare Spawn

`bosses/skeleton_king.json`
```json
{
  "enabled": true,
  "target_mobs": ["minecraft:skeleton"],
  "spawn_chance": 0.001,

  "display_name": "§e§l👑 Skeleton King 👑",
  "tier": 3,
  "level": 800,
  "ignore_level_cap": true,

  "boss_bar": {
    "color": "yellow",
    "style": "notched_20",
    "visible": true
  },

  "size_multiplier": 2.5,
  "glow_effect": true,
  "glow_color": "yellow",
  "particle_effect": "minecraft:enchanted_hit",

  "immunities": ["fire", "projectile", "fall", "magic"],

  "stat_multipliers": {
    "minecraft:generic.max_health": 100.0,
    "minecraft:generic.attack_damage": 12.0,
    "minecraft:generic.armor": 8.0,
    "minecraft:generic.movement_speed": 1.3,
    "minecraft:generic.knockback_resistance": 1.0
  },

  "xp_multiplier": 50.0,

  "minions": {
    "type": "minecraft:skeleton",
    "count": 4,
    "interval_seconds": 15,
    "health_threshold": 0.75,
    "max_minions": 12
  }
}
```

---

## Hostile Passive Mobs

### Dungeon Chickens

`structures/stronghold_chickens.json`
```json
{
  "structure": "minecraft:stronghold",
  "priority": 100,
  "enabled": true,
  "mob_overrides": {
    "minecraft:chicken": {
      "fixed_level": 250,
      "ignore_level_cap": true,
      "can_attack": true,
      "attribute_scaling": {
        "minecraft:generic.max_health": {
          "base_bonus": 50.0,
          "per_level": 0.5,
          "operation": "addition"
        },
        "minecraft:generic.attack_damage": {
          "base_bonus": 5.0,
          "per_level": 0.1,
          "operation": "addition"
        },
        "minecraft:generic.movement_speed": {
          "base_bonus": 0.0,
          "per_level": 0.005,
          "operation": "multiply_base"
        }
      }
    }
  }
}
```

### Angry Farm Animals

`structures/haunted_village.json`
```json
{
  "structure": "minecraft:village_plains",
  "priority": 80,
  "enabled": true,
  "level_mode": "random",
  "level_range": {
    "min": 30,
    "max": 60
  },
  "mob_overrides": {
    "minecraft:cow": {
      "level_bonus": 20,
      "can_attack": true,
      "attribute_scaling": {
        "minecraft:generic.attack_damage": {
          "base_bonus": 4.0,
          "per_level": 0.1,
          "operation": "addition"
        }
      }
    },
    "minecraft:pig": {
      "level_bonus": 15,
      "can_attack": true,
      "attribute_scaling": {
        "minecraft:generic.attack_damage": {
          "base_bonus": 3.0,
          "per_level": 0.08,
          "operation": "addition"
        }
      }
    },
    "minecraft:sheep": {
      "level_bonus": 10,
      "can_attack": true,
      "attribute_scaling": {
        "minecraft:generic.attack_damage": {
          "base_bonus": 2.0,
          "per_level": 0.05,
          "operation": "addition"
        }
      }
    }
  }
}
```

---

## Modded Content

### Alex's Mobs Integration Example

`biomes/savanna_lions.json`
```json
{
  "biome": "minecraft:savanna",
  "priority": 55,
  "enabled": true,
  "level_mode": "distance",
  "level_range": {
    "min": 15,
    "max": 60
  },
  "mob_overrides": {
    "alexsmobs:lion": {
      "level_bonus": 25,
      "attribute_scaling": {
        "minecraft:generic.max_health": {
          "base_bonus": 20.0,
          "per_level": 1.0,
          "operation": "addition"
        },
        "minecraft:generic.attack_damage": {
          "base_bonus": 3.0,
          "per_level": 0.1,
          "operation": "addition"
        }
      }
    }
  }
}
```

### Custom Modded Structure

`structures/twilight_forest.json`
```json
{
  "structure": "twilightforest:hollow_hill",
  "priority": 130,
  "enabled": true,
  "level_mode": "random",
  "level_range": {
    "min": 80,
    "max": 150
  },
  "attribute_scaling": {
    "minecraft:generic.max_health": {
      "base_bonus": 40.0,
      "per_level": 1.2,
      "operation": "addition"
    }
  }
}
```

---

## Complete Datapack Template

Ready-to-use datapack structure:

```
my_leveling_pack/
├── pack.mcmeta
└── data/
    └── botzmobleveling/
        └── mob_levels/
            ├── structures/
            │   ├── stronghold.json
            │   ├── fortress.json
            │   └── monument.json
            ├── biomes/
            │   ├── nether.json
            │   ├── end.json
            │   └── deep_dark.json
            └── bosses/
                ├── stronghold_boss.json
                └── nether_boss.json
```

`pack.mcmeta`:
```json
{
  "pack": {
    "pack_format": 15,
    "description": "Custom Mob Leveling Rules"
  }
}
```

Copy any examples from this page into the appropriate folders!
