package com.botzlabz.mobleveling.boss;

import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.level.MobLevelData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks active boss bars. One bar per boss UUID.
 *
 * <p>Each tracked boss carries a <b>visibility range</b> (from the boss rule's
 * {@code boss_bar_range}, persisted on the mob):
 * <ul>
 *   <li><b>range ≤ 0 → global</b>: every online player sees the bar (legacy behaviour).</li>
 *   <li><b>range &gt; 0 → local</b>: only players within that many blocks (same
 *       dimension) see it; membership is maintained each tick by {@link #tickBars}.</li>
 * </ul>
 */
public final class BossManager {

    /** One tracked boss: its bar, the live mob (for distance checks) and visibility range. */
    private record TrackedBoss(Mob mob, ServerBossEvent bar, int range) {
        boolean isLocal() { return range > 0; }
    }

    private static final Map<UUID, TrackedBoss> tracked = new HashMap<>();

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /** Registers the boss bar and broadcasts the spawn announcement (initial spawn). */
    public static void registerBoss(Mob mob, MobLevelData data, ServerLevel level) {
        registerBoss(mob, data, level, true);
    }

    /**
     * @param announce whether to broadcast the spawn message. False on reload
     *                 re-registration so the announcement isn't repeated every time
     *                 the boss's chunk reloads.
     */
    public static void registerBoss(Mob mob, MobLevelData data, ServerLevel level, boolean announce) {
        if (!MobLevelingConfig.BOSS_ENABLED.get() || !MobLevelingConfig.BOSS_BAR_ENABLED.get()) return;

        String title = data.getBossBarTitle().isEmpty()
            ? mob.getType().getDescription().getString()
            : data.getBossBarTitle();

        Component name = Component.literal(title);
        BossEvent.BossBarColor color = parseColor(data.getBossBarColor());
        ServerBossEvent bar = new ServerBossEvent(name, color, BossEvent.BossBarOverlay.PROGRESS);
        bar.setProgress(1.0f);
        bar.setVisible(true);

        int range = data.getBossBarRange();
        tracked.put(mob.getUUID(), new TrackedBoss(mob, bar, range));

        if (range <= 0) {
            // Global: add every online player.
            level.getServer().getPlayerList().getPlayers().forEach(bar::addPlayer);
        } else {
            // Local: add only players currently in range; tickBars maintains it.
            double r2 = (double) range * range;
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                if (p.level().dimension().equals(level.dimension()) && p.distanceToSqr(mob) <= r2) {
                    bar.addPlayer(p);
                }
            }
        }

        // Announce spawn (only on initial spawn, never on reload re-registration)
        if (announce && MobLevelingConfig.BOSS_ANNOUNCE_SPAWN.get()) {
            String msg = data.getBossAnnouncement().isEmpty()
                ? "§4⚔ A powerful " + title + " (Lv." + data.getTotalLevel() + ") has appeared!"
                : data.getBossAnnouncement();
            Component announcement = Component.literal(msg);
            int radius = MobLevelingConfig.BOSS_ANNOUNCE_RADIUS.get();
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                if (player.level().dimension().equals(level.dimension())
                        && player.distanceTo(mob) <= radius) {
                    player.sendSystemMessage(announcement);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Updates
    // -------------------------------------------------------------------------

    public static void updateHealth(UUID uuid, float current, float max) {
        TrackedBoss tb = tracked.get(uuid);
        if (tb == null) return;
        tb.bar.setProgress(max <= 0 ? 0 : Math.max(0, Math.min(1, current / max)));
    }

    /**
     * Per-tick maintenance of local boss bars: adds players who entered the radius
     * and removes those who left (or changed dimension). Global bars are untouched.
     * Cheap no-op when there are no local bosses.
     */
    public static void tickBars(MinecraftServer server) {
        if (tracked.isEmpty()) return;
        for (TrackedBoss tb : tracked.values()) {
            if (!tb.isLocal()) continue;
            Mob mob = tb.mob;
            if (mob == null || mob.isRemoved()) continue; // death/leave events evict it
            var dim = mob.level().dimension();
            double r2 = (double) tb.range * tb.range;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                boolean inRange = p.level().dimension().equals(dim) && p.distanceToSqr(mob) <= r2;
                boolean member  = tb.bar.getPlayers().contains(p);
                if (inRange && !member)      tb.bar.addPlayer(p);
                else if (!inRange && member) tb.bar.removePlayer(p);
            }
        }
    }

    /** Adds a (re)joining player to every <b>global</b> bar; local bars are handled by {@link #tickBars}. */
    public static void addPlayerToGlobalBars(ServerPlayer player) {
        for (TrackedBoss tb : tracked.values()) {
            if (!tb.isLocal()) tb.bar.addPlayer(player);
        }
    }

    // -------------------------------------------------------------------------
    // Removal
    // -------------------------------------------------------------------------

    public static void removeBoss(UUID uuid) {
        TrackedBoss tb = tracked.remove(uuid);
        if (tb == null) return;
        tb.bar.setVisible(false);
        // Remove from all players
        new java.util.ArrayList<>(tb.bar.getPlayers()).forEach(tb.bar::removePlayer);
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    public static boolean isBossTracked(UUID uuid) {
        return tracked.containsKey(uuid);
    }

    /** Package-visible: returns the set of all currently tracked boss UUIDs. */
    static java.util.Set<UUID> activeUUIDs() {
        return tracked.keySet();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static BossEvent.BossBarColor parseColor(String s) {
        return switch (s.toLowerCase()) {
            case "blue"   -> BossEvent.BossBarColor.BLUE;
            case "green"  -> BossEvent.BossBarColor.GREEN;
            case "yellow" -> BossEvent.BossBarColor.YELLOW;
            case "purple" -> BossEvent.BossBarColor.PURPLE;
            case "white"  -> BossEvent.BossBarColor.WHITE;
            case "pink"   -> BossEvent.BossBarColor.PINK;
            default       -> BossEvent.BossBarColor.RED;
        };
    }

    private BossManager() {}
}
