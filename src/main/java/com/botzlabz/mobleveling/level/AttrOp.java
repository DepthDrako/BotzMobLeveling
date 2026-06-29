package com.botzlabz.mobleveling.level;

import com.google.gson.JsonElement;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

/**
 * One datapack-driven attribute-scaling operation, parsed from an
 * {@code attribute_scaling} entry on a {@link LevelRule} or {@link MobOverride}.
 *
 * <p>JSON forms (both accepted):
 * <pre>
 * "attribute_scaling": {
 *   "max_health":   2.0,                                          // bare number → add_value
 *   "armor":        { "operation": "add_value",     "value_per_level": 0.5 },
 *   "attack_damage":{ "operation": "multiply_base", "value_per_level": 0.01 }
 * }
 * </pre>
 *
 * <p>The applied modifier value is {@code valuePerLevel × mobLevel}. For
 * {@code multiply_base} that value is a fraction of the attribute's base
 * (0.01/level → +50% base at Lv.50). These stack <b>on top of</b> the config
 * increment stats (additive precedence), they do not replace them.
 *
 * @param operation      the resolved vanilla modifier operation
 * @param valuePerLevel  amount contributed per mob level
 */
public record AttrOp(Operation operation, double valuePerLevel) {

    /**
     * Parses one {@code attribute_scaling} value: a bare number (→ {@code add_value},
     * back-compatible with the old flat map) or an object
     * {@code {"operation": ..., "value_per_level": ...}}. Returns {@code null} when
     * the element can't be parsed.
     */
    public static AttrOp fromJson(JsonElement el) {
        try {
            if (el.isJsonPrimitive()) {
                return new AttrOp(Operation.ADD_VALUE, el.getAsDouble());
            }
            if (el.isJsonObject()) {
                var o = el.getAsJsonObject();
                double vpl = o.has("value_per_level") ? o.get("value_per_level").getAsDouble()
                           : o.has("value")           ? o.get("value").getAsDouble()
                           : 0.0;
                String opStr = o.has("operation") ? o.get("operation").getAsString() : "add_value";
                return new AttrOp(parseOperation(opStr), vpl);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** Maps a user-facing operation string to a vanilla {@link Operation}. */
    public static Operation parseOperation(String s) {
        return switch (s.toLowerCase()) {
            case "multiply_base",  "add_multiplied_base"  -> Operation.ADD_MULTIPLIED_BASE;
            case "multiply_total", "add_multiplied_total" -> Operation.ADD_MULTIPLIED_TOTAL;
            default                                        -> Operation.ADD_VALUE;
        };
    }

    /** Resolves an {@link Operation} from its enum name (NBT round-trip), defaulting to ADD_VALUE. */
    public static Operation operationFromName(String name) {
        try { return Operation.valueOf(name); }
        catch (Exception e) { return Operation.ADD_VALUE; }
    }
}
