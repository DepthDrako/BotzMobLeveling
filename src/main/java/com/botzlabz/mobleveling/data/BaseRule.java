package com.botzlabz.mobleveling.data;

import com.botzlabz.mobleveling.util.ModConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;
import java.util.*;

public class BaseRule implements LevelRule {

    private final ResourceLocation id;
    private final String type;
    private final int priority;
    private final boolean enabled;

    // Filtering - applies to
    private final Set<String> mobTypes;
    private final List<TagKey<EntityType<?>>> mobTags;
    private final Set<ResourceLocation> mobIds;

    // Filtering - excludes
    private final Set<ResourceLocation> excludedMobIds;
    private final List<TagKey<EntityType<?>>> excludedMobTags;

    private final int minLevel;
    private final int maxLevel;
    private final String levelMode;
    @Nullable
    private final Integer fixedLevel;
    private final boolean ignoreDistanceScaling;
    private final double distanceMultiplier;
    private final Map<ResourceLocation, AttributeScaling> attributeScaling;
    private final Map<ResourceLocation, MobOverride> mobOverrides;
    private final boolean huntToLevel;
    private final double huntToLevelChance;

    public BaseRule(ResourceLocation id, String type, int priority, boolean enabled,
                    Set<String> mobTypes, List<TagKey<EntityType<?>>> mobTags, Set<ResourceLocation> mobIds,
                    Set<ResourceLocation> excludedMobIds, List<TagKey<EntityType<?>>> excludedMobTags,
                    int minLevel, int maxLevel, String levelMode, @Nullable Integer fixedLevel,
                    boolean ignoreDistanceScaling, double distanceMultiplier,
                    Map<ResourceLocation, AttributeScaling> attributeScaling,
                    Map<ResourceLocation, MobOverride> mobOverrides, boolean huntToLevel, double huntToLevelChance) {
        this.id = id;
        this.type = type;
        this.priority = priority;
        this.enabled = enabled;
        this.mobTypes = mobTypes;
        this.mobTags = mobTags;
        this.mobIds = mobIds;
        this.excludedMobIds = excludedMobIds;
        this.excludedMobTags = excludedMobTags;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.levelMode = levelMode;
        this.fixedLevel = fixedLevel;
        this.ignoreDistanceScaling = ignoreDistanceScaling;
        this.distanceMultiplier = distanceMultiplier;
        this.attributeScaling = attributeScaling;
        this.mobOverrides = mobOverrides;
        this.huntToLevel = huntToLevel;
        this.huntToLevelChance = huntToLevelChance;
    }

    public static BaseRule fromJson(ResourceLocation id, JsonObject json) {
        String type = GsonHelper.getAsString(json, "type", "default");
        int priority = GsonHelper.getAsInt(json, "priority", 0);
        boolean enabled = GsonHelper.getAsBoolean(json, "enabled", true);

        // Parse applies_to
        Set<String> mobTypes = new HashSet<>();
        List<TagKey<EntityType<?>>> mobTags = new ArrayList<>();
        Set<ResourceLocation> mobIds = new HashSet<>();

        if (json.has("applies_to")) {
            JsonObject appliesTo = GsonHelper.getAsJsonObject(json, "applies_to");

            if (appliesTo.has("mob_types")) {
                JsonArray typesArray = GsonHelper.getAsJsonArray(appliesTo, "mob_types");
                for (JsonElement element : typesArray) {
                    mobTypes.add(element.getAsString().toLowerCase());
                }
            }

            if (appliesTo.has("mob_tags")) {
                JsonArray tagsArray = GsonHelper.getAsJsonArray(appliesTo, "mob_tags");
                for (JsonElement element : tagsArray) {
                    ResourceLocation tagId = new ResourceLocation(element.getAsString());
                    mobTags.add(TagKey.create(Registries.ENTITY_TYPE, tagId));
                }
            }

            if (appliesTo.has("mob_ids")) {
                JsonArray idsArray = GsonHelper.getAsJsonArray(appliesTo, "mob_ids");
                for (JsonElement element : idsArray) {
                    mobIds.add(new ResourceLocation(element.getAsString()));
                }
            }
        }

        // Parse excludes
        Set<ResourceLocation> excludedMobIds = new HashSet<>();
        List<TagKey<EntityType<?>>> excludedMobTags = new ArrayList<>();

        if (json.has("excludes")) {
            JsonObject excludes = GsonHelper.getAsJsonObject(json, "excludes");

            if (excludes.has("mob_ids")) {
                JsonArray idsArray = GsonHelper.getAsJsonArray(excludes, "mob_ids");
                for (JsonElement element : idsArray) {
                    excludedMobIds.add(new ResourceLocation(element.getAsString()));
                }
            }

            if (excludes.has("mob_tags")) {
                JsonArray tagsArray = GsonHelper.getAsJsonArray(excludes, "mob_tags");
                for (JsonElement element : tagsArray) {
                    ResourceLocation tagId = new ResourceLocation(element.getAsString());
                    excludedMobTags.add(TagKey.create(Registries.ENTITY_TYPE, tagId));
                }
            }
        }

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
        boolean huntToLevel = GsonHelper.getAsBoolean(json, "hunt_to_level", false);
        double huntToLevelChance = GsonHelper.getAsFloat(json, "hunt_to_level_chance", 1.0f);

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

        return new BaseRule(id, type, priority, enabled, mobTypes, mobTags, mobIds,
                excludedMobIds, excludedMobTags, minLevel, maxLevel, levelMode, fixedLevel,
                ignoreDistanceScaling, distanceMultiplier, attributeScaling, mobOverrides, huntToLevel, huntToLevelChance);
    }

    public String getType() {
        return type;
    }

    public Set<String> getMobTypes() {
        return mobTypes;
    }

    public List<TagKey<EntityType<?>>> getMobTags() {
        return mobTags;
    }

    public Set<ResourceLocation> getMobIds() {
        return mobIds;
    }

    public Set<ResourceLocation> getExcludedMobIds() {
        return excludedMobIds;
    }

    public List<TagKey<EntityType<?>>> getExcludedMobTags() {
        return excludedMobTags;
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
    public boolean shouldHuntToLevel() {
        return huntToLevel;
    }

    @Override
    public double getHuntToLevelChance() {
        return huntToLevelChance;
    }

    @Override
    public String getRuleType() {
        return ModConstants.RULE_TYPE_BASE;
    }
}
