# BotzMobLeveling — 1.21.1 NeoForge Port
**Mod ID:** `botzmobleveling`
**Target:** Minecraft 1.21.1 · NeoForge
**Java:** 21
**Original:** 1.20.1 Forge 47.4.0
**Hard Dependency:** `botz_lib` (stat system, shared interfaces)
**Soft Dependency:** `botz_ai` (reads mob stats via IStatHolder)
**Optional:** `epicfight`, `irons_spells_n_spellbooks`

> **Design decision:** No rank/letter class system (SSR→E removed entirely).
> One unified mode — level assigned by rules, stats scale linearly with level.
> Both this mod and `leveling_system` use the same stat keys (from botz_lib) but
> are independently configurable. Mob stat values and player stat values are separate
> configs even when the keys are identical.

---

## Lib Delegation

The following live in `botz_lib` — do not reimplement:
- `StatKey` constants → `com.botzlabz.lib.stats.StatKey`
- `StatBlock` data class → `com.botzlabz.lib.stats.StatBlock`
- `IStatHolder` interface → `com.botzlabz.lib.api.IStatHolder`
- `CapabilityHelper` utilities → `com.botzlabz.lib.capability.CapabilityHelper`
- `StaminaState` enum → `com.botzlabz.lib.api.StaminaState`

`MobStatBlock` (capability data) implements `IStatHolder` so `botz_ai` reads mob
stats without importing this mod directly.

---

## 1. Stat Definitions

One stat block per mob. All stats scale linearly with mob level. No rank multipliers.

| Stat Key         | minecraft Attribute / Effect                          | Base   | Per-Level Increment        | Conditional        |
|------------------|-------------------------------------------------------|--------|----------------------------|--------------------|
| `vigor`          | `minecraft:max_health`                                | +0     | +1.0 HP                    | Always             |
| `strength`       | `minecraft:attack_damage`                             | +0     | +0.5 dmg                   | Always             |
| `endurance`      | Damage reduction % (additive, hard cap 75%)           | 0%     | +0.4% per level            | Always             |
| `stamina`        | Stamina pool size (read by botz_ai)                | base   | +0.5 units per level       | Always             |
| `dexterity`      | `minecraft:attack_speed`                              | +0     | +0.02 per level            | Always             |
| `agility`        | `minecraft:movement_speed`                            | +0     | +0.002 per level           | Always             |
| `attack_speed`   | Weapon attack speed multiplier (hard cap ×3.0)        | ×1.0   | +0.025× per level          | Always             |
| `mana_pool`      | Max mana — ISS pool if loaded, internal pool if not   | +0     | +5 mana per level          | Show only if ISS loaded |
| `mana_density`   | Magic damage multiplier — hooks into ISS if loaded    | ×1.0   | +0.02× per level           | Show only if ISS loaded |
| `fire`           | Elemental affinity — stored, bonus via botz_elemental | +0     | +0.5% per level            | Stored always, applied by botz_elemental |
| `water`          | Elemental affinity — stored, bonus via botz_elemental | +0     | +0.5% per level            | Stored always, applied by botz_elemental |
| `earth`          | Elemental affinity — stored, bonus via botz_elemental | +0     | +0.5% per level            | Stored always, applied by botz_elemental |
| `air`            | Elemental affinity — stored, bonus via botz_elemental | +0     | +0.5% per level            | Stored always, applied by botz_elemental |
| `light`          | Elemental affinity — stored, bonus via botz_elemental | +0     | +0.5% per level            | Stored always, applied by botz_elemental |
| `dark`           | Elemental affinity — stored, bonus via botz_elemental | +0     | +0.5% per level            | Stored always, applied by botz_elemental |

### 1.1 Stat Application Notes

**Endurance** — does not map to a vanilla attribute. Applied in `LivingIncomingDamageEvent`:
```
reducedDamage = incomingDamage × (1 - min(enduranceValue / 100, 0.75))
```
Stacks additively with vanilla armor (armor runs first, then endurance reduction applies to post-armor damage).

**Stamina** — mob stamina pool size is passed to `botz_ai` StaminaCapability on spawn.
Stamina itself is managed by botz_ai; this mod only sets the pool size.
For mobs, the hunger-drain reduction from stamina (player mechanic) does NOT apply.

**Attack Speed** — separate from Dexterity. Dexterity modifies `minecraft:attack_speed` attribute
(vanilla, affects animation timing). Attack Speed is an internal multiplier on damage dealt
within an attack window (handled by eidolon_combat or botz_ai if loaded, else stored only).

**Mana Pool / Mana Density** — if ISS is NOT loaded:
- Mana pool is tracked internally (simple float on the capability, not shown to player)
- Mana density stored but has no effect until ISS or another magic mod hooks it
- These stats are still written to NBT regardless — so loading ISS later preserves the values
- UI: stat names hidden on mob name display if ISS absent (config toggle)

