# Getting Started

This guide walks through setting up your first mob-leveling datapack on 1.21.1.

## Prerequisites

- Minecraft 1.21.1 + NeoForge 21.1.172+
- BotzMobLeveling **and eidolon_lib** installed

## Creating Your First Datapack

### 1. Folder structure

In your world's `datapacks` folder:

```
world/
└── datapacks/
    └── my_leveling_pack/
        ├── pack.mcmeta
        └── data/
            └── botzmobleveling/
                └── mob_levels/
                    ├── base/
                    ├── biomes/
                    ├── dimensions/
                    ├── structures/
                    └── bosses/
```

### 2. pack.mcmeta

```json
{
  "pack": {
    "pack_format": 48,
    "description": "My custom mob leveling rules"
  }
}
```

> `pack_format` **48** is for Minecraft 1.21.1.

### 3. Your first rule

`data/botzmobleveling/mob_levels/structures/stronghold_zombies.json` — zombies in
strongholds spawn at level 100:

```json
{
  "structure": "minecraft:stronghold",
  "entity": "minecraft:zombie",
  "priority": 100,
  "mode": "fixed",
  "level": 100
}
```

### 4. Apply

Run `/reload` in-game, then visit a stronghold.

## The Basics

### Level modes

| Mode | Description |
|------|-------------|
| `fixed` | Every matched mob gets `level`. |
| `random` | A random level in `[min_level, max_level]`. |
| `distance` | Scales with distance from the configured origin (see [Configuration](Configuration)). |
| `skip` | Matched mobs are **not** leveled (use to exempt specific mobs/areas). |

### Rule types

| Type | Folder | Matches |
|------|--------|---------|
| Boss | `bosses/` | Turns the matched mob into a boss. |
| Structure | `structures/` | Mobs inside a `structure`. |
| Biome | `biomes/` | Mobs in a `biome`. |
| Dimension | `dimensions/` | Mobs in a `dimension`. |
| Base | `base/` | Default fallback. |

The first matching rule (in that order; ties broken by `priority`) wins.

### What gets scaled

By default, a leveled mob receives, per level (config-tunable):

- **+Max Health** (vigor), **+Attack Damage** (strength), **+Attack Speed** (dexterity), **+Movement Speed** (agility)
- **Damage reduction** (endurance, capped)
- A name tag like `[Lv.100] Zombie`

Datapack [Attribute Scaling](Attribute-Scaling) can add scaling for **any** attribute on top of these.

## Filtering Which Mobs Are Leveled

Any rule may narrow what it matches:

| Field | Type | Meaning |
|-------|------|---------|
| `entity` | String | Only this entity id (full `minecraft:zombie` or path `zombie`). Omit = any mob. |
| `hostile_only` | Boolean | Only monsters. |
| `passive_only` | Boolean | Only passive/neutral mobs. |

By default, passive mobs are **not** leveled unless `levelPassiveMobs` is enabled in
the config (see [Configuration](Configuration)).

## Minimal Rules

Structure (fixed):
```json
{ "structure": "minecraft:stronghold", "mode": "fixed", "level": 50 }
```

Biome (random):
```json
{ "biome": "minecraft:desert", "mode": "random", "min_level": 10, "max_level": 30 }
```

Dimension (random):
```json
{ "dimension": "minecraft:the_nether", "mode": "random", "min_level": 50, "max_level": 150 }
```

Missing fields fall back to sensible defaults.

## Next Steps

- [Datapack Structure](Datapack-Structure) — folders, naming, reload behavior
- [Structure Rules](Structure-Rules) / [Biome Rules](Biome-Rules) / [Dimension Rules](Dimension-Rules)
- [Boss Rules](Boss-Rules) — boss encounters
- [Mob Overrides](Mob-Overrides) + [Attribute Scaling](Attribute-Scaling) — fine-tuning
