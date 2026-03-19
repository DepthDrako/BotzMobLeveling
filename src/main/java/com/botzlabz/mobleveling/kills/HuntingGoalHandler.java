package com.botzlabz.mobleveling.kills;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * Adds mob-hunting AI goals to mobs whose datapack rule has "hunt_to_level": true.
 * Goals sit at low priority so they don't interfere with the mob's existing combat behaviour.
 * An NBT flag prevents the goals from being added more than once.
 *
 * Adaptive Mob AI compatibility:
 *   If Adaptive Mob AI is present we skip injecting MeleeAttackGoal entirely — their DQN
 *   handles combat execution. We still inject NearestAttackableTargetGoal so the mob
 *   acquires mob targets; their DQN then runs its combat actions against whatever target
 *   is set, naturally extending its learning to mob-vs-mob scenarios over time.
 */
public class HuntingGoalHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NBT_HUNT_GOALS_ADDED = "botzmobleveling_HuntGoalsAdded";

    // Checked once at class-load time so there's no per-tick overhead.
    private static final boolean ADAPTIVE_MOB_AI_LOADED =
            net.minecraftforge.fml.ModList.get().isLoaded("adaptivemobai");

    // Goal-selector priorities (lower number = higher priority in Minecraft AI).
    // We use high numbers so existing combat goals always win.
    private static final int MELEE_PRIORITY  = 5;
    // Target priority is slightly higher than melee so the mob picks a target first.
    // When AMAI is present we use a lower-priority slot (6) so their player-targeting
    // goal (which we assume sits around 3–4) stays dominant; mob targets only fill in
    // when no player target is active.
    private static final int TARGET_PRIORITY         = 4;
    private static final int TARGET_PRIORITY_AMAI    = 6;

    /**
     * Enable mob-hunting on the given mob. Safe to call multiple times —
     * goals are only added once, tracked via NBT.
     */
    public static void enableHunting(Mob mob) {
        if (!MobLevelingConfig.HUNT_TO_LEVEL_ENABLED.get()) {
            return;
        }

        // Only PathfinderMob supports goal selectors
        if (!(mob instanceof PathfinderMob pathfinder)) {
            return;
        }

        if (pathfinder.getPersistentData().getBoolean(NBT_HUNT_GOALS_ADDED)) {
            return;
        }

        addHuntingGoals(pathfinder);
        pathfinder.getPersistentData().putBoolean(NBT_HUNT_GOALS_ADDED, true);
    }

    /**
     * Called on chunk load — re-adds goals if the mob had hunt behaviour before unloading.
     * Minecraft saves goal-selector state only implicitly; we restore from our NBT flag.
     */
    public static void reapplyHuntingOnLoad(Mob mob) {
        if (!MobLevelingConfig.HUNT_TO_LEVEL_ENABLED.get()) {
            return;
        }

        if (!(mob instanceof PathfinderMob pathfinder)) {
            return;
        }

        if (!pathfinder.getPersistentData().getBoolean(NBT_HUNT_GOALS_ADDED)) {
            return;
        }

        // The NBT flag is already set — just re-add the goals without setting it again
        addHuntingGoals(pathfinder);
    }

    private static void addHuntingGoals(PathfinderMob mob) {
        try {
            if (ADAPTIVE_MOB_AI_LOADED) {
                // Adaptive Mob AI is present — skip MeleeAttackGoal to avoid movement
                // stuttering from two competing melee goals fighting each tick.
                // Their DQN already handles combat execution; we only need to give the mob
                // a mob target so their system has something to act on.
                mob.targetSelector.addGoal(TARGET_PRIORITY_AMAI, new NearestAttackableTargetGoal<>(
                        mob, Mob.class,
                        /* chance */ 10,
                        /* mustSee */ true,
                        /* mustReach */ false,
                        target -> target != mob && !(target instanceof Player)
                ));

                if (MobLevelingConfig.DEBUG_MODE.get()) {
                    LOGGER.debug("[HuntToLevel] AMAI detected — added targeting-only hunting goal to {} (melee skipped)",
                            ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()));
                }
            } else {
                // Standard path — inject both melee and targeting goals.
                mob.goalSelector.addGoal(MELEE_PRIORITY, new MeleeAttackGoal(mob, 1.1D, false));

                // Target goal: hunt any nearby Mob that isn't this mob and isn't a Player.
                // Players are excluded because hostile mobs already target players natively.
                mob.targetSelector.addGoal(TARGET_PRIORITY, new NearestAttackableTargetGoal<>(
                        mob, Mob.class,
                        /* chance */ 10,
                        /* mustSee */ true,
                        /* mustReach */ false,
                        target -> target != mob && !(target instanceof Player)
                ));

                if (MobLevelingConfig.DEBUG_MODE.get()) {
                    LOGGER.debug("[HuntToLevel] Added hunting goals to {}",
                            ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[HuntToLevel] Failed to add hunting goals to {}: {}",
                    ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()), e.getMessage());
        }
    }
}
