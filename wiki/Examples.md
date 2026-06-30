# Examples

Copy-paste rules using the 1.21.1 schema. Drop each into the matching folder under
`data/botzmobleveling/mob_levels/`.

## Dungeon Difficulty

`structures/stronghold.json` — fixed Lv.200, with signature mobs:
```json
{
  "structure": "minecraft:stronghold",
  "priority": 100,
  "mode": "fixed",
  "level": 200,
  "mob_overrides": {
    "minecraft:silverfish": { "level": 100 },
    "minecraft:enderman":   { "level": 300, "ignore_level_cap": true }
  },
  "attribute_scaling": {
    "max_health":    { "operation": "add_value", "value_per_level": 1.0 },
    "attack_damage": { "operation": "add_value", "value_per_level": 0.05 }
  }
}
```

`structures/fortress.json` — random Lv.100–200, fire-themed:
```json
{
  "structure": "minecraft:fortress",
  "priority": 100,
  "mode": "random",
  "min_level": 100,
  "max_level": 200,
  "mob_overrides": {
    "minecraft:blaze":           { "level_bonus": 50 },
    "minecraft:wither_skeleton": { "level_bonus": 75, "ignore_level_cap": true }
  },
  "attribute_scaling": {
    "max_health":    { "operation": "add_value",    "value_per_level": 0.8 },
    "attack_damage": { "operation": "multiply_base", "value_per_level": 0.01 }
  }
}
```

## Progressive Overworld (distance)

`base/overworld.json` — hostiles scale with distance from the origin:
```json
{
  "mode": "distance",
  "hostile_only": true,
  "min_level": 1,
  "max_level": 200,
  "distance_scale": 0.02
}
```
Tune the origin and global multiplier in `[leveling.distance]` (see
[Configuration](Configuration#distance-scaling)).

## Biome Spike

`biomes/deep_dark.json` — fixed Lv.100, Warden far above the cap:
```json
{
  "biome": "minecraft:deep_dark",
  "priority": 50,
  "mode": "fixed",
  "level": 100,
  "mob_overrides": {
    "minecraft:warden": {
      "level": 1000,
      "ignore_level_cap": true,
      "attribute_scaling": {
        "max_health":    { "operation": "add_value", "value_per_level": 1.0 },
        "attack_damage": { "operation": "add_value", "value_per_level": 0.1 }
      }
    }
  }
}
```

## Dimension Baseline

`dimensions/the_nether.json` — floor of Lv.50–150 across the Nether:
```json
{
  "dimension": "minecraft:the_nether",
  "priority": 30,
  "mode": "random",
  "min_level": 50,
  "max_level": 150,
  "attribute_scaling": {
    "max_health":    { "operation": "add_value",    "value_per_level": 1.0 },
    "attack_damage": { "operation": "multiply_base", "value_per_level": 0.01 }
  }
}
```

## Boss Encounter

`bosses/cluckthulhu.json` — a scoped stronghold boss chicken:
```json
{
  "entity": "minecraft:chicken",
  "structure": "minecraft:stronghold",
  "mode": "fixed",
  "level": 750,
  "ignore_level_cap": true,
  "health_multiplier": 50.0,
  "damage_multiplier": 8.0,
  "boss_bar_color": "red",
  "boss_bar_title": "§4§l☠ Cluckthulhu ☠",
  "boss_bar_range": 80,
  "announcement": "§4§l☠ Cluckthulhu has awoken! ☠",
  "glow": true,
  "immunities": ["fire", "fall"],
  "minion_type": "minecraft:chicken",
  "minion_count": 6,
  "minion_spread": 5
}
```
> Scoped to `entity: chicken` **inside** strongholds, so normal chickens are
> unaffected. Bosses always trigger on a match (no spawn chance) — keep them scoped.

## Hostile Passive Mobs

Make village cows fight back at Lv.40 (also set `levelPassiveMobs = true` or rely on
this rule's `entity` scope):
```json
{
  "structure": "minecraft:village_plains",
  "entity": "minecraft:cow",
  "mode": "fixed",
  "level": 40,
  "attribute_scaling": {
    "attack_damage": { "operation": "add_value", "value_per_level": 0.2 }
  }
}
```
Leveled passive mobs automatically gain retaliation AI.

## Modded Content

Any modded entity, biome, dimension, structure, or attribute works — use its
registry id:
```json
{
  "dimension": "twilightforest:twilight_forest",
  "mode": "random",
  "min_level": 75,
  "max_level": 175,
  "attribute_scaling": {
    "max_health": { "operation": "add_value", "value_per_level": 1.2 },
    "some_mod:custom_attribute": { "operation": "add_value", "value_per_level": 1.0 }
  }
}
```
