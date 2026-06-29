package com.botzlabz.mobleveling.ai;

import com.botzlabz.mobleveling.config.MobLevelingConfig;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

/**
 * Injects melee attack and target goals into leveled mobs.
 *
 * <p>{@link MeleeAttackGoal} and {@link HurtByTargetGoal} require a
 * {@link PathfinderMob} (which all standard overworld mobs extend), so we
 * guard with an {@code instanceof} check.  Mobs that don't extend
 * {@code PathfinderMob} (e.g. Slimes) skip AI injection cleanly.
 *
 * <p>The AT in {@code META-INF/accesstransformer.cfg} makes
 * {@code goalSelector} and {@code targetSelector} public.
 */
public final class HuntingGoalInjector {

    /**
     * Inject hunting AI into {@code mob} if the feature is enabled and the
     * mob's total level meets the configured minimum.
     */
    public static void inject(Mob mob, int level) {
        if (!MobLevelingConfig.HUNTING_AI_ENABLED.get()) return;
        if (level < MobLevelingConfig.HUNTING_AI_MIN_LEVEL.get()) return;
        if (!(mob instanceof PathfinderMob pfMob)) return;

        if (canMelee(mob) && !hasGoal(mob, MeleeAttackGoal.class)) {
            mob.goalSelector.addGoal(12, new MeleeAttackGoal(pfMob, 1.2, true));
        }
        if (!hasTargetGoal(mob, NearestAttackableTargetGoal.class)) {
            mob.targetSelector.addGoal(12, new NearestAttackableTargetGoal<>(mob, Player.class, true));
        }
        if (!hasTargetGoal(mob, HurtByTargetGoal.class)) {
            mob.targetSelector.addGoal(13, new HurtByTargetGoal(pfMob));
        }
    }

    /**
     * For passive mobs: only inject retaliation / short-range defence.
     */
    public static void injectPassiveCombat(Mob mob) {
        if (!MobLevelingConfig.HUNTING_AI_ENABLED.get()) return;
        if (!MobLevelingConfig.LEVEL_PASSIVE_MOBS.get()) return;
        if (!(mob instanceof PathfinderMob pfMob)) return;
        // Most passive mobs (sheep, cows, ...) have no ATTACK_DAMAGE attribute —
        // a MeleeAttackGoal on them throws from doHurtTarget on the first swing.
        if (!canMelee(mob)) return;

        if (!hasTargetGoal(mob, HurtByTargetGoal.class)) {
            mob.targetSelector.addGoal(10, new HurtByTargetGoal(pfMob));
        }
        if (!hasGoal(mob, MeleeAttackGoal.class)) {
            mob.goalSelector.addGoal(10, new MeleeAttackGoal(pfMob, 1.1, true));
        }
    }

    // -------------------------------------------------------------------------

    private static boolean canMelee(Mob mob) {
        return mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE);
    }

    private static boolean hasGoal(Mob mob, Class<?> goalClass) {
        for (var wrapped : mob.goalSelector.getAvailableGoals()) {
            if (goalClass.isInstance(wrapped.getGoal())) return true;
        }
        return false;
    }

    /** Target goals live in targetSelector — checking goalSelector would always miss them. */
    private static boolean hasTargetGoal(Mob mob, Class<?> goalClass) {
        for (var wrapped : mob.targetSelector.getAvailableGoals()) {
            if (goalClass.isInstance(wrapped.getGoal())) return true;
        }
        return false;
    }

    private HuntingGoalInjector() {}
}
