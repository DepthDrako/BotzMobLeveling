package com.botzlabz.mobleveling.adaptive;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.integration.EpicFightIntegration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.UUID;

/**
 * Applies mob modifiers based on calculated threat level
 */
public class MobResponseHandler {

    // UUIDs for our attribute modifiers (valid hex UUIDs)
    private static final UUID ADAPTIVE_HEALTH_MODIFIER = UUID.fromString("7B6B4C3A-9E2D-4F1B-8C5A-1D2E3F4A5B6C");
    private static final UUID ADAPTIVE_DAMAGE_MODIFIER = UUID.fromString("8C7D5E4B-AF3E-5A2C-9D6B-2E3F4A5B6C7D");
    private static final UUID ADAPTIVE_SPEED_MODIFIER = UUID.fromString("9D8E6F5C-BA4F-6A3D-AE7C-3F4A5B6C7D8E");
    private static final UUID ADAPTIVE_ARMOR_MODIFIER = UUID.fromString("AE9F7A6D-CA5A-7A4E-BF8D-4A5B6C7D8E9F");

    /**
     * Applies adaptive difficulty modifiers to a mob
     * 
     * @param mob The mob to modify
     * @param gearScore The gear score of nearby players
     * @param random Random source for equipment selection
     */
    public static void applyAdaptiveModifiers(Mob mob, double gearScore, RandomSource random) {
        if (!MobLevelingConfig.ADAPTIVE_DIFFICULTY_ENABLED.get()) {
            return;
        }

        ThreatCalculator.ThreatLevel threat = ThreatCalculator.calculateThreatLevel(gearScore);

        // Apply attribute modifiers
        applyAttributeModifiers(mob, gearScore);

        // Apply equipment
        applyEquipment(mob, gearScore, random);

        if (MobLevelingConfig.ADAPTIVE_DEBUG_LOGGING.get()) {
            System.out.println("[MobLeveling] Applied adaptive modifiers to " + mob.getName().getString() +
                    " - Threat: " + ThreatCalculator.getThreatDescription(threat) +
                    " - Gear Score: " + gearScore);
        }
    }

    /**
     * Applies attribute modifiers to the mob
     */
    private static void applyAttributeModifiers(Mob mob, double gearScore) {
        double multiplier = ThreatCalculator.getAttributeMultiplier(gearScore);

        // Health modifier
        var healthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(ADAPTIVE_HEALTH_MODIFIER);
            double healthBonus = (multiplier - 1.0) * 0.5; // Half the multiplier bonus to health
            if (healthBonus > 0) {
                healthAttr.addPermanentModifier(new AttributeModifier(
                        ADAPTIVE_HEALTH_MODIFIER,
                        "adaptive_difficulty_health",
                        healthBonus,
                        AttributeModifier.Operation.MULTIPLY_BASE
                ));
            }
        }

