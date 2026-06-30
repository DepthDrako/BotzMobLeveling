package com.botzlabz.mobleveling.adaptive;

import com.botzlabz.mobleveling.config.MobLevelingConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

import java.util.List;

/**
 * Computes adaptive difficulty bonus levels for a spawning mob.
 *
 * <h3>Design</h3>
 * Player gear scores are pre-computed asynchronously by
 * {@link PlayerGearWatcher} + {@link GearScoreCache} using
 * botz_lib's {@link com.botzlabz.lib.async.BotzAsync} pool.
 * This method only reads the cached values — it is main-thread safe and
 * returns instantly without any blocking or heavy computation.
 *
 * <p>If a player has no cached score yet (they joined this exact tick),
 * {@link GearScoreCache#getScore} returns {@code 0.0} and their score is
 * ignored for this spawn.  The cache will be populated before any subsequent
 * spawns hit this player.
 */
public final class DifficultyCalculator {

    public static int computeBonus(Mob mob, ServerLevel level) {
        double radius = MobLevelingConfig.ADAPTIVE_SCAN_RADIUS.get();
        List<ServerPlayer> nearby = level.getPlayers(p -> p.distanceTo(mob) <= radius);
        if (nearby.isEmpty()) return 0;

        double minScore = MobLevelingConfig.ADAPTIVE_MIN_GEAR_SCORE.get();
        double totalScore = 0;
        int    counted    = 0;
        for (ServerPlayer player : nearby) {
            double score = GearScoreCache.getScore(player.getUUID());
            // Ignore players below the gear threshold (and unscored players, who
            // return 0.0) so only meaningfully-equipped players ramp difficulty.
            if (score >= minScore && score > 0) {
                totalScore += score;
                counted++;
            }
        }
        if (counted == 0) return 0;

        double avgScore = totalScore / counted;
        ThreatTier tier = ThreatTier.fromScore(avgScore);
        return Math.min(tier.bonusLevels, MobLevelingConfig.ADAPTIVE_MAX_BONUS.get());
    }

    private DifficultyCalculator() {}
}
