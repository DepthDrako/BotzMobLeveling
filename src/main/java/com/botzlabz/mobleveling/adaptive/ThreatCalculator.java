package com.botzlabz.mobleveling.adaptive;

import com.botzlabz.mobleveling.config.MobLevelingConfig;

/**
 * Calculates threat level based on gear score and converts it to
 * mob level bonuses and attribute modifiers.
 */
public class ThreatCalculator {

    // Gear score thresholds for threat levels
    private static final double THREAT_TRIVIAL = 20.0;
    private static final double THREAT_EASY = 50.0;
    private static final double THREAT_NORMAL = 100.0;
    private static final double THREAT_HARD = 200.0;
    private static final double THREAT_VERY_HARD = 350.0;
    private static final double THREAT_EXTREME = 500.0;
    private static final double THREAT_DEADLY = 750.0;

    /**
     * Threat level enum
     */
    public enum ThreatLevel {
        TRIVIAL,      // Very weak player, mobs get no bonus
        EASY,         // Weak player, small bonus
        NORMAL,       // Average player, minor bonus
        HARD,         // Good gear, moderate bonus
        VERY_HARD,    // Strong gear, significant bonus
        EXTREME,      // Excellent gear, major bonus
        DEADLY        // God-tier gear, maximum bonus
    }

    /**
     * Calculates threat level from gear score
     */
    public static ThreatLevel calculateThreatLevel(double gearScore) {
        if (gearScore < THREAT_TRIVIAL) return ThreatLevel.TRIVIAL;
        if (gearScore < THREAT_EASY) return ThreatLevel.EASY;
        if (gearScore < THREAT_NORMAL) return ThreatLevel.NORMAL;
        if (gearScore < THREAT_HARD) return ThreatLevel.HARD;
        if (gearScore < THREAT_VERY_HARD) return ThreatLevel.VERY_HARD;
        if (gearScore < THREAT_EXTREME) return ThreatLevel.EXTREME;
        return ThreatLevel.DEADLY;
    }

    /**
     * Calculates additional levels based on gear score
     */
    public static int calculateLevelBonus(double gearScore) {
        int maxBonus = MobLevelingConfig.ADAPTIVE_MAX_LEVEL_BONUS.get();
        ThreatLevel threat = calculateThreatLevel(gearScore);

        int bonus = switch (threat) {
            case TRIVIAL -> 0;
            case EASY -> (int) (maxBonus * 0.1);
            case NORMAL -> (int) (maxBonus * 0.2);
            case HARD -> (int) (maxBonus * 0.4);
            case VERY_HARD -> (int) (maxBonus * 0.6);
            case EXTREME -> (int) (maxBonus * 0.8);
            case DEADLY -> maxBonus;
        };

        // Scale linearly within threat brackets for smoother progression
        bonus = applySmoothScaling(gearScore, threat, bonus);

        if (MobLevelingConfig.ADAPTIVE_DEBUG_LOGGING.get()) {
            System.out.println("[MobLeveling] Gear score: " + gearScore + 
                    ", Threat: " + threat + ", Level bonus: " + bonus);
        }

        return bonus;
    }

    /**
     * Applies smooth scaling within threat brackets
     */
    private static int applySmoothScaling(double gearScore, ThreatLevel threat, int baseBonus) {
        double lowerBound = getLowerBound(threat);
        double upperBound = getUpperBound(threat);
        double range = upperBound - lowerBound;

        if (range <= 0) return baseBonus;

        double position = (gearScore - lowerBound) / range;
        position = Math.max(0, Math.min(1, position));

        int nextLevelBonus = switch (threat) {
            case TRIVIAL -> (int) (MobLevelingConfig.ADAPTIVE_MAX_LEVEL_BONUS.get() * 0.1);
            case EASY -> (int) (MobLevelingConfig.ADAPTIVE_MAX_LEVEL_BONUS.get() * 0.2);
            case NORMAL -> (int) (MobLevelingConfig.ADAPTIVE_MAX_LEVEL_BONUS.get() * 0.4);
            case HARD -> (int) (MobLevelingConfig.ADAPTIVE_MAX_LEVEL_BONUS.get() * 0.6);
            case VERY_HARD -> (int) (MobLevelingConfig.ADAPTIVE_MAX_LEVEL_BONUS.get() * 0.8);
            case EXTREME, DEADLY -> MobLevelingConfig.ADAPTIVE_MAX_LEVEL_BONUS.get();
        };

        return (int) (baseBonus + (nextLevelBonus - baseBonus) * position);
    }

