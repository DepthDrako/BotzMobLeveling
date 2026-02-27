package com.botzlabz.mobleveling.adaptive;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Main handler for adaptive difficulty system.
 * Integrates with mob spawning to scale difficulty based on nearby player gear.
 */
@Mod.EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AdaptiveDifficultyHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Mob mob = event.getEntity();
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        // Check if adaptive difficulty is enabled
        if (!MobLevelingConfig.ADAPTIVE_DIFFICULTY_ENABLED.get()) {
            return;
        }

        // Find the nearest player
        Player nearestPlayer = findNearestPlayer(mob);
        if (nearestPlayer == null) {
            return;
        }

        // Calculate gear score based on nearby players
        double gearScore = GearAnalyzer.getMaxNearbyGearScore(nearestPlayer);

        // Only apply if gear score is significant
        if (gearScore <= 10) {  // Lowered threshold for testing
            return;
        }

        LOGGER.info("[MobLeveling] Adaptive difficulty triggered for {} near {} - Gear Score: {}{}",
                mob.getName().getString(), nearestPlayer.getName().getString(), String.format("%.1f", gearScore), "");

        // Apply adaptive difficulty modifiers
        MobResponseHandler.applyAdaptiveModifiers(mob, gearScore, level.getRandom());

        LOGGER.info("[MobLeveling] Applied modifiers to {} - Threat: {}",
                mob.getName().getString(), ThreatCalculator.getThreatDescription(ThreatCalculator.calculateThreatLevel(gearScore)));
    }

    /**
     * Finds the nearest player to the mob within the search radius
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

    /**
     * Gets the adaptive level bonus for a mob based on nearby players
     * This is called from the level calculator to integrate with the existing system
     */
    public static int getAdaptiveLevelBonus(Mob mob) {
        if (!MobLevelingConfig.ADAPTIVE_DIFFICULTY_ENABLED.get()) {
            return 0;
        }

        Player nearestPlayer = findNearestPlayer(mob);
        if (nearestPlayer == null) {
            return 0;
        }

        double gearScore = GearAnalyzer.getMaxNearbyGearScore(nearestPlayer);
        return ThreatCalculator.calculateLevelBonus(gearScore);
    }
}
