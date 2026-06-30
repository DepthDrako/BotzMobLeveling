package com.botzlabz.mobleveling.stats;

import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.lib.api.IStatHolder;
import com.botzlabz.lib.stats.StatBlock;
import com.botzlabz.lib.stats.StatKey;
import net.minecraft.nbt.CompoundTag;

/**
 * Computed stat set for a leveled mob.
 *
 * <p>Internally uses botz_lib's {@link StatBlock} for integer level
 * storage and NBT serialisation.  All stat levels are set equal to
 * {@code mobLevel} (uniform linear scaling, no rank system).
 *
 * <p>Float values (used by attribute modifiers, damage reduction, etc.)
 * are derived on-the-fly as {@code configIncrement × mobLevel}.
 *
 * <p>Implements {@link IStatHolder} so {@code botz_ai}, {@code eidolon_combat}
 * and other mods can read this mob's stats through the shared lib interface
 * without importing {@code botzmobleveling} directly.
 */
public class MobStatBlock implements IStatHolder {

    /** Backing integer-level store — shared with the rest of the Eidolon ecosystem. */
    private final StatBlock statBlock = new StatBlock();
    private int mobLevel = 0;

    // -------------------------------------------------------------------------
    // Computation
    // -------------------------------------------------------------------------

    /**
     * Recalculates all stat levels from {@code level}.
     * Every stat gets the same level value (uniform scaling model).
     */
    public void computeFromLevel(int level) {
        this.mobLevel = level;
        for (String key : StatKey.ALL) {
            statBlock.setStatLevel(key, level);
        }
        // No ranks assigned — getRankMultiplier() stays at 1.0 for all keys.
    }

    // -------------------------------------------------------------------------
    // Float value accessors (config-increment × mobLevel)
    // -------------------------------------------------------------------------

    public float getValue(String key) {
        return incrementFor(key) * mobLevel;
    }

    public String getDominantElement() {
        return StatKey.ELEMENTAL.stream()
            .max((a, b) -> Float.compare(getValue(a), getValue(b)))
            .orElse(StatKey.FIRE);
    }

    // -------------------------------------------------------------------------
    // Direct StatBlock access (for eidolon_combat / botz_ai)
    // -------------------------------------------------------------------------

    /**
     * Returns the underlying {@link StatBlock} so other Eidolon mods can call
     * {@code statBlock.computeValue(key, base, increment)} directly.
     */
    public StatBlock getStatBlock() {
        return statBlock;
    }

    // -------------------------------------------------------------------------
    // IStatHolder — cross-mod interface implementation
    // -------------------------------------------------------------------------

    /** Returns {@code mobLevel} for every stat (uniform scaling). */
    @Override public int     getStatLevel(String key)                            { return statBlock.getStatLevel(key); }
    /** Always 1.0 — no rank system on leveled mobs. */
    @Override public float   getRankMultiplier(String key)                       { return 1.0f; }
    /** base + increment × mobLevel (rank multiplier = 1.0). */
    @Override public float   computeStatValue(String key, float base, float inc) { return base + inc * mobLevel; }
    @Override public int     getEntityLevel()                                    { return mobLevel; }
    @Override public boolean hasRanks()                                          { return false; }

    // -------------------------------------------------------------------------
    // NBT — delegates to StatBlock
    // -------------------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", mobLevel);
        tag.put("statBlock", statBlock.save());
        return tag;
    }

    public void load(CompoundTag tag) {
        mobLevel = tag.getInt("level");
        if (tag.contains("statBlock")) {
            statBlock.load(tag.getCompound("statBlock"));
        } else {
            // Legacy format — reconstruct from level
            computeFromLevel(mobLevel);
        }
    }

    // -------------------------------------------------------------------------
    // Config-driven increment lookup
    // -------------------------------------------------------------------------

    private static float incrementFor(String key) {
        return switch (key) {
            case StatKey.VIGOR        -> MobLevelingConfig.VIGOR_INCREMENT.get().floatValue();
            case StatKey.STRENGTH     -> MobLevelingConfig.STRENGTH_INCREMENT.get().floatValue();
            case StatKey.ENDURANCE    -> MobLevelingConfig.ENDURANCE_INCREMENT.get().floatValue();
            case StatKey.STAMINA      -> MobLevelingConfig.STAMINA_INCREMENT.get().floatValue();
            case StatKey.DEXTERITY    -> MobLevelingConfig.DEXTERITY_INCREMENT.get().floatValue();
            case StatKey.AGILITY      -> MobLevelingConfig.AGILITY_INCREMENT.get().floatValue();
            case StatKey.ATTACK_SPEED -> MobLevelingConfig.ATTACK_SPEED_INCREMENT.get().floatValue();
            case StatKey.MANA_POOL    -> MobLevelingConfig.MANA_POOL_INCREMENT.get().floatValue();
            case StatKey.MANA_DENSITY -> MobLevelingConfig.MANA_DENSITY_INCREMENT.get().floatValue();
            default                   -> MobLevelingConfig.ELEMENTAL_INCREMENT.get().floatValue();
        };
    }
}
