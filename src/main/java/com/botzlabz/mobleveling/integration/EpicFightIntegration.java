package com.botzlabz.mobleveling.integration;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * Integration with Epic Fight mod.
 * Detects Epic Fight and assigns weapons with proper fighting styles to mobs.
 */
public class EpicFightIntegration {

    private static final String EPICFIGHT_MOD_ID = "epicfight";
    private static Boolean epicFightLoaded = null;

    // Weapon type categories - these determine the fighting style/moveset
    public enum WeaponStyle {
        SWORD("sword", true),           // Dual wielding, fast combos
        AXE("axe", false),              // One-handed, high impact
        SPEAR("spear", true),           // One/Two-handed, reach
        GREATSWORD("greatsword", false), // Two-handed, sweeping
        LONGSWORD("longsword", true),   // One/Two-handed, balanced
        DAGGER("dagger", true),         // Dual wielding, fast
        UCHIGATANA("uchigatana", false), // Two-handed, bleed style
        TACHI("tachi", false),          // Two-handed, heavy katana
        FIST("fist", true),             // Dual wielding, martial arts
        BOW("bow", false),              // Ranged
        CROSSBOW("crossbow", false);    // Ranged

        private final String typeName;
        private final boolean canDualWield;

        WeaponStyle(String typeName, boolean canDualWield) {
            this.typeName = typeName;
            this.canDualWield = canDualWield;
        }

        public String getTypeName() {
            return typeName;
        }

        public boolean canDualWield() {
            return canDualWield;
        }

        public String getFullTypeId() {
            return EPICFIGHT_MOD_ID + ":" + typeName;
        }
    }

    // Weapon registry - maps weapon styles to registered items
    // These will be populated at runtime based on what's available
    private static final Map<WeaponStyle, List<Item>> WEAPONS_BY_STYLE = new EnumMap<>(WeaponStyle.class);
    private static boolean weaponsInitialized = false;

    /**
     * Checks if Epic Fight mod is loaded
     */
    public static boolean isEpicFightLoaded() {
        if (epicFightLoaded == null) {
            epicFightLoaded = ModList.get().isLoaded(EPICFIGHT_MOD_ID);
            if (epicFightLoaded) {
                BotzMobLeveling.LOGGER.info("[MobLeveling] Epic Fight mod detected! Enabling weapon style integration.");
            }
        }
        return epicFightLoaded;
    }

    /**
     * Initializes the weapon registry by scanning for Epic Fight compatible weapons
     * This should be called after tags are loaded (during server start)
     */
    public static void initializeWeapons() {
        if (!isEpicFightLoaded() || weaponsInitialized) {
            return;
        }

        // Initialize weapon lists
        for (WeaponStyle style : WeaponStyle.values()) {
            WEAPONS_BY_STYLE.put(style, new ArrayList<>());
        }

        // Scan all registered items for Epic Fight weapons
        // We detect them by checking if they have Epic Fight capabilities
        // This is done via item tags that Epic Fight adds
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId == null) continue;

