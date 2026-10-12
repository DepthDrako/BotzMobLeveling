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
    private final double flatPerLevel;
    private final Double flatMaxBonus;

    public AttributeScaling(ResourceLocation attributeId, double baseBonus, double perLevel, String operation, Double maxBonus) {
        this(attributeId, baseBonus, perLevel, operation, maxBonus, 0.0, null);
    }

    public AttributeScaling(ResourceLocation attributeId, double baseBonus, double perLevel, String operation, Double maxBonus,
                            double flatPerLevel, Double flatMaxBonus) {
        this.flatPerLevel = flatPerLevel;
        this.flatMaxBonus = flatMaxBonus;
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
        double flatPerLevel = GsonHelper.getAsDouble(json, "flat_per_level", 0.0);
        Double flatMaxBonus = json.has("flat_max_bonus") ? GsonHelper.getAsDouble(json, "flat_max_bonus") : null;

        return new AttributeScaling(attributeId, baseBonus, perLevel, operation, maxBonus, flatPerLevel, flatMaxBonus);
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

    /**
     * Optional flat amount added as an ADDITION modifier next to the main one. For attack damage this
     * matters because a multiply_* bonus also multiplies a held weapon's damage, so on its own an
     * unarmed mob gains far less from its level than an armed one.
     */
    public double calculateFlatBonus(int level) {
        double bonus = flatPerLevel * level;
        if (flatMaxBonus != null) {
            bonus = Math.min(bonus, flatMaxBonus);
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
                maxBonus != null ? maxBonus * multiplier : null,
                flatPerLevel * multiplier,
                flatMaxBonus != null ? flatMaxBonus * multiplier : null
        );
    }
}
