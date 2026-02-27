# Getting Started

This guide will walk you through setting up your first mob leveling datapack.

## Prerequisites

- Minecraft 1.20.1
- Minecraft Forge (latest for 1.20.1)
- BotzMobLeveling mod installed

## Creating Your First Datapack

### Step 1: Create the Folder Structure

In your world's `datapacks` folder, create a new folder for your datapack:

```
world/
└── datapacks/
    └── my_leveling_pack/
        ├── pack.mcmeta
        └── data/
            └── botzmobleveling/
                └── mob_levels/
                    ├── structures/
                    ├── biomes/
                    ├── dimensions/
                    └── bosses/
```

### Step 2: Create pack.mcmeta

Create a `pack.mcmeta` file in your datapack's root folder:

```json
{
  "pack": {
    "pack_format": 15,
    "description": "My custom mob leveling rules"
  }
}
```

### Step 3: Create Your First Rule

Let's create a simple structure rule that makes zombies in strongholds spawn at level 100.

Create `data/botzmobleveling/mob_levels/structures/stronghold_zombies.json`:

```json
{
  "structure": "minecraft:stronghold",
  "priority": 100,
  "enabled": true,
  "level_mode": "fixed",
  "fixed_level": 100,
  "mob_overrides": {
    "minecraft:zombie": {
      "fixed_level": 100
    }
  }
}
```

### Step 4: Apply the Changes

1. Make sure the datapack is in your world's `datapacks` folder
2. Enter the game and run `/reload`
3. Visit a stronghold to see your level 100 zombies!

## Understanding the Basics

### Level Modes

There are three ways to assign levels:

| Mode | Description |
|------|-------------|
| `fixed` | All mobs get the exact level specified in `fixed_level` |
| `random` | Random level between `level_range.min` and `level_range.max` |
| `distance` | Level scales based on distance from world spawn |

### Rule Types

| Type | Priority | Use Case |
|------|----------|----------|
| Structure | 100+ (default) | Dungeons, strongholds, monuments |
| Biome | 50 (default) | Specific biomes like deserts or the Crimson Forest |
| Dimension | 30 (default) | Whole dimensions — Nether, End, modded dimensions |
| Base | N/A | Fallback when no other rules match |

### What Gets Scaled

By default, leveled mobs receive:
- Increased max health
- Increased attack damage
- Visual level indicator in their name tag (e.g., `[Lv.100] Zombie`)

## Next Steps

- Learn about [Structure Rules](Structure-Rules) for dungeon-specific leveling
- Explore [Biome Rules](Biome-Rules) for world-wide difficulty zones
- Set dimension baselines with [Dimension Rules](Dimension-Rules)
- Create epic [Boss Rules](Boss-Rules) for powerful boss encounters
- Fine-tune with [Attribute Scaling](Attribute-Scaling)

## Quick Reference

### Minimum Viable Rule (Structure)

```json
{
  "structure": "minecraft:stronghold",
  "fixed_level": 50
}
```

### Minimum Viable Rule (Biome)

```json
{
  "biome": "minecraft:desert",
  "level_range": {
    "min": 10,
    "max": 30
  }
}
```

### Minimum Viable Rule (Dimension)

```json
{
  "dimension": "minecraft:the_nether",
  "level_range": {
    "min": 50,
    "max": 150
  }
}
```

The mod will use sensible defaults for any missing fields!
