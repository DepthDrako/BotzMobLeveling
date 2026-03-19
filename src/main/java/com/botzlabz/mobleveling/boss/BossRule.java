package com.botzlabz.mobleveling.boss;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.BossEvent;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Represents a boss rule loaded from a datapack.
 * Defines when and how a mob can become a boss.
 */
public class BossRule {

    private final ResourceLocation id;
    private final boolean enabled;

    // Target mobs
    private final Set<ResourceLocation> targetMobs;

    // Spawn conditions
    private final double spawnChance; // 0.0 to 1.0
    private final Set<ResourceLocation> structures; // Empty = anywhere
    private final Set<ResourceLocation> biomes; // Empty = any biome

    // Boss properties
    private final String displayName;
    private final int tier; // 1 = normal, 2 = elite, 3 = legendary, etc.
    private final int level; // Fixed level for the boss
    private final boolean ignoreLevelCap;

    // Visual properties
    private final BossBarProperties bossBar;
    private final float sizeMultiplier;
    private final boolean glowEffect;
    private final String glowColor;
    private final String particleEffect;

    // Immunities
    private final Set<String> immunities;

    // Stats
    private final Map<ResourceLocation, Double> statMultipliers;

    // Loot
    private final double xpMultiplier;
    @Nullable
    private final ResourceLocation lootTable;

    // Minions
    @Nullable
    private final MinionConfig minionConfig;

    // Hunting
    private final boolean huntToLevel;
    private final double huntToLevelChance;

    private BossRule(Builder builder) {
        this.id = builder.id;
        this.enabled = builder.enabled;
        this.targetMobs = builder.targetMobs;
        this.spawnChance = builder.spawnChance;
        this.structures = builder.structures;
        this.biomes = builder.biomes;
        this.displayName = builder.displayName;
        this.tier = builder.tier;
        this.level = builder.level;
        this.ignoreLevelCap = builder.ignoreLevelCap;
        this.bossBar = builder.bossBar;
        this.sizeMultiplier = builder.sizeMultiplier;
        this.glowEffect = builder.glowEffect;
        this.glowColor = builder.glowColor;
        this.particleEffect = builder.particleEffect;
        this.immunities = builder.immunities;
        this.statMultipliers = builder.statMultipliers;
        this.xpMultiplier = builder.xpMultiplier;
        this.lootTable = builder.lootTable;
        this.minionConfig = builder.minionConfig;
        this.huntToLevel = builder.huntToLevel;
        this.huntToLevelChance = builder.huntToLevelChance;
    }

    // ==================== Getters ====================

    public ResourceLocation getId() { return id; }
    public boolean isEnabled() { return enabled; }
    public Set<ResourceLocation> getTargetMobs() { return targetMobs; }
    public double getSpawnChance() { return spawnChance; }
    public Set<ResourceLocation> getStructures() { return structures; }
    public Set<ResourceLocation> getBiomes() { return biomes; }
    public String getDisplayName() { return displayName; }
    public int getTier() { return tier; }
    public int getLevel() { return level; }
    public boolean isIgnoreLevelCap() { return ignoreLevelCap; }
    public BossBarProperties getBossBar() { return bossBar; }
    public float getSizeMultiplier() { return sizeMultiplier; }
    public boolean hasGlowEffect() { return glowEffect; }
    public String getGlowColor() { return glowColor; }
    @Nullable
    public String getParticleEffect() { return particleEffect; }
    public Set<String> getImmunities() { return immunities; }
    public Map<ResourceLocation, Double> getStatMultipliers() { return statMultipliers; }
    public double getXpMultiplier() { return xpMultiplier; }
    @Nullable
    public ResourceLocation getLootTable() { return lootTable; }
    @Nullable
    public MinionConfig getMinionConfig() { return minionConfig; }
    public boolean shouldHuntToLevel() { return huntToLevel; }
    public double getHuntToLevelChance() { return huntToLevelChance; }

    public boolean appliesToMob(ResourceLocation mobId) {
        return targetMobs.isEmpty() || targetMobs.contains(mobId);
    }

    public boolean requiresStructure() {
        return !structures.isEmpty();
    }

    public boolean appliesToStructure(ResourceLocation structureId) {
        return structures.isEmpty() || structures.contains(structureId);
    }

    public boolean appliesToBiome(ResourceLocation biomeId) {
        return biomes.isEmpty() || biomes.contains(biomeId);
    }

    // ==================== JSON Parsing ====================

