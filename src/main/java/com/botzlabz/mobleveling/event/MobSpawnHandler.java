package com.botzlabz.mobleveling.event;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.attribute.AttributeScalingManager;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.data.AttributeScaling;
import com.botzlabz.mobleveling.display.LevelDisplayManager;
import com.botzlabz.mobleveling.kills.HuntingGoalHandler;
import com.botzlabz.mobleveling.kills.KillLevelData;
import com.botzlabz.mobleveling.level.LevelResolver;
import com.botzlabz.mobleveling.level.LevelResult;
import com.botzlabz.mobleveling.level.MobLevelData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobSpawnHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final LevelResolver levelResolver = new LevelResolver();
    private static final AttributeScalingManager attributeManager = new AttributeScalingManager();
    private static final LevelDisplayManager displayManager = new LevelDisplayManager();

    /**
     * Handle mob finalize spawn - primary hook for natural spawns.
     * This fires after mob spawning is complete but before it's added to the world.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onMobFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!MobLevelingConfig.ENABLED.get()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Skip processing during initial world generation to prevent hangs
        // Mobs spawned during world gen will be processed when they're loaded later
        if (!isServerReady(serverLevel)) {
            return;
        }

        Mob mob = event.getEntity();
        BlockPos pos = mob.blockPosition();

        // Process the mob
        processNewMob(mob, serverLevel, pos, "FinalizeSpawn");
    }

    /**
     * Handle entity join level - fallback for spawn eggs, commands, loaded entities.
     * This catches mobs that weren't processed by FinalizeSpawn.
     * Also handles reapplying transient modifiers to loaded mobs.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!MobLevelingConfig.ENABLED.get()) {
            return;
        }

        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Skip processing during initial world generation to prevent hangs
        if (!isServerReady(serverLevel)) {
            return;
        }

        BlockPos pos = mob.blockPosition();

        // Check if this mob was already processed (has level data from previous save)
        if (MobLevelData.isProcessed(mob)) {
            // Mob was loaded from chunk - reapply transient modifiers if it has a level
            if (MobLevelData.hasLevel(mob)) {
                reapplyModifiersToLoadedMob(mob, serverLevel);
            }
            return;
        }

        // New mob that wasn't caught by FinalizeSpawn
        processNewMob(mob, serverLevel, pos, "EntityJoinLevel");
    }

    /**
     * Reapply transient attribute modifiers to a mob that was loaded from chunk.
     * Since we use transient modifiers, they don't persist to NBT and need reapplication.
     */
    private static void reapplyModifiersToLoadedMob(Mob mob, ServerLevel level) {
        try {
            int baseLevel = MobLevelData.getLevel(mob);
            // Include any kill levels earned so attributes stay at the correct combined level
            int killLevel = KillLevelData.getKillLevel(mob);
            int mobLevel = Math.min(baseLevel + killLevel, MobLevelingConfig.GLOBAL_LEVEL_CAP.get());

            // Get the rule that was used (if stored)
            var ruleIdOpt = MobLevelData.getSourceRuleId(mob);
            var ruleTypeOpt = MobLevelData.getSourceRuleType(mob);

            // Try to get attribute scaling from the original rule
            Map<ResourceLocation, AttributeScaling> scaling = new HashMap<>();

            if (ruleIdOpt.isPresent() && ruleTypeOpt.isPresent()) {
                // Try to find the original rule and get its scaling
                var rule = levelResolver.findRuleById(ruleIdOpt.get(), ruleTypeOpt.get());
                if (rule != null) {
                    scaling = rule.getAttributeScaling();
                }
            }

            // If we couldn't find the rule, try resolving fresh
            if (scaling.isEmpty()) {
                LevelResult result = levelResolver.resolve(mob, mob.blockPosition(), level);
                if (!result.shouldSkip()) {
                    scaling = result.getAttributeScaling();
                }
            }

            // Apply scaling
            if (!scaling.isEmpty()) {
                attributeManager.applyScaling(mob, mobLevel, scaling);
            }

            // Re-add hunting goals if the mob had hunt_to_level enabled before unloading
            HuntingGoalHandler.reapplyHuntingOnLoad(mob);

            if (MobLevelingConfig.DEBUG_MODE.get()) {
                LOGGER.debug("[{}] Reapplied modifiers to loaded {} at level {}",
                        BotzMobLeveling.MOD_ID,
                        ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()),
                        mobLevel
                );
            }
        } catch (Exception e) {
            // Don't crash the game if reapplication fails
            if (MobLevelingConfig.DEBUG_MODE.get()) {
                LOGGER.warn("Failed to reapply modifiers to loaded mob: {}", e.getMessage());
            }
        }
    }

    // Track if the server has fully started (player has joined)
    private static volatile boolean serverFullyReady = false;
    private static volatile long lastReadyCheckTime = 0;

    /**
     * Check if the server is ready for mob processing.
     * During world creation, we skip processing to prevent hangs and conflicts with other mods.
     */
    private static boolean isServerReady(ServerLevel level) {
        // If we've already confirmed the server is ready, use cached result
        // But re-check periodically in case of dimension changes
        if (serverFullyReady && (System.currentTimeMillis() - lastReadyCheckTime) < 5000) {
            return true;
        }

        try {
            var server = level.getServer();
            if (server == null) {
                return false;
            }

            // Server must be running (not just starting up)
            if (!server.isRunning()) {
                serverFullyReady = false;
                return false;
            }

            // During world creation, game time is 0
            // Wait until the world has actually started ticking significantly
            // Use a longer delay (200 ticks = 10 seconds) to let other mods finish their initialization
            if (level.getGameTime() < 200) {
                return false;
            }

            // Additional check: make sure there's at least one player in the world
            // This ensures world generation is complete
            if (server.getPlayerList().getPlayerCount() == 0) {
                return false;
            }

            // Server is ready - cache this result
            serverFullyReady = true;
            lastReadyCheckTime = System.currentTimeMillis();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Reset the ready state when server stops.
     * Called from mod lifecycle events.
     */
    public static void resetServerReadyState() {
        serverFullyReady = false;
        lastReadyCheckTime = 0;
    }

    private static void processNewMob(Mob mob, ServerLevel level, BlockPos pos, String source) {
        // Skip if already processed (double-check for race conditions)
        if (MobLevelData.isProcessed(mob)) {
            return;
        }

        // Resolve level using priority system
        LevelResult result = levelResolver.resolve(mob, pos, level);

        if (result.shouldSkip()) {
            // Mark as processed so we don't try again
            MobLevelData.markProcessed(mob);
            return;
        }

        int mobLevel = result.getLevel();

        // Store level in persistent data
        MobLevelData.setLevel(mob, mobLevel);
        MobLevelData.markProcessed(mob);

        // Store source rule info for debugging
        if (result.getSourceRule() != null) {
            MobLevelData.setSourceRule(mob, result.getSourceRuleId(), result.getSourceRuleType());
        }

        // Apply attribute scaling
        if (!result.getAttributeScaling().isEmpty()) {
            attributeManager.applyScaling(mob, mobLevel, result.getAttributeScaling());
        }

        // Handle passive mob combat ability
        applyPassiveCombatSettings(mob, result);

        // Enable mob-hunting AI if the rule requests it, subject to a per-rule chance roll
        if (result.shouldHuntToLevel()) {
            // Per-rule chance takes precedence; falls back to global config default
            double chance = result.getHuntToLevelChance();
            if (chance >= 1.0 || mob.getRandom().nextDouble() < chance) {
                HuntingGoalHandler.enableHunting(mob);
            }
        }

        // Update display name
        if (MobLevelingConfig.SHOW_LEVEL_IN_NAME.get()) {
            displayManager.updateDisplay(mob, mobLevel);
        }

        if (MobLevelingConfig.DEBUG_MODE.get()) {
            LOGGER.debug("[{}] Assigned level {} to {} at {} (source: {}, rule: {})",
                    BotzMobLeveling.MOD_ID,
                    mobLevel,
                    ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()),
                    pos,
                    source,
                    result.getSourceRuleId()
            );
        }
    }

    /**
     * Apply combat settings to passive mobs based on config and datapack rules.
     */
    private static void applyPassiveCombatSettings(Mob mob, LevelResult result) {
        // Only applies to passive mobs (animals)
        if (!(mob instanceof Animal)) {
            return;
        }

        // Check if mob override has explicit can_attack setting
        var mobOverride = result.getMobOverride();
        Boolean canAttack = null;

        if (mobOverride != null && mobOverride.hasCanAttackOverride()) {
            canAttack = mobOverride.getCanAttack();
        }

        // If no override, use config default
        if (canAttack == null) {
            canAttack = MobLevelingConfig.LEVELED_PASSIVES_CAN_ATTACK.get();
        }

        // Apply the setting
        PassiveMobCombatHandler.setCombatEnabled(mob, canAttack);

        // If can_attack is explicitly true and mob is a PathfinderMob, pre-add combat goals
        if (canAttack && mob instanceof PathfinderMob pathfinderMob) {
            PassiveMobCombatHandler.enableCombatOnSpawn(pathfinderMob);
        }
    }
}
