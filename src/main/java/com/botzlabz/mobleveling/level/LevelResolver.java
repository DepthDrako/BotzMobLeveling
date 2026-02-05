package com.botzlabz.mobleveling.level;

import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.data.*;
import com.botzlabz.mobleveling.util.ModConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

public class LevelResolver {

    private static final Logger LOGGER = LogUtils.getLogger();

    private enum RuleTier {
        STRUCTURE(0),
        BIOME(1),
        BASE(2);

        private final int order;

        RuleTier(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    private record ApplicableRule(LevelRule rule, RuleTier tier) {
        public int getTierOrder() {
            return tier.getOrder();
        }
    }

    public LevelResult resolve(Mob mob, BlockPos pos, ServerLevel level) {
        MobLevelingDataManager dataManager = MobLevelingDataManager.getInstance();

        // Check basic requirements (enabled, blacklist)
        if (!shouldProcessMobBasic(mob)) {
            return LevelResult.SKIP;
        }

        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (mobId == null) {
            return LevelResult.SKIP;
        }

        // Determine if this is a passive or boss mob (for filter override logic)
        boolean isPassiveMob = mob instanceof Animal || mob instanceof AbstractVillager;
        boolean isBossMob = mob instanceof EnderDragon || mob instanceof WitherBoss || mob.getType().is(Tags.EntityTypes.BOSSES);

        // Check if mob would be filtered out by passive/boss settings
        boolean wouldBeFilteredByPassive = isPassiveMob && !MobLevelingConfig.APPLY_TO_PASSIVE_MOBS.get();
        boolean wouldBeFilteredByBoss = isBossMob && !MobLevelingConfig.APPLY_TO_BOSS_MOBS.get();

        // Gather all applicable rules
        List<ApplicableRule> applicableRules = new ArrayList<>();
        StructureRule structureRule = null;
        boolean hasStructureRuleForThisMob = false;

        // Check structure rules first (highest priority tier)
        // Structure rules can override passive/boss filters if configured
        if (MobLevelingConfig.STRUCTURE_LEVELING_ENABLED.get() && dataManager != null) {
            structureRule = findStructureRule(pos, level, dataManager);
            if (structureRule != null && structureRule.isEnabled()) {
                applicableRules.add(new ApplicableRule(structureRule, RuleTier.STRUCTURE));
                // Check if this structure rule has an explicit mob override for this mob
                hasStructureRuleForThisMob = structureRule.getMobOverride(mobId) != null;
            }
        }

        // If mob would be filtered and there's no structure rule with explicit override for this mob, skip
        // The key change: passive/boss mobs only get through if they have an explicit mob_overrides entry
        if (wouldBeFilteredByPassive) {
            boolean canOverride = hasStructureRuleForThisMob && MobLevelingConfig.STRUCTURE_OVERRIDES_PASSIVE_FILTER.get();
            if (!canOverride) {
                return LevelResult.SKIP;
            }
        }

        if (wouldBeFilteredByBoss) {
            boolean canOverride = hasStructureRuleForThisMob && MobLevelingConfig.STRUCTURE_OVERRIDES_BOSS_FILTER.get();
            if (!canOverride) {
                return LevelResult.SKIP;
            }
        }

        if (dataManager == null) {
            // No data loaded, use defaults
            return resolveWithDefaults(mob, pos, level);
        }

        // Check biome rules (medium priority tier)
        if (MobLevelingConfig.BIOME_LEVELING_ENABLED.get()) {
            BiomeRule biomeRule = findBiomeRule(pos, level, dataManager);
            if (biomeRule != null && biomeRule.isEnabled()) {
                applicableRules.add(new ApplicableRule(biomeRule, RuleTier.BIOME));
            }
        }

        // Check base rules (lowest priority tier)
        List<BaseRule> matchingBaseRules = findApplicableBaseRules(mob, mobId, dataManager);
        for (BaseRule baseRule : matchingBaseRules) {
            if (baseRule.isEnabled()) {
                applicableRules.add(new ApplicableRule(baseRule, RuleTier.BASE));
            }
        }

        // Sort by tier (lower order = higher priority), then by rule priority within tier
        applicableRules.sort(Comparator
                .comparingInt(ApplicableRule::getTierOrder)
                .thenComparingInt(r -> -r.rule().getPriority()));

        if (applicableRules.isEmpty()) {
            return resolveWithDefaults(mob, pos, level);
        }

        // Select winning rule (first in sorted list)
        ApplicableRule winningRule = applicableRules.get(0);
        LevelRule rule = winningRule.rule();

        // Check for mob-specific override
        MobOverride override = rule.getMobOverride(mobId);

        // Calculate level
        LevelCalculator calculator = new LevelCalculator(mob.getRandom());
        int calculatedLevel = calculator.calculateLevel(rule, override, pos, level);

        // Determine if we should ignore global cap
        // Ignore cap if: mob override says so, OR if using fixed level mode (explicit level intent)
        boolean isFixedLevel = (override != null && override.hasFixedLevel()) ||
                               (rule.getFixedLevel() != null && rule.getLevelMode().equals(ModConstants.LEVEL_MODE_FIXED));
        boolean ignoreCap = (override != null && override.isIgnoreLevelCap()) || isFixedLevel;

        // Apply global cap unless explicitly ignored
        int finalLevel = calculator.applyGlobalCap(calculatedLevel, ignoreCap);

        // Merge attribute scaling (override can add/modify)
        Map<ResourceLocation, AttributeScaling> mergedScaling = mergeAttributeScaling(rule, override);

        if (MobLevelingConfig.DEBUG_MODE.get()) {
            LOGGER.debug("[MobLeveling] {} at {} -> Level {} (rule: {}, type: {})",
                    mobId, pos, finalLevel, rule.getId(), rule.getRuleType());
        }

        return LevelResult.builder(finalLevel)
                .sourceRule(rule)
                .mobOverride(override)
                .attributeScaling(mergedScaling)
                .ignoreLevelCap(ignoreCap)
                .build();
    }

    /**
     * Basic mob processing check - only checks enabled state and blacklist.
     * Passive/boss filtering is handled separately to allow structure overrides.
     */
    private boolean shouldProcessMobBasic(Mob mob) {
        if (!MobLevelingConfig.ENABLED.get()) {
            return false;
        }

        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (mobId == null) {
            return false;
        }

        // Check blacklist - this is always respected, no overrides
        List<? extends String> blacklist = MobLevelingConfig.MOB_BLACKLIST.get();
        if (blacklist.contains(mobId.toString())) {
            return false;
        }

        return true;
    }

    @Nullable
    private StructureRule findStructureRule(BlockPos pos, ServerLevel level, MobLevelingDataManager dataManager) {
        // Skip structure checks during world generation to prevent hangs
        // Structure lookups are extremely expensive before the world is fully loaded
        if (!isWorldReady(level)) {
            return null;
        }

        try {
            var structureManager = level.structureManager();

            for (StructureRule rule : dataManager.getStructureRules()) {
                ResourceLocation structureId = rule.getStructureId();

                Optional<Structure> structure = level.registryAccess()
                        .registryOrThrow(Registries.STRUCTURE)
                        .getOptional(structureId);

                if (structure.isPresent()) {
                    StructureStart start = structureManager.getStructureWithPieceAt(pos, structure.get());
                    if (start.isValid()) {
                        return rule;
                    }
                }
            }
        } catch (Exception e) {
            // Structure manager may not be ready during world gen
            if (MobLevelingConfig.DEBUG_MODE.get()) {
                LOGGER.debug("Structure lookup failed (world may be generating): {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * Check if the world is ready for structure lookups.
     * During world generation, structure manager operations can be very expensive or cause hangs.
     */
    private boolean isWorldReady(ServerLevel level) {
        try {
            // Check if the server is still starting up
            var server = level.getServer();
            if (server == null) {
                return false;
            }

            // Check if server is running (not just starting)
            if (!server.isRunning()) {
                return false;
            }

            // Check if the world is still being created
            // During world creation, getGameTime() is 0 or very low
            // Use a longer delay (300 ticks = 15 seconds) for structure lookups
            // as these are particularly expensive during world gen
            if (level.getGameTime() < 300) {
                return false;
            }

            // Additional check: at least one player should be in the game
            if (server.getPlayerList().getPlayerCount() == 0) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    private BiomeRule findBiomeRule(BlockPos pos, ServerLevel level, MobLevelingDataManager dataManager) {
        Holder<Biome> biome = level.getBiome(pos);
        ResourceLocation biomeId = biome.unwrapKey()
                .map(ResourceKey::location)
                .orElse(null);

        if (biomeId == null) {
            return null;
        }

        // Check direct biome match first
        BiomeRule directMatch = dataManager.getBiomeRule(biomeId);
        if (directMatch != null) {
            return directMatch;
        }

        // Check biome tag matches (already sorted by priority)
        for (BiomeRule rule : dataManager.getBiomeTagRules()) {
            for (TagKey<Biome> tag : rule.getBiomeTags()) {
                if (biome.is(tag)) {
                    return rule;
                }
            }
        }

        return null;
    }

    private List<BaseRule> findApplicableBaseRules(Mob mob, ResourceLocation mobId, MobLevelingDataManager dataManager) {
        List<BaseRule> matching = new ArrayList<>();
        EntityType<?> entityType = mob.getType();

        for (BaseRule rule : dataManager.getBaseRules()) {
            // Check exclusions first
            if (rule.getExcludedMobIds().contains(mobId)) {
                continue;
            }

            boolean excludedByTag = false;
            for (TagKey<EntityType<?>> tag : rule.getExcludedMobTags()) {
                if (entityType.is(tag)) {
                    excludedByTag = true;
                    break;
                }
            }
            if (excludedByTag) {
                continue;
            }

            // Check if mob matches inclusion criteria
            boolean matches = false;

            // Check specific mob IDs
            if (rule.getMobIds().contains(mobId)) {
                matches = true;
            }

            // Check mob tags
            if (!matches) {
                for (TagKey<EntityType<?>> tag : rule.getMobTags()) {
                    if (entityType.is(tag)) {
                        matches = true;
                        break;
                    }
                }
            }

            // Check mob types
            if (!matches && !rule.getMobTypes().isEmpty()) {
                for (String mobType : rule.getMobTypes()) {
                    if (matchesMobType(mob, mobType)) {
                        matches = true;
                        break;
                    }
                }
            }

            // If no specific inclusion criteria, it's a catch-all (matches all)
            if (!matches && rule.getMobIds().isEmpty() && rule.getMobTags().isEmpty() && rule.getMobTypes().isEmpty()) {
                matches = true;
            }

            if (matches) {
                matching.add(rule);
            }
        }

        return matching;
    }

    private boolean matchesMobType(Mob mob, String mobType) {
        return switch (mobType.toLowerCase()) {
            case ModConstants.MOB_TYPE_HOSTILE -> mob.getType().getCategory().isFriendly() == false;
            case ModConstants.MOB_TYPE_PASSIVE -> mob instanceof Animal || mob instanceof AbstractVillager;
            case ModConstants.MOB_TYPE_BOSS -> mob instanceof EnderDragon || mob instanceof WitherBoss || mob.getType().is(Tags.EntityTypes.BOSSES);
            case ModConstants.MOB_TYPE_ALL -> true;
            default -> false;
        };
    }

    private LevelResult resolveWithDefaults(Mob mob, BlockPos pos, ServerLevel level) {
        int defaultLevel = LevelCalculator.getDefaultLevel(pos, level);
        int finalLevel = Math.min(defaultLevel, MobLevelingConfig.GLOBAL_LEVEL_CAP.get());

        return LevelResult.builder(finalLevel)
                .attributeScaling(Collections.emptyMap())
                .build();
    }

    private Map<ResourceLocation, AttributeScaling> mergeAttributeScaling(LevelRule rule, @Nullable MobOverride override) {
        Map<ResourceLocation, AttributeScaling> merged = new HashMap<>(rule.getAttributeScaling());

        if (override != null) {
            // Apply multipliers to existing scaling
            for (Map.Entry<ResourceLocation, Double> entry : override.getAttributeMultipliers().entrySet()) {
                ResourceLocation attrId = entry.getKey();
                double multiplier = entry.getValue();

                if (merged.containsKey(attrId)) {
                    AttributeScaling original = merged.get(attrId);
                    merged.put(attrId, original.withMultiplier(multiplier));
                }
            }

            // Add/override with custom scaling
            merged.putAll(override.getCustomScaling());
        }

        return merged;
    }

    /**
     * Find a rule by its ID and type.
     * Used for reapplying modifiers to loaded mobs.
     */
    @Nullable
    public LevelRule findRuleById(ResourceLocation ruleId, String ruleType) {
        MobLevelingDataManager dataManager = MobLevelingDataManager.getInstance();
        if (dataManager == null) {
            return null;
        }

        switch (ruleType) {
            case ModConstants.RULE_TYPE_STRUCTURE:
                for (StructureRule rule : dataManager.getStructureRules()) {
                    if (ruleId.equals(rule.getId())) {
                        return rule;
                    }
                }
                break;
            case ModConstants.RULE_TYPE_BIOME:
                for (BiomeRule rule : dataManager.getBiomeRules()) {
                    if (ruleId.equals(rule.getId())) {
                        return rule;
                    }
                }
                break;
            case ModConstants.RULE_TYPE_BASE:
                for (BaseRule rule : dataManager.getBaseRules()) {
                    if (ruleId.equals(rule.getId())) {
                        return rule;
                    }
                }
                break;
        }

        return null;
    }
}
