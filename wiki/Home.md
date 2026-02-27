# BotzMobLeveling Wiki

Welcome to the **BotzMobLeveling** wiki! This mod adds a comprehensive, data-driven mob leveling system to Minecraft, allowing you to create custom difficulty scaling based on structures, biomes, and distance from spawn.

## Features

- **Data-Driven Leveling** - Full datapack support for customizing mob levels
- **Priority-Based Rules** - Structure > Biome > Base rules (higher priority wins)
- **Boss Module** - Transform mobs into powerful bosses with custom abilities
- **Passive Mob Combat** - Leveled passive mobs can fight back when attacked
- **Attribute Scaling** - Scale health, damage, speed, and more per level
- **Fully Reloadable** - Use `/reload` to apply changes without restarting

## Quick Links

- [Getting Started](Getting-Started)
- [Datapack Structure](Datapack-Structure)
- [Structure Rules](Structure-Rules)
- [Biome Rules](Biome-Rules)
- [Dimension Rules](Dimension-Rules)
- [Boss Rules](Boss-Rules)
- [Mob Overrides](Mob-Overrides)
- [Attribute Scaling](Attribute-Scaling)
- [Configuration](Configuration)
- [Examples](Examples)

## Rule Priority System

When a mob spawns, the system checks rules in this order:

1. **Structure Rules** (Priority 100+) - Highest priority, for mobs in specific structures
2. **Biome Rules** (Priority 50) - For mobs in specific biomes
3. **Dimension Rules** (Priority 30) - Dimension-wide baselines (Nether, End, modded dimensions)
4. **Base Rules** (Distance-based) - Lowest priority, default fallback

Higher priority numbers always win. Within the same type, rules are sorted by their `priority` value.

## Installation

1. Install Minecraft Forge 1.20.1
2. Place the mod JAR in your `mods` folder
3. Create datapacks in `world/datapacks/` for custom rules
4. Use `/reload` to apply changes

## Support

Report issues on our [GitHub Issues](https://github.com/DepthDrako/BotzMobLeveling/issues) page.
