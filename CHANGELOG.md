# Changelog

All notable changes to BotzMobLeveling will be documented here.

---

## [1.0.3] - 2026-03-18

### Added
- **Kill Leveling System** — Mobs gain XP and levels by killing other mobs or players
  - XP scales with the victim's total level (base level + kill level)
  - Players award a configurable bonus on top of normal XP
  - Mobs level up when accumulated XP crosses the threshold (`kill_base_xp_required * kill_xp_scaling ^ (killLevel-1)`)
  - Kill level is capped by `kill_max_level` and cannot push a mob beyond `global_level_cap` for attribute purposes
  - Attributes are reapplied at `min(baseLevel + killLevel, globalLevelCap)` on every level-up and on chunk reload
  - `kill_apply_to_any_mob` toggle — when `true`, any mob can gain kill XP regardless of datapack rules

- **Persistence on First Kill** — Mobs that earn their first kill are automatically marked persistent (`setPersistenceRequired(true)`) so they won't despawn; controlled by `kill_make_persistent` config

- **Kill Indicator in Name Tag** — A configurable prefix (default `★ `) is prepended to leveled mob names once they have at least one kill
  - `kill_show_indicator` — toggle the indicator on/off
  - `kill_indicator_format` — text template; use `{kills}` to embed the kill count
  - `kill_indicator_color` — Minecraft color name for the indicator text

- **Hunt-to-Level AI** — Per-rule datapack toggle `"hunt_to_level": true` injects `MeleeAttackGoal` and `NearestAttackableTargetGoal<Mob>` into any `PathfinderMob`, letting it actively hunt other mobs to accumulate kill XP even without a player nearby
  - Goals are added at low priority so native combat behaviour takes precedence
  - An NBT flag (`botzmobleveling_HuntGoalsAdded`) prevents duplicate goal injection and survives chunk reload

- **Hunt Chance** — `"hunt_to_level_chance": 0.0–1.0` per rule (and `hunt_to_level_chance` global config fallback) controls the fraction of mobs that actually receive hunting AI; per-rule value always takes precedence

### Config additions (`botzmobleveling-common.toml`)
`killLeveling` section:
- `killLevelingEnabled`, `huntToLevelEnabled`, `huntToLevelChance`
- `killApplyToAnyMob`, `killXPBase`, `killXPPerVictimLevel`, `killXPPlayerBonus`
- `killBaseXPRequired`, `killXPScaling`, `killMaxLevel`
- `killMakePersistent`, `killShowIndicator`, `killIndicatorFormat`, `killIndicatorColor`

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
