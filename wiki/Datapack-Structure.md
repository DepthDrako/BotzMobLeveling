# Datapack Structure

This page explains the folder structure and file organization for BotzMobLeveling datapacks.

## Directory Layout

```
your_datapack/
├── pack.mcmeta                          # Required: Datapack metadata
└── data/
    └── botzmobleveling/                 # Mod's namespace
        └── mob_levels/                  # All leveling rules go here
            ├── structures/              # Structure-based rules
            │   ├── stronghold.json
            │   ├── nether_fortress.json
            │   └── ocean_monument.json
            ├── biomes/                  # Biome-based rules
            │   ├── desert.json
            │   ├── nether.json
            │   └── end.json
            ├── dimensions/              # Dimension-wide baseline rules
            │   ├── nether.json
            │   ├── the_end.json
            │   └── twilight_forest.json
            └── bosses/                  # Boss transformation rules
                ├── stronghold_boss.json
                └── nether_boss.json
```

## Namespace

All BotzMobLeveling rules must be placed under the `botzmobleveling` namespace:

```
data/botzmobleveling/mob_levels/...
```

## Rule Folders

### structures/

Contains rules that apply to mobs spawning inside specific structures.

- **Default Priority:** 100 (highest)
- **Default `ignore_distance_scaling`:** true
- **Requires:** `structure` field

**Example:** `structures/stronghold.json`
```json
{
  "structure": "minecraft:stronghold",
  "fixed_level": 100
}
```

### biomes/

Contains rules that apply to mobs spawning in specific biomes or biome tags.

- **Default Priority:** 50 (medium)
- **Default `ignore_distance_scaling`:** false
- **Requires:** `biome` or `biome_tags` field

**Example:** `biomes/desert.json`
```json
{
  "biome": "minecraft:desert",
  "level_range": {
    "min": 20,
    "max": 50
  }
}
```

### dimensions/

Contains rules that apply to **all mobs in an entire dimension**. Dimension rules sit below biome rules in priority, so a biome rule will override them for specific biomes. Use these to set a dimension-wide difficulty floor.

- **Default Priority:** 30 (below biome)
- **Default `ignore_distance_scaling`:** false
- **Requires:** `dimension` or `dimensions` field

**Example:** `dimensions/nether.json`
```json
{
  "dimension": "minecraft:the_nether",
  "level_range": {
    "min": 50,
    "max": 150
  }
}
```

### bosses/

Contains rules for transforming regular mobs into powerful bosses.

- **Chance-based spawning**
- **Can be restricted to structures/biomes**
- **Supports boss bars, minions, immunities**

**Example:** `bosses/stronghold_overlord.json`
```json
{
  "target_mobs": ["minecraft:zombie"],
  "spawn_chance": 0.05,
  "structures": ["minecraft:stronghold"],
  "display_name": "Stronghold Overlord",
  "level": 200
}
```

## File Naming

- Use **lowercase** letters only
- Use **underscores** for spaces (e.g., `nether_fortress.json`)
- File extension must be `.json`
- File name becomes the rule's internal ID

**Good examples:**
- `stronghold_zombies.json`
- `desert_skeleton.json`
- `ocean_monument_guardians.json`

**Bad examples:**
- `Stronghold Zombies.json` (spaces and capitals)
- `desert-skeleton.json` (hyphens)
- `rule.txt` (wrong extension)

## pack.mcmeta

Every datapack requires a `pack.mcmeta` file in its root:

```json
{
  "pack": {
    "pack_format": 15,
    "description": "My custom mob leveling datapack"
  }
}
```

| Field | Description |
|-------|-------------|
| `pack_format` | Use `15` for Minecraft 1.20.1 |
| `description` | Shows in the datapack selection screen |

## Loading Order

1. Datapacks are loaded alphabetically by folder name
2. Rules within the same priority level are processed in load order
3. Use the `priority` field to explicitly control order

## Hot Reloading

After making changes to your datapack:

1. Save your JSON files
2. Run `/reload` in-game
3. Changes apply immediately to newly spawning mobs

**Note:** Already-spawned mobs keep their existing levels. Only new spawns use the updated rules.

## Merging Multiple Datapacks

You can have multiple datapacks with mob leveling rules. They will merge based on:

1. **Different structures/biomes:** Both rules apply to their respective locations
2. **Same structure/biome:** Higher priority rule wins
3. **Same priority:** Later-loaded datapack wins

## Troubleshooting

### Rules Not Loading

1. Check the folder structure matches exactly
2. Verify JSON syntax with a validator
3. Enable debug mode in the mod config
4. Check game logs for error messages

### Wrong Levels Appearing

1. Check rule priorities
2. Verify structure/biome IDs are correct
3. Remember: Structure rules override biome rules by default

### Debug Mode

Enable in config (`botzmobleveling-common.toml`):
```toml
debugMode = true
```

This logs which rules are being applied and why.
