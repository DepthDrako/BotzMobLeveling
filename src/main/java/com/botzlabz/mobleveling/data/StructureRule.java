package com.botzlabz.mobleveling.data;

import com.botzlabz.mobleveling.util.ModConstants;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class StructureRule implements LevelRule {

    private final ResourceLocation id;
    private final ResourceLocation structureId;
    private final int priority;
    private final boolean enabled;
    private final int minLevel;
    private final int maxLevel;
    private final String levelMode;
    @Nullable
    private final Integer fixedLevel;
    private final boolean ignoreDistanceScaling;
    private final double distanceMultiplier;
    private final Map<ResourceLocation, AttributeScaling> attributeScaling;
    private final Map<ResourceLocation, MobOverride> mobOverrides;

    public StructureRule(ResourceLocation id, ResourceLocation structureId, int priority, boolean enabled,
                         int minLevel, int maxLevel, String levelMode, @Nullable Integer fixedLevel,
                         boolean ignoreDistanceScaling, double distanceMultiplier,
                         Map<ResourceLocation, AttributeScaling> attributeScaling,
                         Map<ResourceLocation, MobOverride> mobOverrides) {
        this.id = id;
        this.structureId = structureId;
        this.priority = priority;
        this.enabled = enabled;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.levelMode = levelMode;
        this.fixedLevel = fixedLevel;
        this.ignoreDistanceScaling = ignoreDistanceScaling;
        this.distanceMultiplier = distanceMultiplier;
        this.attributeScaling = attributeScaling;
        this.mobOverrides = mobOverrides;
    }

    public static StructureRule fromJson(ResourceLocation id, JsonObject json) {
        String structureIdStr = GsonHelper.getAsString(json, "structure");
        ResourceLocation structureId = new ResourceLocation(structureIdStr);

        int priority = GsonHelper.getAsInt(json, "priority", 100);
        boolean enabled = GsonHelper.getAsBoolean(json, "enabled", true);

        int minLevel = 1;
        int maxLevel = 100;
        if (json.has("level_range")) {
            JsonObject range = GsonHelper.getAsJsonObject(json, "level_range");
            minLevel = GsonHelper.getAsInt(range, "min", 1);
            maxLevel = GsonHelper.getAsInt(range, "max", 100);
        }

        String levelMode = GsonHelper.getAsString(json, "level_mode", ModConstants.LEVEL_MODE_RANDOM);
        Integer fixedLevel = json.has("fixed_level") ? GsonHelper.getAsInt(json, "fixed_level") : null;
        boolean ignoreDistanceScaling = GsonHelper.getAsBoolean(json, "ignore_distance_scaling", true);
        double distanceMultiplier = GsonHelper.getAsDouble(json, "distance_multiplier", 1.0);

        Map<ResourceLocation, AttributeScaling> attributeScaling = new HashMap<>();
        if (json.has("attribute_scaling")) {
            JsonObject scalingJson = GsonHelper.getAsJsonObject(json, "attribute_scaling");
            for (Map.Entry<String, JsonElement> entry : scalingJson.entrySet()) {
                AttributeScaling scaling = AttributeScaling.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                attributeScaling.put(scaling.getAttributeId(), scaling);
            }
        }

        Map<ResourceLocation, MobOverride> mobOverrides = new HashMap<>();
        if (json.has("mob_overrides")) {
            JsonObject overridesJson = GsonHelper.getAsJsonObject(json, "mob_overrides");
            for (Map.Entry<String, JsonElement> entry : overridesJson.entrySet()) {
                MobOverride override = MobOverride.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                mobOverrides.put(override.getMobId(), override);
            }
        }

        return new StructureRule(id, structureId, priority, enabled, minLevel, maxLevel,
                levelMode, fixedLevel, ignoreDistanceScaling, distanceMultiplier, attributeScaling, mobOverrides);
    }

    public ResourceLocation getStructureId() {
        return structureId;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int getMinLevel() {
        return minLevel;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public String getLevelMode() {
        return levelMode;
    }

    @Override
    @Nullable
    public Integer getFixedLevel() {
        return fixedLevel;
    }

    @Override
    public boolean ignoresDistanceScaling() {
        return ignoreDistanceScaling;
    }

    @Override
    public double getDistanceMultiplier() {
        return distanceMultiplier;
    }

    @Override
    public Map<ResourceLocation, AttributeScaling> getAttributeScaling() {
        return attributeScaling;
    }

    @Override
    public Map<ResourceLocation, MobOverride> getMobOverrides() {
        return mobOverrides;
    }

    @Override
    public String getRuleType() {
        return ModConstants.RULE_TYPE_STRUCTURE;
    }
}
