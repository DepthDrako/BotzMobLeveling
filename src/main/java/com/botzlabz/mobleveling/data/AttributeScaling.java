package com.botzlabz.mobleveling.data;

import com.botzlabz.mobleveling.util.ModConstants;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttributeScaling {

    private final ResourceLocation attributeId;
    private final double baseBonus;
    private final double perLevel;
    private final String operation;
    private final Double maxBonus;

    public AttributeScaling(ResourceLocation attributeId, double baseBonus, double perLevel, String operation, Double maxBonus) {
        this.attributeId = attributeId;
        this.baseBonus = baseBonus;
        this.perLevel = perLevel;
        this.operation = operation;
        this.maxBonus = maxBonus;
    }

    public static AttributeScaling fromJson(String attributeIdStr, JsonObject json) {
        ResourceLocation attributeId = new ResourceLocation(attributeIdStr);
        double baseBonus = GsonHelper.getAsDouble(json, "base_bonus", 0.0);
        double perLevel = GsonHelper.getAsDouble(json, "per_level", 0.0);
        String operation = GsonHelper.getAsString(json, "operation", ModConstants.OP_ADDITION);
        Double maxBonus = json.has("max_bonus") ? GsonHelper.getAsDouble(json, "max_bonus") : null;

        return new AttributeScaling(attributeId, baseBonus, perLevel, operation, maxBonus);
    }

    public ResourceLocation getAttributeId() {
        return attributeId;
    }

    public double getBaseBonus() {
        return baseBonus;
    }

    public double getPerLevel() {
        return perLevel;
    }

    public String getOperation() {
        return operation;
    }

    public Double getMaxBonus() {
        return maxBonus;
    }

    public double calculateBonus(int level) {
        double bonus = baseBonus + (perLevel * level);
        if (maxBonus != null) {
            bonus = Math.min(bonus, maxBonus);
        }
        return bonus;
    }

    public AttributeModifier.Operation getModifierOperation() {
        return switch (operation) {
            case ModConstants.OP_MULTIPLY_BASE -> AttributeModifier.Operation.MULTIPLY_BASE;
            case ModConstants.OP_MULTIPLY_TOTAL -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> AttributeModifier.Operation.ADDITION;
        };
    }

    public AttributeScaling withMultiplier(double multiplier) {
        return new AttributeScaling(
                attributeId,
                baseBonus * multiplier,
                perLevel * multiplier,
                operation,
                maxBonus != null ? maxBonus * multiplier : null
        );
    }
}
