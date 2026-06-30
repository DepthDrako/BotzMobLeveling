# BotzMobLeveling Wiki (1.21.1)

Welcome to the **BotzMobLeveling** wiki for **Minecraft 1.21.1 / NeoForge**.

This mod adds a data-driven mob leveling system: mobs spawn with a level (from
datapack rules), gain scaled attributes, can earn more levels by killing, and can
optionally scale to nearby players' gear. A configurable boss module turns chosen
mobs into boss encounters with health/damage multipliers, boss bars, immunities,
and minions.

> **Version note:** this wiki documents the **1.21.1 NeoForge port**, which is a
> ground-up rewrite. Datapack fields, the config layout, and the boss system
> differ from the 1.20.1 version — do not mix 1.20.1 examples with 1.21.1.

## Features

- **Data-driven levels** — datapack rules per base / biome / dimension / structure / boss.
- **Priority resolution** — boss → structure → biome → dimension → base → fallback.
- **Level modes** — `fixed`, `random`, `distance`, `skip`.
- **Per-entity overrides** — `mob_overrides` apply a `level_bonus` or fixed `level` to specific mobs within a rule.
- **Cap exemption** — `ignore_level_cap` lets rules/overrides exceed the global cap (persisted across reloads).
- **Datapack attribute scaling** — scale any attribute via `add_value` / `multiply_base` / `multiply_total` per level.
- **Kill leveling** — mobs level up by killing other entities.
- **Adaptive difficulty** — optional bonus levels based on nearby players' gear.
- **Boss module** — multipliers, boss bar (global **or** local radius), immunities, minions.
- **Loot scaling** (opt-in), **hunting AI**, name-tag display with color tiers + elemental title.
- **Commands & API** for reading area/mob levels.
- **Fully reloadable** — `/reload` applies datapack changes to new spawns.

## Quick Links

- [Getting Started](Getting-Started)
- [Datapack Structure](Datapack-Structure)
- [Structure Rules](Structure-Rules) · [Biome Rules](Biome-Rules) · [Dimension Rules](Dimension-Rules)
- [Boss Rules](Boss-Rules)
- [Mob Overrides](Mob-Overrides) · [Attribute Scaling](Attribute-Scaling)
- [Configuration](Configuration)
- [Commands & API](Commands-and-API)
- [Examples](Examples)

## Rule Priority

When a mob spawns, the first matching rule wins, checked in this order:

1. **Boss rules** — turn the mob into a boss.
2. **Structure rules** — mobs inside a matched structure.
3. **Biome rules** — mobs in a matched biome.
4. **Dimension rules** — mobs in a matched dimension.
5. **Base rules** — the default fallback.
6. **Fallback** — level 1 if nothing matches.

Within a category, rules are sorted by their `priority` field (higher first).

## Requirements

- Minecraft **1.21.1**
- **NeoForge 21.1.172+**
- **botz_lib** (required dependency)
- Iron's Spells 'n Spellbooks, Epic Fight — optional

## Installation

1. Install NeoForge for 1.21.1.
2. Place the mod jar **and `botz_lib`** in your `mods` folder.
3. Add datapacks under `world/datapacks/` for custom rules.
4. Run `/reload` to apply rule changes.

## Support

Report issues on [GitHub Issues](https://github.com/DepthDrako/BotzMobLeveling/issues).
