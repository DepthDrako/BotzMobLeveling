# Structure Rules

Rules for mobs spawning inside a specific structure. Highest non-boss priority.

## Location

```
data/botzmobleveling/mob_levels/structures/<rulename>.json
```

## Fields

Structure rules use the [common rule fields](Datapack-Structure#common-rule-fields).
The targeting field is:

| Field | Type | Description |
|-------|------|-------------|
| `structure` | String | Structure id, full (`minecraft:stronghold`) or path (`stronghold`). |

All other fields (`mode`, `level`, `min_level`, `max_level`, `entity`, `priority`,
`ignore_level_cap`, `mob_overrides`, `attribute_scaling`, …) work as described in
[Datapack Structure](Datapack-Structure).

## Examples

### Fixed level
```json
{ "structure": "minecraft:stronghold", "mode": "fixed", "level": 100 }
```

### Random range
```json
{ "structure": "minecraft:fortress", "mode": "random", "min_level": 100, "max_level": 200 }
```

### With per-entity overrides + attribute scaling
```json
{
  "structure": "minecraft:stronghold",
  "priority": 100,
  "mode": "random",
  "min_level": 100,
  "max_level": 150,
  "mob_overrides": {
    "minecraft:zombie":   { "level": 200, "ignore_level_cap": true },
    "minecraft:skeleton": { "level_bonus": 50 }
  },
  "attribute_scaling": {
    "max_health":    { "operation": "add_value",     "value_per_level": 1.0 },
    "attack_damage": { "operation": "multiply_base",  "value_per_level": 0.01 }
  }
}
```

## Common Vanilla Structure IDs

| Structure | ID |
|-----------|-----|
| Stronghold | `minecraft:stronghold` |
| Nether Fortress | `minecraft:fortress` |
| Bastion Remnant | `minecraft:bastion_remnant` |
| Ocean Monument | `minecraft:monument` |
| Woodland Mansion | `minecraft:mansion` |
| End City | `minecraft:end_city` |
| Ancient City | `minecraft:ancient_city` |
| Pillager Outpost | `minecraft:pillager_outpost` |
| Trail Ruins | `minecraft:trail_ruins` |

> Structure matching uses the live structure at the mob's position, so it only
> resolves while the chunk is loaded.

## Tips

- Use `entity` (or `mob_overrides`) to give specific dungeon mobs signature levels.
- Combine with a [Boss Rule](Boss-Rules) for a structure's centerpiece encounter.
- `mode: "skip"` on a structure rule makes that structure a safe (un-leveled) zone.
