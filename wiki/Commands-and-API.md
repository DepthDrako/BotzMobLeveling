# Commands & API

Read-only ways to query levels — for datapacks, command blocks, and other mods.

## Commands

All commands are **read-only** and available to everyone (permission level 0). By
default they print **nothing** and return the value as the command result (so a
datapack can capture it with `execute store result ...` and poll it without chat
spam). Append `print` to also echo the value to chat.

### `/botzmobleveling arealevel [pos] [print]`

The level a generic mob **would** spawn at, at your position (or `pos` if given) —
from the active structure → biome → dimension → base rules, clamped to the global
cap. Deterministic (no RNG), so it's stable when polled.

```
/botzmobleveling arealevel
/botzmobleveling arealevel 100 64 -200
/botzmobleveling arealevel ~ ~ ~ print
```

Capture into a scoreboard:
```
execute store result score @s area_level run botzmobleveling arealevel
```

### `/botzmobleveling moblevel <entity> [print]`

The total level of a specific mob (0 if it isn't leveled).

```
/botzmobleveling moblevel @e[type=zombie,limit=1,sort=nearest]
/botzmobleveling moblevel @e[type=zombie,limit=1,sort=nearest] print
```

## Public API (`BotzMobLevelingAPI`)

Server-side, side-effect free, safe to poll. All methods return `0` for unleveled
mobs.

| Method | Returns |
|--------|---------|
| `getAreaLevel(ServerLevel, BlockPos)` | The level a generic mob would spawn at (cap-clamped). |
| `getMobLevel(Mob)` | Total level (base + kill levels). |
| `getMobBaseLevel(Mob)` | Spawn (base) level. |
| `getMobKillLevel(Mob)` | Levels earned from kills. |

## Reading Levels From Other Mods (no dependency)

A mob's level/stats are exposed through **botz_lib's `STAT_HOLDER` capability**,
so other Eidolon mods can read them without depending on BotzMobLeveling:

```java
IStatHolder stats = entity.getCapability(BotzLibCapabilities.STAT_HOLDER);
int level = (stats != null) ? stats.getEntityLevel() : 0;
```

The total level is also mirrored to the mob's persistent data as the **`BML_Level`**
int, for callers that prefer raw NBT:

```
/data get entity <selector> ForgeData.BML_Level
```

> `BML_Level` reflects the cap-correct total level, including cap-exempt bosses that
> exceed the global cap.
