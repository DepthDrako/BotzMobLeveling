package com.botzlabz.mobleveling.level;

import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * A single leveling rule loaded from a datapack JSON file under
 * {@code data/<ns>/mob_levels/<category>/<name>.json}.
 *
 * <p>Supports three resolution modes:
 * <ul>
 *   <li>{@code FIXED}    – always returns a fixed level</li>
 *   <li>{@code RANDOM}   – returns a random value in [minLevel, maxLevel]</li>
 *   <li>{@code DISTANCE} – scales with distance from the configured origin</li>
 *   <li>{@code SKIP}     – explicitly disables leveling for matched entities</li>
 * </ul>
 */
public class LevelRule {

    public enum Mode { FIXED, RANDOM, DISTANCE, SKIP }

    /** Parsed directly from JSON. */
    public final String  id;
    public final Mode    mode;
    public final int     fixedLevel;
    public final int     minLevel;
    public final int     maxLevel;
    public final double  distanceScale;   // levels per block from origin
    public final int     priority;        // higher = checked first within same category

    // Entity / location filters (empty string = match-all)
    public final String entityTypeFilter;   // e.g. "minecraft:zombie"
    public final String biomeFilter;        // registry path, e.g. "minecraft:desert"
    public final String dimensionFilter;    // registry path, e.g. "minecraft:the_nether"
    public final String structureFilter;    // registry path, e.g. "minecraft:stronghold"

    /** If true, this rule applies only to hostile mobs (MobCategory.MONSTER). */
    public final boolean hostileOnly;
    /** If true, this rule applies only to passive/neutral mobs. */
    public final boolean passiveOnly;

    /**
     * Datapack-driven per-attribute scaling applied on top of the config-increment
     * stats. Key = attribute id (e.g. "max_health" or "minecraft:generic.armor"),
     * value = the {@link AttrOp} (operation + value-per-level). Empty when unused.
     */
    public final Map<String, AttrOp> attributeScaling;

    /**
     * Per-entity overrides keyed by entity id (full {@code "minecraft:zombie"} or
     * bare path {@code "zombie"}). Empty when the rule defines none.
     */
    public final Map<String, MobOverride> mobOverrides;

    /** If true, entities matched by this rule are exempt from the global level cap. */
    public final boolean ignoreLevelCap;

    // -------------------------------------------------------------------------

