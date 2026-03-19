package com.botzlabz.mobleveling.kills;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.attribute.AttributeScalingManager;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.data.AttributeScaling;
import com.botzlabz.mobleveling.display.LevelDisplayManager;
import com.botzlabz.mobleveling.level.LevelResolver;
import com.botzlabz.mobleveling.level.LevelResult;
import com.botzlabz.mobleveling.level.MobLevelData;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class KillLevelManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LevelResolver levelResolver = new LevelResolver();
    private static final AttributeScalingManager attributeManager = new AttributeScalingManager();
    private static final LevelDisplayManager displayManager = new LevelDisplayManager();

    public static void onMobKill(Mob killer, LivingEntity victim, ServerLevel level) {
        if (!MobLevelingConfig.KILL_LEVELING_ENABLED.get()) {
            return;
        }

        // Only leveled mobs can gain kill XP unless the config allows any mob
        if (!MobLevelData.isProcessed(killer) && !MobLevelingConfig.KILL_APPLY_TO_ANY_MOB.get()) {
            return;
        }

        // Grant XP and record kill
        long xpGained = calculateXP(victim);
        long newXP = KillLevelData.getKillXP(killer) + xpGained;
        KillLevelData.setKillXP(killer, newXP);
        KillLevelData.incrementKillCount(killer);

        // Make persistent on very first kill
        if (MobLevelingConfig.KILL_MAKE_PERSISTENT.get() && KillLevelData.getKillCount(killer) == 1) {
            killer.setPersistenceRequired();
        }

        // Consume XP into kill levels
        int currentKillLevel = KillLevelData.getKillLevel(killer);
        int maxKillLevel = MobLevelingConfig.KILL_MAX_LEVEL.get();
        int newKillLevel = currentKillLevel;

        while (newKillLevel < maxKillLevel) {
            long xpRequired = getXPRequiredForLevel(newKillLevel + 1);
            if (newXP >= xpRequired) {
                newXP -= xpRequired;
                newKillLevel++;
            } else {
                break;
            }
        }
        KillLevelData.setKillXP(killer, newXP);

        if (newKillLevel > currentKillLevel) {
            KillLevelData.setKillLevel(killer, newKillLevel);
            onKillLevelUp(killer, newKillLevel, level);
        } else if (KillLevelData.getKillCount(killer) == 1 && MobLevelingConfig.KILL_SHOW_INDICATOR.get()) {
            // First kill, no level-up yet — refresh display to add the kill indicator
            int baseLevel = MobLevelData.hasLevel(killer) ? MobLevelData.getLevel(killer) : 0;
            displayManager.updateDisplay(killer, baseLevel);
        }

        if (MobLevelingConfig.DEBUG_MODE.get()) {
            LOGGER.debug("[{}] {} gained {} kill XP (banked: {}, kills: {}, killLv: {})",
                    BotzMobLeveling.MOD_ID,
                    ForgeRegistries.ENTITY_TYPES.getKey(killer.getType()),
                    xpGained, KillLevelData.getKillXP(killer),
                    KillLevelData.getKillCount(killer),
                    KillLevelData.getKillLevel(killer));
        }
    }

    private static void onKillLevelUp(Mob mob, int newKillLevel, ServerLevel level) {
        int baseLevel = MobLevelData.hasLevel(mob) ? MobLevelData.getLevel(mob) : 0;
        int totalLevel = Math.min(baseLevel + newKillLevel, MobLevelingConfig.GLOBAL_LEVEL_CAP.get());

        // Reapply attributes with the new combined level (replaces spawn-time modifier via same UUID)
        Map<ResourceLocation, AttributeScaling> scaling = getAttributeScaling(mob, level);
        if (!scaling.isEmpty()) {
            attributeManager.applyScaling(mob, totalLevel, scaling);
        }

        // Update display with new total level (+ kill indicator via LevelDisplayManager)
        if (MobLevelingConfig.SHOW_LEVEL_IN_NAME.get()) {
            displayManager.updateDisplay(mob, totalLevel);
        }

        if (MobLevelingConfig.DEBUG_MODE.get()) {
            LOGGER.debug("[{}] {} kill level-up! killLv: {} totalLv: {}",
                    BotzMobLeveling.MOD_ID,
                    ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()),
                    newKillLevel, totalLevel);
        }
    }

    private static Map<ResourceLocation, AttributeScaling> getAttributeScaling(Mob mob, ServerLevel level) {
        // Prefer the original spawn rule so attribute scaling stays consistent
        var ruleIdOpt = MobLevelData.getSourceRuleId(mob);
        var ruleTypeOpt = MobLevelData.getSourceRuleType(mob);

        if (ruleIdOpt.isPresent() && ruleTypeOpt.isPresent()) {
            var rule = levelResolver.findRuleById(ruleIdOpt.get(), ruleTypeOpt.get());
            if (rule != null) {
                return rule.getAttributeScaling();
            }
        }

        // Fall back to a fresh resolve
        LevelResult result = levelResolver.resolve(mob, mob.blockPosition(), level);
        if (!result.shouldSkip()) {
            return result.getAttributeScaling();
        }

        return new HashMap<>();
    }

    private static long calculateXP(LivingEntity victim) {
        int base = MobLevelingConfig.KILL_XP_BASE.get();

        if (victim instanceof Player) {
            return base + MobLevelingConfig.KILL_XP_PLAYER_BONUS.get();
        }

        if (victim instanceof Mob victimMob && MobLevelData.hasLevel(victimMob)) {
            int victimTotalLevel = MobLevelData.getLevel(victimMob) + KillLevelData.getKillLevel(victimMob);
            return base + (long) victimTotalLevel * MobLevelingConfig.KILL_XP_PER_VICTIM_LEVEL.get();
        }

        return base;
    }

    /**
     * XP required to reach the next kill level, using exponential scaling.
     * Level 1 costs baseXpRequired. Each subsequent level costs xpScaling times more.
     */
    private static long getXPRequiredForLevel(int killLevel) {
        if (killLevel <= 0) return 0;
        long base = MobLevelingConfig.KILL_BASE_XP_REQUIRED.get();
        double scaling = MobLevelingConfig.KILL_XP_SCALING.get();
        return (long) (base * Math.pow(scaling, killLevel - 1));
    }
}
