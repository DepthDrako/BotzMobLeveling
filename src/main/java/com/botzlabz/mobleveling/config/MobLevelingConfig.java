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

        SPEC = builder.build();
    }
}
