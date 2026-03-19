package com.botzlabz.mobleveling.kills;

import com.botzlabz.mobleveling.util.ModConstants;
import net.minecraft.world.entity.Mob;

public final class KillLevelData {

    private KillLevelData() {}

    public static long getKillXP(Mob mob) {
        return mob.getPersistentData().getLong(ModConstants.NBT_KILL_XP);
    }

    public static void setKillXP(Mob mob, long xp) {
        mob.getPersistentData().putLong(ModConstants.NBT_KILL_XP, xp);
    }

    public static int getKillLevel(Mob mob) {
        return mob.getPersistentData().getInt(ModConstants.NBT_KILL_LEVEL);
    }

    public static void setKillLevel(Mob mob, int level) {
        mob.getPersistentData().putInt(ModConstants.NBT_KILL_LEVEL, level);
    }

    public static int getKillCount(Mob mob) {
        return mob.getPersistentData().getInt(ModConstants.NBT_KILL_COUNT);
    }

    public static void incrementKillCount(Mob mob) {
        mob.getPersistentData().putInt(ModConstants.NBT_KILL_COUNT, getKillCount(mob) + 1);
    }

    public static boolean hasKills(Mob mob) {
        return getKillCount(mob) > 0;
    }
}
