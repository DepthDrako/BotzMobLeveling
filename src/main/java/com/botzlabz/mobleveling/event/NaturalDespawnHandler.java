package com.botzlabz.mobleveling.event;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.display.LevelDisplay;
import com.botzlabz.mobleveling.level.MobLevelCapability;
import com.botzlabz.mobleveling.level.MobLevelData;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;

/**
 * Restores natural despawning for mobs whose only persistence reason is the
 * level name tag this mod applies.
 *
 * <p><b>Why:</b> vanilla {@code Mob.isPersistenceRequired()} returns true for any
 * mob with a custom name.  Since {@link LevelDisplay} names every leveled mob,
 * without this handler no leveled mob would EVER despawn naturally — the mob cap
 * fills with stale mobs, fresh spawning grinds down, and downstream systems that
 * react to natural despawn (botz_ai's nemesis dormancy) never trigger.
 *
 * <p><b>What it does:</b> on the despawn check, if the mob is leveled, carries OUR
 * tag (not a player rename), and has no real persistence reason, we replicate the
 * vanilla hard-despawn distance rule and force-allow the despawn the name tag
 * would otherwise block.
 *
 * <p>Hands off when:
 * <ul>
 *   <li>the raw {@code persistenceRequired} flag is set (raid captains, bucketed
 *       mobs — read via reflection; {@code isPersistenceRequired()} ORs in
 *       {@code hasCustomName()} so it cannot make this distinction)</li>
 *   <li>{@code requiresCustomPersistence()} (vehicles, leashed, etc.)</li>
 *   <li>the current name differs from our generated tag (player renamed it)</li>
 *   <li>the mob is a rule boss (bosses are meant to stay)</li>
 * </ul>
 */
@EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class NaturalDespawnHandler {

    // NeoForge 1.21.1 runs on Mojang official mappings, so "persistenceRequired"
    // is the live field name.  Cached once at class load.  If access ever fails,
    // the handler fails closed (treats the flag as set) and never force-despawns.
    private static final java.lang.reflect.Field PERSISTENCE_REQUIRED_FIELD;
    static {
        java.lang.reflect.Field f = null;
        try {
            f = Mob.class.getDeclaredField("persistenceRequired");
            f.setAccessible(true);
        } catch (Exception ignored) {}
        PERSISTENCE_REQUIRED_FIELD = f;
    }

    private static boolean hasRealPersistenceFlag(Mob mob) {
        if (PERSISTENCE_REQUIRED_FIELD == null) return true; // fail closed
        try {
            return PERSISTENCE_REQUIRED_FIELD.getBoolean(mob);
        } catch (Exception e) {
            return true; // fail closed
        }
    }

    @SubscribeEvent
    public static void onDespawnCheck(MobDespawnEvent event) {
        Mob mob = event.getEntity();
        if (!mob.hasCustomName()) return;                 // nothing to undo
        if (hasRealPersistenceFlag(mob)) return;          // real persistence — hands off
        if (mob.requiresCustomPersistence()) return;

        MobLevelData data = MobLevelCapability.get(mob);
        if (data == null || !data.isProcessed()) return;
        if (data.isBoss()) return;
        if (data.getOriginalName() == null) return;       // we never tagged it

        // Distance check first — it fails in the common case (player nearby) and
        // is cheaper than building the tag string. This handler runs every tick
        // for every named leveled mob, so order matters.
        // Replicates the vanilla hard-despawn rule the name tag is blocking:
        // nearest player beyond the category's despawn distance -> despawn.
        Player nearest = mob.level().getNearestPlayer(mob, -1.0);
        if (nearest == null) return;
        double distSqr     = nearest.distanceToSqr(mob);
        int    despawnDist = mob.getType().getCategory().getDespawnDistance();
        if (distSqr <= (double) despawnDist * despawnDist) return;
        if (!mob.removeWhenFarAway(distSqr)) return;

        // Only act while OUR tag is the current name — a player rename means
        // the player wants this mob kept, exactly like vanilla.
        String current = mob.getCustomName().getString();
        String ourTag  = LevelDisplay.buildName(data).getString();
        if (!current.equals(ourTag)) return;

        event.setResult(MobDespawnEvent.Result.ALLOW);
    }

    private NaturalDespawnHandler() {}
}
