package com.botzlabz.mobleveling.adaptive;

/**
 * Gear-quality tiers used by {@link GearAnalyzer}.
 *
 * <p>Tier is determined by the combined gear score of the nearest players.
 * Each tier maps to a bonus level awarded to the spawning mob.
 */
public enum ThreatTier {

    /** No or minimal gear (leather / wood). */
    BARE       (0,   0),
    /** Iron-tier gear. */
    IRON       (15,  1),
    /** Diamond-tier gear, few enchants. */
    DIAMOND    (30,  2),
    /** Diamond + light enchantments. */
    ENCHANTED  (50,  4),
    /** Full diamond / netherite, moderate enchants. */
    VETERAN    (75,  6),
    /** Heavy netherite, strong enchants. */
    ELITE      (110, 8),
    /** Max-enchanted netherite, end-game weapons. */
    LEGENDARY  (160, 10);

    /** Minimum gear score to reach this tier. */
    public final int minScore;
    /** Bonus levels awarded to spawning mobs at this tier. */
    public final int bonusLevels;

    ThreatTier(int minScore, int bonusLevels) {
        this.minScore    = minScore;
        this.bonusLevels = bonusLevels;
    }

    /** Returns the highest tier whose {@code minScore} ≤ {@code score}. */
    public static ThreatTier fromScore(double score) {
        ThreatTier result = BARE;
        for (ThreatTier t : values()) {
            if (score >= t.minScore) result = t;
        }
        return result;
    }
}
