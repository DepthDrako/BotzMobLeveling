package com.botzlabz.mobleveling.level;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session cache backing {@link com.botzlabz.mobleveling.BotzMobLeveling#MOB_LEVEL_CAP}.
 *
 * <p><b>Why this exists:</b> NeoForge invokes entity capability providers on EVERY
 * {@code getCapability} call — there is no built-in instance caching.  A provider
 * that returns {@code new MobLevelData()} hands every caller a fresh, blank object,
 * so mutations made by one handler are invisible to all others and persistence
 * silently breaks.  This store guarantees one shared instance per entity per
 * session, hydrated from the entity's persistent NBT on first access.
 *
 * <p><b>Lifecycle:</b>
 * <ul>
 *   <li>First query → hydrate from {@code entity.getPersistentData()} if present</li>
 *   <li>Entity leaves level → {@link #flushAndEvict} writes back and drops the entry</li>
 *   <li>Server stops → {@link #clear}</li>
 * </ul>
 *
 * <p>Server-side only.  The capability provider returns {@code null} on the client,
 * so client entities (which share UUIDs with server entities in singleplayer) can
 * never poison the cache.
 */
public final class MobLevelStore {

    private static final ConcurrentHashMap<UUID, MobLevelData> CACHE = new ConcurrentHashMap<>();

    private MobLevelStore() {}

    /**
     * Returns the cached data for this mob, creating and hydrating it from
     * persistent NBT on first access.
     */
    public static MobLevelData getOrCreate(Mob mob) {
        return CACHE.computeIfAbsent(mob.getUUID(), id -> {
            MobLevelData data = new MobLevelData();
            CompoundTag pd = mob.getPersistentData();
            if (pd.contains(MobLevelData.NBT_KEY)) {
                data.load(pd.getCompound(MobLevelData.NBT_KEY));
            }
            return data;
        });
    }

    /**
     * Writes the data to the mob's persistent NBT.  Also mirrors the total level
     * to the {@code BML_Level} int that eidolon_ai reads as a brain input —
     * keep both in sync on every save.
     */
    public static void persist(Mob mob, MobLevelData data) {
        mob.getPersistentData().put(MobLevelData.NBT_KEY, data.save());
        mob.getPersistentData().putInt("BML_Level", data.getTotalLevel());
    }

    /**
     * Flushes the cached data to persistent NBT and removes the cache entry.
     * Called when the entity leaves the level (chunk unload, death, despawn,
     * dimension transfer).  Rejoin re-hydrates from NBT via {@link #getOrCreate}.
     */
    public static void flushAndEvict(Mob mob) {
        MobLevelData data = CACHE.remove(mob.getUUID());
        if (data != null && data.isProcessed()) {
            persist(mob, data);
        }
    }

    /** Drops all cached entries (server shutdown). */
    public static void clear() {
        CACHE.clear();
    }
}