    /**
     * Gets the lower bound for a threat level
     */
    private static double getLowerBound(ThreatLevel threat) {
        return switch (threat) {
            case TRIVIAL -> 0;
            case EASY -> THREAT_TRIVIAL;
            case NORMAL -> THREAT_EASY;
            case HARD -> THREAT_NORMAL;
            case VERY_HARD -> THREAT_HARD;
            case EXTREME -> THREAT_VERY_HARD;
            case DEADLY -> THREAT_EXTREME;
        };
    }

    /**
     * Gets the upper bound for a threat level
     */
    private static double getUpperBound(ThreatLevel threat) {
        return switch (threat) {
            case TRIVIAL -> THREAT_TRIVIAL;
            case EASY -> THREAT_EASY;
            case NORMAL -> THREAT_NORMAL;
            case HARD -> THREAT_HARD;
            case VERY_HARD -> THREAT_VERY_HARD;
            case EXTREME -> THREAT_EXTREME;
            case DEADLY -> Double.MAX_VALUE;
        };
    }

    /**
     * Calculates attribute multiplier based on gear score
     */
    public static double getAttributeMultiplier(double gearScore) {
        double baseScaling = MobLevelingConfig.ADAPTIVE_ATTRIBUTE_SCALING.get();
        ThreatLevel threat = calculateThreatLevel(gearScore);

        return switch (threat) {
            case TRIVIAL -> 1.0;
            case EASY -> 1.0 + (0.1 * baseScaling);
            case NORMAL -> 1.0 + (0.2 * baseScaling);
            case HARD -> 1.0 + (0.4 * baseScaling);
            case VERY_HARD -> 1.0 + (0.7 * baseScaling);
            case EXTREME -> 1.0 + (1.0 * baseScaling);
            case DEADLY -> 1.0 + (1.5 * baseScaling);
        };
    }

    /**
     * Calculates equipment tier chance based on gear score
     * Returns a value from 0-4 representing equipment tier
     */
    public static int getEquipmentTier(double gearScore) {
        int maxTier = MobLevelingConfig.ADAPTIVE_MAX_EQUIPMENT_TIER.get();
        ThreatLevel threat = calculateThreatLevel(gearScore);

        int baseTier = switch (threat) {
            case TRIVIAL, EASY -> 0;
            case NORMAL -> 1;
            case HARD -> 2;
            case VERY_HARD -> 3;
            case EXTREME, DEADLY -> 4;
        };

        return Math.min(baseTier, maxTier);
    }

    /**
     * Gets the chance for a mob to spawn with equipment
     */
    public static double getEquipmentChance(double gearScore) {
        double baseChance = MobLevelingConfig.ADAPTIVE_EQUIPMENT_CHANCE.get();
        ThreatLevel threat = calculateThreatLevel(gearScore);

        double multiplier = switch (threat) {
            case TRIVIAL -> 0.0;
            case EASY -> 0.5;
            case NORMAL -> 0.8;
            case HARD -> 1.0;
            case VERY_HARD -> 1.3;
            case EXTREME -> 1.6;
            case DEADLY -> 2.0;
        };

        return Math.min(1.0, baseChance * multiplier);
    }

    /**
     * Gets a human-readable description of the threat level
     */
    public static String getThreatDescription(ThreatLevel threat) {
        return switch (threat) {
            case TRIVIAL -> "Trivial";
            case EASY -> "Easy";
            case NORMAL -> "Normal";
            case HARD -> "Hard";
            case VERY_HARD -> "Very Hard";
            case EXTREME -> "Extreme";
            case DEADLY -> "Deadly";
        };
    }
}
