package com.botzlabz.mobleveling.event;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.level.MobLevelData;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Handles making leveled passive mobs fight back when attacked.
 */
@Mod.EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PassiveMobCombatHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NBT_COMBAT_ENABLED = "botzmobleveling_CombatEnabled";
    private static final String NBT_COMBAT_GOALS_ADDED = "botzmobleveling_CombatGoalsAdded";

    /**
     * When a leveled passive mob is hurt, make it fight back.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!MobLevelingConfig.ENABLED.get()) {
            return;
        }

        if (!MobLevelingConfig.LEVELED_PASSIVES_CAN_ATTACK.get()) {
            return;
        }

        // Check if the hurt entity is a passive mob (Animal extends PathfinderMob)
        if (!(event.getEntity() instanceof Animal animal)) {
            return;
        }

        // Check if mob has a level
        if (!MobLevelData.hasLevel(animal)) {
            return;
        }

        // Animal extends PathfinderMob, so we can use it directly
        PathfinderMob pathfinderMob = animal;

        // Check if combat is disabled for this specific mob (via datapack)
        if (animal.getPersistentData().contains(NBT_COMBAT_ENABLED) &&
            !animal.getPersistentData().getBoolean(NBT_COMBAT_ENABLED)) {
            return;
        }

        // Get the attacker
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) {
            return;
        }

        // Only retaliate against living entities
        if (!(attacker instanceof LivingEntity livingAttacker)) {
            return;
        }

        // Add combat goals if not already added
        if (!pathfinderMob.getPersistentData().getBoolean(NBT_COMBAT_GOALS_ADDED)) {
            addCombatGoals(pathfinderMob);
            pathfinderMob.getPersistentData().putBoolean(NBT_COMBAT_GOALS_ADDED, true);
        }

        // Set the attacker as target
        pathfinderMob.setTarget(livingAttacker);

        if (MobLevelingConfig.DEBUG_MODE.get()) {
            LOGGER.debug("[PassiveMobCombat] {} is now targeting {} after being attacked",
                    animal.getType().getDescription().getString(),
                    livingAttacker.getType().getDescription().getString());
        }
    }

    /**
     * Add melee attack and retaliation goals to a passive mob.
     */
    private static void addCombatGoals(PathfinderMob mob) {
        try {
            // Add melee attack goal (priority 1 - high priority)
            mob.goalSelector.addGoal(1, new MeleeAttackGoal(mob, 1.2D, false));

            // Add hurt by target goal - retaliates when hurt
            mob.targetSelector.addGoal(1, new HurtByTargetGoal(mob));

            // Add target goal for players who attack them
            mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Player.class, 10, true, false,
                    (entity) -> mob.getLastHurtByMob() == entity));

            if (MobLevelingConfig.DEBUG_MODE.get()) {
                LOGGER.debug("[PassiveMobCombat] Added combat goals to {}",
                        mob.getType().getDescription().getString());
            }
        } catch (Exception e) {
            LOGGER.warn("[PassiveMobCombat] Failed to add combat goals to {}: {}",
                    mob.getType().getDescription().getString(), e.getMessage());
        }
    }

    /**
     * Enable or disable combat for a specific mob.
     * Called from the spawn handler based on datapack settings.
     */
    public static void setCombatEnabled(Mob mob, boolean enabled) {
        mob.getPersistentData().putBoolean(NBT_COMBAT_ENABLED, enabled);
    }

    /**
     * Check if combat is enabled for a mob (defaults to true if not set).
     */
    public static boolean isCombatEnabled(Mob mob) {
        if (!mob.getPersistentData().contains(NBT_COMBAT_ENABLED)) {
            return true; // Default to enabled
        }
        return mob.getPersistentData().getBoolean(NBT_COMBAT_ENABLED);
    }

    /**
     * Pre-add combat goals to a mob during spawn.
     * Used when datapack specifies the mob should be aggressive.
     */
    public static void enableCombatOnSpawn(PathfinderMob mob) {
        if (!mob.getPersistentData().getBoolean(NBT_COMBAT_GOALS_ADDED)) {
            addCombatGoals(mob);
            mob.getPersistentData().putBoolean(NBT_COMBAT_GOALS_ADDED, true);
            mob.getPersistentData().putBoolean(NBT_COMBAT_ENABLED, true);
        }
    }
}
