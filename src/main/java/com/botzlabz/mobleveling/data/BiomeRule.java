package com.botzlabz.mobleveling.data;

import com.botzlabz.mobleveling.util.ModConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;
import java.util.*;

public class BiomeRule implements LevelRule {

    private final ResourceLocation id;
    @Nullable
    private final ResourceLocation biomeId;
    private final List<TagKey<Biome>> biomeTags;
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

    public BiomeRule(ResourceLocation id, @Nullable ResourceLocation biomeId, List<TagKey<Biome>> biomeTags,
                     int priority, boolean enabled, int minLevel, int maxLevel, String levelMode,
                     @Nullable Integer fixedLevel, boolean ignoreDistanceScaling, double distanceMultiplier,
                     Map<ResourceLocation, AttributeScaling> attributeScaling,
                     Map<ResourceLocation, MobOverride> mobOverrides) {
        this.id = id;
        this.biomeId = biomeId;
        this.biomeTags = biomeTags;
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

    public static BiomeRule fromJson(ResourceLocation id, JsonObject json) {
        ResourceLocation biomeId = null;
        if (json.has("biome")) {
            biomeId = new ResourceLocation(GsonHelper.getAsString(json, "biome"));
        }

        List<TagKey<Biome>> biomeTags = new ArrayList<>();
        if (json.has("biome_tags")) {
            JsonArray tagsArray = GsonHelper.getAsJsonArray(json, "biome_tags");
            for (JsonElement element : tagsArray) {
                ResourceLocation tagId = new ResourceLocation(element.getAsString());
                biomeTags.add(TagKey.create(Registries.BIOME, tagId));
            }
        }

        int priority = GsonHelper.getAsInt(json, "priority", 50);
        boolean enabled = GsonHelper.getAsBoolean(json, "enabled", true);

        int minLevel = 1;
        int maxLevel = 100;
        if (json.has("level_range")) {
            JsonObject range = GsonHelper.getAsJsonObject(json, "level_range");
            minLevel = GsonHelper.getAsInt(range, "min", 1);
            maxLevel = GsonHelper.getAsInt(range, "max", 100);
        }

        String levelMode = GsonHelper.getAsString(json, "level_mode", ModConstants.LEVEL_MODE_DISTANCE);
        Integer fixedLevel = json.has("fixed_level") ? GsonHelper.getAsInt(json, "fixed_level") : null;
        boolean ignoreDistanceScaling = GsonHelper.getAsBoolean(json, "ignore_distance_scaling", false);
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

        return new BiomeRule(id, biomeId, biomeTags, priority, enabled, minLevel, maxLevel,
                levelMode, fixedLevel, ignoreDistanceScaling, distanceMultiplier, attributeScaling, mobOverrides);
    }

    @Nullable
    public ResourceLocation getBiomeId() {
        return biomeId;
    }

    public List<TagKey<Biome>> getBiomeTags() {
        return biomeTags;
    }

    public boolean hasBiomeTags() {
        return !biomeTags.isEmpty();
    }

    public boolean hasBiomeId() {
        return biomeId != null;
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
        return ModConstants.RULE_TYPE_BIOME;
    }
}
