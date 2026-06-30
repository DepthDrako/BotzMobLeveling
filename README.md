# BotzMobLeveling — 1.21.1

A data-driven, level-based mob stat system. Mobs spawn with a level (from datapack
rules), gain scaled attributes, can earn further levels by killing, and optionally
scale to nearby players' gear. Includes a configurable boss module.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge 21.1.172+
- **Java:** 21
- **License:** MIT

> This is the **1.21.1 port** — a leaner ground-up rewrite of the original 1.20.1
> mod (on the [`main`](../../tree/main) branch). It is **not** a file-for-file copy:
> the architecture, package layout (`com.botzlabz.mobleveling`), and several
> subsystems differ.

## Dependencies

| Mod | Type | Notes |
|---|---|---|
| `botz_lib` | **required** | Shared library (capabilities, stat keys, weak-spot registry, async pool). |
| Iron's Spells 'n Spellbooks | optional | Mana stats are computed/exposed; direct application is a stub pending the ISS 1.21.1 API. |
| Epic Fight | optional | Integration hook is a placeholder in this mod (combat behaviour lives in companion mods). |

## Features

- **Data-driven levels** — rules under `data/<namespace>/mob_levels/{base,biomes,dimensions,structures,bosses}/*.json`, resolved by priority (boss → structure → biome → dimension → base → fallback). Modes: `fixed`, `random`, `distance`, `skip`.
- **Per-entity overrides** — `mob_overrides` apply a `level_bonus` or fixed `level` (and optional `ignore_level_cap` / `attribute_scaling`) to specific entity types within a rule.
- **Cap exemption** — `ignore_level_cap` lets a rule/override exceed the global level cap, persisted across reloads.
- **Datapack attribute scaling** — `attribute_scaling` maps any attribute to an op (`add_value` / `multiply_base` / `multiply_total`) × `value_per_level`, stacked on top of the config-increment stats.
- **Kill leveling** — mobs gain levels by killing other entities (configurable XP curve and cap).
- **Adaptive difficulty** — optional level bonus based on nearby players' gear score (computed off-thread).
- **Boss module** — health/damage multipliers, boss bar (global **or** local via `boss_bar_range`), color/title/announcement, glow, damage-type immunities, and minion spawning.
- **Display** — level name tags with config-driven color tiers and an optional dominant-element title.
- **Loot scaling** (opt-in), **hunting AI** injection, natural-despawn restoration for tagged mobs.
- **Public API & commands** — `BotzMobLevelingAPI` (area/mob level queries) and `/botzmobleveling arealevel|moblevel`.

Cross-mod consumers read a mob's stats via botz_lib's `STAT_HOLDER` capability
(or the mirrored `BML_Level` persistent-data int) — no compile dependency required.

## Building

```bash
./gradlew build
```

> **Note:** this project depends on a locally-built `botz_lib` jar at
> `../botz_lib/build/libs/botz_lib-1.0.0.jar` (see `build.gradle`). Build
> `botz_lib` first, or adjust the dependency to your setup.

The built jar lands in `build/libs/`.
