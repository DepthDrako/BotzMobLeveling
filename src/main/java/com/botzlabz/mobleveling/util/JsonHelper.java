package com.botzlabz.mobleveling.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public final class JsonHelper {

    private JsonHelper() {}

    public static void validateLevelRange(JsonObject json, ResourceLocation id) throws JsonParseException {
        if (json.has("level_range")) {
            JsonObject range = GsonHelper.getAsJsonObject(json, "level_range");
            int min = GsonHelper.getAsInt(range, "min", 1);
            int max = GsonHelper.getAsInt(range, "max", 100);

            if (min < 1) {
                throw new JsonParseException("Invalid min level " + min + " in " + id + " (must be >= 1)");
            }
            if (max < min) {
                throw new JsonParseException("Invalid level range in " + id + " (max " + max + " < min " + min + ")");
            }
            if (max > 10000) {
                throw new JsonParseException("Invalid max level " + max + " in " + id + " (must be <= 10000)");
            }
        }
    }

    public static void validateAttributeScaling(JsonObject json, ResourceLocation id) throws JsonParseException {
        if (json.has("attribute_scaling")) {
            JsonObject scaling = GsonHelper.getAsJsonObject(json, "attribute_scaling");
            for (var entry : scaling.entrySet()) {
                String attrId = entry.getKey();
                if (!isValidResourceLocation(attrId)) {
                    throw new JsonParseException("Invalid attribute ID '" + attrId + "' in " + id);
                }

                JsonElement value = entry.getValue();
                if (!value.isJsonObject()) {
                    throw new JsonParseException("Attribute scaling for '" + attrId + "' must be an object in " + id);
                }

                JsonObject attrJson = value.getAsJsonObject();
                String operation = GsonHelper.getAsString(attrJson, "operation", ModConstants.OP_ADDITION);
                if (!isValidOperation(operation)) {
                    throw new JsonParseException("Invalid operation '" + operation + "' for attribute '" + attrId + "' in " + id);
                }
            }
        }
    }

    public static boolean isValidResourceLocation(String str) {
        try {
            new ResourceLocation(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidOperation(String operation) {
        return operation.equals(ModConstants.OP_ADDITION) ||
                operation.equals(ModConstants.OP_MULTIPLY_BASE) ||
                operation.equals(ModConstants.OP_MULTIPLY_TOTAL);
    }

    public static boolean isValidLevelMode(String mode) {
        return mode.equals(ModConstants.LEVEL_MODE_FIXED) ||
                mode.equals(ModConstants.LEVEL_MODE_RANDOM) ||
                mode.equals(ModConstants.LEVEL_MODE_DISTANCE);
    }
}
