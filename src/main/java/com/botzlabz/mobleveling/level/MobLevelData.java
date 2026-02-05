package com.botzlabz.mobleveling.level;

import com.botzlabz.mobleveling.util.ModConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;
import java.util.Optional;

public final class MobLevelData {

    private MobLevelData() {}

    public static int getLevel(Mob mob) {
        return mob.getPersistentData().getInt(ModConstants.NBT_LEVEL);
    }

    public static void setLevel(Mob mob, int level) {
        mob.getPersistentData().putInt(ModConstants.NBT_LEVEL, level);
    }

    public static boolean hasLevel(Mob mob) {
        return mob.getPersistentData().contains(ModConstants.NBT_LEVEL);
    }

    public static boolean isProcessed(Mob mob) {
        return mob.getPersistentData().getBoolean(ModConstants.NBT_PROCESSED);
    }

    public static void markProcessed(Mob mob) {
        mob.getPersistentData().putBoolean(ModConstants.NBT_PROCESSED, true);
    }

    public static void setSourceRule(Mob mob, @Nullable ResourceLocation ruleId, @Nullable String ruleType) {
        CompoundTag data = mob.getPersistentData();
        if (ruleId != null) {
            data.putString(ModConstants.NBT_RULE_ID, ruleId.toString());
        }
        if (ruleType != null) {
            data.putString(ModConstants.NBT_RULE_TYPE, ruleType);
        }
    }

    public static Optional<ResourceLocation> getSourceRuleId(Mob mob) {
        String rule = mob.getPersistentData().getString(ModConstants.NBT_RULE_ID);
        return rule.isEmpty() ? Optional.empty() : Optional.of(new ResourceLocation(rule));
    }

    public static Optional<String> getSourceRuleType(Mob mob) {
        String type = mob.getPersistentData().getString(ModConstants.NBT_RULE_TYPE);
        return type.isEmpty() ? Optional.empty() : Optional.of(type);
    }

    public static void clearData(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        data.remove(ModConstants.NBT_LEVEL);
        data.remove(ModConstants.NBT_PROCESSED);
        data.remove(ModConstants.NBT_RULE_ID);
        data.remove(ModConstants.NBT_RULE_TYPE);
    }

    public static void copyData(Mob source, Mob target) {
        if (isProcessed(source)) {
            setLevel(target, getLevel(source));
            markProcessed(target);
            getSourceRuleId(source).ifPresent(id ->
                    setSourceRule(target, id, getSourceRuleType(source).orElse(null)));
        }
    }
}