    public static BossRule fromJson(ResourceLocation id, JsonObject json) {
        Builder builder = new Builder(id);

        builder.enabled(GsonHelper.getAsBoolean(json, "enabled", true));

        // Target mobs
        if (json.has("target_mobs")) {
            JsonArray mobs = GsonHelper.getAsJsonArray(json, "target_mobs");
            for (JsonElement elem : mobs) {
                builder.addTargetMob(new ResourceLocation(elem.getAsString()));
            }
        }

        // Spawn conditions
        builder.spawnChance(GsonHelper.getAsDouble(json, "spawn_chance", 0.1));

        if (json.has("structures")) {
            JsonArray structs = GsonHelper.getAsJsonArray(json, "structures");
            for (JsonElement elem : structs) {
                builder.addStructure(new ResourceLocation(elem.getAsString()));
            }
        }

        if (json.has("biomes")) {
            JsonArray biomes = GsonHelper.getAsJsonArray(json, "biomes");
            for (JsonElement elem : biomes) {
                builder.addBiome(new ResourceLocation(elem.getAsString()));
            }
        }

        // Boss properties
        builder.displayName(GsonHelper.getAsString(json, "display_name", "§c§lBoss"));
        builder.tier(GsonHelper.getAsInt(json, "tier", 1));
        builder.level(GsonHelper.getAsInt(json, "level", 50));
        builder.ignoreLevelCap(GsonHelper.getAsBoolean(json, "ignore_level_cap", true));

        // Boss bar
        if (json.has("boss_bar")) {
            JsonObject barJson = GsonHelper.getAsJsonObject(json, "boss_bar");
            builder.bossBar(BossBarProperties.fromJson(barJson));
        }

        // Visuals
        builder.sizeMultiplier(GsonHelper.getAsFloat(json, "size_multiplier", 1.0f));
        builder.glowEffect(GsonHelper.getAsBoolean(json, "glow_effect", true));
        builder.glowColor(GsonHelper.getAsString(json, "glow_color", "red"));

        if (json.has("particle_effect")) {
            builder.particleEffect(GsonHelper.getAsString(json, "particle_effect"));
        }

        // Immunities
        if (json.has("immunities")) {
            JsonArray immunitiesArr = GsonHelper.getAsJsonArray(json, "immunities");
            for (JsonElement elem : immunitiesArr) {
                builder.addImmunity(elem.getAsString());
            }
        }

        // Stat multipliers
        if (json.has("stat_multipliers")) {
            JsonObject stats = GsonHelper.getAsJsonObject(json, "stat_multipliers");
            for (Map.Entry<String, JsonElement> entry : stats.entrySet()) {
                builder.addStatMultiplier(new ResourceLocation(entry.getKey()), entry.getValue().getAsDouble());
            }
        }

        // Loot
        builder.xpMultiplier(GsonHelper.getAsDouble(json, "xp_multiplier", 5.0));
        if (json.has("loot_table")) {
            builder.lootTable(new ResourceLocation(GsonHelper.getAsString(json, "loot_table")));
        }

        // Minions
        if (json.has("minions")) {
            builder.minionConfig(MinionConfig.fromJson(GsonHelper.getAsJsonObject(json, "minions")));
        }

        // Hunting
        builder.huntToLevel(GsonHelper.getAsBoolean(json, "hunt_to_level", false));
        builder.huntToLevelChance(GsonHelper.getAsDouble(json, "hunt_to_level_chance", 1.0));

        return builder.build();
    }

    // ==================== Builder ====================

    public static class Builder {
        private final ResourceLocation id;
        private boolean enabled = true;
        private final Set<ResourceLocation> targetMobs = new HashSet<>();
        private double spawnChance = 0.1;
        private final Set<ResourceLocation> structures = new HashSet<>();
        private final Set<ResourceLocation> biomes = new HashSet<>();
        private String displayName = "§c§lBoss";
        private int tier = 1;
        private int level = 50;
        private boolean ignoreLevelCap = true;
        private BossBarProperties bossBar = new BossBarProperties();
        private float sizeMultiplier = 1.0f;
        private boolean glowEffect = true;
        private String glowColor = "red";
        private String particleEffect = null;
        private final Set<String> immunities = new HashSet<>();
        private final Map<ResourceLocation, Double> statMultipliers = new HashMap<>();
        private double xpMultiplier = 5.0;
        private ResourceLocation lootTable = null;
        private MinionConfig minionConfig = null;
        private boolean huntToLevel = false;
        private double huntToLevelChance = 1.0;

