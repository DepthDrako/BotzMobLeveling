package com.botzlabz.mobleveling.level;

import com.botzlabz.mobleveling.data.MobLevelingDataManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

/**
 * Thin delegation layer — all actual resolution is in
 * {@link MobLevelingDataManager#resolve(Mob, ServerLevel)}.
 *
 * <p>{@link LevelResult#SKIP} is a sentinel that means "do not level this mob".
 */
public final class LevelResolver {

    public static LevelResult resolve(Mob mob, ServerLevel level) {
        return MobLevelingDataManager.INSTANCE.resolve(mob, level);
    }

    // -------------------------------------------------------------------------

    public static class LevelResult {

        /** Sentinel: a SKIP rule matched — do not assign any level. */
        public static final LevelResult SKIP = new LevelResult(-1, "skip", "skip", null);

        public final int       level;
        public final String    ruleId;
        public final String    ruleType;

        /** Non-null only when the winning rule was a {@link BossRule}. */
        @Nullable
        public final BossRule  bossRule;

        /** The matched per-entity override from the winning rule, or {@code null}. */
        @Nullable
        public final MobOverride override;

        /**
         * Effective cap exemption: {@code true} when the winning rule OR its matched
         * override sets {@code ignore_level_cap}. The spawn handler skips the global
         * cap clamp when this is set, and the value is persisted on the mob.
         */
        public final boolean ignoreCap;

        /**
         * Datapack attribute-scaling ops for this mob — the winning rule's
         * {@code attribute_scaling} merged with the matched override's (override wins
         * per attribute). Never null; empty when none apply.
         */
        public final java.util.Map<String, AttrOp> attributeOps;

        public LevelResult(int level, String ruleId, String ruleType, @Nullable BossRule bossRule) {
            this(level, ruleId, ruleType, bossRule, null, false, java.util.Map.of());
        }

        public LevelResult(int level, String ruleId, String ruleType, @Nullable BossRule bossRule,
                           @Nullable MobOverride override, boolean ignoreCap,
                           java.util.Map<String, AttrOp> attributeOps) {
            this.level        = level;
            this.ruleId       = ruleId;
            this.ruleType     = ruleType;
            this.bossRule     = bossRule;
            this.override     = override;
            this.ignoreCap    = ignoreCap;
            this.attributeOps = attributeOps;
        }

        public boolean isSkip() { return level < 0; }
    }

    private LevelResolver() {}
}
