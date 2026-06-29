package com.botzlabz.mobleveling.data;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.level.BossRule;
import com.botzlabz.mobleveling.level.LevelRule;
import com.botzlabz.mobleveling.level.LevelResolver;
import com.botzlabz.mobleveling.level.MobOverride;
import org.jetbrains.annotations.Nullable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Mob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads all mob-leveling rules from datapacks.
 *
 * <p>Rule files live at {@code data/<ns>/mob_levels/<category>/<name>.json}
 * where {@code category} is one of: {@code base}, {@code biomes},
 * {@code dimensions}, {@code structures}, {@code bosses}.
 *
 * <p>Resolution priority (highest first):
 * <ol>
 *   <li>Boss rules</li>
 *   <li>Structure rules</li>
 *   <li>Biome rules</li>
 *   <li>Dimension rules</li>
 *   <li>Base rules</li>
 *   <li>Fallback level (1)</li>
 * </ol>
 *
 * <p>Register via {@code AddReloadListenerEvent} on the GAME bus.
 */
public class MobLevelingDataManager extends SimpleJsonResourceReloadListener {

    // GSON and LOGGER MUST be declared before INSTANCE so they are non-null when
    // the constructor calls super(GSON, ...).  Java initialises static fields in
    // declaration order, so order matters here.
    private static final Logger LOGGER = LogManager.getLogger(BotzMobLeveling.MOD_ID);
    private static final Gson   GSON   = new GsonBuilder().setPrettyPrinting().create();

    public static final MobLevelingDataManager INSTANCE = new MobLevelingDataManager();

    // Sorted lists — higher priority first within category
    private List<BossRule>  bossRules      = Collections.emptyList();
    private List<LevelRule> structureRules = Collections.emptyList();
    private List<LevelRule> biomeRules     = Collections.emptyList();
    private List<LevelRule> dimensionRules = Collections.emptyList();
    private List<LevelRule> baseRules      = Collections.emptyList();

    // Index boss rules by entity type for quick lookup (used by MobSpawnHandler)
    private Map<String, BossRule> bossByEntity = Collections.emptyMap();

    public MobLevelingDataManager() {
        super(GSON, "mob_levels");
    }