**Elemental stats** — always stored in the stat block and exposed via `IStatHolder`.
`botz_elemental` reads them to apply damage bonuses and affinities.
`botz_ai` reads the dominant elemental stat to bias spell school selection.
This mod applies NO direct game effect from elemental stats — pure data layer only.

---

## 2. Level System

### 2.1 Level Resolution (4-Tier Priority, unchanged from original)
1. Structure Rules (highest)
2. Biome Rules
3. Dimension Rules
4. Base Rules (fallback)

### 2.2 Level Assignment Modes
- **Fixed:** Exact level from rule
- **Random:** Random within `min`/`max` range
- **Distance:** Scales with distance from world spawn

### 2.3 Stat Calculation from Level
```
statValue(key, level) = statBase(key) + statIncrement(key) × level
```
All increments are configurable in `botzmobleveling-common.toml` under `[stats]`.
The table in §1 is the default — pack makers can override any increment.

### 2.4 Kill Leveling
Mob gains XP when it kills another entity. `xpBank` accumulates; converts to kill levels
at exponential threshold. `totalLevel = min(baseLevel + killLevel, globalLevelCap)`.
Kill levels raise all stats equally (same level multiplier applied to all stat keys).

### 2.5 Adaptive Difficulty
Scans nearby player gear quality. Adds bonus levels proportional to gear score.
Applies before stat calculation — bonus levels feed into the same stat formula.

---

## 3. Attribute Application

`AttributeScalingManager` applies stats to vanilla attributes on spawn and on level change.

| Stat          | Attribute Modified                    | Operation              |
|---------------|---------------------------------------|------------------------|
| `vigor`       | `minecraft:max_health`                | ADD_VALUE              |
| `strength`    | `minecraft:attack_damage`             | ADD_VALUE              |
| `dexterity`   | `minecraft:attack_speed`              | ADD_VALUE              |
| `agility`     | `minecraft:movement_speed`            | ADD_VALUE              |

All modifiers keyed by `ResourceLocation` (NeoForge 1.21.1 — no UUID keys):
```java
new AttributeModifier(
    ResourceLocation.fromNamespaceAndPath("botzmobleveling", "vigor_bonus"),
    statValue, AttributeModifier.Operation.ADD_VALUE
)
```
Remove by ResourceLocation key before re-applying (level change / kill level).

`endurance`, `stamina`, `attack_speed`, `mana_pool`, `mana_density`, and elemental stats
are NOT applied via vanilla attribute system — they are read directly from the capability
by botz_ai, eidolon_combat, and botz_elemental.

---

## 4. Boss Transformation

Special rules trigger boss behavior:
- Boss bar display
- Minion spawning at HP thresholds
- Despawn prevention
- Spawn announcements

Boss level thresholds configurable per rule. Boss stats follow the same formula — no
special boss stat block. Level drives everything.

---

## 5. Display

Default format: `[Lvl X] MobName`

Optional elemental title: `[Lvl 12 ★Dark] Zombie`
- Shows only if dominant elemental stat is above configured threshold
- Threshold configurable: `elementalTitleMinValue` (default: 5.0)
- Config toggle: `showElementalTitle = true | false`
- If ISS absent: mana stats hidden from any display

---

## 6. NeoForge 1.21.1 Migration — Breaking Changes

### 6.1 Capability System
```
OLD: implements ICapabilityProvider / @CapabilityInject / LazyOptional<T>
NEW: RegisterCapabilitiesEvent (mod bus) + AttachCapabilitiesEvent<Entity> (game bus)
     entity.getCapability(cap) returns T | null — no LazyOptional
```

### 6.2 AttributeModifier Keys
```
OLD: UUID-keyed AttributeModifier
NEW: ResourceLocation-keyed AttributeModifier
```

### 6.3 Event Bus References
```
OLD: MinecraftForge.EVENT_BUS / @Mod.EventBusSubscriber(bus = Bus.FORGE)
NEW: NeoForge.EVENT_BUS / @EventBusSubscriber(bus = Bus.GAME)
```

### 6.4 Mod Loading Context
```
OLD: FMLJavaModLoadingContext.get().getModEventBus()
NEW: IEventBus modEventBus injected into @Mod constructor
```

### 6.5 Config
```
OLD: ForgeConfigSpec.Builder
NEW: ModConfigSpec.Builder (net.neoforged.neoforge.common.ModConfigSpec)
```

### 6.6 Network
```
OLD: SimpleChannel
NEW: RegisterPayloadHandlersEvent + registrar.playBidirectional(...)
```

### 6.7 Spawn Event
```
Confirm package: net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent
```

---

## 7. Config File (`botzmobleveling-common.toml`)

