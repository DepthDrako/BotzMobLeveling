package com.botzlabz.mobleveling.data;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.boss.BossRule;
import com.botzlabz.mobleveling.util.JsonHelper;
import com.botzlabz.mobleveling.util.ModConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

public class MobLevelingDataManager extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static MobLevelingDataManager instance;

    private Map<ResourceLocation, StructureRule> structureRules = new HashMap<>();
    private Map<ResourceLocation, BiomeRule> biomeRulesByBiome = new HashMap<>();
    private List<BiomeRule> biomeTagRules = new ArrayList<>();
    private List<DimensionRule> dimensionRules = new ArrayList<>();
    private List<BaseRule> baseRules = new ArrayList<>();
    private List<BossRule> bossRules = new ArrayList<>();

    public MobLevelingDataManager() {
        super(GSON, ModConstants.DATA_DIRECTORY);
        instance = this;
    }

    @Nullable
    public static MobLevelingDataManager getInstance() {
        return instance;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        // Clear existing data
        structureRules = new HashMap<>();
        biomeRulesByBiome = new HashMap<>();
        biomeTagRules = new ArrayList<>();
        dimensionRules = new ArrayList<>();
        baseRules = new ArrayList<>();
        bossRules = new ArrayList<>();

        int structureCount = 0;
        int biomeCount = 0;
        int dimensionCount = 0;
        int baseCount = 0;
        int bossCount = 0;
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();

            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "rule");

                // Validate common fields
                JsonHelper.validateLevelRange(json, id);
                JsonHelper.validateAttributeScaling(json, id);

                // Determine rule type from path
                String path = id.getPath();

                if (path.startsWith(ModConstants.STRUCTURES_PATH + "/")) {
                    StructureRule rule = StructureRule.fromJson(id, json);
                    structureRules.put(rule.getStructureId(), rule);
                    structureCount++;
                    LOGGER.debug("Loaded structure rule: {} for structure {}", id, rule.getStructureId());
                } else if (path.startsWith(ModConstants.BIOMES_PATH + "/")) {
                    BiomeRule rule = BiomeRule.fromJson(id, json);
                    if (rule.hasBiomeTags()) {
                        biomeTagRules.add(rule);
                    }
                    if (rule.hasBiomeId()) {
                        biomeRulesByBiome.put(rule.getBiomeId(), rule);
                    }
                    biomeCount++;
                    LOGGER.debug("Loaded biome rule: {}", id);
                } else if (path.startsWith(ModConstants.DIMENSIONS_PATH + "/")) {
                    DimensionRule rule = DimensionRule.fromJson(id, json);
                    if (rule.hasDimensions()) {
                        dimensionRules.add(rule);
                        dimensionCount++;
                        LOGGER.debug("Loaded dimension rule: {} for dimensions {}", id, rule.getDimensionIds());
                    } else {
                        LOGGER.warn("Dimension rule {} has no 'dimension' or 'dimensions' field, skipping", id);
                    }
                } else if (path.startsWith(ModConstants.BASE_PATH + "/")) {
                    BaseRule rule = BaseRule.fromJson(id, json);
                    baseRules.add(rule);
                    baseCount++;
                    LOGGER.debug("Loaded base rule: {} type {}", id, rule.getType());
                } else if (path.startsWith(ModConstants.BOSSES_PATH + "/")) {
                    BossRule rule = BossRule.fromJson(id, json);
                    bossRules.add(rule);
                    bossCount++;
                    LOGGER.debug("Loaded boss rule: {}", id);
                } else {
                    LOGGER.warn("Unknown rule path: {} - should be in structures/, biomes/, dimensions/, base/, or bosses/", path);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load mob level rule {}: {}", id, e.getMessage());
                errorCount++;
            }
        }

        // Sort base rules by priority (descending - higher priority first)
        baseRules.sort(Comparator.comparingInt(BaseRule::getPriority).reversed());

        // Sort biome tag rules by priority (descending)
        biomeTagRules.sort(Comparator.comparingInt(BiomeRule::getPriority).reversed());

        // Sort dimension rules by priority (descending)
        dimensionRules.sort(Comparator.comparingInt(DimensionRule::getPriority).reversed());

        // Sort boss rules by tier (descending - higher tier = more specific/important)
        bossRules.sort(Comparator.comparingInt(BossRule::getTier).reversed());

        LOGGER.info("[{}] Loaded {} structure rules, {} biome rules, {} dimension rules, {} base rules, {} boss rules ({} errors)",
                BotzMobLeveling.MOD_ID, structureCount, biomeCount, dimensionCount, baseCount, bossCount, errorCount);
    }

    // Structure rules

    public Collection<StructureRule> getStructureRules() {
        return structureRules.values();
    }

    @Nullable
    public StructureRule getStructureRule(ResourceLocation structureId) {
        return structureRules.get(structureId);
    }

    public boolean hasStructureRule(ResourceLocation structureId) {
        return structureRules.containsKey(structureId);
    }

    // Biome rules

    @Nullable
    public BiomeRule getBiomeRule(ResourceLocation biomeId) {
        return biomeRulesByBiome.get(biomeId);
    }

    public List<BiomeRule> getBiomeTagRules() {
        return biomeTagRules;
    }

    public Collection<BiomeRule> getAllBiomeRules() {
        Set<BiomeRule> all = new HashSet<>(biomeRulesByBiome.values());
        all.addAll(biomeTagRules);
        return all;
    }

    public Collection<BiomeRule> getBiomeRules() {
        return getAllBiomeRules();
    }

    // Dimension rules

    public List<DimensionRule> getDimensionRules() {
        return dimensionRules;
    }

    // Base rules

    public List<BaseRule> getBaseRules() {
        return baseRules;
    }

    // Stats

    public int getTotalRuleCount() {
        return structureRules.size() + biomeRulesByBiome.size() + biomeTagRules.size() + dimensionRules.size() + baseRules.size();
    }

    public String getStats() {
        return String.format("Structure: %d, Biome: %d (+ %d tag rules), Dimension: %d, Base: %d, Boss: %d",
                structureRules.size(), biomeRulesByBiome.size(), biomeTagRules.size(), dimensionRules.size(), baseRules.size(), bossRules.size());
    }

    // Boss rules

    public List<BossRule> getBossRules() {
        return bossRules;
    }

    @Nullable
    public BossRule getBossRule(ResourceLocation ruleId) {
        for (BossRule rule : bossRules) {
            if (rule.getId().equals(ruleId)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * Find all boss rules that could apply to a specific mob type.
     */
    public List<BossRule> getBossRulesForMob(ResourceLocation mobId) {
        List<BossRule> matching = new ArrayList<>();
        for (BossRule rule : bossRules) {
            if (rule.isEnabled() && rule.appliesToMob(mobId)) {
                matching.add(rule);
            }
        }
        return matching;
    }
}
