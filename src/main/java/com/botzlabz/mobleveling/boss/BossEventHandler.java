package com.botzlabz.mobleveling.boss;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.level.MobLevelCapability;
import com.botzlabz.mobleveling.level.MobLevelData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Maintains boss bar lifecycle: join → update → death.
 */
@EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class BossEventHandler {

    /**
     * Re-attach boss bar after world reload (if boss was persisted).
     * Fires AFTER {@link com.botzlabz.mobleveling.event.AttributeRestoreHandler}
     * has already reloaded the MobLevelData from persistent NBT.
     */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        MobLevelData data = MobLevelCapability.get(mob);
        if (data == null || !data.isProcessed() || !data.isBoss()) return;
        if (BossManager.isBossTracked(mob.getUUID())) return;

        // Re-register the boss bar (data already loaded from NBT) — no re-announce.
        BossManager.registerBoss(mob, data, serverLevel, false);
    }

    /** Update boss bar health when the boss takes damage. */
    @SubscribeEvent
    public static void onBossDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        MobLevelData data = MobLevelCapability.get(mob);
        if (data == null || !data.isBoss()) return;
        BossManager.updateHealth(mob.getUUID(), mob.getHealth(), mob.getMaxHealth());
    }

    /** Clean up boss bar when the boss dies. */
    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        MobLevelData data = MobLevelCapability.get(mob);
        if (data == null || !data.isBoss()) return;
        BossManager.removeBoss(mob.getUUID());
    }

    /**
     * Drop the boss bar when the boss leaves the level (chunk unload, despawn,
     * dimension transfer).  Without this the bar lingers on players' screens
     * until server restart.  Rejoin re-registers it via {@link #onEntityJoin}.
     */
    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (BossManager.isBossTracked(mob.getUUID())) {
            BossManager.removeBoss(mob.getUUID());
        }
    }

    /**
     * Enforces the boss rule's "poison" / "wither" immunities.  These have no
     * usable damage-type hook (poison damage is plain {@code minecraft:magic}),
     * so the effect application itself is blocked instead.
     */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide()) return;
        MobLevelData data = MobLevelCapability.get(mob);
        if (data == null || !data.isBoss()) return;

        var instance = event.getEffectInstance();
        if (instance == null) return;
        if ((data.hasBossImmunity("poison") && instance.is(MobEffects.POISON))
         || (data.hasBossImmunity("wither") && instance.is(MobEffects.WITHER))) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /**
     * Add joining players to all <b>global</b> boss bars. Local bars are populated
     * by the per-tick proximity sweep in {@link #onServerTick}, so a joining player
     * out of range won't see them until they approach.
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BossManager.addPlayerToGlobalBars(player);
    }

    /**
     * Maintains local boss-bar membership (players moving in/out of a boss's
     * {@code boss_bar_range}). Throttled — distance polling every tick for every
     * boss × player is wasteful and players don't move far in a few ticks.
     */
    private static int tickCounter = 0;
    private static final int TICK_INTERVAL = 10; // twice per second

    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (++tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;
        BossManager.tickBars(event.getServer());
    }

    private BossEventHandler() {}
}
