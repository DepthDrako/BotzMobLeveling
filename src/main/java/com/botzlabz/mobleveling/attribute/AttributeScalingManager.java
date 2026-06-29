package com.botzlabz.mobleveling.attribute;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.level.AttrOp;
import com.botzlabz.mobleveling.level.MobLevelData;
import com.botzlabz.mobleveling.stats.MobStatBlock;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies and removes level-based attribute modifiers for leveled mobs.
 *
 * <p>Uses {@code addTransientModifier} so modifiers are NOT saved to entity NBT
 * and must be re-applied every time the entity joins the world (handled by
 * {@link com.botzlabz.mobleveling.event.AttributeRestoreHandler}).
 * This avoids stacking issues and gives us full control over persistence.
 *
 * <p>Two layers stack additively:
 * <ul>
 *   <li><b>Config-increment stats</b> — four hardcoded {@link MobStatBlock} stats
 *       (vigor/strength/dexterity/agility) with fixed keys.</li>
 *   <li><b>Datapack ops</b> — arbitrary attributes from a rule's/override's
 *       {@code attribute_scaling} (Phase 3b), each keyed by its attribute id so it
 *       layers on top of the config stats rather than replacing them.</li>
 * </ul>
 *
 * <p>Modifiers use {@link ResourceLocation} keys so they can be cleanly removed
 * by ID before re-application.
 */
public final class AttributeScalingManager {

    private static final Logger LOGGER = LogManager.getLogger(BotzMobLeveling.MOD_ID);

    private static final ResourceLocation KEY_VIGOR       = rl("vigor_bonus");
    private static final ResourceLocation KEY_STRENGTH    = rl("strength_bonus");
    private static final ResourceLocation KEY_DEXTERITY   = rl("dexterity_bonus");
    private static final ResourceLocation KEY_AGILITY     = rl("agility_bonus");
    private static final ResourceLocation KEY_BOSS_HEALTH = rl("boss_health_mul");

