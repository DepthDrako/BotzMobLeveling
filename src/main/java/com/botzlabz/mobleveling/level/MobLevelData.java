package com.botzlabz.mobleveling.level;

import com.botzlabz.mobleveling.stats.MobStatBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * Capability data attached to every LivingEntity.
 *
 * <p>Persisted to {@code entity.getPersistentData()} under the key
 * {@code botzmobleveling:level_data} so data survives restarts and
 * chunk unloads/reloads.
 */
public class MobLevelData {

    /** NBT key used in {@code entity.getPersistentData()}. */
    public static final String NBT_KEY = "botzmobleveling:level_data";

    // ------------------------------------------------------------------
    // Core level data
    // ------------------------------------------------------------------
    private int     baseLevel    = 0;
    private int     killLevel    = 0;
    private int     totalLevel   = 0;
    private boolean processed    = false;
    private long    xpBank       = 0;
    private int     killCount    = 0;
    private boolean persistent   = false;
    /** When true this mob is exempt from the global level cap (rule/override flag). Persisted. */
    private boolean ignoreCap    = false;
    private String  sourceRuleId   = "none";
    private String  sourceRuleType = "base";

    // ------------------------------------------------------------------
    // Display
    // ------------------------------------------------------------------
    /** The mob's original (pre-tag) display name, stored so we can rebuild the tag cleanly. */
    @Nullable
    private String originalName = null;

    // ------------------------------------------------------------------
    // Boss data
    // ------------------------------------------------------------------
    private boolean isBoss          = false;
    private String  bossRuleId      = "";   // used to re-lookup BossRule on chunk reload
    private double  bossHealthMul   = 1.0;
    private double  bossDamageMul   = 1.0;
    private String  bossBarColor    = "red";
    private String  bossBarTitle    = "";
    /** Boss-bar visibility radius in blocks; 0 = global. Persisted so reload re-registers correctly. */
    private int     bossBarRange    = 0;
    private String  bossAnnouncement = "";
    private boolean bossGlow        = false;
    /** Damage-type immunities from the boss rule ("fire", "fall", "explosion", "poison", "wither"). */
    private java.util.List<String> bossImmunities = java.util.Collections.emptyList();

    // ------------------------------------------------------------------
    // Datapack attribute ops (Phase 3b)
    // ------------------------------------------------------------------
    /**
     * Resolved datapack attribute-scaling ops for this mob (rule ∪ override). Persisted
     * so they can be re-applied on reload without re-resolving the rule, mirroring how
     * {@link #statBlock} is persisted rather than recomputed.
     */
    private java.util.Map<String, AttrOp> attributeOps = java.util.Map.of();

    // ------------------------------------------------------------------

    private final MobStatBlock statBlock = new MobStatBlock();

    // ------------------------------------------------------------------
    // Level application
    // ------------------------------------------------------------------

    public void applyLevel(int base, String ruleId, String ruleType) {
        this.baseLevel      = base;
        this.totalLevel     = base;
        this.sourceRuleId   = ruleId;
        this.sourceRuleType = ruleType;
        this.processed      = true;
        statBlock.computeFromLevel(totalLevel);
    }

    public void applyBossRule(BossRule rule) {
        this.isBoss           = true;
        this.bossRuleId       = rule.id;
        this.bossHealthMul    = rule.healthMultiplier;
        this.bossDamageMul    = rule.damageMultiplier;
        this.bossBarColor     = rule.bossBarColor;
        this.bossBarTitle     = rule.bossBarTitle;
        this.bossBarRange     = rule.bossBarRange;
        this.bossAnnouncement = rule.announcement;
        this.bossGlow         = rule.glow;
        this.bossImmunities   = java.util.List.copyOf(rule.immunities);
    }

    // ------------------------------------------------------------------
    // Kill leveling
    // ------------------------------------------------------------------

