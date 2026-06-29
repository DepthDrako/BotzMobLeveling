package com.botzlabz.mobleveling.display;

import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.level.MobLevelData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/**
 * Builds and applies the level name tag.
 *
 * <p>Color tier thresholds are driven by config so pack makers can tune them.
 * The original mob name is preserved in {@link MobLevelData#getOriginalName()}
 * so re-applications always show the vanilla name, not a stacked tag.
 *
 * <p>Format example:
 * <pre>
 *   §6[Lv.42 ★Fire] §fZombie  §7(×3 kills)
 * </pre>
 */
public final class LevelDisplay {

    /**
     * Stores the entity's original name (if not already stored) then applies
     * a freshly built name tag.
     */
    public static void apply(LivingEntity entity, MobLevelData data) {
        if (!MobLevelingConfig.DISPLAY_ENABLED.get()) return;

        // Capture the original name the FIRST time we see this mob.
        if (data.getOriginalName() == null) {
            String raw = entity.hasCustomName()
                ? entity.getCustomName().getString()
                : entity.getType().getDescription().getString();
            data.setOriginalName(raw);
        } else if (entity.hasCustomName()) {
            // A player may have renamed the mob with a name tag since we tagged it.
            // If the current name isn't one of ours, adopt it as the new base name
            // so the next refresh doesn't clobber the player's rename.
            String current = entity.getCustomName().getString();
            if (!current.contains("[Lv.") && !current.equals(data.getOriginalName())) {
                data.setOriginalName(current);
            }
        }

        entity.setCustomName(buildName(data));
        entity.setCustomNameVisible(true);
    }

    // -------------------------------------------------------------------------

    public static Component buildName(MobLevelData data) {
        int    level    = data.getTotalLevel();
        String baseName = data.getOriginalName() != null
            ? data.getOriginalName()
            : "Mob";

        // Determine tier color for the bracket
        ChatFormatting bracketColor = tierColor(level, data.isBoss());

        // Build the bracket label
        StringBuilder bracket = new StringBuilder("[Lv.");
        bracket.append(level);

        // Elemental suffix
        if (MobLevelingConfig.SHOW_ELEMENTAL_TITLE.get()) {
            String dom   = data.getStatBlock().getDominantElement();
            float  val   = data.getStatBlock().getValue(dom);
            if (val >= MobLevelingConfig.ELEMENTAL_TITLE_MIN_VALUE.get().floatValue()) {
                bracket.append(" ★").append(capitalize(dom));
            }
        }

        bracket.append("]");

        // Kill-count suffix
        String killSuffix = "";
        if (MobLevelingConfig.SHOW_KILL_COUNT.get() && data.getKillCount() > 0) {
            killSuffix = " " + ChatFormatting.GRAY + "(×" + data.getKillCount() + " kills)";
        }

        return Component.literal(
            bracketColor + bracket.toString()
            + " " + ChatFormatting.WHITE + baseName
            + killSuffix
        );
    }

    // -------------------------------------------------------------------------
    // Color tier
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link ChatFormatting} colour for a given level.
     *
     * <p>Tiers (driven by config):
     * <ul>
     *   <li>BOSS       → DARK_RED</li>
     *   <li>>epicMax   → RED</li>
     *   <li>>highMax   → GOLD</li>
     *   <li>>midMax    → YELLOW</li>
     *   <li>>lowMax    → GREEN</li>
     *   <li>else       → WHITE</li>
     * </ul>
     */
    private static ChatFormatting tierColor(int level, boolean isBoss) {
        if (isBoss) return ChatFormatting.DARK_RED;
        int low  = MobLevelingConfig.COLOR_TIER_LOW_MAX.get();
        int mid  = MobLevelingConfig.COLOR_TIER_MID_MAX.get();
        int high = MobLevelingConfig.COLOR_TIER_HIGH_MAX.get();
        int epic = MobLevelingConfig.COLOR_TIER_EPIC_MAX.get();
        if (level > epic) return ChatFormatting.RED;
        if (level > high) return ChatFormatting.GOLD;
        if (level > mid)  return ChatFormatting.YELLOW;
        if (level > low)  return ChatFormatting.GREEN;
        return ChatFormatting.WHITE;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private LevelDisplay() {}
}