    /** Attribute ids we've already warned about being unresolvable, so we log once. */
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(BotzMobLeveling.MOD_ID, path);
    }

    // -------------------------------------------------------------------------
    // Full apply/remove (config stats + datapack ops) — the path callers use
    // -------------------------------------------------------------------------

    public static void apply(LivingEntity entity, MobLevelData data) {
        apply(entity, data.getStatBlock());
        applyDatapackOps(entity, data.getAttributeOps(), data.getTotalLevel());
        applyBossHealth(entity, data);
    }

    public static void remove(LivingEntity entity, MobLevelData data) {
        remove(entity);
        removeDatapackOps(entity, data.getAttributeOps());
        removeModifier(entity, Attributes.MAX_HEALTH, KEY_BOSS_HEALTH);
    }

    /**
     * Boss MAX_HEALTH multiplier, as an {@code ADD_MULTIPLIED_BASE} modifier so it
     * scales the base health and stacks cleanly with the vigor {@code ADD_VALUE}
     * bonus (boss HP = base × mul + vigor). Applied here — not only at spawn — so it
     * is re-applied on reload (transient modifiers aren't saved to entity NBT).
     */
    private static void applyBossHealth(LivingEntity entity, MobLevelData data) {
        if (!data.isBoss()) return;
        double mul = data.getBossHealthMul();
        if (mul <= 1.0) return;
        applyModifier(entity, Attributes.MAX_HEALTH, KEY_BOSS_HEALTH,
                      (float) (mul - 1.0), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    // -------------------------------------------------------------------------
    // Config-increment stats (four fixed attributes)
    // -------------------------------------------------------------------------

    public static void apply(LivingEntity entity, MobStatBlock stats) {
        applyModifier(entity, Attributes.MAX_HEALTH,     KEY_VIGOR,     stats.getValue("vigor"),
                      AttributeModifier.Operation.ADD_VALUE);
        applyModifier(entity, Attributes.ATTACK_DAMAGE,  KEY_STRENGTH,  stats.getValue("strength"),
                      AttributeModifier.Operation.ADD_VALUE);
        applyModifier(entity, Attributes.ATTACK_SPEED,   KEY_DEXTERITY, stats.getValue("dexterity"),
                      AttributeModifier.Operation.ADD_VALUE);
        applyModifier(entity, Attributes.MOVEMENT_SPEED, KEY_AGILITY,   stats.getValue("agility"),
                      AttributeModifier.Operation.ADD_VALUE);
    }

    public static void remove(LivingEntity entity) {
        removeModifier(entity, Attributes.MAX_HEALTH,     KEY_VIGOR);
        removeModifier(entity, Attributes.ATTACK_DAMAGE,  KEY_STRENGTH);
        removeModifier(entity, Attributes.ATTACK_SPEED,   KEY_DEXTERITY);
        removeModifier(entity, Attributes.MOVEMENT_SPEED, KEY_AGILITY);
    }

    // -------------------------------------------------------------------------
    // Datapack attribute ops (Phase 3b)
    // -------------------------------------------------------------------------

    /**
     * Applies each datapack {@link AttrOp} as a distinct transient modifier keyed by
     * the resolved attribute id ({@code mobleveling:datapack/<ns>/<path>}), with value
     * {@code valuePerLevel × level}. Existing same-key modifiers are removed first so
     * re-application (kill level-up / reload) doesn't stack.
     */
    public static void applyDatapackOps(LivingEntity entity, Map<String, AttrOp> ops, int level) {
        if (ops == null || ops.isEmpty()) return;
        for (Map.Entry<String, AttrOp> e : ops.entrySet()) {
            Holder<Attribute> attr = resolveAttribute(e.getKey());
            if (attr == null) {
                if (WARNED.add(e.getKey())) {
                    LOGGER.warn("[BotzMobLeveling] attribute_scaling references unknown attribute '{}' — skipping.", e.getKey());
                }
                continue;
            }
            var instance = entity.getAttribute(attr);
            if (instance == null) continue; // entity doesn't have this attribute (e.g. armor on some mobs)
            ResourceLocation key = datapackKey(attr);
            instance.removeModifier(key);
            double value = e.getValue().valuePerLevel() * level;
            instance.addTransientModifier(new AttributeModifier(key, value, e.getValue().operation()));
        }
    }

    public static void removeDatapackOps(LivingEntity entity, Map<String, AttrOp> ops) {
        if (ops == null || ops.isEmpty()) return;
        for (String id : ops.keySet()) {
            Holder<Attribute> attr = resolveAttribute(id);
            if (attr == null) continue;
            var instance = entity.getAttribute(attr);
            if (instance != null) instance.removeModifier(datapackKey(attr));
        }
    }

    /** Stable per-attribute modifier id, derived from the resolved attribute's registry key. */
    private static ResourceLocation datapackKey(Holder<Attribute> attr) {
        ResourceLocation attrId = BuiltInRegistries.ATTRIBUTE.getKey(attr.value());
        String suffix = attrId != null ? attrId.getNamespace() + "/" + attrId.getPath() : "unknown";
        return rl("datapack/" + suffix);
    }

    /**
     * Resolves an attribute id string to its registry {@link Holder}. Accepts a bare
     * path ("max_health"), a namespaced id, and — since vanilla attributes keep the
     * {@code generic.} prefix on 1.21.1 — retries with that prefix when a prefix-less
     * lookup misses. Returns {@code null} when unresolvable.
     */
    private static Holder<Attribute> resolveAttribute(String raw) {
        ResourceLocation rl = ResourceLocation.tryParse(raw.contains(":") ? raw : "minecraft:" + raw);
        if (rl == null) return null;
        Holder<Attribute> h = lookup(rl);
        if (h == null && !rl.getPath().contains(".")) {
            // friendly retry: "max_health" → "generic.max_health"
            h = lookup(ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), "generic." + rl.getPath()));
        }
        return h;
    }

    private static Holder<Attribute> lookup(ResourceLocation rl) {
        return BuiltInRegistries.ATTRIBUTE
                .getHolder(ResourceKey.create(Registries.ATTRIBUTE, rl))
                .map(h -> (Holder<Attribute>) h)
                .orElse(null);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void applyModifier(LivingEntity entity,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation key, float value, AttributeModifier.Operation op) {
        var instance = entity.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(key);
        instance.addTransientModifier(new AttributeModifier(key, value, op));
    }

    private static void removeModifier(LivingEntity entity,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            ResourceLocation key) {
        var instance = entity.getAttribute(attribute);
        if (instance != null) instance.removeModifier(key);
    }

    private AttributeScalingManager() {}
}
