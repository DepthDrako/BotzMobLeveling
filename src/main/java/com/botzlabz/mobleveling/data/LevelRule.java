package com.botzlabz.mobleveling.data;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;

public interface LevelRule {

    ResourceLocation getId();

    int getPriority();

    boolean isEnabled();

    int getMinLevel();

    int getMaxLevel();

    String getLevelMode();

    @Nullable
    Integer getFixedLevel();

    boolean ignoresDistanceScaling();

    double getDistanceMultiplier();

    Map<ResourceLocation, AttributeScaling> getAttributeScaling();

    Map<ResourceLocation, MobOverride> getMobOverrides();

    @Nullable
    default MobOverride getMobOverride(ResourceLocation mobId) {
        return getMobOverrides().get(mobId);
    }

    String getRuleType();
}