    // -------------------------------------------------------------------------
    // Loading
    // -------------------------------------------------------------------------

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data,
                         ResourceManager manager, ProfilerFiller profiler) {
        List<BossRule>  newBoss      = new ArrayList<>();
        List<LevelRule> newStructure = new ArrayList<>();
        List<LevelRule> newBiome     = new ArrayList<>();
        List<LevelRule> newDimension = new ArrayList<>();
        List<LevelRule> newBase      = new ArrayList<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            ResourceLocation id = entry.getKey();
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject json = entry.getValue().getAsJsonObject();

            // Path pattern: <namespace>:<category>/<name>
            // e.g. botzmobleveling:base/hostile
            String path     = id.getPath();
            int    slashIdx = path.indexOf('/');
            if (slashIdx < 0) {
                LOGGER.warn("[BotzMobLeveling] Skipping rule at unexpected path: {}", id);
                continue;
            }
            String category = path.substring(0, slashIdx);
            String ruleId   = id.getNamespace() + ":" + path.substring(slashIdx + 1);

            try {
                switch (category) {
                    case "bosses"     -> newBoss.add(BossRule.from(ruleId, json));
                    case "structures" -> newStructure.add(LevelRule.from(ruleId, json));
                    case "biomes"     -> newBiome.add(LevelRule.from(ruleId, json));
                    case "dimensions" -> newDimension.add(LevelRule.from(ruleId, json));
                    case "base"       -> newBase.add(LevelRule.from(ruleId, json));
                    default -> LOGGER.warn("[BotzMobLeveling] Unknown category '{}' in {}", category, id);
                }
            } catch (Exception e) {
                LOGGER.error("[BotzMobLeveling] Failed to parse rule {}: {}", id, e.getMessage());
            }
        }

        Comparator<LevelRule> byPriority = Comparator.comparingInt((LevelRule r) -> r.priority).reversed();
        this.bossRules      = newBoss.stream().sorted(byPriority).collect(Collectors.toList());
        this.structureRules = newStructure.stream().sorted(byPriority).collect(Collectors.toList());
        this.biomeRules     = newBiome.stream().sorted(byPriority).collect(Collectors.toList());
        this.dimensionRules = newDimension.stream().sorted(byPriority).collect(Collectors.toList());
        this.baseRules      = newBase.stream().sorted(byPriority).collect(Collectors.toList());

        // Build entity → boss index
        Map<String, BossRule> idx = new HashMap<>();
        for (BossRule br : bossRules) {
            if (!br.entityTypeFilter.isEmpty()) idx.put(br.entityTypeFilter, br);
        }
        this.bossByEntity = Collections.unmodifiableMap(idx);

        if (MobLevelingConfig.DEBUG_MODE.get()) {
            LOGGER.info("[BotzMobLeveling] Loaded rules — boss:{} structure:{} biome:{} dimension:{} base:{}",
                bossRules.size(), structureRules.size(), biomeRules.size(),
                dimensionRules.size(), baseRules.size());
        }
    }

    // -------------------------------------------------------------------------
    // Resolution
    // -------------------------------------------------------------------------

    /**
     * Full resolution pipeline. Returns a {@link LevelResolver.LevelResult}
     * with {@code level == -1} when a SKIP rule matched.
     */
    public LevelResolver.LevelResult resolve(Mob mob, ServerLevel level) {
        net.minecraft.core.BlockPos pos = mob.blockPosition();

        // 1 – Boss rules
        for (BossRule rule : bossRules) {
            if (!rule.matchesEntity(mob)) continue;
            if (!rule.matchesDimension(level)) continue;
            if (!rule.matchesBiome(level, pos)) continue;
            if (!rule.matchesStructure(level, pos)) continue;
            int lvl = rule.resolve(mob, level);
            if (lvl == 0) return LevelResolver.LevelResult.SKIP; // SKIP mode
            return build(lvl, rule, "boss", rule, mob);
        }

        // 2 – Structure rules
        for (LevelRule rule : structureRules) {
            if (!rule.matchesEntity(mob)) continue;
            if (!rule.matchesDimension(level)) continue;
            if (!rule.matchesStructure(level, pos)) continue;
            int lvl = rule.resolve(mob, level);
            if (lvl == 0) return LevelResolver.LevelResult.SKIP;
            return build(lvl, rule, "structure", null, mob);
        }

        // 3 – Biome rules
        for (LevelRule rule : biomeRules) {
            if (!rule.matchesEntity(mob)) continue;
            if (!rule.matchesDimension(level)) continue;
            if (!rule.matchesBiome(level, pos)) continue;
            int lvl = rule.resolve(mob, level);
            if (lvl == 0) return LevelResolver.LevelResult.SKIP;
            return build(lvl, rule, "biome", null, mob);
        }

        // 4 – Dimension rules
        for (LevelRule rule : dimensionRules) {
            if (!rule.matchesEntity(mob)) continue;
            if (!rule.matchesDimension(level)) continue;
            int lvl = rule.resolve(mob, level);
            if (lvl == 0) return LevelResolver.LevelResult.SKIP;
            return build(lvl, rule, "dimension", null, mob);
        }

        // 5 – Base rules
        for (LevelRule rule : baseRules) {
            if (!rule.matchesEntity(mob)) continue;
            int lvl = rule.resolve(mob, level);
            if (lvl == 0) return LevelResolver.LevelResult.SKIP;
            return build(lvl, rule, "base", null, mob);
        }

        // 6 – Fallback
        return new LevelResolver.LevelResult(1, "fallback", "base", null);
    }

    /**
     * Builds a {@link LevelResolver.LevelResult} for a matched rule, resolving its
     * per-entity {@link MobOverride} and the effective cap exemption (rule flag OR
     * override flag).
     */
    private static LevelResolver.LevelResult build(int lvl, LevelRule rule, String type,
                                                   @Nullable BossRule boss, Mob mob) {
        MobOverride ov = rule.overrideFor(mob);
        boolean ignoreCap = rule.ignoreLevelCap || (ov != null && ov.ignoreLevelCap());

        // Merge datapack attribute ops: rule's baseline, then the override's on top
        // (override wins per attribute). Empty map stays empty (cheap common case).
        Map<String, com.botzlabz.mobleveling.level.AttrOp> ops;
        if (rule.attributeScaling.isEmpty() && (ov == null || ov.attributeScaling().isEmpty())) {
            ops = Map.of();
        } else {
            ops = new HashMap<>(rule.attributeScaling);
            if (ov != null) ops.putAll(ov.attributeScaling());
        }
        return new LevelResolver.LevelResult(lvl, rule.id, type, boss, ov, ignoreCap, ops);
    }

    /**
     * Resolves the "area level" at a position with no mob present — the level a
     * generic mob would be assigned there. Used by the public API / commands so
     * datapacks and HUDs can show area difficulty.
     *
     * <p>Mirrors {@link #resolve}'s priority chain (structure → biome → dimension
     * → base) but ignores entity-type filters and uses the deterministic
     * {@link LevelRule#levelAt} (no RNG). Result is clamped to the global cap.
     */
    public int resolveAreaLevel(ServerLevel level, net.minecraft.core.BlockPos pos) {
        for (LevelRule rule : structureRules) {
            if (rule.matchesDimension(level) && rule.matchesStructure(level, pos))
                return clampArea(rule.levelAt(level, pos));
        }
        for (LevelRule rule : biomeRules) {
            if (rule.matchesDimension(level) && rule.matchesBiome(level, pos))
                return clampArea(rule.levelAt(level, pos));
        }
        for (LevelRule rule : dimensionRules) {
            if (rule.matchesDimension(level))
                return clampArea(rule.levelAt(level, pos));
        }
        for (LevelRule rule : baseRules) {
            return clampArea(rule.levelAt(level, pos));
        }
        return 1; // matches resolve()'s fallback level
    }

    private static int clampArea(int lvl) {
        if (lvl <= 0) return 0;
        return Math.min(lvl, MobLevelingConfig.GLOBAL_LEVEL_CAP.get());
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public Optional<BossRule> getBossRuleForEntity(String entityTypeId) {
        return Optional.ofNullable(bossByEntity.get(entityTypeId));
    }

    public List<BossRule>  getBossRules()      { return bossRules; }
    public List<LevelRule> getBaseRules()      { return baseRules; }
    public List<LevelRule> getBiomeRules()     { return biomeRules; }
    public List<LevelRule> getDimensionRules() { return dimensionRules; }
    public List<LevelRule> getStructureRules() { return structureRules; }
}