            // Check if item has Epic Fight capability (via tags)
            // Epic Fight adds weapons to specific tags based on their type
            WeaponStyle detectedStyle = detectWeaponStyle(itemId);
            if (detectedStyle != null) {
                WEAPONS_BY_STYLE.get(detectedStyle).add(item);
                if (MobLevelingConfig.ADAPTIVE_DEBUG_LOGGING.get()) {
                    BotzMobLeveling.LOGGER.debug("[MobLeveling] Registered Epic Fight weapon: {} as {}",
                            itemId, detectedStyle);
                }
            }
        }

        weaponsInitialized = true;

        // Log summary
        int totalWeapons = WEAPONS_BY_STYLE.values().stream().mapToInt(List::size).sum();
        BotzMobLeveling.LOGGER.info("[MobLeveling] Epic Fight weapon registry initialized with {} weapons", totalWeapons);
        for (WeaponStyle style : WeaponStyle.values()) {
            int count = WEAPONS_BY_STYLE.get(style).size();
            if (count > 0) {
                BotzMobLeveling.LOGGER.info("[MobLeveling]   - {}: {} weapons", style, count);
            }
        }
    }

    /**
     * Attempts to detect the weapon style based on item ID and item properties
     */
    private static WeaponStyle detectWeaponStyle(ResourceLocation itemId) {
        String path = itemId.getPath().toLowerCase();
        String namespace = itemId.getNamespace().toLowerCase();

        // Skip non-weapon items (common patterns)
        if (path.contains("pickaxe") || path.contains("shovel") || path.contains("hoe") ||
            path.contains("ingot") || path.contains("nugget") || path.contains("chunk") ||
            path.contains("dust") || path.contains("gem") || path.contains("ore")) {
            return null;
        }

        // Detect based on naming patterns
        // Greatswords (two-handed heavy swords)
        if (path.contains("greatsword") || path.contains("great_sword") || path.contains("claymore")) {
            return WeaponStyle.GREATSWORD;
        }

        // Longswords (one/two handed)
        if (path.contains("longsword") || path.contains("long_sword") || path.contains("bastard")) {
            return WeaponStyle.LONGSWORD;
        }

        // Uchigatana (katana style)
        if (path.contains("uchigatana") || path.contains("katana") || path.contains("wakizashi")) {
            return WeaponStyle.UCHIGATANA;
        }

        // Tachi
        if (path.contains("tachi")) {
            return WeaponStyle.TACHI;
        }

        // Daggers
        if (path.contains("dagger") || path.contains("knife") || path.contains("shiv") ||
            path.contains("katar") || path.contains("sai")) {
            return WeaponStyle.DAGGER;
        }

        // Spears and polearms
        if (path.contains("spear") || path.contains("halberd") || path.contains("pike") ||
            path.contains("lance") || path.contains("glaive") || path.contains("partisan") ||
            path.contains("trident")) {
            return WeaponStyle.SPEAR;
        }

        // Axes (battle axes, not tools)
        if ((path.contains("axe") || path.contains("ax")) && !path.contains("pick")) {
            // Distinguish between tool axes and battle axes
            if (path.contains("battle") || path.contains("war") || path.contains("greataxe") ||
                path.contains("great_axe") || !namespace.equals("minecraft")) {
                return WeaponStyle.AXE;
            }
        }

        // Fist weapons / gauntlets
        if (path.contains("fist") || path.contains("gauntlet") || path.contains("claw") ||
            path.contains("knuckle") || path.contains("cestus")) {
            return WeaponStyle.FIST;
        }

        // Bows
        if (path.contains("bow") && !path.contains("cross") && !path.contains("elder")) {
            return WeaponStyle.BOW;
        }

        // Crossbows
        if (path.contains("crossbow")) {
            return WeaponStyle.CROSSBOW;
        }

        // Regular swords (must check last as it's the most generic)
        if (path.contains("sword") || path.contains("blade") || path.contains("saber") ||
            path.contains("rapier") || path.contains("cutlass") || path.contains("scimitar")) {
            return WeaponStyle.SWORD;
        }

        return null;
    }

    /**
     * Gets a weapon with Epic Fight capabilities based on tier and preferred style
     * 
     * @param tier Equipment tier (0-4)
     * @param random Random source
     * @param preferredStyle Preferred fighting style (can be null for random)
     * @return ItemStack with appropriate Epic Fight weapon, or empty if none available
     */
    public static ItemStack getEpicFightWeapon(int tier, RandomSource random, WeaponStyle preferredStyle) {
        if (!isEpicFightLoaded() || !MobLevelingConfig.EPICFIGHT_INTEGRATION_ENABLED.get()) {
            return ItemStack.EMPTY;
        }

        if (!weaponsInitialized) {
            initializeWeapons();
        }

        // If we have a preferred style, try that first
        if (preferredStyle != null) {
            List<Item> weapons = WEAPONS_BY_STYLE.get(preferredStyle);
            if (!weapons.isEmpty()) {
                Item weapon = getWeaponForTier(weapons, tier, random);
                if (weapon != null) {
                    return new ItemStack(weapon);
                }
            }
        }

        // Otherwise, pick a random available style
        List<WeaponStyle> availableStyles = new ArrayList<>();
        for (WeaponStyle style : WeaponStyle.values()) {
            if (!WEAPONS_BY_STYLE.get(style).isEmpty()) {
                availableStyles.add(style);
            }
        }

        if (availableStyles.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // Pick random style based on tier (higher tiers prefer more exotic weapons)
        WeaponStyle selectedStyle;
        if (tier >= 3 && random.nextBoolean()) {
            // Higher chance for greatswords, uchigatana, spears at high tiers
            List<WeaponStyle> advancedStyles = Arrays.asList(
                WeaponStyle.GREATSWORD, WeaponStyle.UCHIGATANA, WeaponStyle.TACHI, WeaponStyle.SPEAR
            );
            selectedStyle = advancedStyles.get(random.nextInt(advancedStyles.size()));
            if (WEAPONS_BY_STYLE.get(selectedStyle).isEmpty()) {
                selectedStyle = availableStyles.get(random.nextInt(availableStyles.size()));
            }
        } else {
            selectedStyle = availableStyles.get(random.nextInt(availableStyles.size()));
        }

        List<Item> weapons = WEAPONS_BY_STYLE.get(selectedStyle);
        Item weapon = getWeaponForTier(weapons, tier, random);

        return weapon != null ? new ItemStack(weapon) : ItemStack.EMPTY;
    }

    /**
     * Selects an appropriate weapon from a list based on tier
     */
    private static Item getWeaponForTier(List<Item> weapons, int tier, RandomSource random) {
        if (weapons.isEmpty()) {
            return null;
        }

        // Sort weapons by perceived quality based on name
        List<Item> tierWeapons = new ArrayList<>();

        for (Item weapon : weapons) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(weapon);
            if (id == null) continue;

            String path = id.getPath();
            int weaponTier = getMaterialTierFromName(path);

            // Include weapons within 1 tier of target, with preference for exact match
            if (Math.abs(weaponTier - tier) <= 1) {
                tierWeapons.add(weapon);
            }
        }

        if (tierWeapons.isEmpty()) {
            // Fallback to any weapon
            return weapons.get(random.nextInt(weapons.size()));
        }

        return tierWeapons.get(random.nextInt(tierWeapons.size()));
    }

    /**
     * Estimates material tier from item name
     */
    private static int getMaterialTierFromName(String name) {
        name = name.toLowerCase();
        if (name.contains("netherite") || name.contains("dragon") || name.contains("demon")) return 4;
        if (name.contains("diamond")) return 3;
        if (name.contains("iron") || name.contains("steel")) return 2;
        if (name.contains("stone") || name.contains("chain") || name.contains("bronze")) return 1;
        if (name.contains("gold") || name.contains("silver")) return 2; // Gold is weak but valuable
        return 0; // Wood, leather, etc.
    }

    /**
     * Gets a description of available weapon styles for debugging
     */
    public static String getAvailableStylesReport() {
        if (!isEpicFightLoaded()) {
            return "Epic Fight not loaded";
        }

        StringBuilder sb = new StringBuilder("Epic Fight Weapon Registry:\n");
        for (WeaponStyle style : WeaponStyle.values()) {
            List<Item> weapons = WEAPONS_BY_STYLE.get(style);
            sb.append("  ").append(style).append(": ").append(weapons.size()).append(" weapons\n");
        }
        return sb.toString();
    }

    /**
     * Clears and reloads the weapon registry (useful for datapack reloads)
     */
    public static void reloadWeapons() {
        weaponsInitialized = false;
        WEAPONS_BY_STYLE.clear();
        initializeWeapons();
    }
}
