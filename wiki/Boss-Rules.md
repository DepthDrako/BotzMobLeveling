# Boss Rules

Boss rules turn matched mobs into boss encounters: health/damage multipliers, a
boss bar, immunities, glow, minions, and (via the normal level fields) a level.

## How Boss Rules Work

A boss rule is matched **exactly like any other rule** — by `entity`, optional
`structure` / `biome` / `dimension`, and `priority` — but it lives in `bosses/` and
is checked **first** (highest category). When it matches a spawning mob, that mob
**always** becomes a boss.

> **There is no `spawn_chance` in 1.21.1.** A matched mob always becomes a boss, so
> **scope your boss rules tightly** — pin them to a specific `entity` and usually a
> `structure`/`biome`/`dimension`, or every matching mob turns into a boss. (1.20.1's
> `target_mobs`, `spawn_chance`, `tier`, `size_multiplier`, `glow_color`,
> `particle_effect`, `xp_multiplier`, `loot_table`, `stat_multipliers`, boss-bar
> `style`, and the multi-wave `minions` object are **not** part of 1.21.1.)

## Location

```
data/botzmobleveling/mob_levels/bosses/<rulename>.json
```

## Fields

A boss rule supports all [common rule fields](Datapack-Structure#common-rule-fields)
(`entity`, `mode`, `level`, `structure`/`biome`/`dimension`, `priority`,
`ignore_level_cap`, `attribute_scaling`, …) **plus** these boss-specific fields:

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `health_multiplier` | Double | `3.0` | Multiplies the boss's **base** max health. (≤ 1.0 falls back to config `healthMultiplier`.) |
| `damage_multiplier` | Double | `1.5` | Multiplies the boss's **outgoing** damage. |
| `boss_bar_color` | String | `"red"` | `red`, `blue`, `green`, `yellow`, `purple`, `white`, `pink`. |
| `boss_bar_title` | String | `""` | Bar label. Empty = the mob's name. |
| `boss_bar_range` | Integer | `0` | **0 = global** (all players). **>0 = local** radius in blocks (only players within range, maintained as they move). |
| `announcement` | String | `""` | Spawn broadcast. Empty = a default message. Sent once, to players within the config `announceRadius`. |
| `glow` | Boolean | `true` | Apply the glowing effect. |
| `minion_type` | String | `""` | Entity id of minions to spawn. Empty = none. |
| `minion_count` | Integer | `0` | Number of minions spawned **once, at boss spawn**. |
| `minion_spread` | Integer | `5` | Spawn radius (blocks) for minions. |
| `immunities` | Array | `[]` | Damage/effect immunities — see below. |

### Boss level

The boss's **level** comes from the normal `mode` / `level` / `min_level` /
`max_level` fields. For a fixed boss above the global cap, use `mode: "fixed"` +
`level` + `ignore_level_cap: true`.

### Health vs damage

- `health_multiplier` scales the **base** max health (then the level's vigor bonus
  adds on top), and is **re-applied on reload** so bosses keep their HP across
  chunk unloads/restarts.
- `damage_multiplier` scales damage the boss **deals**.
- For finer control, add [Attribute Scaling](Attribute-Scaling) (e.g. armor,
  knockback resistance, movement speed).

### Immunities

Supported values: `fire`, `fall`, `explosion`, `wither`, `poison`.

| Value | Effect |
|-------|--------|
| `fire` | Immune to fire/lava damage. |
| `fall` | Immune to fall damage. |
| `explosion` | Immune to explosion damage. |
| `wither` | Immune to wither damage **and** the Wither effect. |
| `poison` | Immune to the Poison effect. |

## Examples

### Stronghold boss (scoped to a structure)
```json
{
  "entity": "minecraft:ravager",
  "structure": "minecraft:stronghold",
  "mode": "fixed",
  "level": 250,
  "ignore_level_cap": true,
  "health_multiplier": 5.0,
  "damage_multiplier": 2.0,
  "boss_bar_color": "purple",
  "boss_bar_title": "The Warden of the Deep",
  "boss_bar_range": 64,
  "announcement": "§5The Warden of the Deep stirs...",
  "glow": true,
  "immunities": ["fire", "explosion"],
  "minion_type": "minecraft:vindicator",
  "minion_count": 4,
  "minion_spread": 6,
  "attribute_scaling": {
    "armor":                { "operation": "add_value", "value_per_level": 0.2 },
    "knockback_resistance": { "operation": "add_value", "value_per_level": 0.004 }
  }
}
```

### Global "world boss"
```json
{
  "entity": "minecraft:skeleton",
  "dimension": "minecraft:overworld",
  "mode": "fixed",
  "level": 500,
  "ignore_level_cap": true,
  "health_multiplier": 20.0,
  "damage_multiplier": 4.0,
  "boss_bar_color": "yellow",
  "boss_bar_title": "§e§lSkeleton King",
  "boss_bar_range": 0,
  "immunities": ["fire", "fall"]
}
```

> The world-boss example will turn **every overworld skeleton** into the Skeleton
> King — only do that intentionally. For a single encounter, scope by a rare
> `structure`/`biome` or a unique `entity`.

## Config

Global boss settings (overridable per-rule where noted) — see [Configuration](Configuration#boss-module):

```toml
[boss]
enabled = true
bossBarEnabled = true
announceSpawn = true
glow = true
healthMultiplier = 3.0   # fallback when a rule's health_multiplier <= 1.0
damageMultiplier = 1.5
announceRadius = 128
```

## Reload Safety

Bosses re-register their boss bar and re-apply their health multiplier when their
chunk reloads — the spawn announcement is **not** repeated on reload.
