# Configuration

Config file: `config/botzmobleveling-common.toml` (generated on first launch).
Config changes require a restart; **datapack** rule changes apply with `/reload`.

> This is the 1.21.1 layout. It differs entirely from 1.20.1 — there is no
> `[ruleToggles]`, `[filtering]`, or attribute whitelist; distance scaling and the
> boss module are configured differently.

## `[general]`
```toml
[general]
enabled = true     # master toggle for the whole mod
debugMode = false  # log spawn/level/rule-matching detail
```

## `[leveling]`
```toml
[leveling]
globalLevelCap = 100      # hard cap on total level (base + kill levels)
levelPassiveMobs = false  # also level passive/neutral mobs
levelBossMobs = false     # also level vanilla bosses (Wither, Ender Dragon)
mobBlacklist = []         # entity ids that are never leveled
```
Datapack rules/overrides can exceed `globalLevelCap` with `ignore_level_cap`.

### Distance scaling — `[leveling.distance]`
```toml
[leveling.distance]
originX = 0        # world X origin for distance mode
originZ = 0        # world Z origin for distance mode
scaleFactor = 1.0  # global multiplier on the distance formula
```
For a `distance`-mode rule: `level ≈ distanceFromOrigin × distance_scale × scaleFactor`,
clamped to the rule's `min_level`/`max_level` (and the global cap unless exempt).

## `[killLeveling]`
```toml
[killLeveling]
enabled = true     # mobs gain levels by killing other entities
xpBase = 100       # XP for the first kill-level
xpScale = 1.5      # threshold = xpBase * xpScale^killLevel
killLevelCap = 0   # max kill-levels per mob (0 = unlimited up to globalLevelCap)
```

## `[adaptiveDifficulty]`
```toml
[adaptiveDifficulty]
enabled = true       # bonus levels based on nearby players' gear
scanRadius = 48.0    # block radius to scan for players
maxBonus = 10        # max bonus levels added
minGearScore = 20.0  # players below this gear score don't ramp difficulty
```
Gear scoring runs off-thread; the spawn read is instant.

## `[lootScaling]`  (opt-in)
```toml
[lootScaling]
enabled = false             # leveled mobs grant extra effective Looting on death
lootingBonusPerLevels = 10  # +1 effective Looting per N levels
maxLootingBonus = 10        # cap on the granted Looting bonus
```
Grows stackable drops only; never duplicates equipment.

## `[display]`
```toml
[display]
enabled = true               # show the level name tag
showElementalTitle = true    # append a dominant-element title (e.g. ★Fire)
elementalTitleMinValue = 5.0 # min elemental stat before the title shows
showKillCount = false        # append (×N kills)
hideManaStatsIfNoISS = true  # hide mana labels when Iron's Spells isn't loaded
```

### Color tiers — `[display.colorTiers]`
```toml
[display.colorTiers]
lowMax = 10   # 1..10   white
midMax = 25   # 11..25  green
highMax = 50  # 26..50  yellow
epicMax = 75  # 51..75  gold; above -> red; bosses -> dark red
```

## `[boss]`
```toml
[boss]
enabled = true          # boss module master toggle
bossBarEnabled = true   # show boss bars
announceSpawn = true    # broadcast boss spawns
glow = true             # bosses glow by default
healthMultiplier = 3.0  # fallback when a boss rule's health_multiplier <= 1.0
damageMultiplier = 1.5  # default boss outgoing-damage multiplier
announceRadius = 128    # block radius for the spawn announcement
```
Per-boss bar visibility (global vs local radius) is set with `boss_bar_range` in the
boss rule — see [Boss Rules](Boss-Rules).

## `[huntingAI]`
```toml
[huntingAI]
enabled = true  # inject melee/target goals into leveled mobs that lack them
minLevel = 5    # minimum level before hunting AI is injected
```

## `[stats]` — per-level increments
```toml
[stats]
vigor_increment = 1.0           # +max health / level
strength_increment = 0.5        # +attack damage / level
endurance_increment = 0.4       # +damage reduction % / level (see caps)
stamina_increment = 0.5         # stamina stat / level (exposed cross-mod)
dexterity_increment = 0.02      # +attack speed / level
agility_increment = 0.002       # +movement speed / level
attack_speed_increment = 0.025  # attack-speed stat / level
mana_pool_increment = 5.0       # mana pool / level (Iron's Spells)
mana_density_increment = 0.02   # mana density / level (Iron's Spells)
elemental_increment = 0.5       # each elemental stat / level
```
Health, attack damage, attack speed, and movement speed are applied as vanilla
attributes; endurance becomes damage reduction. Stamina, mana, and elemental stats
are computed and exposed to other Eidolon mods via the shared stat capability
(mana application to Iron's Spells is reserved for a future update).

## `[caps]`
```toml
[caps]
endurance_cap = 75.0    # max endurance damage reduction (%)
attack_speed_cap = 3.0  # max attack-speed multiplier
```

## Troubleshooting

- **No levels:** `enabled = true`? mob not in `mobBlacklist`? passive mob without `levelPassiveMobs`? Turn on `debugMode` to see rule matching.
- **Levels too high/low:** check `globalLevelCap` and your rules; remember `ignore_level_cap`.
- **No boss bar / no announcement:** check `[boss] enabled`, `bossBarEnabled`, `announceSpawn`, and the rule's `boss_bar_range`.
