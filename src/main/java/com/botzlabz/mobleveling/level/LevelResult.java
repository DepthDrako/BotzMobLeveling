package com.botzlabz.mobleveling.level;

import com.botzlabz.mobleveling.data.AttributeScaling;
import com.botzlabz.mobleveling.data.LevelRule;
import com.botzlabz.mobleveling.data.MobOverride;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LevelResult {

    public static final LevelResult SKIP = new LevelResult(true);

    private final boolean skip;
    private final int level;
    @Nullable
    private final LevelRule sourceRule;
    @Nullable
    private final MobOverride mobOverride;
    private final Map<ResourceLocation, AttributeScaling> attributeScaling;
    private final boolean ignoreLevelCap;
    private final boolean huntToLevel;
    private final double huntToLevelChance;
    private final double adaptiveGearScore;

    private LevelResult(boolean skip) {
        this.skip = skip;
        this.level = 0;
        this.sourceRule = null;
        this.mobOverride = null;
        this.attributeScaling = Collections.emptyMap();
        this.ignoreLevelCap = false;
        this.huntToLevel = false;
        this.huntToLevelChance = 1.0;
        this.adaptiveGearScore = 0.0;
    }

    public LevelResult(int level, @Nullable LevelRule sourceRule, @Nullable MobOverride mobOverride,
                       Map<ResourceLocation, AttributeScaling> attributeScaling, boolean ignoreLevelCap,
                       boolean huntToLevel, double huntToLevelChance, double adaptiveGearScore) {
        this.skip = false;
        this.level = level;
        this.sourceRule = sourceRule;
        this.mobOverride = mobOverride;
        this.attributeScaling = attributeScaling;
        this.ignoreLevelCap = ignoreLevelCap;
        this.huntToLevel = huntToLevel;
        this.huntToLevelChance = huntToLevelChance;
        this.adaptiveGearScore = adaptiveGearScore;
    }

    public boolean shouldSkip() {
        return skip;
    }

    public int getLevel() {
        return level;
    }

    @Nullable
    public LevelRule getSourceRule() {
        return sourceRule;
    }

    @Nullable
    public MobOverride getMobOverride() {
        return mobOverride;
    }

    public Map<ResourceLocation, AttributeScaling> getAttributeScaling() {
        return attributeScaling;
    }

    public boolean isIgnoreLevelCap() {
        return ignoreLevelCap;
    }

    public boolean shouldHuntToLevel() {
        return huntToLevel;
    }

    public double getHuntToLevelChance() {
        return huntToLevelChance;
    }

    /**
     * The nearby-player gear score computed during resolution (0 if adaptive difficulty
     * is off or no player was in range). Reused by the spawn handler to apply adaptive
     * stat/equipment modifiers without recomputing.
     */
    public double getAdaptiveGearScore() {
        return adaptiveGearScore;
    }

    @Nullable
    public ResourceLocation getSourceRuleId() {
        return sourceRule != null ? sourceRule.getId() : null;
    }

    @Nullable
    public String getSourceRuleType() {
        return sourceRule != null ? sourceRule.getRuleType() : null;
    }

    public static Builder builder(int level) {
        return new Builder(level);
    }

    public static class Builder {
        private final int level;
        private LevelRule sourceRule;
        private MobOverride mobOverride;
        private Map<ResourceLocation, AttributeScaling> attributeScaling = new HashMap<>();
        private boolean ignoreLevelCap = false;
        private boolean huntToLevel = false;
        private double huntToLevelChance = 1.0;
        private double adaptiveGearScore = 0.0;

        public Builder(int level) {
            this.level = level;
        }

        public Builder sourceRule(LevelRule rule) {
            this.sourceRule = rule;
            return this;
        }

        public Builder mobOverride(MobOverride override) {
            this.mobOverride = override;
            return this;
        }

        public Builder attributeScaling(Map<ResourceLocation, AttributeScaling> scaling) {
            this.attributeScaling = new HashMap<>(scaling);
            return this;
        }

        public Builder ignoreLevelCap(boolean ignore) {
            this.ignoreLevelCap = ignore;
            return this;
        }

        public Builder huntToLevel(boolean hunt) {
            this.huntToLevel = hunt;
            return this;
        }

        public Builder huntToLevelChance(double chance) {
            this.huntToLevelChance = chance;
            return this;
        }

        public Builder adaptiveGearScore(double score) {
            this.adaptiveGearScore = score;
            return this;
        }

        public LevelResult build() {
            return new LevelResult(level, sourceRule, mobOverride, attributeScaling, ignoreLevelCap,
                    huntToLevel, huntToLevelChance, adaptiveGearScore);
        }
    }
}
