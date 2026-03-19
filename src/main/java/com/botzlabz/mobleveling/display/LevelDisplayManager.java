package com.botzlabz.mobleveling.display;

import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.kills.KillLevelData;
import com.botzlabz.mobleveling.level.MobLevelData;
import com.botzlabz.mobleveling.util.ModConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;

public class LevelDisplayManager {

    private static final String ORIGINAL_NAME_KEY = "botzmobleveling_OriginalName";
    private static final String HAS_CUSTOM_NAME_KEY = "botzmobleveling_HadCustomName";

    public void updateDisplay(Mob mob, int level) {
        if (!MobLevelingConfig.SHOW_LEVEL_IN_NAME.get()) {
            return;
        }

        String format = MobLevelingConfig.LEVEL_FORMAT.get();
        String colorStr = MobLevelingConfig.LEVEL_COLOR.get();

        // Build level prefix
        String levelText = format.replace("{level}", String.valueOf(level));

        // Parse color
        TextColor color = parseColor(colorStr);
        Style levelStyle = Style.EMPTY.withColor(color);

        // Get original name (stored or from type)
        Component originalName = getOriginalName(mob);

        // Build full name — prepend kill indicator when the mob has kills
        MutableComponent levelComponent = Component.literal(levelText).withStyle(levelStyle);
        MutableComponent fullName;

        if (MobLevelingConfig.KILL_SHOW_INDICATOR.get() && KillLevelData.hasKills(mob)) {
            String indicatorText = MobLevelingConfig.KILL_INDICATOR_FORMAT.get()
                    .replace("{kills}", String.valueOf(KillLevelData.getKillCount(mob)));
            TextColor indicatorColor = parseColor(MobLevelingConfig.KILL_INDICATOR_COLOR.get());
            MutableComponent indicatorComponent = Component.literal(indicatorText)
                    .withStyle(Style.EMPTY.withColor(indicatorColor));
            fullName = indicatorComponent.append(levelComponent).append(originalName);
        } else {
            fullName = levelComponent.append(originalName);
        }

        // Apply to mob
        mob.setCustomName(fullName);
        mob.setCustomNameVisible(MobLevelingConfig.ALWAYS_SHOW_NAME.get());
    }

    public void clearDisplay(Mob mob) {
        CompoundTag data = mob.getPersistentData();

        // Restore original name if it had one
        if (data.getBoolean(HAS_CUSTOM_NAME_KEY) && data.contains(ORIGINAL_NAME_KEY)) {
            String originalName = data.getString(ORIGINAL_NAME_KEY);
            mob.setCustomName(Component.literal(originalName));
        } else {
            mob.setCustomName(null);
        }
        mob.setCustomNameVisible(false);
    }

    private Component getOriginalName(Mob mob) {
        CompoundTag data = mob.getPersistentData();

        // Check if we already stored the original name
        if (data.contains(ORIGINAL_NAME_KEY)) {
            return Component.literal(data.getString(ORIGINAL_NAME_KEY));
        }

        // First time - store the original name
        Component currentName = mob.getCustomName();

        if (currentName != null && !isOurLevelTag(currentName.getString())) {
            // Mob had a custom name before we touched it - save it
            String originalStr = currentName.getString();
            data.putString(ORIGINAL_NAME_KEY, originalStr);
            data.putBoolean(HAS_CUSTOM_NAME_KEY, true);
            return currentName;
        }

        // No custom name, use type description and store it
        String typeName = mob.getType().getDescription().getString();
        data.putString(ORIGINAL_NAME_KEY, typeName);
        data.putBoolean(HAS_CUSTOM_NAME_KEY, false);
        return mob.getType().getDescription();
    }

    private boolean isOurLevelTag(String name) {
        // Check if the name already contains our level format
        String format = MobLevelingConfig.LEVEL_FORMAT.get();
        String prefix = format.split("\\{level\\}")[0];
        return name.startsWith(prefix) || name.contains("[Lv.");
    }

    @Nullable
    private TextColor parseColor(String color) {
        if (color == null || color.isEmpty()) {
            return TextColor.fromLegacyFormat(ChatFormatting.GOLD);
        }

        // Try hex color first
        if (color.startsWith("#")) {
            try {
                int rgb = Integer.parseInt(color.substring(1), 16);
                return TextColor.fromRgb(rgb);
            } catch (NumberFormatException e) {
                // Fall through to named colors
            }
        }

        // Try named ChatFormatting colors
        ChatFormatting formatting = ChatFormatting.getByName(color.toUpperCase());
        if (formatting != null && formatting.isColor()) {
            return TextColor.fromLegacyFormat(formatting);
        }

        // Try lowercase
        formatting = ChatFormatting.getByName(color.toLowerCase());
        if (formatting != null && formatting.isColor()) {
            return TextColor.fromLegacyFormat(formatting);
        }

        // Common color name mappings
        TextColor mapped = mapColorName(color.toLowerCase());
        if (mapped != null) {
            return mapped;
        }

        // Default to gold
        return TextColor.fromLegacyFormat(ChatFormatting.GOLD);
    }

    @Nullable
    private TextColor mapColorName(String name) {
        return switch (name) {
            case "gold", "orange" -> TextColor.fromLegacyFormat(ChatFormatting.GOLD);
            case "red" -> TextColor.fromLegacyFormat(ChatFormatting.RED);
            case "dark_red", "darkred" -> TextColor.fromLegacyFormat(ChatFormatting.DARK_RED);
            case "green" -> TextColor.fromLegacyFormat(ChatFormatting.GREEN);
            case "dark_green", "darkgreen" -> TextColor.fromLegacyFormat(ChatFormatting.DARK_GREEN);
            case "blue" -> TextColor.fromLegacyFormat(ChatFormatting.BLUE);
            case "dark_blue", "darkblue" -> TextColor.fromLegacyFormat(ChatFormatting.DARK_BLUE);
            case "aqua", "cyan" -> TextColor.fromLegacyFormat(ChatFormatting.AQUA);
            case "dark_aqua", "darkaqua", "darkcyan" -> TextColor.fromLegacyFormat(ChatFormatting.DARK_AQUA);
            case "yellow" -> TextColor.fromLegacyFormat(ChatFormatting.YELLOW);
            case "purple", "light_purple", "lightpurple", "magenta" -> TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE);
            case "dark_purple", "darkpurple" -> TextColor.fromLegacyFormat(ChatFormatting.DARK_PURPLE);
            case "white" -> TextColor.fromLegacyFormat(ChatFormatting.WHITE);
            case "gray", "grey" -> TextColor.fromLegacyFormat(ChatFormatting.GRAY);
            case "dark_gray", "darkgray", "dark_grey", "darkgrey" -> TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY);
            case "black" -> TextColor.fromLegacyFormat(ChatFormatting.BLACK);
            default -> null;
        };
    }

    public static Component formatLevelComponent(int level, String colorStr) {
        String format = MobLevelingConfig.LEVEL_FORMAT.get();
        String levelText = format.replace("{level}", String.valueOf(level));

        LevelDisplayManager manager = new LevelDisplayManager();
        TextColor color = manager.parseColor(colorStr);
        Style style = Style.EMPTY.withColor(color);

        return Component.literal(levelText).withStyle(style);
    }
}
