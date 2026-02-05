package com.botzlabz.mobleveling.attribute;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.data.AttributeScaling;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class AttributeScalingManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MODIFIER_NAME_PREFIX = BotzMobLeveling.MOD_ID + "_level_";

    // Base UUID used to generate consistent UUIDs for each attribute
    private static final UUID BASE_UUID = UUID.fromString("b0721ab5-0001-4e31-8000-000000000000");

    public void applyScaling(Mob mob, int level, Map<ResourceLocation, AttributeScaling> scalingMap) {
        Set<String> allowedAttributes = new HashSet<>(MobLevelingConfig.ALLOWED_ATTRIBUTES.get());

        for (Map.Entry<ResourceLocation, AttributeScaling> entry : scalingMap.entrySet()) {
            ResourceLocation attrId = entry.getKey();

            // Check whitelist
            if (!allowedAttributes.contains(attrId.toString())) {
                if (MobLevelingConfig.DEBUG_MODE.get()) {
                    LOGGER.debug("Skipping attribute {} - not in whitelist", attrId);
                }
                continue;
            }

            AttributeScaling scaling = entry.getValue();
            applyAttributeModifier(mob, attrId, level, scaling);
        }

        // Heal mob to new max health after modifications
        healToMaxHealth(mob);
    }

    private void applyAttributeModifier(Mob mob, ResourceLocation attrId, int level, AttributeScaling scaling) {
        try {
            // Get attribute from registry (supports modded attributes)
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attrId);

            if (attribute == null) {
                // Try built-in registry as fallback
                attribute = BuiltInRegistries.ATTRIBUTE.get(attrId);
            }

            if (attribute == null) {
                if (MobLevelingConfig.DEBUG_MODE.get()) {
                    LOGGER.warn("Unknown attribute: {} - may be from unloaded mod", attrId);
                }
                return;
            }

            AttributeInstance instance = mob.getAttribute(attribute);
            if (instance == null) {
                // Mob doesn't have this attribute
                return;
            }

            // Calculate bonus
            double bonus = scaling.calculateBonus(level);

            if (bonus == 0.0) {
                return; // No change needed
            }

            // Generate consistent UUID for this attribute
            UUID modifierId = generateModifierUUID(attrId);

            // Check if modifier already exists with same value - skip if identical
            AttributeModifier existing = instance.getModifier(modifierId);
            if (existing != null) {
                // If modifier already exists with same value, skip to avoid issues
                if (Math.abs(existing.getAmount() - bonus) < 0.001) {
                    if (MobLevelingConfig.DEBUG_MODE.get()) {
                        LOGGER.debug("Skipping {} - identical modifier already exists on {}",
                                attrId.getPath(), mob.getType().getDescription().getString());
                    }
                    return;
                }
                // Remove existing modifier safely
                try {
                    instance.removeModifier(modifierId);
                } catch (Exception e) {
                    LOGGER.debug("Failed to remove existing modifier {} - continuing anyway", modifierId);
                }
            }

            // Get operation
            AttributeModifier.Operation operation = scaling.getModifierOperation();

            // Create and apply modifier
            String modifierName = MODIFIER_NAME_PREFIX + attrId.getPath();
            AttributeModifier modifier = new AttributeModifier(
                    modifierId,
                    modifierName,
                    bonus,
                    operation
            );

            // Use transient modifier to avoid save/load issues with permanent modifiers
            // Transient modifiers don't persist to NBT, so they're reapplied each time
            instance.addTransientModifier(modifier);

            if (MobLevelingConfig.DEBUG_MODE.get()) {
                LOGGER.debug("Applied {} modifier to {}: {} {} (level {})",
                        attrId.getPath(), mob.getType().getDescription().getString(),
                        formatBonus(bonus, operation), operation.name(), level);
            }
        } catch (Exception e) {
            // Catch any exception to prevent crashing the game
            LOGGER.warn("Failed to apply attribute {} to mob {}: {}",
                    attrId, mob.getType().getDescription().getString(), e.getMessage());
            if (MobLevelingConfig.DEBUG_MODE.get()) {
                LOGGER.debug("Full stack trace:", e);
            }
        }
    }

    private UUID generateModifierUUID(ResourceLocation attrId) {
        // Generate a consistent UUID based on the attribute ID
        String seed = BASE_UUID.toString() + attrId.toString();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private void healToMaxHealth(Mob mob) {
        try {
            float maxHealth = mob.getMaxHealth();
            if (mob.getHealth() < maxHealth) {
                mob.setHealth(maxHealth);
            }
        } catch (Exception e) {
            // Don't crash if health can't be set
            if (MobLevelingConfig.DEBUG_MODE.get()) {
                LOGGER.debug("Could not heal mob to max health: {}", e.getMessage());
            }
        }
    }

    private String formatBonus(double bonus, AttributeModifier.Operation operation) {
        if (operation == AttributeModifier.Operation.ADDITION) {
            return String.format("%+.2f", bonus);
        } else {
            return String.format("%+.1f%%", bonus * 100);
        }
    }

    public void removeAllModifiers(Mob mob) {
        for (String attrStr : MobLevelingConfig.ALLOWED_ATTRIBUTES.get()) {
            ResourceLocation attrId = new ResourceLocation(attrStr);
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attrId);

            if (attribute == null) {
                attribute = BuiltInRegistries.ATTRIBUTE.get(attrId);
            }

            if (attribute == null) {
                continue;
            }

            AttributeInstance instance = mob.getAttribute(attribute);
            if (instance == null) {
                continue;
            }

            UUID modifierId = generateModifierUUID(attrId);
            if (instance.getModifier(modifierId) != null) {
                instance.removeModifier(modifierId);
            }
        }
    }
}
