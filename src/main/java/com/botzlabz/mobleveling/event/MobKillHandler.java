package com.botzlabz.mobleveling.event;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.attribute.AttributeScalingManager;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.display.LevelDisplay;
import com.botzlabz.mobleveling.level.MobLevelCapability;
import com.botzlabz.mobleveling.level.MobLevelData;
import com.botzlabz.mobleveling.level.MobLevelStore;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Awards kill XP to leveled mobs when they kill another entity.
 * XP and scale constants are driven by {@link MobLevelingConfig}.
 */
@EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class MobKillHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!MobLevelingConfig.KILL_LEVELING_ENABLED.get()) return;

        LivingEntity killed = event.getEntity();
        LivingEntity killer = killed.getLastHurtByMob();
        if (!(killer instanceof Mob killerMob)) return;

        MobLevelData killerData = MobLevelCapability.get(killerMob);
        MobLevelData killedData = MobLevelCapability.get(killed);
        if (killerData == null || !killerData.isProcessed()) return;

        // XP reward scales with victim's level (leveled mobs give more)
        int victimLevel = (killedData != null && killedData.isProcessed())
            ? killedData.getTotalLevel() : 1;
        long xpReward = Math.max(10L, victimLevel * 20L);

        int prevLevel = killerData.getTotalLevel();

        boolean leveledUp = killerData.addKillXP(
            xpReward,
            MobLevelingConfig.KILL_LEVELING_XP_BASE.get(),
            MobLevelingConfig.KILL_LEVELING_XP_SCALE.get(),
            MobLevelingConfig.GLOBAL_LEVEL_CAP.get(),
            MobLevelingConfig.KILL_LEVELING_CAP.get()
        );

        if (leveledUp) {
            // Re-apply scaled attributes (config stats + datapack ops at the new level)
            AttributeScalingManager.remove(killerMob, killerData);
            AttributeScalingManager.apply(killerMob, killerData);

            // Small HP regen on level-up (reward)
            killerMob.setHealth(Math.min(killerMob.getHealth() + 2f, killerMob.getMaxHealth()));

            // Refresh name tag
            LevelDisplay.apply(killerMob, killerData);
        }

        // Always persist updated XP / level data (also syncs BML_Level for eidolon_ai)
        MobLevelStore.persist(killerMob, killerData);
    }
}