    public LevelRule(String id, Mode mode, int fixedLevel, int minLevel, int maxLevel,
                     double distanceScale, int priority,
                     String entityTypeFilter, String biomeFilter,
                     String dimensionFilter, String structureFilter,
                     boolean hostileOnly, boolean passiveOnly,
                     Map<String, AttrOp> attributeScaling,
                     Map<String, MobOverride> mobOverrides,
                     boolean ignoreLevelCap) {
        this.id               = id;
        this.mode             = mode;
        this.fixedLevel       = fixedLevel;
        this.minLevel         = minLevel;
        this.maxLevel         = maxLevel;
        this.distanceScale    = distanceScale;
        this.priority         = priority;
        this.entityTypeFilter = entityTypeFilter;
        this.biomeFilter      = biomeFilter;
        this.dimensionFilter  = dimensionFilter;
        this.structureFilter  = structureFilter;
        this.hostileOnly      = hostileOnly;
        this.passiveOnly      = passiveOnly;
        this.attributeScaling = attributeScaling;
        this.mobOverrides     = mobOverrides;
        this.ignoreLevelCap   = ignoreLevelCap;
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static LevelRule from(String id, JsonObject json) {
        Mode   mode     = parseMode(getStr(json, "mode", "random"));
        int    fixed    = getInt(json, "level",     1);
        int    minLvl   = getInt(json, "min_level", 1);
        // DISTANCE growth is open-ended by design: when a distance rule omits
        // max_level it stays unbounded (the global cap is applied later by the
        // spawn handler), instead of collapsing to the FIXED/RANDOM default.
        int    maxDef   = (mode == Mode.DISTANCE) ? Integer.MAX_VALUE : Math.max(fixed, minLvl);
        int    maxLvl   = getInt(json, "max_level", maxDef);
        double dScale   = getDbl(json, "distance_scale", 0.0);
        int    priority = getInt(json, "priority",  0);

        String entity    = getStr(json, "entity",    "");
        String biome     = getStr(json, "biome",     "");
        String dimension = getStr(json, "dimension", "");
        String structure = getStr(json, "structure", "");

        boolean hostileOnly = getBool(json, "hostile_only", false);
        boolean passiveOnly = getBool(json, "passive_only", false);

        Map<String, AttrOp> scaling = new HashMap<>();
        if (json.has("attribute_scaling") && json.get("attribute_scaling").isJsonObject()) {
            JsonObject sc = json.getAsJsonObject("attribute_scaling");
            for (Map.Entry<String, JsonElement> e : sc.entrySet()) {
                AttrOp op = AttrOp.fromJson(e.getValue());
                if (op != null) scaling.put(e.getKey(), op);
            }
        }

        Map<String, MobOverride> overrides = new HashMap<>();
        if (json.has("mob_overrides") && json.get("mob_overrides").isJsonObject()) {
            JsonObject mo = json.getAsJsonObject("mob_overrides");
            for (Map.Entry<String, JsonElement> e : mo.entrySet()) {
                if (e.getValue().isJsonObject()) {
                    overrides.put(e.getKey(), MobOverride.from(e.getValue().getAsJsonObject()));
                }
            }
        }

        boolean ignoreLevelCap = getBool(json, "ignore_level_cap", false);

        return new LevelRule(id, mode, fixed, minLvl, maxLvl, dScale, priority,
                entity, biome, dimension, structure, hostileOnly, passiveOnly, scaling,
                overrides, ignoreLevelCap);
    }

    // -------------------------------------------------------------------------
    // Matching
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link MobOverride} this rule defines for the given mob, matching
     * by full entity id first then bare path (mirrors {@link #matchesEntity}), or
     * {@code null} when the rule has no override for it.
     */
    @org.jetbrains.annotations.Nullable
    public MobOverride overrideFor(Mob mob) {
        if (mobOverrides.isEmpty()) return null;
        ResourceLocation id = mob.getType().builtInRegistryHolder().key().location();
        MobOverride ov = mobOverrides.get(id.toString());
        return ov != null ? ov : mobOverrides.get(id.getPath());
    }

    public boolean matchesEntity(Mob mob) {
        if (!entityTypeFilter.isEmpty()) {
            ResourceLocation mobId = mob.getType().builtInRegistryHolder().key().location();
            if (!mobId.toString().equals(entityTypeFilter) &&
                !mobId.getPath().equals(entityTypeFilter)) return false;
        }
        // Category-based filtering
        boolean isMonster = mob.getType().getCategory() == MobCategory.MONSTER;
        if (passiveOnly && isMonster) return false;
        if (hostileOnly && !isMonster) return false;
        return true;
    }

    public boolean matchesBiome(ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (biomeFilter.isEmpty()) return true;
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> b = level.getBiome(pos);
        return b.unwrapKey()
                .map(k -> k.location().toString().equals(biomeFilter)
                       || k.location().getPath().equals(biomeFilter))
                .orElse(false);
    }

    public boolean matchesDimension(ServerLevel level) {
        if (dimensionFilter.isEmpty()) return true;
        ResourceLocation dim = level.dimension().location();
        return dim.toString().equals(dimensionFilter) || dim.getPath().equals(dimensionFilter);
    }

    public boolean matchesStructure(ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (structureFilter.isEmpty()) return true;
        // Only check if the chunk is loaded to avoid blocking
        if (!level.isLoaded(pos)) return false;
        try {
            ResourceLocation rl = ResourceLocation.tryParse(structureFilter);
            if (rl == null) return false;
            net.minecraft.core.Registry<net.minecraft.world.level.levelgen.structure.Structure> registry =
                level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
            java.util.Optional<net.minecraft.world.level.levelgen.structure.Structure> structure =
                registry.getOptional(rl);
            if (structure.isEmpty()) return false;
            net.minecraft.world.level.levelgen.structure.StructureStart start =
                level.structureManager().getStructureAt(pos, structure.get());
            return start != net.minecraft.world.level.levelgen.structure.StructureStart.INVALID_START;
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Resolution
    // -------------------------------------------------------------------------

    /**
     * Resolves this rule's level output. Returns -1 if the rule should be skipped
     * for this mob, or 0 for SKIP mode (caller should treat as "do not level").
     */
    public int resolve(Mob mob, ServerLevel level) {
        if (mode == Mode.SKIP) return 0; // special: skip leveling
        return switch (mode) {
            case FIXED  -> fixedLevel;
            case RANDOM -> {
                int range = Math.max(1, maxLevel - minLevel + 1);
                yield minLevel + new Random().nextInt(range);
            }
            case DISTANCE -> {
                int ox = MobLevelingConfig.DISTANCE_ORIGIN_X.get();
                int oz = MobLevelingConfig.DISTANCE_ORIGIN_Z.get();
                double mul = MobLevelingConfig.DISTANCE_SCALE_FACTOR.get();
                net.minecraft.core.BlockPos bp = mob.blockPosition();
                double dist = Math.sqrt((bp.getX() - ox) * (double)(bp.getX() - ox)
                                      + (bp.getZ() - oz) * (double)(bp.getZ() - oz));
                // Clamp into the rule's own [minLevel, maxLevel] range so callers
                // receive a base level the override bonus can stack on cleanly.
                // maxLevel is Integer.MAX_VALUE for distance rules without an
                // explicit max_level, leaving them unbounded until the global cap.
                int raw = (int)(dist * distanceScale * mul);
                yield Math.min(Math.max(minLevel, raw), maxLevel);
            }
            default -> fixedLevel;
        };
    }

    /**
     * Deterministic level for area queries (no RNG) at an arbitrary position —
     * used by {@code BotzMobLevelingAPI.getAreaLevel} so a HUD can read "how
     * dangerous is this area" with no mob present.
     *
     * <p>FIXED → the fixed level; RANDOM → the midpoint of [min,max] (stable, so
     * a polled HUD doesn't flicker); DISTANCE → the same distance formula as
     * {@link #resolve} but evaluated at {@code pos}; SKIP → 0.
     */
    public int levelAt(ServerLevel level, net.minecraft.core.BlockPos pos) {
        return switch (mode) {
            case SKIP   -> 0;
            case FIXED  -> fixedLevel;
            case RANDOM -> (minLevel + maxLevel) / 2;
            case DISTANCE -> {
                int    ox  = MobLevelingConfig.DISTANCE_ORIGIN_X.get();
                int    oz  = MobLevelingConfig.DISTANCE_ORIGIN_Z.get();
                double mul = MobLevelingConfig.DISTANCE_SCALE_FACTOR.get();
                double dist = Math.sqrt((pos.getX() - ox) * (double)(pos.getX() - ox)
                                      + (pos.getZ() - oz) * (double)(pos.getZ() - oz));
                int raw = (int)(dist * distanceScale * mul);
                yield Math.min(Math.max(minLevel, raw), maxLevel);
            }
        };
    }

    // -------------------------------------------------------------------------
    // JSON helpers
    // -------------------------------------------------------------------------

    private static String  getStr(JsonObject j, String k, String def)  { return j.has(k) ? j.get(k).getAsString()  : def; }
    private static int     getInt(JsonObject j, String k, int def)     { return j.has(k) ? j.get(k).getAsInt()     : def; }
    private static double  getDbl(JsonObject j, String k, double def)  { return j.has(k) ? j.get(k).getAsDouble()  : def; }
    private static boolean getBool(JsonObject j, String k, boolean def){ return j.has(k) ? j.get(k).getAsBoolean() : def; }

    private static Mode parseMode(String s) {
        return switch (s.toLowerCase()) {
            case "fixed"    -> Mode.FIXED;
            case "distance" -> Mode.DISTANCE;
            case "skip"     -> Mode.SKIP;
            default         -> Mode.RANDOM;
        };
    }
}
