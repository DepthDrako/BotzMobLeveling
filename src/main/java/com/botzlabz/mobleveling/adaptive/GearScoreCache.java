package com.botzlabz.mobleveling.adaptive;

import com.eidolonreach.eidolon_lib.async.EidolonAsync;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Async-populated cache of player gear scores.
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>{@link com.botzlabz.mobleveling.event.PlayerGearWatcher} calls
 *       {@link #updateAsync} whenever a player's equipment changes or they log in.</li>
 *   <li>The scoring work runs on the {@link EidolonAsync} thread pool — it only
 *       touches the already-copied {@link ItemStack} snapshots, never live world
 *       state.</li>
 *   <li>{@link DifficultyCalculator} calls {@link #getScore} synchronously on the
 *       main thread during mob spawn — instant, no blocking.</li>
 * </ol>
 *
 * <p>If a player's score hasn't been computed yet (e.g. they spawned this exact
 * tick), the returned score is 0.0 and a fresh computation is triggered.  The
 * next mob spawn will use the cached result.
 */
public final class GearScoreCache {

    private static final Map<UUID, Double> CACHE = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------
    // Read (main thread — synchronous, instant)
    // ------------------------------------------------------------------

    /**
     * Returns the last computed gear score for this player UUID,
     * or {@code 0.0} if no score has been cached yet.
     */
    public static double getScore(UUID playerUUID) {
        return CACHE.getOrDefault(playerUUID, 0.0);
    }

    // ------------------------------------------------------------------
    // Write (triggered on main thread, computed on EidolonAsync pool)
    // ------------------------------------------------------------------

    /**
     * Snapshots the player's gear <em>on the calling (main) thread</em>,
     * then submits the actual scoring calculation to the {@link EidolonAsync}
     * pool.  The result is written back to {@link #CACHE} on the main thread
     * via the server's task queue.
     *
     * <p>Safe to call every equipment change — the async pool is light and
     * the snapshots are cheap copies.
     */
    public static void updateAsync(ServerPlayer player, MinecraftServer server) {
        UUID           uuid    = player.getUUID();
        List<ItemStack> armor  = GearAnalyzer.snapshotArmor(player);
        ItemStack       weapon = GearAnalyzer.snapshotWeapon(player);
        ItemStack       offhand = GearAnalyzer.snapshotOffhand(player);

        EidolonAsync.submit(
            () -> GearAnalyzer.scoreFromSnapshots(armor, weapon, offhand),
            score -> CACHE.put(uuid, score),
            server
        );
    }

    // ------------------------------------------------------------------
    // Eviction
    // ------------------------------------------------------------------

    /** Remove a player's score when they log out. */
    public static void evict(UUID playerUUID) {
        CACHE.remove(playerUUID);
    }

    /** Clears all cached scores (called on server stop). */
    public static void clear() {
        CACHE.clear();
    }

    private GearScoreCache() {}
}
