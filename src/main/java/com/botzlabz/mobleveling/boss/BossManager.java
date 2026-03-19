package com.botzlabz.mobleveling.boss;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.data.MobLevelingDataManager;
import com.botzlabz.mobleveling.kills.HuntingGoalHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
        BossData.setOriginalMaxHealth(mob, mob.getMaxHealth());

        // Prevent despawning
        if (MobLevelingConfig.BOSS_PREVENT_DESPAWN.get()) {
            mob.setPersistenceRequired();
        }

        // Apply size multiplier
        if (rule.getSizeMultiplier() != 1.0f) {
            // Note: Size scaling requires special handling via attributes or rendering
            // For now we'll store it in NBT for the visual manager to use
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

            for (var entry : registry.entrySet()) {
                Structure structure = entry.getValue();
                StructureStart start = structureManager.getStructureWithPieceAt(pos, structure);
                if (start.isValid()) {
                    return entry.getKey().location();
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
