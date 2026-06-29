package com.botzlabz.mobleveling.compat;

import com.botzlabz.mobleveling.level.MobLevelCapability;
import com.botzlabz.mobleveling.level.MobLevelData;
import com.eidolonreach.eidolon_lib.api.IStatHolder;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

/**
 * Internal convenience accessor for this mob's {@link IStatHolder}.
 *
 * <p>External mods must NOT call this (they have no dependency on this mod).
 * The supported cross-mod path is eidolon_lib's capability token:
 * {@code entity.getCapability(EidolonLibCapabilities.STAT_HOLDER)} — registered
 * by {@link com.botzlabz.mobleveling.BotzMobLeveling#registerCapabilities}.
 */
public final class EidolonAIIntegration {

    public static final boolean EIDOLON_AI_LOADED =
        ModList.get().isLoaded("eidolon_ai");

    public static IStatHolder getStatHolder(LivingEntity entity) {
        MobLevelData data = MobLevelCapability.get(entity);
        return data != null ? data.getStatBlock() : null;
    }

    private EidolonAIIntegration() {}
}
