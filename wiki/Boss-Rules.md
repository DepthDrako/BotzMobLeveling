# Boss Rules

Boss rules allow you to transform regular mobs into powerful boss creatures with custom abilities, boss bars, minions, and more!

## Location

```
data/botzmobleveling/mob_levels/bosses/<rulename>.json
```

## Basic Structure

```json
{
  "enabled": true,
  "target_mobs": ["minecraft:zombie"],
  "spawn_chance": 0.05,
  "structures": [],
  "biomes": [],
  "display_name": "Boss Zombie",
  "tier": 1,
  "level": 100,
  "ignore_level_cap": true,
  "boss_bar": {
    "color": "red",
    "style": "progress",
    "visible": true
  },
  "size_multiplier": 1.5,
  "glow_effect": true,
  "glow_color": "red",
  "particle_effect": "minecraft:flame",
  "immunities": [],
  "stat_multipliers": {},
  "xp_multiplier": 5.0,
  "loot_table": null,
  "minions": null
}
```

## Field Reference

### Core Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `enabled` | Boolean | `true` | Toggle this boss rule on/off |
| `target_mobs` | Array | `[]` | List of mob IDs that can become this boss. Empty = any mob |
| `spawn_chance` | Double | `0.1` | Chance (0.0-1.0) for an eligible mob to become this boss |
| `structures` | Array | `[]` | Restrict to specific structures. Empty = anywhere |
| `biomes` | Array | `[]` | Restrict to specific biomes. Empty = any biome |

### Boss Properties

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `display_name` | String | `"Boss"` | Name shown above boss. Supports color codes |
| `tier` | Integer | `1` | Boss tier (1 = normal, 2 = elite, 3 = legendary) |
| `level` | Integer | `50` | Fixed level for the boss |
| `ignore_level_cap` | Boolean | `true` | Bypass global level cap |

### Visual Effects

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `size_multiplier` | Float | `1.0` | Scale the boss model size |
| `glow_effect` | Boolean | `true` | Apply glowing effect |
| `glow_color` | String | `"red"` | Color of the glow |
| `particle_effect` | String | `null` | Particle effect around boss |

### Boss Bar

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `boss_bar.color` | String | `"red"` | Boss bar color |
| `boss_bar.style` | String | `"progress"` | Boss bar style |
| `boss_bar.visible` | Boolean | `true` | Show the boss bar |

### Combat

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `immunities` | Array | `[]` | Damage types the boss is immune to |
| `stat_multipliers` | Object | `{}` | Attribute multipliers |
| `xp_multiplier` | Double | `5.0` | XP drop multiplier |
| `loot_table` | String | `null` | Custom loot table ID |

### Minions

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `minions.type` | String | Required | Mob type to spawn as minions |
| `minions.count` | Integer | `2` | Minions per spawn wave |
| `minions.interval_seconds` | Integer | `30` | Time between spawn waves |
| `minions.health_threshold` | Double | `0.5` | HP% to trigger spawning (0.0-1.0) |
| `minions.max_minions` | Integer | `10` | Maximum active minions |

## Color Codes

### Display Name Colors

Use Minecraft color codes with `§`:

| Code | Color |
|------|-------|
| `§0` | Black |
| `§1` | Dark Blue |
| `§2` | Dark Green |
| `§3` | Dark Aqua |
| `§4` | Dark Red |
| `§5` | Dark Purple |
| `§6` | Gold |
| `§7` | Gray |
| `§8` | Dark Gray |
| `§9` | Blue |
| `§a` | Green |
| `§b` | Aqua |
| `§c` | Red |
| `§d` | Light Purple |
| `§e` | Yellow |
| `§f` | White |
| `§l` | **Bold** |
| `§o` | *Italic* |
| `§n` | <u>Underline</u> |
| `§k` | Obfuscated |
| `§r` | Reset |

**Example:** `"§4§l☠ Cluckthulhu §4§l☠"` = Dark red, bold, with skull symbols

### Boss Bar Colors

- `red`
- `blue`
- `green`
- `yellow`
- `purple`
- `pink`
- `white`

### Boss Bar Styles

- `progress` - Solid bar
- `notched_6` - 6 segments
- `notched_10` - 10 segments
- `notched_12` - 12 segments
- `notched_20` - 20 segments

## Immunity Types

