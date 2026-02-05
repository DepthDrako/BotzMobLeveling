package com.botzlabz.mobleveling.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores and retrieves boss-related data from a mob's persistent NBT.
 */
public final class BossData {

    private static final String NBT_IS_BOSS = "botzmobleveling_IsBoss";
    private static final String NBT_BOSS_RULE_ID = "botzmobleveling_BossRuleId";
    private static final String NBT_BOSS_UUID = "botzmobleveling_BossUUID";
    private static final String NBT_BOSS_DISPLAY_NAME = "botzmobleveling_BossDisplayName";
    private static final String NBT_BOSS_TIER = "botzmobleveling_BossTier";
    private static final String NBT_BOSS_MAX_HEALTH = "botzmobleveling_BossMaxHealth";

    private BossData() {}

    // ==================== Core Boss State ====================

    public static boolean isBoss(Mob mob) {
        return mob.getPersistentData().getBoolean(NBT_IS_BOSS);
    }

    public static void markAsBoss(Mob mob, boolean isBoss) {
        mob.getPersistentData().putBoolean(NBT_IS_BOSS, isBoss);
        if (isBoss) {
            // Generate a unique ID for this boss instance
            mob.getPersistentData().putUUID(NBT_BOSS_UUID, UUID.randomUUID());
        }
    }

    @Nullable
    public static UUID getBossUUID(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (data.contains(NBT_BOSS_UUID)) {
            return data.getUUID(NBT_BOSS_UUID);
        }
        return null;
    }

    // ==================== Boss Rule Reference ====================

    public static void setBossRuleId(Mob mob, ResourceLocation ruleId) {
        mob.getPersistentData().putString(NBT_BOSS_RULE_ID, ruleId.toString());
    }

    public static Optional<ResourceLocation> getBossRuleId(Mob mob) {
        String ruleStr = mob.getPersistentData().getString(NBT_BOSS_RULE_ID);
        if (ruleStr.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ResourceLocation(ruleStr));
    }

    // ==================== Display Properties ====================

    public static void setDisplayName(Mob mob, String displayName) {
        mob.getPersistentData().putString(NBT_BOSS_DISPLAY_NAME, displayName);
    }

    @Nullable
    public static String getDisplayName(Mob mob) {
        String name = mob.getPersistentData().getString(NBT_BOSS_DISPLAY_NAME);
        return name.isEmpty() ? null : name;
    }

    // ==================== Boss Tier ====================

    public static void setBossTier(Mob mob, int tier) {
        mob.getPersistentData().putInt(NBT_BOSS_TIER, tier);
    }

    public static int getBossTier(Mob mob) {
        return mob.getPersistentData().getInt(NBT_BOSS_TIER);
    }

    // ==================== Health Tracking (for phases) ====================

    public static void setOriginalMaxHealth(Mob mob, float maxHealth) {
        mob.getPersistentData().putFloat(NBT_BOSS_MAX_HEALTH, maxHealth);
    }

    public static float getOriginalMaxHealth(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (data.contains(NBT_BOSS_MAX_HEALTH)) {
            return data.getFloat(NBT_BOSS_MAX_HEALTH);
        }
        return mob.getMaxHealth();
    }

    // ==================== Utility ====================

    public static void clearBossData(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        data.remove(NBT_IS_BOSS);
        data.remove(NBT_BOSS_RULE_ID);
        data.remove(NBT_BOSS_UUID);
        data.remove(NBT_BOSS_DISPLAY_NAME);
        data.remove(NBT_BOSS_TIER);
        data.remove(NBT_BOSS_MAX_HEALTH);
    }
}
