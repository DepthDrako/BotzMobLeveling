package com.botzlabz.mobleveling.event;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.adaptive.GearScoreCache;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Keeps {@link GearScoreCache} up-to-date using eidolon_lib's async pool.
 *
 * <ul>
 *   <li>Player logs in → initial score computed.</li>
 *   <li>Player equips / unequips anything → score recomputed.</li>
 *   <li>Player logs out → score evicted from cache.</li>
 * </ul>
 *
 * <p>All actual gear-scoring work runs off the main thread via
 * {@link com.eidolonreach.eidolon_lib.async.EidolonAsync}.
 */
@EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class PlayerGearWatcher {

    /** Initial score when a player joins the world. */
    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        GearScoreCache.updateAsync(player, server);
    }

    /** Re-score whenever any equipment slot changes (armor, hand, offhand). */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        GearScoreCache.updateAsync(player, server);
    }

    /** Evict the cached score when a player logs out. */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GearScoreCache.evict(event.getEntity().getUUID());
    }

    private PlayerGearWatcher() {}
}