| Type | Blocks |
|------|--------|
| `fire` | Fire, lava, fire aspect |
| `fall` | Fall damage |
| `drown` | Drowning damage |
| `explosion` | Explosion damage |
| `projectile` | Arrow, trident damage |
| `magic` | Potion damage |
| `wither` | Wither effect |
| `cactus` | Cactus damage |
| `lightning` | Lightning strikes |

## Complete Examples

### Legendary Stronghold Chicken Boss

```json
{
  "enabled": true,
  "target_mobs": ["minecraft:chicken"],
  "spawn_chance": 0.10,
  "structures": ["minecraft:stronghold"],

  "display_name": "§4§l☠ Cluckthulhu §4§l☠",
  "tier": 3,
  "level": 750,
  "ignore_level_cap": true,

  "boss_bar": {
    "color": "red",
    "style": "notched_10",
    "visible": true
  },

  "size_multiplier": 2.0,
  "glow_effect": true,
  "glow_color": "red",
  "particle_effect": "minecraft:flame",

  "immunities": ["fire", "fall"],

  "stat_multipliers": {
    "minecraft:generic.max_health": 50.0,
    "minecraft:generic.attack_damage": 10.0,
    "minecraft:generic.movement_speed": 1.5,
    "minecraft:generic.knockback_resistance": 0.8
  },

  "xp_multiplier": 20.0,

  "minions": {
    "type": "minecraft:chicken",
    "count": 3,
    "interval_seconds": 20,
    "health_threshold": 0.5,
    "max_minions": 9
  }
}
```

### Nether Fortress Elite Guard

```json
{
  "enabled": true,
  "target_mobs": ["minecraft:wither_skeleton"],
  "spawn_chance": 0.02,
  "structures": ["minecraft:fortress"],

  "display_name": "§5§lFortress Champion",
  "tier": 2,
  "level": 300,
  "ignore_level_cap": true,

  "boss_bar": {
    "color": "purple",
    "style": "notched_6",
    "visible": true
  },

  "size_multiplier": 1.3,
  "glow_effect": true,
  "glow_color": "purple",
  "particle_effect": "minecraft:soul",

  "immunities": ["fire", "wither"],

  "stat_multipliers": {
    "minecraft:generic.max_health": 15.0,
    "minecraft:generic.attack_damage": 3.0,
    "minecraft:generic.armor": 2.0
  },

  "xp_multiplier": 10.0
}
```

### World Boss (Anywhere)

```json
{
  "enabled": true,
  "target_mobs": ["minecraft:skeleton"],
  "spawn_chance": 0.001,

  "display_name": "§e§lSkeleton King",
  "tier": 3,
  "level": 500,
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

  "immunities": ["fire", "projectile", "fall"],

  "stat_multipliers": {
    "minecraft:generic.max_health": 100.0,
    "minecraft:generic.attack_damage": 15.0,
    "minecraft:generic.movement_speed": 1.2,
    "minecraft:generic.knockback_resistance": 1.0
  },

  "xp_multiplier": 50.0,

  "minions": {
    "type": "minecraft:skeleton",
    "count": 5,
    "interval_seconds": 15,
    "health_threshold": 0.75,
    "max_minions": 15
  }
}
```

## How Boss Spawning Works

1. A mob spawns normally
2. If it matches `target_mobs` (or list is empty)
3. AND it's in a valid `structure` (or list is empty)
4. AND it's in a valid `biome` (or list is empty)
5. THEN roll `spawn_chance`
6. If roll succeeds, mob becomes a boss

**Tip:** Use low spawn chances (0.01-0.05) for powerful bosses to keep them rare and exciting!

## Config Integration

The boss module can be toggled in the config:

```toml
[bossModule]
enabled = true
showBossBar = true
bossBarRenderDistance = 64
preventDespawn = true
glowEffect = true
spawnAnnouncement = true
announcementRadius = 64
```

These global settings can be overridden per-boss in the datapack.

## Tips

1. **Start with low spawn chances** - Bosses should feel special
2. **Use tiered difficulties** - Tier 1 for dungeons, Tier 3 for raids
3. **Balance minion spawning** - Too many minions can cause lag
4. **Test in creative** - Spawn mobs manually to test boss configuration
5. **Combine with structure rules** - Make entire dungeons challenging
