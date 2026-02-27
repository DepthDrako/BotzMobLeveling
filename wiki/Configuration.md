# Configuration

BotzMobLeveling is highly configurable through its config file. This page documents all available options.

## Config File Location

```
config/botzmobleveling-common.toml
```

The config file is automatically generated on first launch.

## Configuration Sections

### General Settings

```toml
[general]
# Enable or disable the entire mob leveling system
enabled = true

# Display mob level in their name tag
showLevelInName = true

# Enable debug logging for troubleshooting
debugMode = false
```

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `true` | Master toggle for the entire mod |
| `showLevelInName` | Boolean | `true` | Show `[Lv.X]` in mob name tags |
| `debugMode` | Boolean | `false` | Log detailed rule matching info |

### Rule Toggles

```toml
[ruleToggles]
# Enable structure-based leveling rules (highest priority)
structureLevelingEnabled = true

# Enable biome-based leveling rules (medium priority)
biomeLevelingEnabled = true

# Enable dimension-based leveling rules (below biome, above base rules)
dimensionLevelingEnabled = true

# Enable distance-from-spawn leveling (used in base rules)
distanceLevelingEnabled = true
```

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `structureLevelingEnabled` | Boolean | `true` | Enable structure rules |
| `biomeLevelingEnabled` | Boolean | `true` | Enable biome rules |
| `dimensionLevelingEnabled` | Boolean | `true` | Enable dimension rules |
| `distanceLevelingEnabled` | Boolean | `true` | Enable distance-based scaling |

### Mob Filtering

```toml
[filtering]
# Apply leveling to passive mobs (animals, villagers, etc)
applyToPassiveMobs = false

# Apply leveling to boss mobs (Wither, Ender Dragon, etc)
applyToBossMobs = false

# Allow structure rules to level passive mobs even when applyToPassiveMobs is false
structureOverridesPassiveFilter = true

# Allow structure rules to level boss mobs even when applyToBossMobs is false
structureOverridesBossFilter = true

# When enabled, leveled passive mobs can fight back when attacked
leveledPassivesCanAttack = true

# List of mob IDs that will never receive levels
mobBlacklist = [
    "minecraft:armor_stand",
    "minecraft:marker",
    "minecraft:item_frame",
    "minecraft:glow_item_frame",
    "minecraft:painting"
]
```

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `applyToPassiveMobs` | Boolean | `false` | Level passive mobs globally |
| `applyToBossMobs` | Boolean | `false` | Level vanilla bosses globally |
| `structureOverridesPassiveFilter` | Boolean | `true` | Allow structure rules to level passive mobs |
| `structureOverridesBossFilter` | Boolean | `true` | Allow structure rules to level bosses |
| `leveledPassivesCanAttack` | Boolean | `true` | Leveled passive mobs fight back |
| `mobBlacklist` | List | (see above) | Mobs that never receive levels |

#### Understanding Passive/Boss Overrides

When `applyToPassiveMobs = false`:
- Passive mobs don't normally get levels
- BUT if a structure rule has that mob in `mob_overrides`, it CAN get a level
- This allows special dungeon chickens while keeping world chickens safe

**Example:** World chickens stay level 0, but stronghold chickens get level 500.

### Level Ranges

```toml
[levels]
# Default minimum level for mobs without specific rules
defaultMinLevel = 1

# Default maximum level for mobs without specific rules
defaultMaxLevel = 100

# Absolute maximum level any mob can reach, regardless of rules
globalLevelCap = 100
```

| Option | Type | Default | Range | Description |
|--------|------|---------|-------|-------------|
| `defaultMinLevel` | Integer | `1` | 1-10000 | Base minimum level |
| `defaultMaxLevel` | Integer | `100` | 1-10000 | Base maximum level |
| `globalLevelCap` | Integer | `100` | 1-10000 | Hard cap on all levels |

**Note:** Use `ignore_level_cap: true` in datapacks to bypass `globalLevelCap`.

### Distance Scaling

```toml
[distance]
# Distance from spawn (in blocks) where level scaling begins
distanceStartRadius = 100

# Number of blocks of distance required per additional level
distancePerLevel = 50

# Multiplier applied to distance-calculated levels
distanceLevelMultiplier = 1.0
```

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `distanceStartRadius` | Integer | `100` | Radius where leveling starts |
| `distancePerLevel` | Integer | `50` | Blocks per level increase |
| `distanceLevelMultiplier` | Double | `1.0` | Scale distance effect |

#### Distance Calculation

```
level = ((distance - distanceStartRadius) / distancePerLevel) * distanceLevelMultiplier
```

