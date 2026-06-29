package com.botzlabz.mobleveling.event;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.adaptive.DifficultyCalculator;
import com.botzlabz.mobleveling.ai.HuntingGoalInjector;
import com.botzlabz.mobleveling.attribute.AttributeScalingManager;
import com.botzlabz.mobleveling.boss.BossManager;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.display.LevelDisplay;
import com.botzlabz.mobleveling.level.BossRule;
import com.botzlabz.mobleveling.level.LevelResolver;
import com.botzlabz.mobleveling.level.MobLevelCapability;
import com.botzlabz.mobleveling.level.MobLevelData;
import com.botzlabz.mobleveling.level.MobLevelStore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class MobSpawnHandler {

    private static final Logger LOGGER = LogManager.getLogger(BotzMobLeveling.MOD_ID);

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!MobLevelingConfig.ENABLED.get()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        MobLevelData data = MobLevelCapability.get(mob);
        if (data == null || data.isProcessed()) return;

        // ---- Filtering ----
        ResourceLocation entityId = mob.getType().builtInRegistryHolder().key().location();

        // Blacklist check
        List<? extends String> blacklist = MobLevelingConfig.MOB_BLACKLIST.get();
        if (!blacklist.isEmpty()) {
            String idStr = entityId.toString();
            if (blacklist.contains(idStr) || blacklist.contains(entityId.getPath())) return;
        }

        // Passive mob toggle
        boolean isPassive = mob.getType().getCategory() != MobCategory.MONSTER;
        if (isPassive && !MobLevelingConfig.LEVEL_PASSIVE_MOBS.get()) return;

        // Boss mob toggle (vanilla bosses — Wither, Ender Dragon)
        boolean isVanillaBoss = mob.getType().getCategory() == MobCategory.MISC
            || mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss
            || mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon;
        if (isVanillaBoss && !MobLevelingConfig.LEVEL_BOSS_MOBS.get()) return;

        // ---- Level resolution ----
        LevelResolver.LevelResult result = LevelResolver.resolve(mob, serverLevel);
        if (result.isSkip()) return;

        int cap = MobLevelingConfig.GLOBAL_LEVEL_CAP.get();
        boolean ignoreCap = result.ignoreCap;

        // 1. Rule-range-clamped base level (DISTANCE rules without a max_level stay
        //    unbounded here; the global cap is applied at the end unless exempt).
        int level = result.level;

        // 2. Per-entity override: a fixed level replaces the base outright, otherwise
        //    the bonus stacks on top.
        if (result.override != null) {
            Integer fixed = result.override.fixedLevel();
            level = (fixed != null) ? fixed : level + result.override.levelBonus();
        }

        // 3. Adaptive difficulty bonus on top.
        if (MobLevelingConfig.ADAPTIVE_DIFFICULTY_ENABLED.get()) {
            level += DifficultyCalculator.computeBonus(mob, serverLevel);
        }

        // 4. Final clamp — skipped only for cap-exempt mobs (bosses / ignore_level_cap).
        if (!ignoreCap) level = Math.min(level, cap);
        if (level < 1) level = 1;

        // Apply level to data
        data.applyLevel(level, result.ruleId, result.ruleType);
        data.setIgnoreCap(ignoreCap);
        data.setAttributeOps(result.attributeOps);

        // Boss module
        if (result.bossRule != null && MobLevelingConfig.BOSS_ENABLED.get()) {
            setupBoss(mob, data, result.bossRule, serverLevel);
        }

        // Apply attribute modifiers (config stats + datapack ops)
        AttributeScalingManager.apply(mob, data);
        mob.setHealth(mob.getMaxHealth());

        // Inject hunting AI (for hostile leveled mobs)
        if (!isPassive) {
            HuntingGoalInjector.inject(mob, level);
        } else {
            HuntingGoalInjector.injectPassiveCombat(mob);
        }

        // Display name
        LevelDisplay.apply(mob, data);

        // Persist to entity's persistent data (survives restart).
        // The store helper also writes the BML_Level int that eidolon_ai reads.
        MobLevelStore.persist(mob, data);

        if (MobLevelingConfig.DEBUG_MODE.get()) {
            LOGGER.info("[BotzMobLeveling] Spawned {} Lv.{} (rule: {} / {})",
                entityId, level, result.ruleId, result.ruleType);
        }
    }

    // -------------------------------------------------------------------------

    private static void setupBoss(Mob mob, MobLevelData data, BossRule rule, ServerLevel serverLevel) {
        data.applyBossRule(rule);

        // Store the effective health multiplier; the MAX_HEALTH modifier itself is
        // applied by AttributeScalingManager.apply(mob, data) — which also runs on
        // reload, so the multiplier survives chunk unload / server restart (the
        // transient modifier is otherwise not persisted in entity NBT).
        double healthMul = rule.healthMultiplier > 1.0
            ? rule.healthMultiplier
            : MobLevelingConfig.BOSS_HEALTH_MULTIPLIER.get();
        data.setBossHealthMul(healthMul);

        // Glow
        if (rule.glow || MobLevelingConfig.BOSS_GLOW.get()) {
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.GLOWING, Integer.MAX_VALUE, 0, false, false));
        }

        // Immunities
        applyImmunities(mob, rule.immunities);

        // Spawn minions
        if (!rule.minionType.isEmpty() && rule.minionCount > 0) {
            spawnMinions(mob, rule, serverLevel);
        }

        // Register boss bar AFTER everything is set up
        BossManager.registerBoss(mob, data, serverLevel);
    }

    private static void applyImmunities(Mob mob, java.util.List<String> immunities) {
        for (String immunity : immunities) {
            switch (immunity.toLowerCase()) {
                case "fire"  -> mob.clearFire(); // boss is immune to fire; clear any existing fire ticks
                case "fall"  -> {}  // fall damage handled via DamageReductionHandler
                // Store immunities in data so DamageReductionHandler can check them
                default -> {}
            }
        }
    }

    private static void spawnMinions(Mob boss, BossRule rule, ServerLevel serverLevel) {
        ResourceLocation minionId = ResourceLocation.tryParse(rule.minionType);
        if (minionId == null) return;
        // ENTITY_TYPE is a defaulted registry: get() on an unknown id returns the
        // default entry (pig) instead of null, so a typo'd minion_type would
        // silently spawn pigs.  containsKey is the real existence check.
        if (!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(minionId)) return;
        net.minecraft.world.entity.EntityType<?> minionType =
            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(minionId);

        net.minecraft.core.BlockPos bossPos = boss.blockPosition();
        int spread = Math.max(1, rule.minionSpread);
        java.util.Random rand = new java.util.Random();

        for (int i = 0; i < rule.minionCount; i++) {
            int dx = rand.nextInt(spread * 2 + 1) - spread;
            int dz = rand.nextInt(spread * 2 + 1) - spread;
            net.minecraft.core.BlockPos spawnPos = bossPos.offset(dx, 0, dz);

            net.minecraft.world.entity.Entity minion = minionType.create(serverLevel);
            if (minion == null) continue;
            minion.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                          rand.nextFloat() * 360f, 0f);
            serverLevel.addFreshEntity(minion);
        }
    }
}
