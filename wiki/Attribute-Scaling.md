# Attribute Scaling

`attribute_scaling` lets a datapack scale **any** attribute with mob level. It
stacks **on top of** the built-in per-level stats (it never replaces them).

## Built-in Per-Level Stats

Every leveled mob already gets these (per-level amounts are config-tunable):

| Attribute | Driven by | Default / level |
|-----------|-----------|-----------------|
| Max Health | vigor | +1.0 |
| Attack Damage | strength | +0.5 |
| Attack Speed | dexterity | +0.02 |
| Movement Speed | agility | +0.002 |
| Damage reduction | endurance | +0.4 % (capped, default 75 %) |

`attribute_scaling` adds further modifiers on top of these (as separate modifiers,
so they don't overwrite the built-ins).

## Syntax

`attribute_scaling` maps an attribute id to an **operation**. Two forms:

```json
"attribute_scaling": {
  "max_health": 2.0,
  "armor": { "operation": "add_value", "value_per_level": 0.5 },
  "attack_damage": { "operation": "multiply_base", "value_per_level": 0.01 }
}
```

- A **bare number** is shorthand for `add_value` with that `value_per_level`.
- The **object form** sets an explicit `operation` and `value_per_level`.

The modifier amount applied is **`value_per_level × mob level`**.

### Operations

| Operation | Meaning |
|-----------|---------|
| `add_value` | Add a flat amount: `value_per_level × level`. |
| `multiply_base` | Add `(value_per_level × level)` as a fraction of the attribute's **base** (e.g. `0.01`/level → +50 % base at Lv.50). |
| `multiply_total` | Multiply the **final** value after other modifiers. |

> Field names changed from 1.20.1. There is **no** `base_bonus`, no `max_bonus`,
> and the additive operation is `add_value` (not `addition`).

## Attribute IDs

You may write the id three ways — all resolve to the same attribute:

- bare path: `max_health`
- with the vanilla prefix: `generic.max_health`
- fully qualified: `minecraft:generic.armor`

If a bare path doesn't resolve, the mod retries with the `generic.` prefix (vanilla
attributes keep it on 1.21.1). Unknown attribute ids are logged once and skipped;
mobs that don't have a given attribute are skipped silently.

### Common attribute ids

| Id (bare) | Attribute |
|-----------|-----------|
| `max_health` | Max health |
| `attack_damage` | Attack damage |
| `attack_speed` | Attack speed |
| `armor` | Armor |
| `armor_toughness` | Armor toughness |
| `knockback_resistance` | Knockback resistance (0–1) |
| `movement_speed` | Movement speed |
| `follow_range` | Targeting range |
| `attack_knockback` | Knockback dealt |

Modded attributes work too — use their registry id.

## Examples

### Tanky scaling
```json
"attribute_scaling": {
  "max_health":           { "operation": "add_value",  "value_per_level": 3.0 },
  "armor":                { "operation": "add_value",  "value_per_level": 0.3 },
  "knockback_resistance": { "operation": "add_value",  "value_per_level": 0.004 }
}
```

### Percentage damage growth
```json
"attribute_scaling": {
  "attack_damage": { "operation": "multiply_base", "value_per_level": 0.02 }
}
```
At Lv.50 a base-3 attacker gains +100 % base (`0.02 × 50 = 1.0`) → 6 damage, on top
of the built-in strength bonus.

### Mixed, with shorthand
```json
"attribute_scaling": {
  "max_health": 5.0,
  "movement_speed": { "operation": "multiply_base", "value_per_level": 0.002 }
}
```

## Where It Applies

`attribute_scaling` can sit on a rule (applies to all mobs the rule levels) or
inside a [`mob_overrides`](Mob-Overrides) entry (applies to that mob; merged over
the rule's, override winning per attribute).

## Tips

- Prefer `multiply_base` for damage/speed so it scales with the mob's own base.
- Keep `multiply_total` for rare "final multiplier" effects — it compounds fast.
- High `movement_speed` can cause erratic AI; scale it gently.
