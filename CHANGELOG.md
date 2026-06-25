# Changelog

All notable changes to BotzMobLeveling will be documented here.

---

## [1.0.6] - 2026-06-24

### Added
- **Level-based loot scaling (opt-in).** New `lootScaling` config section: leveled mobs can grant extra effective Looting when they die, increasing looting-affected drops (arrows, bones, ender pearls, most mob loot) in proportion to their level. `lootingBonusPerLevels` gives +1 Looting per N levels (default 10, so a level-50 mob drops like Looting V); `maxLootingBonus` caps it. Off by default. Implemented via `LootingLevelEvent` so it stacks with vanilla/modded loot tables without duplicating item entities. The effective level used is `base level + kill levels`.
  - **Reading the level externally (KubeJS / datapacks / other mods):** a mob's level lives in its Forge persistent data — `botzmobleveling_Level` (spawn level) and `botzmobleveling_KillLevel` (kill levels); total = the sum. In KubeJS: `entity.persistentData.getInt('botzmobleveling_Level') + entity.persistentData.getInt('botzmobleveling_KillLevel')`.

### Fixed
- **Boss `level` field is now actually applied.** Previously a boss rule's `level` was parsed but never used — the transformed boss kept whatever level the normal spawn rules produced (e.g. a distance-based level 68), and its nameplate, boss bar, and kill XP all reflected that instead of the configured boss level. `applyBossTransformation` now stamps the boss level onto the mob and re-runs the spawn rule's attribute scaling at that level (on top of the flat `stat_multipliers`), honoring `ignore_level_cap`.
- **`level_bonus` mob overrides now stack on top of distance/random scaling.** The bonus was being added *before* the level was clamped to the rule's `level_range`, so a `+50` bonus over a 1–100 rule was silently capped back to 100. The base level is now clamped first, then the bonus is added on top (still subject to the global level cap unless `ignore_level_cap` is set). This makes per-entity additive scaling — e.g. "this mob is always +50 levels above the surrounding mobs" — work as documented.

### Changed
- **Example datapack now uses proportional (percentage-of-base) scaling for health and attack damage.** `overworld_scaling.json` switched `minecraft:generic.max_health` and `minecraft:generic.attack_damage` from flat `addition` to `multiply_base` (+4%/level health, +3%/level damage). Flat additive bonuses don't keep up for high-base-HP modded mobs (a flat `+N` is huge for a 20 HP zombie but trivial for a 300 HP boss); percentage scaling keeps every mob proportionally threatening. Armor and movement speed remain additive (their base values are often 0, where a percentage does nothing).

### Fixed (audit pass)
- **Adaptive difficulty no longer silently bypasses the global level cap.** Any nearby geared player produced an adaptive bonus, which set an internal "ignore cap" flag — so `globalLevelCap` was effectively void whenever adaptive difficulty (on by default) saw a geared player. The adaptive bonus is now added on top of the level but still clamped to `globalLevelCap`.
- **Adaptive stat/equipment modifiers now respect the leveling filters.** They were applied by a separate spawn subscriber to *every* mob — ignoring the blacklist, passive/boss filters, and the world-ready gate — and recomputed the player gear score a second time. Adaptive modifiers are now applied from the spawn handler only to mobs that passed all leveling checks, reusing the single gear score computed during resolution.
- **Boss bars now survive chunk reloads and server restarts.** The in-memory boss-bar map was only populated at spawn; a reloaded boss permanently lost its bar. Bars are now lazily recreated for saved bosses.
- **`particle_effect` boss field is now implemented** — it was parsed and documented but never spawned particles. Simple particle types now emit around the boss each tick.

### Performance
- **Per-spawn structure lookups are cheaper.** `BossManager` no longer scans the entire structure registry per mob, and `LevelResolver` short-circuits when no structure references the spawn position — both now use `StructureManager.getAllStructuresAt`.

### Notes
- `size_multiplier` (boss field) is parsed but **not functional in 1.20.1** — there is no entity scale attribute, so model resizing needs a rendering mixin that does not yet exist. Documented as not-yet-implemented.
- New config `adaptiveDifficulty.minGearScore` (default 20.0) gates adaptive stat/equipment modifiers, replacing a hardcoded test threshold.
- Replaced stray `System.out.println` debug output in the adaptive package with proper logger calls; removed per-spawn `INFO` log spam.

### Fixed (audit pass 2)
- **Boss minions can no longer recursively become bosses.** A boss summoning minions of its own type (e.g. Cluckthulhu's chickens inside a stronghold, where the chicken→boss rule also applies) could turn each minion into a fresh boss, cascading into a boss explosion. Minions are now flagged on spawn and excluded from boss transformation.
- **Ranged mobs now earn kill XP.** Kill leveling read the *direct* damage source, so a skeleton's arrow (not the skeleton) was seen as the killer and no XP was granted. It now credits the responsible attacker, covering both melee and ranged kills.
- **Cap-exempt mobs keep their level after reload / kill level-ups.** `ignore_level_cap` (and fixed-level / boss) mobs above the global cap had their attribute scaling silently re-clamped to the cap on chunk reload or when gaining a kill level. The cap-exempt state is now persisted and honored, so a level-750 boss stays level-750 after unloading.

---

## [1.0.5] - 2026-05-10

### Fixed
- **Custom boss display name now applied to the entity nameplate.** Previously `BossManager` only stored the rule's `display_name` in NBT and used it for the boss bar — the mob's own nameplate kept the leveled vanilla type description (e.g. `[Lv.10] Ravager`) set earlier by `LevelDisplayManager`. The boss name only appeared at the top of the screen, never above the mob.
- `applyBossNameplate()` now calls `mob.setCustomName(...)` and `setCustomNameVisible(true)` immediately after stat transformation, with the level prefix included when `showLevelInName` is enabled. The cached "original name" used by `LevelDisplayManager` is rewritten to the boss display name so future level-display refreshes (e.g. on kill XP gain) keep the boss name as the base instead of resurrecting the type description.

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
