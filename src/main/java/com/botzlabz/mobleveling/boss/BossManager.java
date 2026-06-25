package com.botzlabz.mobleveling.boss;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.attribute.AttributeScalingManager;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.data.AttributeScaling;
import com.botzlabz.mobleveling.data.LevelRule;
import com.botzlabz.mobleveling.data.MobLevelingDataManager;
import com.botzlabz.mobleveling.display.LevelDisplayManager;
import com.botzlabz.mobleveling.kills.HuntingGoalHandler;
import com.botzlabz.mobleveling.level.LevelResolver;
import com.botzlabz.mobleveling.level.MobLevelData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for boss mobs - handles boss bars, tracking, and lifecycle.
 */
public class BossManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static BossManager instance;

    // Active boss bars mapped by boss UUID
    private final Map<UUID, ServerBossEvent> activeBossBars = new ConcurrentHashMap<>();

    // Track which players are tracking which bosses
    private final Map<UUID, Set<ServerPlayer>> bossPlayerTracking = new ConcurrentHashMap<>();

    // Track active minions spawned by bosses
    private final Map<UUID, Set<UUID>> bossMinions = new ConcurrentHashMap<>();

    // Track last minion spawn time for interval checks
    private final Map<UUID, Long> lastMinionSpawnTime = new ConcurrentHashMap<>();

    // Used to re-derive the spawn rule's attribute scaling when applying a boss level
    private final LevelResolver levelResolver = new LevelResolver();
    private final AttributeScalingManager attributeManager = new AttributeScalingManager();

    private BossManager() {}

    public static BossManager getInstance() {
        if (instance == null) {
            instance = new BossManager();
        }
        return instance;
    }

    public static void reset() {
        if (instance != null) {
            instance.cleanup();
        }
        instance = null;
    }

    // ==================== Boss Spawning ====================

    /**
     * Attempt to transform a mob into a boss based on rules.
     * @return true if the mob became a boss
     */
    public boolean tryMakeBoss(Mob mob, ServerLevel level, BlockPos pos) {
        if (!MobLevelingConfig.BOSS_MODULE_ENABLED.get()) {
            return false;
        }

        if (BossData.isBoss(mob)) {
            return false; // Already a boss
        }

        if (BossData.isMinion(mob)) {
            return false; // Boss-summoned minions can't recursively become bosses
        }

        MobLevelingDataManager dataManager = MobLevelingDataManager.getInstance();
        if (dataManager == null) {
            return false;
        }

        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (mobId == null) {
            return false;
        }

        // Get boss rules that could apply to this mob
        List<BossRule> candidateRules = dataManager.getBossRulesForMob(mobId);
        if (candidateRules.isEmpty()) {
            return false;
        }

        // Get current biome and structure for filtering
        ResourceLocation currentBiome = getCurrentBiome(level, pos);
        ResourceLocation currentStructure = getCurrentStructure(level, pos);

        // Find a matching rule
        for (BossRule rule : candidateRules) {
            if (!rule.isEnabled()) {
                continue;
            }

            // Check structure requirement
            if (rule.requiresStructure()) {
                if (currentStructure == null || !rule.appliesToStructure(currentStructure)) {
                    continue;
                }
            }

            // Check biome requirement
            if (!rule.getBiomes().isEmpty()) {
                if (currentBiome == null || !rule.appliesToBiome(currentBiome)) {
                    continue;
                }
            }

            // Roll the dice
            double roll = mob.getRandom().nextDouble();
            if (roll <= rule.getSpawnChance()) {
                // Success! Make this mob a boss
                applyBossTransformation(mob, rule, level);
                return true;
            }
        }

        return false;
    }

    /**
     * Apply boss transformation to a mob.
     */
    private void applyBossTransformation(Mob mob, BossRule rule, ServerLevel level) {
        // Mark as boss
        BossData.markAsBoss(mob, true);
        BossData.setBossRuleId(mob, rule.getId());
        BossData.setBossTier(mob, rule.getTier());
        BossData.setDisplayName(mob, rule.getDisplayName());

        // Apply the boss's configured level BEFORE reading max health or building
        // the nameplate. Previously the rule's "level" field was parsed but never
        // applied, so the boss kept whatever level the normal spawn rules produced
        // (e.g. a distance-based level 68) and its nameplate/boss bar/kill XP all
        // reflected that instead of the intended boss level.
        applyBossLevel(mob, rule, level);

        BossData.setOriginalMaxHealth(mob, mob.getMaxHealth());

        // Apply the boss display name to the entity itself so its nameplate
        // matches the boss bar. Without this, only the boss bar gets the name —
        // the entity nameplate stays as the leveled vanilla type description
        // ([Lv.X] Ravager) set earlier by LevelDisplayManager.
        applyBossNameplate(mob, rule);

        // Prevent despawning
        if (MobLevelingConfig.BOSS_PREVENT_DESPAWN.get()) {
            mob.setPersistenceRequired();
        }

        // Store the size multiplier in NBT for a future renderer. NOTE: this is currently
        // NOT consumed anywhere — 1.20.1 has no entity scale attribute, so actually
        // resizing the model requires a rendering mixin that does not yet exist. The field
        // is documented as not-yet-functional; the NBT is written so a later renderer can
        // pick it up without a data migration.
        if (rule.getSizeMultiplier() != 1.0f) {
            mob.getPersistentData().putFloat("botzmobleveling_SizeMultiplier", rule.getSizeMultiplier());
        }

        // Apply glow effect
        if (rule.hasGlowEffect() && MobLevelingConfig.BOSS_GLOW_EFFECT.get()) {
            mob.setGlowingTag(true);
        }

        // Apply stat multipliers
        applyStatMultipliers(mob, rule);

        // Create boss bar
        if (MobLevelingConfig.BOSS_SHOW_BOSS_BAR.get() && rule.getBossBar().isVisible()) {
            createBossBar(mob, rule);
        }

        // Enable hunting AI if the rule requests it
        if (rule.shouldHuntToLevel()) {
            double chance = rule.getHuntToLevelChance();
            if (chance >= 1.0 || mob.getRandom().nextDouble() < chance) {
                HuntingGoalHandler.enableHunting(mob);
            }
        }

        // Announce spawn
        if (MobLevelingConfig.BOSS_SPAWN_ANNOUNCEMENT.get()) {
            announceSpawn(mob, rule, level);
        }

        if (MobLevelingConfig.DEBUG_MODE.get()) {
            LOGGER.debug("[BossManager] Transformed {} into boss with rule {}",
                    ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()), rule.getId());
        }
    }

    /**
     * Sets the entity's nameplate to the boss display name, optionally prefixed
     * with the level tag if level-in-name display is enabled. Also rewrites the
     * stored "original name" cache so subsequent {@link LevelDisplayManager#updateDisplay}
     * calls (e.g. when level changes via kills) keep the boss name as the base.
     *
     * <p>Called from {@link #applyBossTransformation} immediately after the
     * other identity fields are stamped into NBT. Boss display strings may
     * include legacy color codes (§4§l...), which {@link Component#literal} parses
     * for rendering on the nameplate.
     */
    private void applyBossNameplate(Mob mob, BossRule rule) {
        String bossName = rule.getDisplayName();
        Component bossNameComponent = Component.literal(bossName);

        Component fullName;
        if (MobLevelingConfig.SHOW_LEVEL_IN_NAME.get() && MobLevelData.hasLevel(mob)) {
            int level = MobLevelData.getLevel(mob);
            Component levelPrefix = LevelDisplayManager.formatLevelComponent(
                    level, MobLevelingConfig.LEVEL_COLOR.get());
            MutableComponent combined = Component.empty();
            combined.append(levelPrefix);
            combined.append(bossNameComponent);
            fullName = combined;
        } else {
            fullName = bossNameComponent;
        }

        mob.setCustomName(fullName);
        mob.setCustomNameVisible(true);

        // Refresh LevelDisplayManager's cached "original name" so future level
        // updates rebuild as "[Lv.N] <bossName>" instead of resurrecting the
        // vanilla type description (e.g. "Ravager"). Keys must match
        // LevelDisplayManager.ORIGINAL_NAME_KEY / HAS_CUSTOM_NAME_KEY.
        mob.getPersistentData().putString("botzmobleveling_OriginalName", bossName);
        mob.getPersistentData().putBoolean("botzmobleveling_HadCustomName", true);
    }

    /**
     * Stamp the boss's configured level onto the mob and re-run the spawn rule's
     * attribute scaling at that level. This makes the boss's health/damage/etc.
     * scale with its boss level (on top of the flat {@code stat_multipliers}),
     * and ensures the nameplate, boss bar, and kill-XP all use the boss level.
     *
     * <p>The level honors {@code ignore_level_cap}: when false it is clamped to
     * the global level cap, matching how normal leveled mobs are capped.
     */
    private void applyBossLevel(Mob mob, BossRule rule, ServerLevel level) {
        int bossLevel = rule.getLevel();
        if (!rule.isIgnoreLevelCap()) {
            bossLevel = Math.min(bossLevel, MobLevelingConfig.GLOBAL_LEVEL_CAP.get());
        }

        MobLevelData.setLevel(mob, bossLevel);
        MobLevelData.setIgnoreLevelCap(mob, rule.isIgnoreLevelCap());
        MobLevelData.markProcessed(mob);

        // Re-derive the attribute scaling from the rule the mob was originally
        // leveled by (stored in NBT during normal spawn handling) and reapply it
        // at the boss level. Falls back to a fresh resolve if the source rule is
        // unavailable. Wrapped defensively so a scaling failure never aborts the
        // boss transformation.
        try {
            Map<ResourceLocation, AttributeScaling> scaling = Collections.emptyMap();

            var ruleIdOpt = MobLevelData.getSourceRuleId(mob);
            var ruleTypeOpt = MobLevelData.getSourceRuleType(mob);
            if (ruleIdOpt.isPresent() && ruleTypeOpt.isPresent()) {
                LevelRule sourceRule = levelResolver.findRuleById(ruleIdOpt.get(), ruleTypeOpt.get());
                if (sourceRule != null) {
                    scaling = sourceRule.getAttributeScaling();
                }
            }

            if (!scaling.isEmpty()) {
                attributeManager.applyScaling(mob, bossLevel, scaling);
            }
        } catch (Exception e) {
            if (MobLevelingConfig.DEBUG_MODE.get()) {
                LOGGER.warn("[BossManager] Failed to reapply level scaling for boss {}: {}",
                        rule.getId(), e.getMessage());
            }
        }
    }

    private void applyStatMultipliers(Mob mob, BossRule rule) {
        Map<ResourceLocation, Double> multipliers = rule.getStatMultipliers();

        for (Map.Entry<ResourceLocation, Double> entry : multipliers.entrySet()) {
            ResourceLocation attrId = entry.getKey();
            double multiplier = entry.getValue();

            var attribute = ForgeRegistries.ATTRIBUTES.getValue(attrId);
            if (attribute != null) {
                var instance = mob.getAttribute(attribute);
                if (instance != null) {
                    double baseValue = instance.getBaseValue();
                    instance.setBaseValue(baseValue * multiplier);
                }
            }
        }

        // Heal to max health after stat changes
        mob.setHealth(mob.getMaxHealth());
    }

    // ==================== Boss Bars ====================

    /**
     * Recreate the in-memory boss bar for a boss that was loaded from disk (chunk reload
     * or server restart). The {@code activeBossBars} map lives only in memory, so without
     * this a saved boss would permanently lose its bar — {@link #updateBossBar} silently
     * no-ops when the bar is missing. Safe to call every tick; it only acts when the bar
     * is actually absent.
     */
    public void ensureBossBar(Mob mob) {
        if (!MobLevelingConfig.BOSS_SHOW_BOSS_BAR.get()) {
            return;
        }

        UUID bossId = BossData.getBossUUID(mob);
        if (bossId == null || activeBossBars.containsKey(bossId)) {
            return;
        }

        BossRule rule = getBossRule(mob);
        if (rule == null || !rule.getBossBar().isVisible()) {
            return;
        }

        createBossBar(mob, rule);
    }

    private void createBossBar(Mob mob, BossRule rule) {
        UUID bossId = BossData.getBossUUID(mob);
        if (bossId == null) {
            return;
        }

        Component displayName = Component.literal(rule.getDisplayName());
        BossRule.BossBarProperties barProps = rule.getBossBar();

        ServerBossEvent bossBar = new ServerBossEvent(
                displayName,
                barProps.getColor(),
                barProps.getStyle()
        );
        bossBar.setProgress(1.0f);

        activeBossBars.put(bossId, bossBar);
        bossPlayerTracking.put(bossId, ConcurrentHashMap.newKeySet());
    }

    public void updateBossBar(Mob mob) {
        UUID bossId = BossData.getBossUUID(mob);
        if (bossId == null) {
            return;
        }

        ServerBossEvent bossBar = activeBossBars.get(bossId);
        if (bossBar == null) {
            return;
        }

        // Update health progress
        float healthPercent = mob.getHealth() / mob.getMaxHealth();
        bossBar.setProgress(Math.max(0, Math.min(1, healthPercent)));
    }

    public void addPlayerToBossBar(Mob boss, ServerPlayer player) {
        UUID bossId = BossData.getBossUUID(boss);
        if (bossId == null) {
            return;
        }

        ServerBossEvent bossBar = activeBossBars.get(bossId);
        if (bossBar == null) {
            return;
        }

        Set<ServerPlayer> players = bossPlayerTracking.get(bossId);
        if (players != null && !players.contains(player)) {
            bossBar.addPlayer(player);
            players.add(player);
        }
    }

    public void removePlayerFromBossBar(Mob boss, ServerPlayer player) {
        UUID bossId = BossData.getBossUUID(boss);
        if (bossId == null) {
            return;
        }

        ServerBossEvent bossBar = activeBossBars.get(bossId);
        if (bossBar == null) {
            return;
        }

        Set<ServerPlayer> players = bossPlayerTracking.get(bossId);
        if (players != null) {
            bossBar.removePlayer(player);
            players.remove(player);
        }
    }

    public void removeBossBar(Mob mob) {
        UUID bossId = BossData.getBossUUID(mob);
        if (bossId == null) {
            return;
        }

        ServerBossEvent bossBar = activeBossBars.remove(bossId);
        if (bossBar != null) {
            bossBar.removeAllPlayers();
        }

        bossPlayerTracking.remove(bossId);
        bossMinions.remove(bossId);
        lastMinionSpawnTime.remove(bossId);
    }

    // ==================== Minion Management ====================

    public void tickMinions(Mob boss, ServerLevel level) {
        if (!BossData.isBoss(boss)) {
            return;
        }

        BossRule rule = getBossRule(boss);
        if (rule == null || rule.getMinionConfig() == null) {
            return;
        }

        BossRule.MinionConfig minionConfig = rule.getMinionConfig();
        UUID bossId = BossData.getBossUUID(boss);
        if (bossId == null) {
            return;
        }

        // Check health threshold
        float healthPercent = boss.getHealth() / boss.getMaxHealth();
        if (healthPercent > minionConfig.getHealthThreshold()) {
            return;
        }

        // Check spawn interval
        long currentTime = level.getGameTime();
        long lastSpawn = lastMinionSpawnTime.getOrDefault(bossId, 0L);
        long intervalTicks = minionConfig.getIntervalSeconds() * 20L;

        if (currentTime - lastSpawn < intervalTicks) {
            return;
        }

        // Check max minions
        Set<UUID> minions = bossMinions.computeIfAbsent(bossId, k -> ConcurrentHashMap.newKeySet());

        // Clean up dead minions
        minions.removeIf(minionId -> {
            var entity = level.getEntity(minionId);
            return entity == null || !entity.isAlive();
        });

        if (minions.size() >= minionConfig.getMaxMinions()) {
            return;
        }

        // Spawn minions
        spawnMinions(boss, minionConfig, level, minions);
        lastMinionSpawnTime.put(bossId, currentTime);
    }

    private void spawnMinions(Mob boss, BossRule.MinionConfig config, ServerLevel level, Set<UUID> minions) {
        var entityType = ForgeRegistries.ENTITY_TYPES.getValue(config.getMinionType());
        if (entityType == null) {
            return;
        }

        for (int i = 0; i < config.getCount(); i++) {
            if (minions.size() >= config.getMaxMinions()) {
                break;
            }

            // Spawn at random offset from boss
            double offsetX = (boss.getRandom().nextDouble() - 0.5) * 4;
            double offsetZ = (boss.getRandom().nextDouble() - 0.5) * 4;

            var minion = entityType.create(level);
            if (minion != null) {
                minion.setPos(boss.getX() + offsetX, boss.getY(), boss.getZ() + offsetZ);
                // Flag as a minion BEFORE adding to the world. addFreshEntity fires
                // EntityJoinLevel synchronously, where the boss handler would otherwise
                // be able to transform this minion into another boss.
                BossData.markAsMinion(minion);
                level.addFreshEntity(minion);
                minions.add(minion.getUUID());

                if (MobLevelingConfig.DEBUG_MODE.get()) {
                    LOGGER.debug("[BossManager] Spawned minion {} for boss {}",
                            config.getMinionType(), BossData.getBossUUID(boss));
                }
            }
        }
    }

    // ==================== Utility ====================

    @Nullable
    private ResourceLocation getCurrentBiome(ServerLevel level, BlockPos pos) {
        try {
            Holder<Biome> biome = level.getBiome(pos);
            return biome.unwrapKey()
                    .map(key -> key.location())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private ResourceLocation getCurrentStructure(ServerLevel level, BlockPos pos) {
        try {
            var structureManager = level.structureManager();
            var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);

            // Only consider structures that actually reference this position, instead of
            // scanning the entire structure registry for every mob spawn.
            for (Structure structure : structureManager.getAllStructuresAt(pos).keySet()) {
                StructureStart start = structureManager.getStructureWithPieceAt(pos, structure);
                if (start.isValid()) {
                    return registry.getKey(structure);
                }
            }
        } catch (Exception e) {
            // Ignore - structure lookup can fail during world gen
        }
        return null;
    }

    @Nullable
    public BossRule getBossRule(Mob mob) {
        var ruleIdOpt = BossData.getBossRuleId(mob);
        if (ruleIdOpt.isEmpty()) {
            return null;
        }

        MobLevelingDataManager dataManager = MobLevelingDataManager.getInstance();
        if (dataManager == null) {
            return null;
        }

        return dataManager.getBossRule(ruleIdOpt.get());
    }

    private void announceSpawn(Mob mob, BossRule rule, ServerLevel level) {
        int radius = MobLevelingConfig.BOSS_ANNOUNCEMENT_RADIUS.get();
        BlockPos pos = mob.blockPosition();

        Component message = Component.literal("§4§l[!] " + rule.getDisplayName() + " §r§chas appeared!");

        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().closerThan(pos, radius)) {
                player.sendSystemMessage(message);
            }
        }
    }

    public void cleanup() {
        for (ServerBossEvent bossBar : activeBossBars.values()) {
            bossBar.removeAllPlayers();
        }
        activeBossBars.clear();
        bossPlayerTracking.clear();
        bossMinions.clear();
        lastMinionSpawnTime.clear();
    }

    public boolean isActiveBoss(UUID bossId) {
        return activeBossBars.containsKey(bossId);
    }

    public int getActiveBossCount() {
        return activeBossBars.size();
    }
}
