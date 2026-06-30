# Datapack Structure

Folder layout and conventions for BotzMobLeveling rules on 1.21.1.

## Directory Layout

```
your_datapack/
├── pack.mcmeta
└── data/
    └── botzmobleveling/
        └── mob_levels/
            ├── base/            # default fallback rules
            ├── biomes/          # biome-matched rules
            ├── dimensions/      # dimension-matched rules
            ├── structures/      # structure-matched rules
            └── bosses/          # boss rules
```

All rules live under the `botzmobleveling` namespace at
`data/botzmobleveling/mob_levels/<category>/<name>.json`. The **folder** decides
the rule's category (and therefore its resolution priority); the **file name**
becomes the rule's internal id.

## Resolution Order

For each spawning mob the first matching rule wins, checked by category:

```
bosses → structures → biomes → dimensions → base → fallback (Lv.1)
```

Within a category, rules are sorted by their `priority` field (higher first).

## Common Rule Fields

These apply to rules in any category (boss rules add more — see [Boss Rules](Boss-Rules)):

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `mode` | String | `random` | `fixed`, `random`, `distance`, or `skip`. |
| `level` | Integer | `1` | Level for `fixed` mode (also the override fixed level). |
| `min_level` | Integer | `1` | Lower bound for `random` / `distance`. |
| `max_level` | Integer | `random`: `max(level,min)`, `distance`: unbounded | Upper bound. For `distance`, omit to stay unbounded (global cap still applies). |
| `distance_scale` | Double | `0.0` | Levels per block from the origin (`distance` mode). |
| `priority` | Integer | `0` | Higher = checked first within the category. |
| `entity` | String | `""` | Restrict to one entity id (full or path). Empty = any. |
| `biome` | String | `""` | (biome rules) biome id, full or path. |
| `dimension` | String | `""` | (dimension/other rules) dimension id. |
| `structure` | String | `""` | (structure rules) structure id. |
| `hostile_only` | Boolean | `false` | Match only monsters. |
| `passive_only` | Boolean | `false` | Match only passive/neutral mobs. |
| `ignore_level_cap` | Boolean | `false` | Matched mobs may exceed the global cap. |
| `mob_overrides` | Object | `{}` | Per-entity tweaks — see [Mob Overrides](Mob-Overrides). |
| `attribute_scaling` | Object | `{}` | Per-attribute scaling — see [Attribute Scaling](Attribute-Scaling). |

> **There is no per-rule `enabled` field.** To stop matched mobs from being leveled,
> use `"mode": "skip"`, or simply remove the file. (1.20.1's `enabled`,
> `ignore_distance_scaling`, `distance_multiplier`, `level_range`, `biome_tags`, and
> `dimensions` array are **not** used in 1.21.1.)

## File Naming

- Lowercase, underscores for spaces, `.json` extension.
- Good: `stronghold_zombies.json`, `desert_skeleton.json`
- Bad: `Stronghold Zombies.json`, `desert-skeleton.json`, `rule.txt`

## pack.mcmeta

```json
{
  "pack": {
    "pack_format": 48,
    "description": "My custom mob leveling datapack"
  }
}
```

`pack_format` **48** = Minecraft 1.21.1.

## Reloading

Run `/reload` after editing rules. Changes apply to **newly spawned** mobs;
already-spawned mobs keep their assigned level.

## Merging Datapacks

Multiple datapacks merge by category and `priority`. For mobs that match rules in
several categories, the higher category (boss > structure > …) wins; within a
category, higher `priority` wins, then load order.

## Troubleshooting

- **Rules not loading:** verify the folder path and JSON syntax; enable `debugMode` in the config and check the log for parse errors.
- **Wrong level:** check category order and `priority`; confirm the `entity`/`biome`/`dimension`/`structure` ids are exact.
- **Nothing happens for passive mobs:** set `levelPassiveMobs = true` in the config, or scope a rule with `passive_only`/`entity`.
