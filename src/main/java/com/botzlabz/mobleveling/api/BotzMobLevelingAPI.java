package com.botzlabz.mobleveling.api;

import com.botzlabz.mobleveling.data.MobLevelingDataManager;
import com.botzlabz.mobleveling.level.MobLevelCapability;
import com.botzlabz.mobleveling.level.MobLevelData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/**
 * Public, stable read-only API for other mods, KubeJS scripts, and datapacks.
 *
 * <p>Everything here is server-side and side-effect free, so it is safe to poll
 * (e.g. every tick to drive an "area difficulty" HUD). Mob queries read the
 * mob's live level data; {@link #getAreaLevel} computes the level a generic mob
 * <i>would</i> spawn at for a position, even with no mobs nearby.
 *
 * <p>The same level values are also mirrored to each mob's Forge persistent data
 * ({@code botzmobleveling:level_data} and the {@code BML_Level} int) for callers
 * that prefer raw NBT access over this API.
 */
public final class BotzMobLevelingAPI {

    /**
     * The level a generic mob would be assigned at {@code pos} in {@code level},
     * from the active spawn rules (structure → biome → dimension → base),
     * clamped to the global level cap. Returns {@code 0} when a SKIP rule wins
     * at that position. Deterministic — no RNG — so it is stable when polled.
     */
    public static int getAreaLevel(ServerLevel level, BlockPos pos) {
        return MobLevelingDataManager.INSTANCE.resolveAreaLevel(level, pos);
    }

    /** Total level (base + kill levels) of a mob, or {@code 0} if it is not leveled. */
    public static int getMobLevel(Mob mob) {
        MobLevelData data = MobLevelCapability.get(mob);
        return (data != null && data.isProcessed()) ? data.getTotalLevel() : 0;
    }

    /** The mob's spawn (base) level, or {@code 0} if it is not leveled. */
    public static int getMobBaseLevel(Mob mob) {
        MobLevelData data = MobLevelCapability.get(mob);
        return (data != null && data.isProcessed()) ? data.getBaseLevel() : 0;
    }

    /** The levels a mob has earned by killing other entities, or {@code 0}. */
    public static int getMobKillLevel(Mob mob) {
        MobLevelData data = MobLevelCapability.get(mob);
        return (data != null && data.isProcessed()) ? data.getKillLevel() : 0;
    }

    private BotzMobLevelingAPI() {}
}