        // Damage modifier
        var damageAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.removeModifier(ADAPTIVE_DAMAGE_MODIFIER);
            double damageBonus = (multiplier - 1.0) * 0.7; // 70% of multiplier bonus to damage
            if (damageBonus > 0) {
                damageAttr.addPermanentModifier(new AttributeModifier(
                        ADAPTIVE_DAMAGE_MODIFIER,
                        "adaptive_difficulty_damage",
                        damageBonus,
                        AttributeModifier.Operation.MULTIPLY_BASE
                ));
            }
        }

        // Speed modifier (smaller bonus)
        var speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(ADAPTIVE_SPEED_MODIFIER);
            double speedBonus = (multiplier - 1.0) * 0.2; // 20% of multiplier bonus to speed
            if (speedBonus > 0) {
                speedBonus = Math.min(speedBonus, 0.3); // Cap at 30% speed increase
                speedAttr.addPermanentModifier(new AttributeModifier(
                        ADAPTIVE_SPEED_MODIFIER,
                        "adaptive_difficulty_speed",
                        speedBonus,
                        AttributeModifier.Operation.MULTIPLY_BASE
                ));
            }
        }

        // Armor modifier
        var armorAttr = mob.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(ADAPTIVE_ARMOR_MODIFIER);
            double armorBonus = (multiplier - 1.0) * 10; // Flat armor bonus
            if (armorBonus > 0) {
                armorBonus = Math.min(armorBonus, 20); // Cap at 20 armor
                armorAttr.addPermanentModifier(new AttributeModifier(
                        ADAPTIVE_ARMOR_MODIFIER,
                        "adaptive_difficulty_armor",
                        armorBonus,
                        AttributeModifier.Operation.ADDITION
                ));
            }
        }

        // Heal the mob to full health after modifying max health
        if (healthAttr != null) {
            mob.setHealth(mob.getMaxHealth());
        }
    }

    /**
     * Applies equipment to the mob based on gear score
     */
    private static void applyEquipment(Mob mob, double gearScore, RandomSource random) {
        // Check if already has adaptive equipment (persisted via NBT)
        if (hasAdaptiveEquipment(mob)) {
            return;
        }

        double equipmentChance = ThreatCalculator.getEquipmentChance(gearScore);

        if (random.nextDouble() > equipmentChance) {
            // Mark as processed even if no equipment given (so we don't keep trying)
            markAdaptiveEquipmentApplied(mob);
            return; // No equipment this time
        }

        int tier = ThreatCalculator.getEquipmentTier(gearScore);
        boolean equipmentGiven = false;

        // Enable loot pickup for equipment to work properly
        boolean originalPickupState = mob.canPickUpLoot();
        mob.setCanPickUpLoot(true);

        // Give weapon - prefer Epic Fight weapons if available
        if (random.nextBoolean()) {
            ItemStack weapon = getWeaponForTier(tier, random);
            
            // Try to get an Epic Fight weapon first
            if (EpicFightIntegration.isEpicFightLoaded() && MobLevelingConfig.EPICFIGHT_WEAPON_CHANCE.get() > random.nextDouble()) {
                ItemStack epicWeapon = EpicFightIntegration.getEpicFightWeapon(tier, random, null);
                if (!epicWeapon.isEmpty()) {
                    weapon = epicWeapon;
                    if (MobLevelingConfig.ADAPTIVE_DEBUG_LOGGING.get()) {
                        BotzMobLeveling.LOGGER.info("[MobLeveling] Assigned Epic Fight weapon {} to {}",
                                weapon.getItem().getDescriptionId(), mob.getName().getString());
                    }
                }
            }
            
            if (!weapon.isEmpty()) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
                mob.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                equipmentGiven = true;
            }
        }

        // Give armor
        // Head
        if (random.nextBoolean()) {
            ItemStack helmet = getHelmetForTier(tier, random);
            if (!helmet.isEmpty()) {
                mob.setItemSlot(EquipmentSlot.HEAD, helmet);
                mob.setGuaranteedDrop(EquipmentSlot.HEAD);
                equipmentGiven = true;
            }
        }

        // Chest
        if (random.nextBoolean()) {
            ItemStack chestplate = getChestplateForTier(tier, random);
            if (!chestplate.isEmpty()) {
                mob.setItemSlot(EquipmentSlot.CHEST, chestplate);
                mob.setGuaranteedDrop(EquipmentSlot.CHEST);
                equipmentGiven = true;
            }
        }

        // Legs
        if (random.nextBoolean()) {
            ItemStack leggings = getLeggingsForTier(tier, random);
            if (!leggings.isEmpty()) {
                mob.setItemSlot(EquipmentSlot.LEGS, leggings);
                mob.setGuaranteedDrop(EquipmentSlot.LEGS);
                equipmentGiven = true;
            }
        }

        // Feet
        if (random.nextBoolean()) {
            ItemStack boots = getBootsForTier(tier, random);
            if (!boots.isEmpty()) {
                mob.setItemSlot(EquipmentSlot.FEET, boots);
                mob.setGuaranteedDrop(EquipmentSlot.FEET);
                equipmentGiven = true;
            }
        }

        // Mark as having adaptive equipment
        markAdaptiveEquipmentApplied(mob);

        // Restore original pickup state if no equipment was given
        if (!equipmentGiven) {
            mob.setCanPickUpLoot(originalPickupState);
        }

        if (MobLevelingConfig.ADAPTIVE_DEBUG_LOGGING.get() && equipmentGiven) {
            System.out.println("[MobLeveling] Gave tier " + tier + " equipment to " + mob.getName().getString());
        }
    }

    /**
     * Checks if the mob already has adaptive equipment (tracked via NBT)
     */
    private static boolean hasAdaptiveEquipment(Mob mob) {
        CompoundTag persistentData = mob.getPersistentData();
        return persistentData.getBoolean("botzmobleveling.adaptive_equipped");
    }

    /**
     * Marks that adaptive equipment has been applied to this mob
     */
    private static void markAdaptiveEquipmentApplied(Mob mob) {
        mob.getPersistentData().putBoolean("botzmobleveling.adaptive_equipped", true);
    }

    /**
     * Gets a weapon for the given tier
     */
    private static ItemStack getWeaponForTier(int tier, RandomSource random) {
        ItemStack weapon = switch (tier) {
            case 0 -> new ItemStack(Items.WOODEN_SWORD);
            case 1 -> new ItemStack(Items.STONE_SWORD);
            case 2 -> random.nextBoolean() ? new ItemStack(Items.IRON_SWORD) : new ItemStack(Items.IRON_AXE);
            case 3 -> random.nextBoolean() ? new ItemStack(Items.DIAMOND_SWORD) : new ItemStack(Items.DIAMOND_AXE);
            case 4 -> new ItemStack(Items.NETHERITE_SWORD);
            default -> new ItemStack(Items.WOODEN_SWORD);
        };

        // Add enchantments for higher tiers
        if (tier >= 2 && random.nextBoolean()) {
            int enchantLevel = tier - 1;
            EnchantmentHelper.enchantItem(random, weapon, enchantLevel * 5, false);
        }

        return weapon;
    }

    /**
     * Gets a helmet for the given tier
     */
    private static ItemStack getHelmetForTier(int tier, RandomSource random) {
        ItemStack helmet = switch (tier) {
            case 0 -> new ItemStack(Items.LEATHER_HELMET);
            case 1 -> new ItemStack(Items.CHAINMAIL_HELMET);
            case 2 -> new ItemStack(Items.IRON_HELMET);
            case 3 -> new ItemStack(Items.DIAMOND_HELMET);
            case 4 -> new ItemStack(Items.NETHERITE_HELMET);
            default -> new ItemStack(Items.LEATHER_HELMET);
        };

        if (tier >= 2 && random.nextBoolean()) {
            int enchantLevel = tier - 1;
            EnchantmentHelper.enchantItem(random, helmet, enchantLevel * 5, false);
        }

        return helmet;
    }

    /**
     * Gets a chestplate for the given tier
     */
    private static ItemStack getChestplateForTier(int tier, RandomSource random) {
        ItemStack chestplate = switch (tier) {
            case 0 -> new ItemStack(Items.LEATHER_CHESTPLATE);
            case 1 -> new ItemStack(Items.CHAINMAIL_CHESTPLATE);
            case 2 -> new ItemStack(Items.IRON_CHESTPLATE);
            case 3 -> new ItemStack(Items.DIAMOND_CHESTPLATE);
            case 4 -> new ItemStack(Items.NETHERITE_CHESTPLATE);
            default -> new ItemStack(Items.LEATHER_CHESTPLATE);
        };

        if (tier >= 2 && random.nextBoolean()) {
            int enchantLevel = tier - 1;
            EnchantmentHelper.enchantItem(random, chestplate, enchantLevel * 5, false);
        }

        return chestplate;
    }

    /**
     * Gets leggings for the given tier
     */
    private static ItemStack getLeggingsForTier(int tier, RandomSource random) {
        ItemStack leggings = switch (tier) {
            case 0 -> new ItemStack(Items.LEATHER_LEGGINGS);
            case 1 -> new ItemStack(Items.CHAINMAIL_LEGGINGS);
            case 2 -> new ItemStack(Items.IRON_LEGGINGS);
            case 3 -> new ItemStack(Items.DIAMOND_LEGGINGS);
            case 4 -> new ItemStack(Items.NETHERITE_LEGGINGS);
            default -> new ItemStack(Items.LEATHER_LEGGINGS);
        };

        if (tier >= 2 && random.nextBoolean()) {
            int enchantLevel = tier - 1;
            EnchantmentHelper.enchantItem(random, leggings, enchantLevel * 5, false);
        }

        return leggings;
    }

    /**
     * Gets boots for the given tier
     */
    private static ItemStack getBootsForTier(int tier, RandomSource random) {
        ItemStack boots = switch (tier) {
            case 0 -> new ItemStack(Items.LEATHER_BOOTS);
            case 1 -> new ItemStack(Items.CHAINMAIL_BOOTS);
            case 2 -> new ItemStack(Items.IRON_BOOTS);
            case 3 -> new ItemStack(Items.DIAMOND_BOOTS);
            case 4 -> new ItemStack(Items.NETHERITE_BOOTS);
            default -> new ItemStack(Items.LEATHER_BOOTS);
        };

        if (tier >= 2 && random.nextBoolean()) {
            int enchantLevel = tier - 1;
            EnchantmentHelper.enchantItem(random, boots, enchantLevel * 5, false);
        }

        return boots;
    }

    /**
     * Removes adaptive modifiers from a mob (used when config is disabled)
     */
    public static void removeAdaptiveModifiers(Mob mob) {
        var healthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(ADAPTIVE_HEALTH_MODIFIER);
        }

        var damageAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.removeModifier(ADAPTIVE_DAMAGE_MODIFIER);
        }

        var speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(ADAPTIVE_SPEED_MODIFIER);
        }

        var armorAttr = mob.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(ADAPTIVE_ARMOR_MODIFIER);
        }
    }
}
