# Changelog

All notable changes to BotzMobLeveling will be documented here.

---

## [1.0.1] - 2026-02-27

### Added
- **Dimension Rules** — New datapack rule type for setting mob levels across entire dimensions
  - Supports single dimension (`"dimension": "minecraft:the_nether"`) or multiple at once (`"dimensions": [...]`)
  - Sits between Biome and Base rules in the priority chain: `Structure > Biome > Dimension > Base`
  - Supports all level modes: `fixed`, `random`, and `distance`
  - Supports `mob_overrides` and `attribute_scaling` just like other rule types
  - Default priority: `30` (below biome rules, above base rules)
  - Toggle via config: `dimensionLevelingEnabled`
  - Rule files go in `data/botzmobleveling/mob_levels/dimensions/`

- **GitHub Wiki** — Full documentation site covering all datapack features:
  - Getting Started guide with folder structure and first-rule walkthrough
  - Datapack Structure reference
  - Structure Rules, Biome Rules, Dimension Rules, Boss Rules pages
  - Mob Overrides and Attribute Scaling reference
  - Configuration page with all config options and TOML examples
  - Examples page with real-world use cases

### Changed
- Priority chain updated from 3 tiers to 4: `Structure → Biome → Dimension → Base`

---

## [1.0.0] - Initial Release

- Data-driven mob leveling system via datapacks
- Structure, Biome, and Base rule types
- Level modes: `fixed`, `random`, `distance`
- Per-mob overrides with `mob_overrides`
- Custom attribute scaling with `attribute_scaling`
- Boss rule support
- Passive mob combat support
- ForgeConfigSpec configuration file (`botzmobleveling-common.toml`)
- Display name formatting with configurable level tag (e.g. `[Lv.50] Zombie`)
