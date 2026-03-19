package com.botzlabz.mobleveling.util;

import com.botzlabz.mobleveling.BotzMobLeveling;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class ModConstants {

    private ModConstants() {}

    // NBT Keys for persistent data
    public static final String NBT_LEVEL = BotzMobLeveling.MOD_ID + "_Level";
    public static final String NBT_PROCESSED = BotzMobLeveling.MOD_ID + "_Processed";
    public static final String NBT_RULE_ID = BotzMobLeveling.MOD_ID + "_RuleId";
    public static final String NBT_RULE_TYPE = BotzMobLeveling.MOD_ID + "_RuleType";

    // NBT Keys for kill leveling
    public static final String NBT_KILL_XP = BotzMobLeveling.MOD_ID + "_KillXP";
    public static final String NBT_KILL_LEVEL = BotzMobLeveling.MOD_ID + "_KillLevel";
    public static final String NBT_KILL_COUNT = BotzMobLeveling.MOD_ID + "_KillCount";

    // UUID base for attribute modifiers
    public static final UUID MODIFIER_UUID_BASE = UUID.fromString("b0721ab5-0001-4e31-8000-000000000001");

    // Datapack directories
    public static final String DATA_DIRECTORY = "mob_levels";
    public static final String STRUCTURES_PATH = "structures";
    public static final String BIOMES_PATH = "biomes";
    public static final String DIMENSIONS_PATH = "dimensions";
    public static final String BASE_PATH = "base";
    public static final String BOSSES_PATH = "bosses";

    // Rule types
    public static final String RULE_TYPE_STRUCTURE = "structure";
    public static final String RULE_TYPE_BIOME = "biome";
    public static final String RULE_TYPE_DIMENSION = "dimension";
    public static final String RULE_TYPE_BASE = "base";

    // Level modes
    public static final String LEVEL_MODE_FIXED = "fixed";
    public static final String LEVEL_MODE_RANDOM = "random";
    public static final String LEVEL_MODE_DISTANCE = "distance";

    // Attribute operations
    public static final String OP_ADDITION = "addition";
    public static final String OP_MULTIPLY_BASE = "multiply_base";
    public static final String OP_MULTIPLY_TOTAL = "multiply_total";

    // Mob types for base rules
    public static final String MOB_TYPE_HOSTILE = "hostile";
    public static final String MOB_TYPE_PASSIVE = "passive";
    public static final String MOB_TYPE_NEUTRAL = "neutral";
    public static final String MOB_TYPE_BOSS = "boss";
    public static final String MOB_TYPE_ALL = "all";

    public static ResourceLocation modLoc(String path) {
        return new ResourceLocation(BotzMobLeveling.MOD_ID, path);
    }
}
