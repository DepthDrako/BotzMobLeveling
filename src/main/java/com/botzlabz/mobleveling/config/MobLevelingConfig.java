package com.botzlabz.mobleveling.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class MobLevelingConfig {

    public static final ModConfigSpec SPEC;

    // -----------------------------------------------------------------------
    // General
    // -----------------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue DEBUG_MODE;

    // -----------------------------------------------------------------------
    // Leveling
    // -----------------------------------------------------------------------
    public static final ModConfigSpec.IntValue     GLOBAL_LEVEL_CAP;
    public static final ModConfigSpec.BooleanValue LEVEL_PASSIVE_MOBS;
    public static final ModConfigSpec.BooleanValue LEVEL_BOSS_MOBS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MOB_BLACKLIST;

    // Distance scaling (used by DISTANCE mode rules)
    public static final ModConfigSpec.IntValue    DISTANCE_ORIGIN_X;
    public static final ModConfigSpec.IntValue    DISTANCE_ORIGIN_Z;
    public static final ModConfigSpec.DoubleValue DISTANCE_SCALE_FACTOR;

    // -----------------------------------------------------------------------
    // Kill leveling
    // -----------------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue KILL_LEVELING_ENABLED;
    public static final ModConfigSpec.IntValue     KILL_LEVELING_XP_BASE;
    public static final ModConfigSpec.DoubleValue  KILL_LEVELING_XP_SCALE;
    public static final ModConfigSpec.IntValue     KILL_LEVELING_CAP;    // max kill-levels a mob can earn

    // -----------------------------------------------------------------------
    // Adaptive difficulty
    // -----------------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue ADAPTIVE_DIFFICULTY_ENABLED;
    public static final ModConfigSpec.DoubleValue  ADAPTIVE_SCAN_RADIUS;
    public static final ModConfigSpec.IntValue     ADAPTIVE_MAX_BONUS;
    public static final ModConfigSpec.DoubleValue  ADAPTIVE_MIN_GEAR_SCORE;

    // -----------------------------------------------------------------------
    // Loot scaling (opt-in)
    // -----------------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue LOOT_SCALING_ENABLED;
    public static final ModConfigSpec.IntValue     LOOT_BONUS_PER_LEVELS;
    public static final ModConfigSpec.IntValue     LOOT_MAX_BONUS;

    // -----------------------------------------------------------------------
    // Display
    // -----------------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue DISPLAY_ENABLED;
    public static final ModConfigSpec.BooleanValue SHOW_ELEMENTAL_TITLE;
    public static final ModConfigSpec.DoubleValue  ELEMENTAL_TITLE_MIN_VALUE;
    public static final ModConfigSpec.BooleanValue SHOW_KILL_COUNT;
    public static final ModConfigSpec.BooleanValue HIDE_MANA_STATS_IF_NO_ISS;

    // Color thresholds (level boundaries for color tier changes)
    public static final ModConfigSpec.IntValue COLOR_TIER_LOW_MAX;   // 1..LOW  → white
    public static final ModConfigSpec.IntValue COLOR_TIER_MID_MAX;   // LOW+1..MID → green
    public static final ModConfigSpec.IntValue COLOR_TIER_HIGH_MAX;  // MID+1..HIGH → yellow
    public static final ModConfigSpec.IntValue COLOR_TIER_EPIC_MAX;  // HIGH+1..EPIC → gold
    //                                                                // EPIC+1..CAP  → red
    //                                                                // BOSS          → dark_red

    // -----------------------------------------------------------------------
    // Boss module
    // -----------------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue BOSS_ENABLED;
    public static final ModConfigSpec.BooleanValue BOSS_BAR_ENABLED;
    public static final ModConfigSpec.BooleanValue BOSS_ANNOUNCE_SPAWN;
    public static final ModConfigSpec.BooleanValue BOSS_GLOW;
    public static final ModConfigSpec.DoubleValue  BOSS_HEALTH_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue  BOSS_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.IntValue     BOSS_ANNOUNCE_RADIUS;

    // -----------------------------------------------------------------------
    // Hunting AI
    // -----------------------------------------------------------------------
    public static final ModConfigSpec.BooleanValue HUNTING_AI_ENABLED;
    public static final ModConfigSpec.IntValue     HUNTING_AI_MIN_LEVEL;

    // -----------------------------------------------------------------------
    // Stat increments (per level)
    // -----------------------------------------------------------------------
    public static final ModConfigSpec.DoubleValue VIGOR_INCREMENT;
    public static final ModConfigSpec.DoubleValue STRENGTH_INCREMENT;
    public static final ModConfigSpec.DoubleValue ENDURANCE_INCREMENT;
    public static final ModConfigSpec.DoubleValue STAMINA_INCREMENT;
    public static final ModConfigSpec.DoubleValue DEXTERITY_INCREMENT;
    public static final ModConfigSpec.DoubleValue AGILITY_INCREMENT;
    public static final ModConfigSpec.DoubleValue ATTACK_SPEED_INCREMENT;
    public static final ModConfigSpec.DoubleValue MANA_POOL_INCREMENT;
    public static final ModConfigSpec.DoubleValue MANA_DENSITY_INCREMENT;
    public static final ModConfigSpec.DoubleValue ELEMENTAL_INCREMENT;

    // -----------------------------------------------------------------------
    // Caps
    // -----------------------------------------------------------------------
    public static final ModConfigSpec.DoubleValue ENDURANCE_CAP;
    public static final ModConfigSpec.DoubleValue ATTACK_SPEED_CAP;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("general");
        ENABLED    = b.comment("Master toggle — set to false to fully disable the mod's effects.")
                      .define("enabled", true);
        DEBUG_MODE = b.comment("Log spawn/level events to console for debugging rule loading.")
                      .define("debugMode", false);
        b.pop();

        b.push("leveling");
        GLOBAL_LEVEL_CAP   = b.comment("Hard cap on total mob level (base + kill levels).")
                              .defineInRange("globalLevelCap", 100, 1, 10000);
        LEVEL_PASSIVE_MOBS = b.comment("Apply leveling to passive/neutral mobs (animals etc.).")
                              .define("levelPassiveMobs", false);
        LEVEL_BOSS_MOBS    = b.comment("Apply leveling to vanilla boss mobs (Wither, Ender Dragon).")
                              .define("levelBossMobs", false);
        MOB_BLACKLIST      = b.comment("Entity type IDs that should never be leveled, e.g. [\"minecraft:armor_stand\"].")
                              .defineListAllowEmpty("mobBlacklist", List.of(), o -> o instanceof String);

        b.push("distance");
        DISTANCE_ORIGIN_X    = b.comment("World X coordinate used as the origin for distance-based rules.")
                                .defineInRange("originX", 0, -30000000, 30000000);
        DISTANCE_ORIGIN_Z    = b.comment("World Z coordinate used as the origin for distance-based rules.")
                                .defineInRange("originZ", 0, -30000000, 30000000);
        DISTANCE_SCALE_FACTOR = b.comment("Global multiplier applied to the distance-based level formula.")
                                 .defineInRange("scaleFactor", 1.0, 0.001, 100.0);
        b.pop();
        b.pop();

        b.push("killLeveling");
        KILL_LEVELING_ENABLED = b.comment("Allow mobs to gain kill levels by killing other entities.")
                                 .define("enabled", true);
        KILL_LEVELING_XP_BASE = b.comment("XP required for the first kill level.")
                                 .defineInRange("xpBase", 100, 1, 100000);
        KILL_LEVELING_XP_SCALE = b.comment("Exponential multiplier per kill level (threshold = base * scale^killLevel).")
                                  .defineInRange("xpScale", 1.5, 1.0, 10.0);
        KILL_LEVELING_CAP = b.comment("Maximum kill levels a single mob can earn (0 = unlimited up to globalLevelCap).")
                             .defineInRange("killLevelCap", 0, 0, 10000);
        b.pop();

        b.push("adaptiveDifficulty");
        ADAPTIVE_DIFFICULTY_ENABLED = b.comment("Scale mob level based on nearby player gear quality.")
                                       .define("enabled", true);
        ADAPTIVE_SCAN_RADIUS        = b.comment("Radius in blocks to scan for players when computing gear bonus.")
                                       .defineInRange("scanRadius", 48.0, 1.0, 256.0);
        ADAPTIVE_MAX_BONUS          = b.comment("Maximum level bonus that adaptive difficulty can award.")
                                       .defineInRange("maxBonus", 10, 0, 100);
        ADAPTIVE_MIN_GEAR_SCORE     = b.comment("Minimum player gear score before that player contributes to the adaptive bonus.",
                                                "Players below this threshold are ignored, so lightly-geared players don't ramp difficulty.")
                                       .defineInRange("minGearScore", 20.0, 0.0, 100000.0);
        b.pop();

        b.push("lootScaling");
        LOOT_SCALING_ENABLED  = b.comment("Leveled mobs grant extra effective Looting on death, scaling looting-affected drops with their level.",
                                          "Off by default. Grows existing drop stacks (no duplicate item entities).")
                                 .define("enabled", false);
        LOOT_BONUS_PER_LEVELS = b.comment("Grant +1 effective Looting per this many levels (effective level = base + kill levels).",
                                          "Default 10 -> a level-50 mob drops like Looting V.")
                                 .defineInRange("lootingBonusPerLevels", 10, 1, 10000);
        LOOT_MAX_BONUS        = b.comment("Hard cap on the effective Looting bonus a single mob can grant.")
                                 .defineInRange("maxLootingBonus", 10, 0, 1000);
        b.pop();

        b.push("display");
        DISPLAY_ENABLED          = b.comment("Show the level name tag above leveled mobs.")
                                    .define("enabled", true);
        SHOW_ELEMENTAL_TITLE     = b.comment("Append a dominant-element suffix (e.g. ★Fire) to the name tag.")
                                    .define("showElementalTitle", true);
        ELEMENTAL_TITLE_MIN_VALUE = b.comment("Minimum elemental stat value required before showing the suffix.")
                                     .defineInRange("elementalTitleMinValue", 5.0, 0.0, 1000.0);
        SHOW_KILL_COUNT          = b.comment("Append kill count to name tag for mobs that have earned kill levels.")
                                    .define("showKillCount", false);
        HIDE_MANA_STATS_IF_NO_ISS = b.comment("Hide mana stat labels when IronsSpellbooks is not loaded.")
                                     .define("hideManaStatsIfNoISS", true);

        b.push("colorTiers");
        COLOR_TIER_LOW_MAX  = b.comment("Levels 1–N shown in WHITE.")
                               .defineInRange("lowMax", 10, 1, 10000);
        COLOR_TIER_MID_MAX  = b.comment("Levels (lowMax+1)–N shown in GREEN.")
                               .defineInRange("midMax", 25, 1, 10000);
        COLOR_TIER_HIGH_MAX = b.comment("Levels (midMax+1)–N shown in YELLOW.")
                               .defineInRange("highMax", 50, 1, 10000);
        COLOR_TIER_EPIC_MAX = b.comment("Levels (highMax+1)–N shown in GOLD; above this → RED; boss → DARK_RED.")
                               .defineInRange("epicMax", 75, 1, 10000);
        b.pop();
        b.pop();

        b.push("boss");
        BOSS_ENABLED          = b.comment("Enable the boss module (boss rules, boss bars, minion spawning).")
                                 .define("enabled", true);
        BOSS_BAR_ENABLED      = b.comment("Show a boss health bar for boss-rule mobs.")
                                 .define("bossBarEnabled", true);
        BOSS_ANNOUNCE_SPAWN   = b.comment("Broadcast a chat message when a boss spawns.")
                                 .define("announceSpawn", true);
        BOSS_GLOW             = b.comment("Give boss mobs the Glowing effect so they show through walls.")
                                 .define("glow", true);
        BOSS_HEALTH_MULTIPLIER = b.comment("Default health multiplier for boss-rule mobs (rule can override).")
                                  .defineInRange("healthMultiplier", 3.0, 1.0, 100.0);
        BOSS_DAMAGE_MULTIPLIER = b.comment("Default damage multiplier for boss-rule mobs (rule can override).")
                                  .defineInRange("damageMultiplier", 1.5, 1.0, 50.0);
        BOSS_ANNOUNCE_RADIUS  = b.comment("Radius in blocks from the boss at which players receive the spawn announcement.")
                                 .defineInRange("announceRadius", 128, 1, 10000);
        b.pop();

        b.push("huntingAI");
        HUNTING_AI_ENABLED   = b.comment("Inject melee attack / target goals into leveled mobs that lack them.")
                                .define("enabled", true);
        HUNTING_AI_MIN_LEVEL = b.comment("Minimum level before hunting AI is injected.")
                                .defineInRange("minLevel", 5, 1, 10000);
        b.pop();

        b.push("stats");
        VIGOR_INCREMENT        = b.comment("Max health added per level.")
                                  .defineInRange("vigor_increment",        1.0,   0.0, 1000.0);
        STRENGTH_INCREMENT     = b.comment("Attack damage added per level.")
                                  .defineInRange("strength_increment",     0.5,   0.0, 1000.0);
        ENDURANCE_INCREMENT    = b.comment("Damage reduction percent added per level (see caps.endurance_cap).")
                                  .defineInRange("endurance_increment",    0.4,   0.0, 100.0);
        STAMINA_INCREMENT      = b.comment("Stamina stat added per level.")
                                  .defineInRange("stamina_increment",      0.5,   0.0, 1000.0);
        DEXTERITY_INCREMENT    = b.comment("Attack speed added per level.")
                                  .defineInRange("dexterity_increment",    0.02,  0.0, 100.0);
        AGILITY_INCREMENT      = b.comment("Movement speed added per level.")
                                  .defineInRange("agility_increment",      0.002, 0.0, 100.0);
        ATTACK_SPEED_INCREMENT = b.comment("Attack speed multiplier increment per level.")
                                  .defineInRange("attack_speed_increment", 0.025, 0.0, 100.0);
        MANA_POOL_INCREMENT    = b.comment("Mana pool added per level (IronsSpellbooks).")
                                  .defineInRange("mana_pool_increment",    5.0,   0.0, 10000.0);
        MANA_DENSITY_INCREMENT = b.comment("Mana density multiplier increment per level (IronsSpellbooks).")
                                  .defineInRange("mana_density_increment", 0.02,  0.0, 100.0);
        ELEMENTAL_INCREMENT    = b.comment("Each elemental stat added per level.")
                                  .defineInRange("elemental_increment",    0.5,   0.0, 1000.0);
        b.pop();

        b.push("caps");
        ENDURANCE_CAP    = b.comment("Max endurance damage reduction as a percentage (0-100).")
                            .defineInRange("endurance_cap", 75.0, 0.0, 100.0);
        ATTACK_SPEED_CAP = b.comment("Max attack speed multiplier (relative to base weapon speed).")
                            .defineInRange("attack_speed_cap", 3.0, 1.0, 10.0);
        b.pop();

        SPEC = b.build();
    }
}
