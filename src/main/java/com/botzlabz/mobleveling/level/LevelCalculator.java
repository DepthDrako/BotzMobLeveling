package com.botzlabz.mobleveling.level;

import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.data.LevelRule;
import com.botzlabz.mobleveling.data.MobOverride;
import com.botzlabz.mobleveling.util.ModConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;

public class LevelCalculator {

    private final RandomSource random;

    public LevelCalculator(RandomSource random) {
        this.random = random;
    }

    public int calculateLevel(LevelRule rule, @Nullable MobOverride override, BlockPos pos, ServerLevel level) {
        // Check for fixed level first (override takes precedence) - these bypass range clamping
        if (override != null && override.hasFixedLevel()) {
            return override.getFixedLevel();
        }

        if (rule.getFixedLevel() != null && rule.getLevelMode().equals(ModConstants.LEVEL_MODE_FIXED)) {
            return rule.getFixedLevel();
        }

        // Determine effective level range
        int minLevel = rule.getMinLevel();
        int maxLevel = rule.getMaxLevel();

        if (override != null && override.hasCustomLevelRange()) {
            if (override.getMinLevel() != null) {
                minLevel = override.getMinLevel();
            }
            if (override.getMaxLevel() != null) {
                maxLevel = override.getMaxLevel();
            }
        }

        // Calculate base level based on mode
        int calculatedLevel;
        String levelMode = rule.getLevelMode();

        switch (levelMode) {
            case ModConstants.LEVEL_MODE_FIXED:
                // Fixed level from rule (already handled above, this is fallback)
                calculatedLevel = rule.getFixedLevel() != null ? rule.getFixedLevel() : minLevel;
                break;

            case ModConstants.LEVEL_MODE_RANDOM:
                calculatedLevel = randomInRange(minLevel, maxLevel);
                break;

            case ModConstants.LEVEL_MODE_DISTANCE:
            default:
                if (rule.ignoresDistanceScaling() || !MobLevelingConfig.DISTANCE_LEVELING_ENABLED.get()) {
                    calculatedLevel = randomInRange(minLevel, maxLevel);
                } else {
                    calculatedLevel = calculateDistanceLevel(pos, level, minLevel, maxLevel, rule.getDistanceMultiplier());
                }
                break;
        }

        // Apply level bonus from override
        if (override != null && override.hasLevelBonus()) {
            calculatedLevel += override.getLevelBonus();
        }

        // Clamp to range (only for non-fixed levels)
        calculatedLevel = Mth.clamp(calculatedLevel, minLevel, maxLevel);

        return calculatedLevel;
    }

    public int calculateDistanceLevel(BlockPos pos, ServerLevel level, int minLevel, int maxLevel, double ruleMultiplier) {
        BlockPos spawn = level.getSharedSpawnPos();

        // Calculate horizontal distance only (ignore Y)
        double dx = pos.getX() - spawn.getX();
        double dz = pos.getZ() - spawn.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        int startRadius = MobLevelingConfig.DISTANCE_START_RADIUS.get();
        int distancePerLevel = MobLevelingConfig.DISTANCE_PER_LEVEL.get();
        double globalMultiplier = MobLevelingConfig.DISTANCE_LEVEL_MULTIPLIER.get();

        // Apply combined multiplier
        double combinedMultiplier = globalMultiplier * ruleMultiplier;

        // Calculate effective distance (beyond start radius)
        double effectiveDistance = Math.max(0, distance - startRadius);

        // Calculate level bonus from distance
        int distanceLevelBonus = (int) (effectiveDistance / distancePerLevel * combinedMultiplier);

        // Add to minimum level and clamp
        int calculatedLevel = minLevel + distanceLevelBonus;

        return Mth.clamp(calculatedLevel, minLevel, maxLevel);
    }

    public int applyGlobalCap(int level, boolean ignoreCap) {
        if (ignoreCap) {
            return level;
        }
        int globalCap = MobLevelingConfig.GLOBAL_LEVEL_CAP.get();
        return Math.min(level, globalCap);
    }

    private int randomInRange(int min, int max) {
        if (min >= max) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    public static int getDefaultLevel(BlockPos pos, ServerLevel level) {
        int minLevel = MobLevelingConfig.DEFAULT_MIN_LEVEL.get();
        int maxLevel = MobLevelingConfig.DEFAULT_MAX_LEVEL.get();

        if (!MobLevelingConfig.DISTANCE_LEVELING_ENABLED.get()) {
            return minLevel;
        }

        BlockPos spawn = level.getSharedSpawnPos();
        double dx = pos.getX() - spawn.getX();
        double dz = pos.getZ() - spawn.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        int startRadius = MobLevelingConfig.DISTANCE_START_RADIUS.get();
        int distancePerLevel = MobLevelingConfig.DISTANCE_PER_LEVEL.get();
        double multiplier = MobLevelingConfig.DISTANCE_LEVEL_MULTIPLIER.get();

        double effectiveDistance = Math.max(0, distance - startRadius);
        int distanceBonus = (int) (effectiveDistance / distancePerLevel * multiplier);

        return Mth.clamp(minLevel + distanceBonus, minLevel, maxLevel);
    }
}