```toml
[leveling]
  globalLevelCap = 100
  killLevelingEnabled = true
  adaptiveDifficultyEnabled = true

[display]
  showElementalTitle = true
  elementalTitleMinValue = 5.0
  hideManaStatsIfNoISS = true

[stats]
  # All increments configurable — defaults match §1 table
  vigor_increment       = 1.0
  strength_increment    = 0.5
  endurance_increment   = 0.4
  stamina_increment     = 0.5
  dexterity_increment   = 0.02
  agility_increment     = 0.002
  attack_speed_increment = 0.025
  mana_pool_increment   = 5.0
  mana_density_increment = 0.02
  elemental_increment   = 0.5

[caps]
  endurance_cap         = 75.0   # percent
  attack_speed_cap      = 3.0    # multiplier on base
```

---

## 8. NBT Structure

```json
{
  "botzmobleveling": {
    "baseLevel": 5,
    "killLevel": 2,
    "totalLevel": 7,
    "processed": true,
    "xpBank": 140,
    "killCount": 3,
    "persistent": true,
    "sourceRuleId": "overworld_base",
    "sourceRuleType": "base",
    "stats": {
      "vigor":        7.0,
      "strength":     3.5,
      "endurance":    2.8,
      "stamina":      3.5,
      "dexterity":    0.14,
      "agility":      0.014,
      "attack_speed": 1.175,
      "mana_pool":    35.0,
      "mana_density": 1.14,
      "fire":         3.5,
      "water":        3.5,
      "earth":        3.5,
      "air":          3.5,
      "light":        3.5,
      "dark":         3.5
    }
  }
}
```
Values shown for a level-7 mob using default increments.

---

## 9. Project Architecture

```
BotzMobLeveling/
├── src/main/java/com/botzlabz/mobleveling/
│   ├── BotzMobLeveling.java
│   ├── event/
│   │   ├── MobSpawnHandler.java          # FinalizeSpawnEvent — assign level + stats
│   │   ├── MobKillHandler.java           # kill leveling XP
│   │   └── DamageReductionHandler.java   # LivingIncomingDamageEvent — endurance %
│   ├── level/
│   │   ├── MobLevelData.java             # Capability data — NeoForge rewrite
│   │   ├── LevelResolver.java            # 4-tier rule resolution
│   │   └── KillLevel.java
│   ├── stats/
│   │   ├── MobStatBlock.java             # implements IStatHolder, NBT save/load
│   │   └── StatScalingManager.java       # computes stat values from level
│   ├── attribute/
│   │   └── AttributeScalingManager.java  # applies vigor/strength/dex/agility to attributes
│   ├── boss/
│   │   └── BossManager.java
│   ├── config/
│   │   └── MobLevelingConfig.java
│   ├── data/
│   │   ├── Rule.java
│   │   ├── BiomeRules.java
│   │   ├── DimensionRules.java
│   │   ├── StructureRules.java
│   │   ├── MobOverride.java
│   │   └── LevelCalculator.java
│   ├── adaptive/
│   │   └── DifficultyCalculator.java
│   ├── display/
│   │   └── LevelDisplay.java
│   ├── network/
│   │   └── NetworkHandler.java           # RegisterPayloadHandlersEvent rewrite
│   └── compat/
│       ├── EpicFightIntegration.java
│       ├── ISSIntegration.java           # ISS mana hook — conditional on ModList
│       └── BotzAIIntegration.java     # exposes IStatHolder to botz_ai
└── src/main/resources/
    ├── META-INF/neoforge.mods.toml
    └── data/botzmobleveling/
        └── (default rule JSONs)
```

---

## 10. Implementation Order

1. Gradle setup — copy from botz_lib, update modId
2. `MobLevelingConfig` — ModConfigSpec with all keys from §7
3. `MobLevelData` capability — NeoForge: RegisterCapabilitiesEvent + AttachCapabilitiesEvent
4. `MobStatBlock` — implements IStatHolder, stores stat floats, NBT save/load
5. `StatScalingManager` — computes statValue(key, level) for all keys
6. `AttributeScalingManager` — applies vigor/strength/dex/agility via ResourceLocation-keyed modifiers
7. `LevelResolver` — 4-tier rule system, JSON loading
8. `MobSpawnHandler` — FinalizeSpawnEvent: resolve level → compute stats → apply attributes
9. `DamageReductionHandler` — LivingIncomingDamageEvent: endurance % reduction
10. `MobKillHandler` + `KillLevel` — kill XP + level-up pipeline
11. `DifficultyCalculator` — adaptive difficulty gear scan
12. `BossManager` — boss threshold rules
13. `LevelDisplay` — name tag format, elemental title
14. `NetworkHandler` — sync level/stats to client for display
15. `ISSIntegration` — conditional mana hook
16. `EpicFightIntegration` — verify 1.21.1 EF API
17. `BotzAIIntegration` — expose IStatHolder, guard with ModList check
