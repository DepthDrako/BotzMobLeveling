package com.botzlabz.mobleveling.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class MobLevelingConfig {

    public static final ForgeConfigSpec SPEC;

    // General settings
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue SHOW_LEVEL_IN_NAME;
    public static final ForgeConfigSpec.BooleanValue DEBUG_MODE;

    // Rule toggles
    public static final ForgeConfigSpec.BooleanValue STRUCTURE_LEVELING_ENABLED;
    public static final ForgeConfigSpec.BooleanValue BIOME_LEVELING_ENABLED;
    public static final ForgeConfigSpec.BooleanValue DIMENSION_LEVELING_ENABLED;
    public static final ForgeConfigSpec.BooleanValue DISTANCE_LEVELING_ENABLED;

    // Mob filtering
    public static final ForgeConfigSpec.BooleanValue APPLY_TO_PASSIVE_MOBS;
    public static final ForgeConfigSpec.BooleanValue APPLY_TO_BOSS_MOBS;
    public static final ForgeConfigSpec.BooleanValue STRUCTURE_OVERRIDES_PASSIVE_FILTER;
    public static final ForgeConfigSpec.BooleanValue STRUCTURE_OVERRIDES_BOSS_FILTER;
    public static final ForgeConfigSpec.BooleanValue LEVELED_PASSIVES_CAN_ATTACK;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_BLACKLIST;

    // Default level ranges
    public static final ForgeConfigSpec.IntValue DEFAULT_MIN_LEVEL;
    public static final ForgeConfigSpec.IntValue DEFAULT_MAX_LEVEL;
    public static final ForgeConfigSpec.IntValue GLOBAL_LEVEL_CAP;

    // Distance scaling
    public static final ForgeConfigSpec.IntValue DISTANCE_START_RADIUS;
    public static final ForgeConfigSpec.IntValue DISTANCE_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue DISTANCE_LEVEL_MULTIPLIER;

    // Display settings
    public static final ForgeConfigSpec.ConfigValue<String> LEVEL_FORMAT;
    public static final ForgeConfigSpec.ConfigValue<String> LEVEL_COLOR;
    public static final ForgeConfigSpec.BooleanValue ALWAYS_SHOW_NAME;

    // Attribute whitelist
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_ATTRIBUTES;

    // Boss Module
    public static final ForgeConfigSpec.BooleanValue BOSS_MODULE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue BOSS_SHOW_BOSS_BAR;
    public static final ForgeConfigSpec.IntValue BOSS_BAR_RENDER_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue BOSS_PREVENT_DESPAWN;
    public static final ForgeConfigSpec.BooleanValue BOSS_GLOW_EFFECT;
    public static final ForgeConfigSpec.BooleanValue BOSS_SPAWN_ANNOUNCEMENT;
    public static final ForgeConfigSpec.IntValue BOSS_ANNOUNCEMENT_RADIUS;

    // Guide Books
    public static final ForgeConfigSpec.BooleanValue GIVE_PLAYER_GUIDE_ON_FIRST_JOIN;
    public static final ForgeConfigSpec.BooleanValue GIVE_DEVELOPER_GUIDE_ON_FIRST_JOIN;

    // Adaptive Difficulty
    public static final ForgeConfigSpec.BooleanValue ADAPTIVE_DIFFICULTY_ENABLED;
    public static final ForgeConfigSpec.IntValue ADAPTIVE_PLAYER_SEARCH_RADIUS;
    public static final ForgeConfigSpec.DoubleValue ADAPTIVE_GEAR_SCORE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue ADAPTIVE_MAX_LEVEL_BONUS;
    public static final ForgeConfigSpec.DoubleValue ADAPTIVE_ATTRIBUTE_SCALING;
    public static final ForgeConfigSpec.DoubleValue ADAPTIVE_EQUIPMENT_CHANCE;
    public static final ForgeConfigSpec.IntValue ADAPTIVE_MAX_EQUIPMENT_TIER;
    public static final ForgeConfigSpec.BooleanValue ADAPTIVE_COMPATIBILITY_MODE;
    public static final ForgeConfigSpec.BooleanValue ADAPTIVE_DEBUG_LOGGING;

    // Epic Fight Integration
    public static final ForgeConfigSpec.BooleanValue EPICFIGHT_INTEGRATION_ENABLED;
    public static final ForgeConfigSpec.DoubleValue EPICFIGHT_WEAPON_CHANCE;
    public static final ForgeConfigSpec.BooleanValue EPICFIGHT_PREFER_EXOTIC_AT_HIGH_TIER;

    // Kill Leveling
    public static final ForgeConfigSpec.BooleanValue KILL_LEVELING_ENABLED;
    public static final ForgeConfigSpec.BooleanValue HUNT_TO_LEVEL_ENABLED;
    public static final ForgeConfigSpec.DoubleValue HUNT_TO_LEVEL_CHANCE;
    public static final ForgeConfigSpec.BooleanValue KILL_APPLY_TO_ANY_MOB;
    public static final ForgeConfigSpec.IntValue KILL_XP_BASE;
    public static final ForgeConfigSpec.IntValue KILL_XP_PER_VICTIM_LEVEL;
    public static final ForgeConfigSpec.IntValue KILL_XP_PLAYER_BONUS;
    public static final ForgeConfigSpec.IntValue KILL_BASE_XP_REQUIRED;
    public static final ForgeConfigSpec.DoubleValue KILL_XP_SCALING;
    public static final ForgeConfigSpec.IntValue KILL_MAX_LEVEL;
    public static final ForgeConfigSpec.BooleanValue KILL_MAKE_PERSISTENT;
    public static final ForgeConfigSpec.BooleanValue KILL_SHOW_INDICATOR;
    public static final ForgeConfigSpec.ConfigValue<String> KILL_INDICATOR_FORMAT;
    public static final ForgeConfigSpec.ConfigValue<String> KILL_INDICATOR_COLOR;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        // General Section
        builder.comment("General Settings").push("general");

        ENABLED = builder
                .comment("Enable or disable the entire mob leveling system")
                .define("enabled", true);

        SHOW_LEVEL_IN_NAME = builder
                .comment("Display mob level in their name tag")
                .define("showLevelInName", true);

        DEBUG_MODE = builder
                .comment("Enable debug logging for troubleshooting")
                .define("debugMode", false);

        builder.pop();

        // Rule Toggles Section
        builder.comment("Rule Type Toggles - Enable/disable specific rule types").push("ruleToggles");

        STRUCTURE_LEVELING_ENABLED = builder
                .comment("Enable structure-based leveling rules (highest priority)")
                .define("structureLevelingEnabled", true);

        BIOME_LEVELING_ENABLED = builder
                .comment("Enable biome-based leveling rules (medium priority)")
                .define("biomeLevelingEnabled", true);

        DIMENSION_LEVELING_ENABLED = builder
                .comment("Enable dimension-based leveling rules (below biome, above base rules)")
                .define("dimensionLevelingEnabled", true);

        DISTANCE_LEVELING_ENABLED = builder
                .comment("Enable distance-from-spawn leveling (used in base rules)")
                .define("distanceLevelingEnabled", true);

        builder.pop();

        // Mob Filtering Section
        builder.comment("Mob Filtering - Control which mobs receive levels").push("filtering");

        APPLY_TO_PASSIVE_MOBS = builder
                .comment("Apply leveling to passive mobs (animals, villagers, etc)")
                .define("applyToPassiveMobs", false);

        APPLY_TO_BOSS_MOBS = builder
                .comment("Apply leveling to boss mobs (Wither, Ender Dragon, etc)")
                .define("applyToBossMobs", false);

        STRUCTURE_OVERRIDES_PASSIVE_FILTER = builder
                .comment("Allow structure rules to level passive mobs even when applyToPassiveMobs is false.",
                         "Example: A chicken in a stronghold can still get leveled if a stronghold rule exists.")
                .define("structureOverridesPassiveFilter", true);

        STRUCTURE_OVERRIDES_BOSS_FILTER = builder
                .comment("Allow structure rules to level boss mobs even when applyToBossMobs is false.",
                         "Example: The Ender Dragon can be leveled by an end_city structure rule.")
                .define("structureOverridesBossFilter", true);

        LEVELED_PASSIVES_CAN_ATTACK = builder
                .comment("When enabled, leveled passive mobs can fight back when attacked.",
                         "This can be overridden per-mob in datapack rules with 'can_attack' field.")
                .define("leveledPassivesCanAttack", true);

        MOB_BLACKLIST = builder
                .comment("List of mob IDs that will never receive levels (e.g., 'minecraft:armor_stand')")
                .defineListAllowEmpty(
                        List.of("mobBlacklist"),
                        () -> List.of("minecraft:armor_stand", "minecraft:marker", "minecraft:item_frame", "minecraft:glow_item_frame", "minecraft:painting"),
                        obj -> obj instanceof String
                );

        builder.pop();

        // Level Ranges Section
        builder.comment("Default Level Ranges - Used when no specific rule applies").push("levels");

        DEFAULT_MIN_LEVEL = builder
                .comment("Default minimum level for mobs without specific rules")
                .defineInRange("defaultMinLevel", 1, 1, 10000);

        DEFAULT_MAX_LEVEL = builder
                .comment("Default maximum level for mobs without specific rules")
                .defineInRange("defaultMaxLevel", 100, 1, 10000);

        GLOBAL_LEVEL_CAP = builder
                .comment("Absolute maximum level any mob can reach, regardless of rules")
                .defineInRange("globalLevelCap", 100, 1, 10000);

        builder.pop();

        // Distance Scaling Section
        builder.comment("Distance-based Scaling - Controls how levels increase with distance from spawn").push("distance");

        DISTANCE_START_RADIUS = builder
                .comment("Distance from spawn (in blocks) where level scaling begins. Mobs within this radius use minimum level.")
                .defineInRange("distanceStartRadius", 100, 0, 100000);

        DISTANCE_PER_LEVEL = builder
                .comment("Number of blocks of distance required per additional level")
                .defineInRange("distancePerLevel", 50, 1, 10000);

        DISTANCE_LEVEL_MULTIPLIER = builder
                .comment("Multiplier applied to distance-calculated levels (1.0 = normal, 2.0 = double levels)")
                .defineInRange("distanceLevelMultiplier", 1.0, 0.1, 10.0);

        builder.pop();

        // Display Section
        builder.comment("Display Settings - Controls how mob levels are shown").push("display");

        LEVEL_FORMAT = builder
                .comment("Format for level display. Use {level} as placeholder for the level number.")
                .define("levelFormat", "[Lv.{level}] ");

        LEVEL_COLOR = builder
                .comment("Color for level display. Use color names (gold, red, green) or hex codes (#FF5500)")
                .define("levelColor", "gold");

        ALWAYS_SHOW_NAME = builder
                .comment("Always show mob name with level (true) or only when looking at mob (false)")
                .define("alwaysShowName", false);

        builder.pop();

        // Attributes Section
        builder.comment("Attribute Whitelist - Only these attributes can be modified by the leveling system").push("attributes");

        ALLOWED_ATTRIBUTES = builder
                .comment("List of attribute IDs that the mod is allowed to modify. Supports modded attributes.")
                .defineListAllowEmpty(
                        List.of("allowedAttributes"),
                        () -> List.of(
                                "minecraft:generic.max_health",
                                "minecraft:generic.attack_damage",
                                "minecraft:generic.armor",
                                "minecraft:generic.armor_toughness",
                                "minecraft:generic.knockback_resistance",
                                "minecraft:generic.movement_speed",
                                "minecraft:generic.follow_range",
                                "minecraft:generic.attack_knockback",
                                "minecraft:generic.attack_speed"
                        ),
                        obj -> obj instanceof String
                );

        builder.pop();

        // Boss Module Section
        builder.comment("Boss Module - Transform mobs into powerful bosses with special effects").push("bossModule");

        BOSS_MODULE_ENABLED = builder
                .comment("Enable the boss module. When enabled, mobs can become bosses based on datapack rules.")
                .define("enabled", true);

        BOSS_SHOW_BOSS_BAR = builder
                .comment("Show a boss bar for boss mobs")
                .define("showBossBar", true);

        BOSS_BAR_RENDER_DISTANCE = builder
                .comment("Maximum distance (in blocks) at which boss bars are visible")
                .defineInRange("bossBarRenderDistance", 64, 16, 256);

        BOSS_PREVENT_DESPAWN = builder
                .comment("Prevent boss mobs from despawning naturally")
                .define("preventDespawn", true);

        BOSS_GLOW_EFFECT = builder
                .comment("Apply glowing effect to boss mobs (can be overridden per-boss in datapack)")
                .define("glowEffect", true);

        BOSS_SPAWN_ANNOUNCEMENT = builder
                .comment("Announce boss spawns to nearby players")
                .define("spawnAnnouncement", true);

        BOSS_ANNOUNCEMENT_RADIUS = builder
                .comment("Radius (in blocks) for boss spawn announcements")
                .defineInRange("announcementRadius", 64, 16, 256);

        builder.pop();

        // Guide Books Section
        builder.comment("Guide Books - Configure automatic Patchouli guide distribution").push("guideBooks");

        GIVE_PLAYER_GUIDE_ON_FIRST_JOIN = builder
                .comment("Give the player guide book to each player on first join")
                .define("givePlayerGuideOnFirstJoin", true);

        GIVE_DEVELOPER_GUIDE_ON_FIRST_JOIN = builder
                .comment("Give the developer guide book to each player on first join")
                .define("giveDeveloperGuideOnFirstJoin", false);

        builder.pop();

        // Adaptive Difficulty Section
        builder.comment("Adaptive Difficulty - Scale mob difficulty based on nearby player gear").push("adaptiveDifficulty");

        ADAPTIVE_DIFFICULTY_ENABLED = builder
                .comment("Enable adaptive difficulty system that scales mobs based on nearby player gear",
                         "Works with Iron's Spells, Apotheosis, and other attribute-modifying mods")
                .define("enabled", true);

        ADAPTIVE_PLAYER_SEARCH_RADIUS = builder
                .comment("Radius (in blocks) to search for players when calculating adaptive difficulty",
                         "Only players within this radius affect mob spawning")
                .defineInRange("playerSearchRadius", 64, 8, 256);

        ADAPTIVE_GEAR_SCORE_MULTIPLIER = builder
                .comment("Multiplier for gear score calculation",
                         "Higher values make mobs scale more aggressively with player gear")
                .defineInRange("gearScoreMultiplier", 1.0, 0.1, 5.0);

        ADAPTIVE_MAX_LEVEL_BONUS = builder
                .comment("Maximum additional levels mobs can gain from adaptive difficulty",
                         "This is added on top of the normal level calculation")
                .defineInRange("maxLevelBonus", 50, 0, 200);

        ADAPTIVE_ATTRIBUTE_SCALING = builder
                .comment("How much mob attributes scale with adaptive difficulty",
                         "1.0 = normal scaling, 2.0 = double attribute bonuses")
                .defineInRange("attributeScaling", 1.0, 0.1, 3.0);

        ADAPTIVE_EQUIPMENT_CHANCE = builder
                .comment("Chance (0.0 to 1.0) for mobs to receive equipment based on player gear level",
                         "Higher gear scores increase this chance up to the maximum")
                .defineInRange("equipmentChance", 0.3, 0.0, 1.0);

        ADAPTIVE_MAX_EQUIPMENT_TIER = builder
                .comment("Maximum equipment tier mobs can spawn with (0-4)",
                         "0 = leather/wood, 1 = chain/stone, 2 = iron, 3 = diamond, 4 = netherite")
                .defineInRange("maxEquipmentTier", 3, 0, 4);

        ADAPTIVE_COMPATIBILITY_MODE = builder
                .comment("Compatibility mode for modded attributes",
                         "When true, scans for known modded attributes (irons_spellbooks, apotheosis, etc)",
                         "Works with: Iron's Spells (mana, spell power), Apotheosis (current hp bonus, etc)")
                .define("compatibilityMode", true);

        ADAPTIVE_DEBUG_LOGGING = builder
                .comment("Enable debug logging for adaptive difficulty calculations")
                .define("debugLogging", false);

        builder.pop();

        // Epic Fight Integration Section
        builder.comment("Epic Fight Integration - Dynamic weapon assignment with fighting styles",
                       "Requires Epic Fight mod to be installed").push("epicFight");

        EPICFIGHT_INTEGRATION_ENABLED = builder
                .comment("Enable Epic Fight integration",
                         "When enabled, mobs can spawn with Epic Fight weapons and use their fighting styles")
                .define("enabled", true);

        EPICFIGHT_WEAPON_CHANCE = builder
                .comment("Chance (0.0 to 1.0) for mobs to receive Epic Fight weapons instead of vanilla ones",
                         "Only applies when Epic Fight is installed")
                .defineInRange("epicFightWeaponChance", 0.7, 0.0, 1.0);

        EPICFIGHT_PREFER_EXOTIC_AT_HIGH_TIER = builder
                .comment("At high threat tiers, prefer exotic weapons (greatswords, katanas, spears)",
                         "over basic swords and axes")
                .define("preferExoticAtHighTier", true);

        builder.pop();

        // Kill Leveling Section
        builder.comment("Kill Leveling - Mobs gain XP and levels by killing other mobs and players").push("killLeveling");

        KILL_LEVELING_ENABLED = builder
                .comment("Enable the kill leveling system. Mobs earn XP and levels from kills.")
                .define("enabled", true);

        HUNT_TO_LEVEL_ENABLED = builder
                .comment("Global toggle for the hunt_to_level datapack feature.",
                         "When false, mobs with hunt_to_level: true in their rule will NOT actively hunt other mobs.")
                .define("huntToLevelEnabled", true);

        HUNT_TO_LEVEL_CHANCE = builder
                .comment("Default probability (0.0–1.0) that a mob with hunt_to_level: true actually receives hunting AI.",
                         "Can be overridden per-rule with 'hunt_to_level_chance' in the datapack JSON.",
                         "1.0 = every matching mob hunts, 0.5 = roughly half will hunt.")
                .defineInRange("huntToLevelChance", 1.0, 0.0, 1.0);

        KILL_APPLY_TO_ANY_MOB = builder
                .comment("Allow any mob to gain kill levels, not just mobs already assigned a level by this mod.",
                         "When false, only mobs processed by spawn rules can accumulate kill XP.")
                .define("applyToAnyMob", false);

        KILL_XP_BASE = builder
                .comment("Base XP granted per kill, regardless of the victim's level")
                .defineInRange("xpBase", 50, 1, 100000);

        KILL_XP_PER_VICTIM_LEVEL = builder
                .comment("Bonus XP granted per level of the victim (only applies when victim has a level assigned by this mod)")
                .defineInRange("xpPerVictimLevel", 10, 0, 10000);

        KILL_XP_PLAYER_BONUS = builder
                .comment("Flat bonus XP added when the victim is a player (stacks with xpBase)")
                .defineInRange("xpPlayerBonus", 200, 0, 100000);

        KILL_BASE_XP_REQUIRED = builder
                .comment("XP required to earn the very first kill level")
                .defineInRange("baseXpRequired", 100, 1, 1000000);

        KILL_XP_SCALING = builder
                .comment("XP cost multiplier per kill level. Each level costs this many times more than the previous.",
                         "1.5 means each level costs 50% more (100, 150, 225, 338, ...)")
                .defineInRange("xpScaling", 1.5, 1.0, 10.0);

        KILL_MAX_LEVEL = builder
                .comment("Maximum number of kill levels a mob can earn on top of its base spawn level")
                .defineInRange("maxKillLevel", 50, 1, 10000);

        KILL_MAKE_PERSISTENT = builder
                .comment("Prevent mobs that have earned at least one kill from despawning naturally")
                .define("makePersistent", true);

        KILL_SHOW_INDICATOR = builder
                .comment("Show a kill indicator in the mob's name tag when it has at least one kill")
                .define("showIndicator", true);

        KILL_INDICATOR_FORMAT = builder
                .comment("Text prepended to the mob's name when it has kills. Use {kills} for the kill count.",
                         "Default: a star character followed by a space.")
                .define("indicatorFormat", "\u2605 ");

        KILL_INDICATOR_COLOR = builder
                .comment("Color for the kill indicator. Supports color names (red, dark_red) or hex (#FF0000)")
                .define("indicatorColor", "red");

        builder.pop();

        SPEC = builder.build();
    }
}
