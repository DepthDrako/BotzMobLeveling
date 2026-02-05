package com.botzlabz.mobleveling.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class MobOverride {

    private final ResourceLocation mobId;
    @Nullable
    private final Integer fixedLevel;
    @Nullable
    private final Integer levelBonus;
    @Nullable
    private final Integer minLevel;
    @Nullable
    private final Integer maxLevel;
    private final boolean ignoreLevelCap;
    private final Map<ResourceLocation, Double> attributeMultipliers;
    private final Map<ResourceLocation, AttributeScaling> customScaling;
    @Nullable
    private final Boolean canAttack; // null = use config default, true/false = override

    public MobOverride(ResourceLocation mobId, @Nullable Integer fixedLevel, @Nullable Integer levelBonus,
                       @Nullable Integer minLevel, @Nullable Integer maxLevel, boolean ignoreLevelCap,
                       Map<ResourceLocation, Double> attributeMultipliers,
                       Map<ResourceLocation, AttributeScaling> customScaling,
                       @Nullable Boolean canAttack) {
        this.mobId = mobId;
        this.fixedLevel = fixedLevel;
        this.levelBonus = levelBonus;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.ignoreLevelCap = ignoreLevelCap;
        this.attributeMultipliers = attributeMultipliers;
        this.customScaling = customScaling;
        this.canAttack = canAttack;
    }

    public static MobOverride fromJson(String mobIdStr, JsonObject json) {
        ResourceLocation mobId = new ResourceLocation(mobIdStr);

        Integer fixedLevel = json.has("fixed_level") ? GsonHelper.getAsInt(json, "fixed_level") : null;
        Integer levelBonus = json.has("level_bonus") ? GsonHelper.getAsInt(json, "level_bonus") : null;
        Integer minLevel = json.has("min_level") ? GsonHelper.getAsInt(json, "min_level") : null;
        Integer maxLevel = json.has("max_level") ? GsonHelper.getAsInt(json, "max_level") : null;

        if (json.has("level_range")) {
            JsonObject range = GsonHelper.getAsJsonObject(json, "level_range");
            minLevel = GsonHelper.getAsInt(range, "min", minLevel != null ? minLevel : 1);
            maxLevel = GsonHelper.getAsInt(range, "max", maxLevel != null ? maxLevel : 100);
        }

        boolean ignoreLevelCap = GsonHelper.getAsBoolean(json, "ignore_level_cap", false);

        // Can attack - null means use config default
        Boolean canAttack = null;
        if (json.has("can_attack")) {
            canAttack = GsonHelper.getAsBoolean(json, "can_attack");
        }

        Map<ResourceLocation, Double> attributeMultipliers = new HashMap<>();
        if (json.has("attribute_multipliers")) {
            JsonObject multipliersJson = GsonHelper.getAsJsonObject(json, "attribute_multipliers");
            for (Map.Entry<String, JsonElement> entry : multipliersJson.entrySet()) {
                ResourceLocation attrId = new ResourceLocation(entry.getKey());
                double multiplier = entry.getValue().getAsDouble();
                attributeMultipliers.put(attrId, multiplier);
            }
        }

        Map<ResourceLocation, AttributeScaling> customScaling = new HashMap<>();
        if (json.has("attribute_scaling")) {
            JsonObject scalingJson = GsonHelper.getAsJsonObject(json, "attribute_scaling");
            for (Map.Entry<String, JsonElement> entry : scalingJson.entrySet()) {
                AttributeScaling scaling = AttributeScaling.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                customScaling.put(scaling.getAttributeId(), scaling);
            }
        }

        return new MobOverride(mobId, fixedLevel, levelBonus, minLevel, maxLevel, ignoreLevelCap, attributeMultipliers, customScaling, canAttack);
    }

    public ResourceLocation getMobId() {
        return mobId;
    }

    @Nullable
    public Integer getFixedLevel() {
        return fixedLevel;
    }

    @Nullable
    public Integer getLevelBonus() {
        return levelBonus;
    }

    @Nullable
    public Integer getMinLevel() {
        return minLevel;
    }

    @Nullable
    public Integer getMaxLevel() {
        return maxLevel;
    }

    public boolean isIgnoreLevelCap() {
        return ignoreLevelCap;
    }

    public Map<ResourceLocation, Double> getAttributeMultipliers() {
        return attributeMultipliers;
    }

    public Map<ResourceLocation, AttributeScaling> getCustomScaling() {
        return customScaling;
    }

    public boolean hasFixedLevel() {
        return fixedLevel != null;
    }

    public boolean hasLevelBonus() {
        return levelBonus != null;
    }

    public boolean hasCustomLevelRange() {
        return minLevel != null || maxLevel != null;
    }

    /**
     * Whether this mob can attack when leveled.
     * @return null = use config default, true = can attack, false = cannot attack
     */
    @Nullable
    public Boolean getCanAttack() {
        return canAttack;
    }

    /**
     * Check if this override explicitly sets attack behavior.
     */
    public boolean hasCanAttackOverride() {
        return canAttack != null;
    }
}