**Example** with defaults:
- At spawn (0 blocks): Level 1
- At 100 blocks: Level 1 (within start radius)
- At 200 blocks: Level 2
- At 500 blocks: Level 8

### Display Settings

```toml
[display]
# Format for level display. Use {level} as placeholder
levelFormat = "[Lv.{level}] "

# Color for level display (color names or hex codes)
levelColor = "gold"

# Always show mob name with level (true) or only when looking at mob (false)
alwaysShowName = false
```

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `levelFormat` | String | `"[Lv.{level}] "` | Level display format |
| `levelColor` | String | `"gold"` | Level text color |
| `alwaysShowName` | Boolean | `false` | Always render name tags |

#### Supported Colors

- Color names: `black`, `dark_blue`, `dark_green`, `dark_aqua`, `dark_red`, `dark_purple`, `gold`, `gray`, `dark_gray`, `blue`, `green`, `aqua`, `red`, `light_purple`, `yellow`, `white`
- Hex codes: `#FF5500`, `#00FF00`, etc.

### Attribute Whitelist

```toml
[attributes]
# List of attribute IDs that the mod is allowed to modify
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

Add modded attributes to this list to enable scaling.

### Boss Module

```toml
[bossModule]
# Enable the boss module
enabled = true

# Show a boss bar for boss mobs
showBossBar = true

# Maximum distance (in blocks) at which boss bars are visible
bossBarRenderDistance = 64

# Prevent boss mobs from despawning naturally
preventDespawn = true

# Apply glowing effect to boss mobs
glowEffect = true

# Announce boss spawns to nearby players
spawnAnnouncement = true

# Radius (in blocks) for boss spawn announcements
announcementRadius = 64
```

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `true` | Master toggle for boss module |
| `showBossBar` | Boolean | `true` | Display boss health bars |
| `bossBarRenderDistance` | Integer | `64` | Boss bar visibility range |
| `preventDespawn` | Boolean | `true` | Bosses won't despawn |
| `glowEffect` | Boolean | `true` | Bosses glow by default |
| `spawnAnnouncement` | Boolean | `true` | Announce boss spawns |
| `announcementRadius` | Integer | `64` | Announcement range |

## Example Configurations

### Easy Mode (Casual)

```toml
[general]
enabled = true
showLevelInName = true

[levels]
defaultMinLevel = 1
defaultMaxLevel = 25
globalLevelCap = 50

[distance]
distanceStartRadius = 500
distancePerLevel = 200
distanceLevelMultiplier = 0.5

[bossModule]
enabled = false
```

### Hard Mode (Challenge)

```toml
[general]
enabled = true
showLevelInName = true

[levels]
defaultMinLevel = 10
defaultMaxLevel = 500
globalLevelCap = 1000

[distance]
distanceStartRadius = 50
distancePerLevel = 25
distanceLevelMultiplier = 2.0

[filtering]
applyToPassiveMobs = true
leveledPassivesCanAttack = true

[bossModule]
enabled = true
showBossBar = true
bossBarRenderDistance = 128
```

### Structure-Only Mode

```toml
[ruleToggles]
structureLevelingEnabled = true
biomeLevelingEnabled = false
dimensionLevelingEnabled = false
distanceLevelingEnabled = false

[levels]
defaultMinLevel = 1
defaultMaxLevel = 1
globalLevelCap = 10000

[filtering]
structureOverridesPassiveFilter = true
structureOverridesBossFilter = true
```

### Dimension-Driven Mode

Let datapacks do all the heavy lifting via dimension/biome/structure rules, with base rules disabled:

```toml
[ruleToggles]
structureLevelingEnabled = true
biomeLevelingEnabled = true
dimensionLevelingEnabled = true
distanceLevelingEnabled = false

[levels]
defaultMinLevel = 1
defaultMaxLevel = 1
globalLevelCap = 10000
```

## Reloading Config

Changes to the config file require a game restart to take effect. Datapack changes can be applied with `/reload`.

## Troubleshooting

### Levels Not Appearing

1. Check `enabled = true`
2. Check `showLevelInName = true`
3. Verify mob isn't in `mobBlacklist`
4. Enable `debugMode` to see rule matching

### Levels Too High/Low

1. Check `globalLevelCap`
2. Review `defaultMinLevel` and `defaultMaxLevel`
3. Check if datapack rules have `ignore_level_cap`

### Bosses Not Spawning

1. Check `bossModule.enabled = true`
2. Verify boss rule's `spawn_chance`
3. Check structure/biome restrictions
4. Enable `debugMode`
