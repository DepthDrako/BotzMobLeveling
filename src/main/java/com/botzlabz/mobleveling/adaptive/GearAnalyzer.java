package com.botzlabz.mobleveling.adaptive;

import com.botzlabz.mobleveling.config.MobLevelingConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Analyzes player gear and calculates a gear score based on equipment quality,
 * enchantments, and modded attributes (Iron's Spells, Apotheosis, etc.)
 */
public class GearAnalyzer {

    // Attribute weights for gear scoring
    private static final Map<ResourceLocation, Double> ATTRIBUTE_WEIGHTS = new HashMap<>();

    static {
        // Vanilla attributes
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("minecraft:generic.armor"), 2.0);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("minecraft:generic.armor_toughness"), 1.5);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("minecraft:generic.attack_damage"), 1.0);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("minecraft:generic.attack_speed"), 0.8);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("minecraft:generic.max_health"), 0.5);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("minecraft:generic.movement_speed"), 0.3);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("minecraft:generic.knockback_resistance"), 0.5);

        // Iron's Spells attributes
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("irons_spellbooks:max_mana"), 1.0);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("irons_spellbooks:mana_regen"), 0.8);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("irons_spellbooks:spell_power"), 1.2);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("irons_spellbooks:spell_resist"), 1.0);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("irons_spellbooks:cooldown_reduction"), 1.0);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("irons_spellbooks:casting_time_reduction"), 0.8);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("irons_spellbooks:spell_level_bonus"), 1.5);

        // Apotheosis attributes
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("apotheosis:current_hp_bonus"), 1.0);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("apotheosis:overheal"), 0.8);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("apotheosis:life_steal"), 1.0);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("apotheosis:crit_chance"), 1.2);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("apotheosis:crit_damage"), 1.0);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("apotheosis:armor_shred"), 1.0);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("apotheosis:prot_pierce"), 1.0);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("apotheosis:prot_shred"), 1.0);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("apotheosis:draw_speed"), 0.6);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("apotheosis:arrow_damage"), 0.8);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("apotheosis:arrow_velocity"), 0.6);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("apotheosis:experience_gained"), 0.3);

        // Ars Nouveau attributes
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("ars_nouveau:ars_nouveau.perk.max_mana"), 1.0);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("ars_nouveau:ars_nouveau.perk.mana_regen"), 0.8);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("ars_nouveau:ars_nouveau.perk.spell_damage"), 1.2);

        // Botania attributes
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("botania:pixie_spawn_chance"), 0.8);

        // Mahou Tsukai attributes
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("mahou_tsukai:mahou"), 1.0);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("mahou_tsukai:mahou_regen"), 0.8);

        // Blood Magic attributes
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("bloodmagic:gravity"), 0.5);
        ATTRIBUTE_WEIGHTS.put(new ResourceLocation("bloodmagic:step_height"), 0.3);
    }

    /**
     * Calculates the total gear score for a player
     */
    public static double calculateGearScore(Player player) {
        double totalScore = 0.0;
        
        if (MobLevelingConfig.ADAPTIVE_DEBUG_LOGGING.get()) {
            System.out.println("[MobLeveling] Analyzing gear for player: " + player.getName().getString());
        }

        // Analyze armor slots
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack item = player.getItemBySlot(slot);
            if (!item.isEmpty()) {
                totalScore += analyzeArmorPiece(item);
            }
        }

        // Analyze main hand weapon/tool
        ItemStack mainHand = player.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!mainHand.isEmpty()) {
            totalScore += analyzeWeapon(mainHand);
        }

        // Analyze offhand item
        ItemStack offHand = player.getItemBySlot(EquipmentSlot.OFFHAND);
        if (!offHand.isEmpty()) {
            totalScore += analyzeOffhand(offHand);
        }

        // Add player attribute bonuses from gear
        totalScore += analyzePlayerAttributes(player);

        // Apply multiplier from config
        totalScore *= MobLevelingConfig.ADAPTIVE_GEAR_SCORE_MULTIPLIER.get();

        if (MobLevelingConfig.ADAPTIVE_DEBUG_LOGGING.get()) {
            System.out.println("[MobLeveling] Gear score for " + player.getName().getString() + ": " + totalScore);
        }

        return totalScore;
    }

    /**
     * Analyzes an armor piece and returns its score contribution
     */
    private static double analyzeArmorPiece(ItemStack stack) {
        double score = 0.0;

        // Base armor value
        if (stack.getItem() instanceof ArmorItem armorItem) {
            score += armorItem.getDefense() * 2.0;
            score += armorItem.getToughness() * 1.5;
        }

        // Material tier bonus
        score += getMaterialTierBonus(stack) * 5.0;

        // Enchantments
        score += analyzeEnchantments(stack);

        // Durability percentage (slight bonus for well-maintained gear)
        double durabilityPercent = (double) stack.getDamageValue() / stack.getMaxDamage();
        score *= (1.0 + (1.0 - durabilityPercent) * 0.1);

        return score;
    }

    /**
     * Analyzes a weapon and returns its score contribution
     */
    private static double analyzeWeapon(ItemStack stack) {
        double score = 0.0;

        // Weapon tier
        if (stack.getItem() instanceof TieredItem tieredItem) {
            Tier tier = tieredItem.getTier();
            score += tier.getAttackDamageBonus() * 2.0;
            score += tier.getEnchantmentValue() * 0.5;
        }

        // Material tier bonus
        score += getMaterialTierBonus(stack) * 8.0;

        // Enchantments (weapons get higher weight)
        score += analyzeEnchantments(stack) * 1.5;

        return score;
    }

    /**
     * Analyzes offhand item and returns its score contribution
     */
    private static double analyzeOffhand(ItemStack stack) {
        double score = 0.0;

        // Shield or totem
        if (stack.getItem() instanceof net.minecraft.world.item.ShieldItem) {
            score += 15.0;
        } else if (stack.is(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)) {
            score += 25.0;
        } else {
            // Other offhand items
            score += getMaterialTierBonus(stack) * 3.0;
            score += analyzeEnchantments(stack) * 0.5;
        }

        return score;
    }

    /**
     * Analyzes player attributes that come from gear
     */
    private static double analyzePlayerAttributes(Player player) {
        double score = 0.0;

        // Check vanilla attributes
        score += getAttributeScore(player, new ResourceLocation("minecraft:generic.armor"));
        score += getAttributeScore(player, new ResourceLocation("minecraft:generic.armor_toughness"));
        score += getAttributeScore(player, new ResourceLocation("minecraft:generic.attack_damage"));
        score += getAttributeScore(player, new ResourceLocation("minecraft:generic.max_health"));

        // Check modded attributes if compatibility mode is enabled
        if (MobLevelingConfig.ADAPTIVE_COMPATIBILITY_MODE.get()) {
            for (ResourceLocation attrId : ATTRIBUTE_WEIGHTS.keySet()) {
                if (!attrId.getNamespace().equals("minecraft")) {
                    score += getAttributeScore(player, attrId);
                }
            }
        }

        return score;
    }

    /**
     * Gets the score contribution from a specific attribute
     */
    private static double getAttributeScore(Player player, ResourceLocation attributeId) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        if (attribute == null) return 0.0;

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return 0.0;

        double baseValue = instance.getBaseValue();
        double currentValue = instance.getValue();
        double bonus = currentValue - baseValue;

        // Only count bonuses from gear (positive bonuses)
        if (bonus <= 0) return 0.0;

        double weight = ATTRIBUTE_WEIGHTS.getOrDefault(attributeId, 0.5);

        // Sum up all modifiers to determine how much is from gear
        double gearBonus = 0.0;
        for (AttributeModifier modifier : instance.getModifiers()) {
            // Skip modifiers from effects, only count equipment
            if (isEquipmentModifier(modifier)) {
                double value = modifier.getAmount();
                switch (modifier.getOperation()) {
                    case ADDITION -> gearBonus += value;
                    case MULTIPLY_BASE -> gearBonus += baseValue * value;
                    case MULTIPLY_TOTAL -> gearBonus += currentValue * value;
                }
            }
        }

        return Math.max(0, gearBonus) * weight;
    }

    /**
     * Checks if an attribute modifier comes from equipment
     */
    private static boolean isEquipmentModifier(AttributeModifier modifier) {
        // Equipment modifiers typically use specific UUIDs
        UUID id = modifier.getId();

        // Armor modifier UUIDs
        if (id.equals(UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B")) || // Armor
            id.equals(UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D")) || // Armor toughness
            id.equals(UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3"))) { // Attack damage
            return true;
        }

        // Check by name - equipment modifiers often have specific names
        String name = modifier.getName().toLowerCase();
        return name.contains("armor") ||
               name.contains("weapon") ||
               name.contains("tool") ||
               name.contains("equipment") ||
               name.contains("item") ||
               name.contains("modifier");
    }

    /**
     * Gets a material tier bonus score
     */
    private static int getMaterialTierBonus(ItemStack stack) {
        String itemName = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();

        // Netherite tier
        if (itemName.contains("netherite")) return 4;
        // Diamond tier
        if (itemName.contains("diamond")) return 3;
        // Iron tier
        if (itemName.contains("iron")) return 2;
        // Chain/Stone tier
        if (itemName.contains("chain") || itemName.contains("stone")) return 1;
        // Leather/Wood tier
        return 0;
    }

    /**
     * Analyzes enchantments on an item
     */
    private static double analyzeEnchantments(ItemStack stack) {
        double score = 0.0;

        var enchantments = stack.getAllEnchantments();
        for (var entry : enchantments.entrySet()) {
            int level = entry.getValue();
            ResourceLocation enchantId = ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey());

            if (enchantId == null) continue;

            // Protection enchantments
            if (enchantId.toString().contains("protection")) {
                score += level * 2.0;
            }
            // Damage enchantments
            else if (enchantId.toString().contains("damage") || enchantId.toString().contains("sharpness") ||
                     enchantId.toString().contains("smite") || enchantId.toString().contains("bane")) {
                score += level * 2.5;
            }
            // Utility enchantments
            else if (enchantId.toString().contains("mending") || enchantId.toString().contains("unbreaking")) {
                score += level * 1.5;
            }
            // Other enchantments
            else {
                score += level * 1.0;
            }
        }

        return score;
    }

    /**
     * Gets the maximum gear score among nearby players
     */
    public static double getMaxNearbyGearScore(Player player) {
        double maxScore = calculateGearScore(player);
        int radius = MobLevelingConfig.ADAPTIVE_PLAYER_SEARCH_RADIUS.get();

        for (Player nearbyPlayer : player.level().getEntitiesOfClass(
                Player.class,
                player.getBoundingBox().inflate(radius))) {
            if (nearbyPlayer != player && !nearbyPlayer.isSpectator()) {
                double score = calculateGearScore(nearbyPlayer);
                maxScore = Math.max(maxScore, score);
            }
        }

        return maxScore;
    }
}
