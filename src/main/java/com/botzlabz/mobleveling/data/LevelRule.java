package com.botzlabz.mobleveling.data;

import com.botzlabz.mobleveling.config.MobLevelingConfig;
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

    /** When true, mobs assigned this rule will actively hunt other mobs to gain kill XP. */
    default boolean shouldHuntToLevel() {
        return false;
    }

    /**
     * Probability (0.0–1.0) that a mob assigned this rule will actually receive hunting AI.
     * Only meaningful when shouldHuntToLevel() is true.
     * Defaults to the global config value (huntToLevelChance).
     */
    default double getHuntToLevelChance() {
        return MobLevelingConfig.HUNT_TO_LEVEL_CHANCE.get();
    }

    String getRuleType();
}