    /**
     * Adds XP and levels up if threshold is crossed.
     *
     * @param xp          XP gained from this kill
     * @param xpBase      threshold for the first kill level
     * @param xpScale     exponential multiplier per kill level
     * @param globalCap   hard maximum total level
     * @param killCap     max kill levels (0 = unlimited within globalCap)
     * @return true if a level-up occurred
     */
    public boolean addKillXP(long xp, int xpBase, double xpScale, int globalCap, int killCap) {
        // Cap-exempt mobs ignore the global cap when accruing kill levels (#8).
        int effectiveCap = ignoreCap ? Integer.MAX_VALUE : globalCap;
        boolean leveled = false;
        xpBank += xp;
        long threshold = (long)(xpBase * Math.pow(xpScale, killLevel));

        while (xpBank >= threshold && totalLevel < effectiveCap) {
            if (killCap > 0 && killLevel >= killCap) break;
            xpBank -= threshold;
            killLevel++;
            killCount++;
            totalLevel = Math.min(baseLevel + killLevel, effectiveCap);
            threshold  = (long)(xpBase * Math.pow(xpScale, killLevel));
            leveled    = true;
        }
        if (leveled) statBlock.computeFromLevel(totalLevel);
        return leveled;
    }

    public void addBonusLevel(int bonus, int globalCap) {
        int effectiveCap = ignoreCap ? Integer.MAX_VALUE : globalCap;
        int newTotal = Math.min(totalLevel + bonus, effectiveCap);
        if (newTotal != totalLevel) {
            totalLevel = newTotal;
            statBlock.computeFromLevel(totalLevel);
        }
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public int          getBaseLevel()        { return baseLevel; }
    public int          getKillLevel()        { return killLevel; }
    public int          getTotalLevel()       { return totalLevel; }
    public boolean      isProcessed()         { return processed; }
    public long         getXpBank()           { return xpBank; }
    public int          getKillCount()        { return killCount; }
    public boolean      isPersistent()        { return persistent; }
    public boolean      isIgnoreCap()         { return ignoreCap; }
    public String       getSourceRuleId()     { return sourceRuleId; }
    public String       getSourceRuleType()   { return sourceRuleType; }
    public MobStatBlock getStatBlock()        { return statBlock; }
    public java.util.Map<String, AttrOp> getAttributeOps() { return attributeOps; }
    @Nullable
    public String       getOriginalName()     { return originalName; }

    public boolean isBoss()              { return isBoss; }
    public String  getBossRuleId()       { return bossRuleId; }
    public double  getBossHealthMul()    { return bossHealthMul; }
    public double  getBossDamageMul()    { return bossDamageMul; }
    public String  getBossBarColor()     { return bossBarColor; }
    public String  getBossBarTitle()     { return bossBarTitle; }
    public int     getBossBarRange()     { return bossBarRange; }
    public String  getBossAnnouncement() { return bossAnnouncement; }
    public boolean isBossGlow()          { return bossGlow; }
    public boolean hasBossImmunity(String type) { return isBoss && bossImmunities.contains(type); }

    // ------------------------------------------------------------------
    // Setters
    // ------------------------------------------------------------------

    public void setPersistent(boolean p)            { this.persistent   = p; }
    public void setIgnoreCap(boolean i)             { this.ignoreCap    = i; }
    public void setOriginalName(@Nullable String n) { this.originalName = n; }
    /** Effective boss health multiplier (rule value or config fallback), stored so the
     *  MAX_HEALTH modifier can be re-applied on reload by AttributeScalingManager. */
    public void setBossHealthMul(double m)          { this.bossHealthMul = m; }
    public void setAttributeOps(java.util.Map<String, AttrOp> ops) {
        this.attributeOps = (ops == null || ops.isEmpty()) ? java.util.Map.of() : java.util.Map.copyOf(ops);
    }

    // ------------------------------------------------------------------
    // NBT
    // ------------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("baseLevel",       baseLevel);
        tag.putInt("killLevel",       killLevel);
        tag.putInt("totalLevel",      totalLevel);
        tag.putBoolean("processed",   processed);
        tag.putLong("xpBank",         xpBank);
        tag.putInt("killCount",       killCount);
        tag.putBoolean("persistent",  persistent);
        tag.putBoolean("ignoreCap",   ignoreCap);
        tag.putString("sourceRuleId",   sourceRuleId);
        tag.putString("sourceRuleType", sourceRuleType);
        if (originalName != null) tag.putString("originalName", originalName);
        // Boss fields
        tag.putBoolean("isBoss",         isBoss);
        tag.putString("bossRuleId",      bossRuleId);
        tag.putDouble("bossHealthMul",   bossHealthMul);
        tag.putDouble("bossDamageMul",   bossDamageMul);
        tag.putString("bossBarColor",    bossBarColor);
        tag.putString("bossBarTitle",    bossBarTitle);
        tag.putInt("bossBarRange",       bossBarRange);
        tag.putString("bossAnnounce",    bossAnnouncement);
        tag.putBoolean("bossGlow",       bossGlow);
        ListTag immTag = new ListTag();
        bossImmunities.forEach(s -> immTag.add(StringTag.valueOf(s)));
        tag.put("bossImmunities", immTag);
        tag.put("statBlock", statBlock.save());
        // Datapack attribute ops (Phase 3b) — key = attribute id, value = {op, vpl}
        if (!attributeOps.isEmpty()) {
            CompoundTag opsTag = new CompoundTag();
            attributeOps.forEach((attr, op) -> {
                CompoundTag o = new CompoundTag();
                o.putString("op", op.operation().name());
                o.putDouble("vpl", op.valuePerLevel());
                opsTag.put(attr, o);
            });
            tag.put("attrOps", opsTag);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        baseLevel      = tag.getInt("baseLevel");
        killLevel      = tag.getInt("killLevel");
        totalLevel     = tag.getInt("totalLevel");
        processed      = tag.getBoolean("processed");
        xpBank         = tag.getLong("xpBank");
        killCount      = tag.getInt("killCount");
        persistent     = tag.getBoolean("persistent");
        ignoreCap      = tag.getBoolean("ignoreCap");
        sourceRuleId   = tag.getString("sourceRuleId");
        sourceRuleType = tag.getString("sourceRuleType");
        if (tag.contains("originalName")) originalName = tag.getString("originalName");
        isBoss           = tag.getBoolean("isBoss");
        bossRuleId       = tag.getString("bossRuleId");
        bossHealthMul    = tag.getDouble("bossHealthMul");
        bossDamageMul    = tag.getDouble("bossDamageMul");
        bossBarColor     = tag.getString("bossBarColor");
        bossBarTitle     = tag.getString("bossBarTitle");
        bossBarRange     = tag.getInt("bossBarRange");
        bossAnnouncement = tag.getString("bossAnnounce");
        bossGlow         = tag.getBoolean("bossGlow");
        if (tag.contains("bossImmunities")) {
            java.util.List<String> imms = new java.util.ArrayList<>();
            ListTag immTag = tag.getList("bossImmunities", Tag.TAG_STRING);
            for (int i = 0; i < immTag.size(); i++) imms.add(immTag.getString(i));
            bossImmunities = imms;
        }
        if (tag.contains("statBlock")) statBlock.load(tag.getCompound("statBlock"));
        if (tag.contains("attrOps")) {
            CompoundTag opsTag = tag.getCompound("attrOps");
            java.util.Map<String, AttrOp> ops = new java.util.HashMap<>();
            for (String attr : opsTag.getAllKeys()) {
                CompoundTag o = opsTag.getCompound(attr);
                ops.put(attr, new AttrOp(AttrOp.operationFromName(o.getString("op")), o.getDouble("vpl")));
            }
            attributeOps = ops.isEmpty() ? java.util.Map.of() : java.util.Map.copyOf(ops);
        } else {
            attributeOps = java.util.Map.of();
        }
    }
}
