package com.botzlabz.mobleveling.compat;

import net.neoforged.fml.ModList;

/**
 * Epic Fight optional integration.
 * Placeholder — verify EF 1.21.1 NeoForge API event names before implementing.
 */
public final class EpicFightIntegration {

    public static final boolean EPIC_FIGHT_LOADED =
        ModList.get().isLoaded("epicfight");

    public static void init() {
        if (!EPIC_FIGHT_LOADED) return;
        // Register EF-specific event listeners here once EF 1.21.1 API is confirmed.
    }

    private EpicFightIntegration() {}
}
