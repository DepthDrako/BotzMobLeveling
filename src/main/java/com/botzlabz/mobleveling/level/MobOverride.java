package com.botzlabz.mobleveling.level;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A per-entity tweak attached to a {@link LevelRule} via its {@code mob_overrides}
 * object. Lets a single rule apply different leveling to specific entity types
 * without authoring a whole new rule file.
 *
 * <p>JSON shape (under {@code "mob_overrides": { "<entity id or path>": { ... } }}):
 * <pre>
 * {
 *   "level_bonus": 50,            // added on top of the rule's resolved base level
 *   "level": 120,                 // OPTIONAL fixed level — overrides the rule's base entirely
 *   "ignore_level_cap": true,     // this entity ignores the global level cap
 *   "attribute_scaling": { ... }  // per-entity attribute ops; override same-attr rule ops
 * }
 * </pre>
 *
 * @param levelBonus       flat level added on top of the rule's clamped base level
 * @param fixedLevel       if non-null, replaces the rule's base level outright (before bonus is ignored)
 * @param ignoreLevelCap   if true, the matched entity is exempt from the global level cap
 * @param attributeScaling per-entity {@link AttrOp}s; merged over the rule's ops (this wins per attribute)
 */
public record MobOverride(int levelBonus,
                          @Nullable Integer fixedLevel,
                          boolean ignoreLevelCap,
                          Map<String, AttrOp> attributeScaling) {

    public static MobOverride from(JsonObject json) {
        int levelBonus = json.has("level_bonus") ? json.get("level_bonus").getAsInt() : 0;

        Integer fixedLevel = null;
        if (json.has("level")) {
            try { fixedLevel = json.get("level").getAsInt(); } catch (Exception ignored) {}
        }

        boolean ignoreLevelCap = json.has("ignore_level_cap")
                && json.get("ignore_level_cap").getAsBoolean();

        Map<String, AttrOp> scaling = new HashMap<>();
        if (json.has("attribute_scaling") && json.get("attribute_scaling").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("attribute_scaling").entrySet()) {
                AttrOp op = AttrOp.fromJson(e.getValue());
                if (op != null) scaling.put(e.getKey(), op);
            }
        }

        return new MobOverride(levelBonus, fixedLevel, ignoreLevelCap, scaling);
    }
}
