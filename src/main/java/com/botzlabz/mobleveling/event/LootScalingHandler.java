package com.botzlabz.mobleveling.event;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.kills.KillLevelData;
import com.botzlabz.mobleveling.level.MobLevelData;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Scales mob loot with level by boosting the effective Looting level used in the mob's
 * loot roll. This increases looting-affected drops (arrows, bones, ender pearls, most
 * mob loot) in proportion to the mob's level — e.g. a high-level skeleton drops many
 * more arrows, and packs get a clean "+1 looting per N levels" knob.
 *
 * <p>Implemented via {@link LootingLevelEvent} so it works with vanilla and most modded
 * loot tables without duplicating item entities. Opt-in via the {@code lootScaling}
 * config; off by default to avoid surprise loot inflation in existing packs.
 */
@Mod.EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LootScalingHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onLootingLevel(LootingLevelEvent event) {
        if (!MobLevelingConfig.ENABLED.get() || !MobLevelingConfig.LOOT_SCALING_ENABLED.get()) {
            return;
        }

        int perLevels = MobLevelingConfig.LOOT_LOOTING_PER_LEVELS.get();
        if (perLevels <= 0) {
            return;
        }

        // The event entity is the mob whose loot is being rolled (the victim).
        if (!(event.getEntity() instanceof Mob mob) || !MobLevelData.hasLevel(mob)) {
            return;
        }

        // Effective level = spawn level + any kill levels earned.
        int level = MobLevelData.getLevel(mob) + KillLevelData.getKillLevel(mob);
        if (level <= 0) {
            return;
        }

        int bonus = Math.min(level / perLevels, MobLevelingConfig.LOOT_MAX_LOOTING_BONUS.get());
        if (bonus <= 0) {
            return;
        }

        event.setLootingLevel(event.getLootingLevel() + bonus);

        if (MobLevelingConfig.DEBUG_MODE.get()) {
            LOGGER.debug("[{}] Loot scaling: +{} looting for level {} {}",
                    BotzMobLeveling.MOD_ID, bonus, level, mob.getType().getDescription().getString());
        }
    }
}
