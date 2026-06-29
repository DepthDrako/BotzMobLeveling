package com.botzlabz.mobleveling.compat;

import net.neoforged.fml.ModList;

/**
 * Hooks for Iron's Spells 'n Spellbooks mana system.
 * Mana Pool and Mana Density stats are always stored in the capability NBT.
 * This class gates any ISS-specific API calls behind a ModList check.
 */
public final class ISSIntegration {

    public static final boolean ISS_LOADED =
        ModList.get().isLoaded("irons_spells_n_spellbooks");

    /**
     * Apply mana_pool stat to ISS mana cap on the entity.
     * No-op if ISS is not loaded.
     */
    public static void applyManaPool(net.minecraft.world.entity.LivingEntity entity, float manaPool) {
        if (!ISS_LOADED) return;
        // ISS API call goes here once ISS 1.21.1 API is confirmed.
        // Pattern: ISSMagicData.getPlayerMagicData(entity).setMaxMana((int) manaPool);
    }

    /**
     * Apply mana_density stat to ISS spell damage multiplier.
     * No-op if ISS is not loaded.
     */
    public static void applyManaDensity(net.minecraft.world.entity.LivingEntity entity, float manaDensity) {
        if (!ISS_LOADED) return;
        // ISS API call goes here once ISS spell damage hook is confirmed.
    }

    private ISSIntegration() {}
}