        public Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder addTargetMob(ResourceLocation mob) { this.targetMobs.add(mob); return this; }
        public Builder spawnChance(double chance) { this.spawnChance = Math.max(0, Math.min(1, chance)); return this; }
        public Builder addStructure(ResourceLocation structure) { this.structures.add(structure); return this; }
        public Builder addBiome(ResourceLocation biome) { this.biomes.add(biome); return this; }
        public Builder displayName(String name) { this.displayName = name; return this; }
        public Builder tier(int tier) { this.tier = tier; return this; }
        public Builder level(int level) { this.level = level; return this; }
        public Builder ignoreLevelCap(boolean ignore) { this.ignoreLevelCap = ignore; return this; }
        public Builder bossBar(BossBarProperties bar) { this.bossBar = bar; return this; }
        public Builder sizeMultiplier(float mult) { this.sizeMultiplier = mult; return this; }
        public Builder glowEffect(boolean glow) { this.glowEffect = glow; return this; }
        public Builder glowColor(String color) { this.glowColor = color; return this; }
        public Builder particleEffect(String effect) { this.particleEffect = effect; return this; }
        public Builder addImmunity(String immunity) { this.immunities.add(immunity); return this; }
        public Builder addStatMultiplier(ResourceLocation stat, double mult) { this.statMultipliers.put(stat, mult); return this; }
        public Builder xpMultiplier(double mult) { this.xpMultiplier = mult; return this; }
        public Builder lootTable(ResourceLocation table) { this.lootTable = table; return this; }
        public Builder minionConfig(MinionConfig config) { this.minionConfig = config; return this; }
        public Builder huntToLevel(boolean hunt) { this.huntToLevel = hunt; return this; }
        public Builder huntToLevelChance(double chance) { this.huntToLevelChance = Math.max(0, Math.min(1, chance)); return this; }

        public BossRule build() {
            return new BossRule(this);
        }
    }

    // ==================== Inner Classes ====================

    public static class BossBarProperties {
        private final BossEvent.BossBarColor color;
        private final BossEvent.BossBarOverlay style;
        private final boolean visible;

        public BossBarProperties() {
            this.color = BossEvent.BossBarColor.RED;
            this.style = BossEvent.BossBarOverlay.PROGRESS;
            this.visible = true;
        }

        public BossBarProperties(BossEvent.BossBarColor color, BossEvent.BossBarOverlay style, boolean visible) {
            this.color = color;
            this.style = style;
            this.visible = visible;
        }

        public BossEvent.BossBarColor getColor() { return color; }
        public BossEvent.BossBarOverlay getStyle() { return style; }
        public boolean isVisible() { return visible; }

        public static BossBarProperties fromJson(JsonObject json) {
            BossEvent.BossBarColor color = parseColor(GsonHelper.getAsString(json, "color", "red"));
            BossEvent.BossBarOverlay style = parseStyle(GsonHelper.getAsString(json, "style", "progress"));
            boolean visible = GsonHelper.getAsBoolean(json, "visible", true);
            return new BossBarProperties(color, style, visible);
        }

        private static BossEvent.BossBarColor parseColor(String colorStr) {
            return switch (colorStr.toLowerCase()) {
                case "pink" -> BossEvent.BossBarColor.PINK;
                case "blue" -> BossEvent.BossBarColor.BLUE;
                case "green" -> BossEvent.BossBarColor.GREEN;
                case "yellow" -> BossEvent.BossBarColor.YELLOW;
                case "purple" -> BossEvent.BossBarColor.PURPLE;
                case "white" -> BossEvent.BossBarColor.WHITE;
                default -> BossEvent.BossBarColor.RED;
            };
        }

        private static BossEvent.BossBarOverlay parseStyle(String styleStr) {
            return switch (styleStr.toLowerCase()) {
                case "notched_6" -> BossEvent.BossBarOverlay.NOTCHED_6;
                case "notched_10" -> BossEvent.BossBarOverlay.NOTCHED_10;
                case "notched_12" -> BossEvent.BossBarOverlay.NOTCHED_12;
                case "notched_20" -> BossEvent.BossBarOverlay.NOTCHED_20;
                default -> BossEvent.BossBarOverlay.PROGRESS;
            };
        }
    }

    public static class MinionConfig {
        private final ResourceLocation minionType;
        private final int count;
        private final int intervalSeconds;
        private final double healthThreshold; // 0.0 to 1.0, spawn when below this health %
        private final int maxMinions;

        public MinionConfig(ResourceLocation minionType, int count, int intervalSeconds, double healthThreshold, int maxMinions) {
            this.minionType = minionType;
            this.count = count;
            this.intervalSeconds = intervalSeconds;
            this.healthThreshold = healthThreshold;
            this.maxMinions = maxMinions;
        }

        public ResourceLocation getMinionType() { return minionType; }
        public int getCount() { return count; }
        public int getIntervalSeconds() { return intervalSeconds; }
        public double getHealthThreshold() { return healthThreshold; }
        public int getMaxMinions() { return maxMinions; }

        public static MinionConfig fromJson(JsonObject json) {
            ResourceLocation type = new ResourceLocation(GsonHelper.getAsString(json, "type", "minecraft:zombie"));
            int count = GsonHelper.getAsInt(json, "count", 2);
            int interval = GsonHelper.getAsInt(json, "interval_seconds", 30);
            double threshold = GsonHelper.getAsDouble(json, "health_threshold", 0.5);
            int max = GsonHelper.getAsInt(json, "max_minions", 10);
            return new MinionConfig(type, count, interval, threshold, max);
        }
    }
}
