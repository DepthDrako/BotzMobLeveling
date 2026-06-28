package com.botzlabz.mobleveling.api;

import com.botzlabz.mobleveling.kills.KillLevelData;
import com.botzlabz.mobleveling.level.LevelCalculator;
import com.botzlabz.mobleveling.level.MobLevelData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/**
 * Stable, public entry point for reading BotzMobLeveling data from datapacks
 * (via the {@code /botzmobleveling} command), KubeJS, or other mods.
 *
 * <p>KubeJS example — show the area level on the actionbar:
 * <pre>{@code
 * const BML = Java.loadClass('com.botzlabz.mobleveling.api.BotzMobLevelingAPI')
 * ServerEvents.tick(e => {
 *   if (e.server.tickCount % 10 !== 0) return
 *   e.server.players.forEach(p => {
 *     let lvl = BML.getAreaLevel(p.level, p.blockPosition())
 *     p.setStatusMessage(Text.gold('Area Level: ' + lvl)) // adjust to your KubeJS version
 *   })
 * })
 * }</pre>
 */
public final class BotzMobLevelingAPI {

    private BotzMobLevelingAPI() {}

    /**
     * The "area difficulty": the level a mob would receive from base distance scaling at this
     * position, even when no mob is present. Reflects the configured distance settings and the
     * default level range. Per-rule overrides (biome / structure / dimension rules) and adaptive
     * bonuses are mob- or rule-specific and are not included here. Returns 0 for a non-server level.
     *
     * <p>Tip: to make a HUD line up with what actually spawns, set {@code defaultMinLevel} /
     * {@code defaultMaxLevel} in the config to match your catch-all base rule's range.
     */
    public static int getAreaLevel(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel && pos != null) {
            return LevelCalculator.getDefaultLevel(pos, serverLevel);
        }
        return 0;
    }

    /** A mob's current effective level: base spawn level plus any earned kill levels. 0 if unleveled. */
    public static int getMobLevel(Mob mob) {
        if (mob == null || !MobLevelData.hasLevel(mob)) {
            return 0;
        }
        return MobLevelData.getLevel(mob) + KillLevelData.getKillLevel(mob);
    }

    /** A mob's base spawn level only (excludes kill levels). 0 if unleveled. */
    public static int getMobBaseLevel(Mob mob) {
        return (mob != null && MobLevelData.hasLevel(mob)) ? MobLevelData.getLevel(mob) : 0;
    }

    /** The number of kill levels a mob has earned on top of its base level. */
    public static int getMobKillLevel(Mob mob) {
        return mob == null ? 0 : KillLevelData.getKillLevel(mob);
    }
}
