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

    private LevelResult(boolean skip) {
        this.skip = skip;
        this.level = 0;
        this.sourceRule = null;
        this.mobOverride = null;
        this.attributeScaling = Collections.emptyMap();
        this.ignoreLevelCap = false;
    }

    public LevelResult(int level, @Nullable LevelRule sourceRule, @Nullable MobOverride mobOverride,
                       Map<ResourceLocation, AttributeScaling> attributeScaling, boolean ignoreLevelCap) {
        this.skip = false;
        this.level = level;
        this.sourceRule = sourceRule;
        this.mobOverride = mobOverride;
        this.attributeScaling = attributeScaling;
        this.ignoreLevelCap = ignoreLevelCap;
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

        public LevelResult build() {
            return new LevelResult(level, sourceRule, mobOverride, attributeScaling, ignoreLevelCap);
        }
    }
}
