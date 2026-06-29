package com.botzlabz.mobleveling.level;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A boss-specific rule extending {@link LevelRule}.
 * Loaded from {@code data/<ns>/mob_levels/bosses/<name>.json}.
 */
public class BossRule extends LevelRule {

    public final double       healthMultiplier;
    public final double       damageMultiplier;
    public final String       bossBarColor;   // "red", "blue", "green", "yellow", "purple", "white", "pink"
    public final String       bossBarTitle;   // empty = use mob display name
    /** Boss-bar visibility radius in blocks. 0 (or absent) = global (every player); >0 = local. */
    public final int          bossBarRange;
    public final String       announcement;   // empty = use default message
    public final boolean      glow;
    public final String       minionType;     // entity id, empty = no minions
    public final int          minionCount;
    public final int          minionSpread;   // blocks
    public final List<String> immunities;     // "fire", "fall", "poison", "wither", "explosion"

    public BossRule(String id, Mode mode, int fixedLevel, int minLevel, int maxLevel,
                    double distanceScale, int priority,
                    String entityTypeFilter, String biomeFilter,
                    String dimensionFilter, String structureFilter,
                    boolean hostileOnly, boolean passiveOnly,
                    Map<String, AttrOp> attributeScaling,
                    Map<String, MobOverride> mobOverrides,
                    boolean ignoreLevelCap,
                    // boss-specific
                    double healthMultiplier, double damageMultiplier,
                    String bossBarColor, String bossBarTitle, int bossBarRange, String announcement,
                    boolean glow, String minionType, int minionCount, int minionSpread,
                    List<String> immunities) {
        super(id, mode, fixedLevel, minLevel, maxLevel, distanceScale, priority,
              entityTypeFilter, biomeFilter, dimensionFilter, structureFilter,
              hostileOnly, passiveOnly, attributeScaling, mobOverrides, ignoreLevelCap);
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.bossBarColor     = bossBarColor;
        this.bossBarTitle     = bossBarTitle;
        this.bossBarRange     = Math.max(0, bossBarRange);
        this.announcement     = announcement;
        this.glow             = glow;
        this.minionType       = minionType;
        this.minionCount      = minionCount;
        this.minionSpread     = minionSpread;
        this.immunities       = Collections.unmodifiableList(immunities);
    }

    public static BossRule from(String id, JsonObject json) {
        // Reuse parent parsing
        LevelRule base = LevelRule.from(id, json);

        double  healthMul  = json.has("health_multiplier") ? json.get("health_multiplier").getAsDouble() : 3.0;
        double  damageMul  = json.has("damage_multiplier") ? json.get("damage_multiplier").getAsDouble() : 1.5;
        String  barColor   = json.has("boss_bar_color")    ? json.get("boss_bar_color").getAsString()    : "red";
        String  barTitle   = json.has("boss_bar_title")    ? json.get("boss_bar_title").getAsString()    : "";
        int     barRange   = json.has("boss_bar_range")    ? json.get("boss_bar_range").getAsInt()        : 0;
        String  announce   = json.has("announcement")      ? json.get("announcement").getAsString()      : "";
        boolean glow       = json.has("glow")              ? json.get("glow").getAsBoolean()             : true;
        String  minionType = json.has("minion_type")       ? json.get("minion_type").getAsString()       : "";
        int     minionCnt  = json.has("minion_count")      ? json.get("minion_count").getAsInt()         : 0;
        int     minionSprd = json.has("minion_spread")     ? json.get("minion_spread").getAsInt()        : 5;

        List<String> immunities = new ArrayList<>();
        if (json.has("immunities") && json.get("immunities").isJsonArray()) {
            json.getAsJsonArray("immunities").forEach(e -> immunities.add(e.getAsString()));
        }

        return new BossRule(
            id, base.mode, base.fixedLevel, base.minLevel, base.maxLevel,
            base.distanceScale, base.priority,
            base.entityTypeFilter, base.biomeFilter, base.dimensionFilter, base.structureFilter,
            base.hostileOnly, base.passiveOnly, base.attributeScaling,
            base.mobOverrides, base.ignoreLevelCap,
            healthMul, damageMul, barColor, barTitle, barRange, announce, glow,
            minionType, minionCnt, minionSprd, immunities
        );
    }
}
