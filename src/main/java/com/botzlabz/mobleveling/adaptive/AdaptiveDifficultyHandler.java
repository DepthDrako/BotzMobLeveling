package com.botzlabz.mobleveling.adaptive;

import com.botzlabz.mobleveling.config.MobLevelingConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Helper for the adaptive difficulty system.
 *
 * <p>This used to be a standalone {@code MobSpawnEvent.FinalizeSpawn} subscriber that
 * applied permanent stat modifiers and equipment to <em>every</em> mob, bypassing the
 * blacklist / passive / boss filters and the world-ready gate used by the leveling
 * system, and recomputing the player gear score a second time. It is now driven from
 * {@code MobSpawnHandler} <em>after</em> a mob has passed all of those checks, and the
 * gear score is computed once and threaded through {@code LevelResult}.
 */
public final class AdaptiveDifficultyHandler {

    private AdaptiveDifficultyHandler() {}

    /**
     * Compute the maximum gear score among players near this mob, or {@code 0} when
     * adaptive difficulty is disabled or no eligible player is in range. Computed once
     * per mob during {@code LevelResolver.resolve} and reused for both the level bonus
     * and the attribute/equipment modifiers.
     */
    public static double getNearbyGearScore(Mob mob) {
        if (!MobLevelingConfig.ADAPTIVE_DIFFICULTY_ENABLED.get()) {
            return 0.0;
        }

        Player nearestPlayer = findNearestPlayer(mob);
        if (nearestPlayer == null) {
            return 0.0;
        }

        return GearAnalyzer.getMaxNearbyGearScore(nearestPlayer);
    }

    /**
     * Convert a gear score into a bonus level count. Returns {@code 0} for scores that
     * fall in the "trivial" threat bracket.
     */
    public static int getLevelBonusForGearScore(double gearScore) {
        if (gearScore <= 0) {
            return 0;
        }
        return ThreatCalculator.calculateLevelBonus(gearScore);
    }

    /**
     * Finds the nearest living, non-spectator player to the mob within the search radius.
     */
    private static Player findNearestPlayer(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return null;
        }

        int radius = MobLevelingConfig.ADAPTIVE_PLAYER_SEARCH_RADIUS.get();
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : level.getEntitiesOfClass(
                Player.class,
                mob.getBoundingBox().inflate(radius))) {

            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }

            double distance = mob.distanceTo(player);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = player;
            }
        }

        return nearestPlayer;
    }
}
