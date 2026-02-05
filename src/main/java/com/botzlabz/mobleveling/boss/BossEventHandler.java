package com.botzlabz.mobleveling.boss;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.data.MobLevelingDataManager;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * Handles boss-related events: spawning, damage, death, tick updates.
 */
@Mod.EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BossEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Attempt to make newly spawned mobs into bosses.
     * Runs after normal leveling is applied.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!MobLevelingConfig.BOSS_MODULE_ENABLED.get()) {
            return;
        }

        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Skip if already a boss
        if (BossData.isBoss(mob)) {
            // Restore boss bar for loaded bosses
            restoreBossBar(mob, serverLevel);
            return;
        }

        // Check if server is ready (use same delay as leveling system)
        if (!isServerReady(serverLevel)) {
            return;
        }

        // Try to make this mob a boss
        BossManager.getInstance().tryMakeBoss(mob, serverLevel, mob.blockPosition());
    }

    /**
     * Restore boss bar for bosses loaded from NBT.
     */
    private static void restoreBossBar(Mob mob, ServerLevel level) {
        if (!MobLevelingConfig.BOSS_SHOW_BOSS_BAR.get()) {
            return;
        }

        BossRule rule = BossManager.getInstance().getBossRule(mob);
        if (rule == null) {
            return;
        }

        // Re-apply glow if needed
        if (rule.hasGlowEffect() && MobLevelingConfig.BOSS_GLOW_EFFECT.get()) {
            mob.setGlowingTag(true);
        }

        // Boss bar will be created/managed on tick
    }

    /**
     * Tick handler for boss-specific logic: boss bars, minion spawning, etc.
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!MobLevelingConfig.BOSS_MODULE_ENABLED.get()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (mob.level().isClientSide()) {
            return;
        }

        if (!BossData.isBoss(mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BossManager manager = BossManager.getInstance();

        // Update boss bar health
        manager.updateBossBar(mob);

        // Update nearby player tracking for boss bar visibility
        updateBossBarPlayers(mob, serverLevel);

        // Tick minion spawning
        manager.tickMinions(mob, serverLevel);
    }

    /**
     * Update which players can see the boss bar based on distance.
     */
    private static void updateBossBarPlayers(Mob boss, ServerLevel level) {
        int renderDistance = MobLevelingConfig.BOSS_BAR_RENDER_DISTANCE.get();
        BossManager manager = BossManager.getInstance();

        for (ServerPlayer player : level.players()) {
            double distance = player.distanceTo(boss);

            if (distance <= renderDistance) {
                manager.addPlayerToBossBar(boss, player);
            } else {
                manager.removePlayerFromBossBar(boss, player);
            }
        }
    }

    /**
     * Handle boss damage - apply immunities.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!MobLevelingConfig.BOSS_MODULE_ENABLED.get()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!BossData.isBoss(mob)) {
            return;
        }

        BossRule rule = BossManager.getInstance().getBossRule(mob);
        if (rule == null) {
            return;
        }

        // Check immunities
        DamageSource source = event.getSource();
        for (String immunity : rule.getImmunities()) {
            if (checkImmunity(source, immunity)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    private static boolean checkImmunity(DamageSource source, String immunity) {
        return switch (immunity.toLowerCase()) {
            case "fire" -> source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE);
            case "fall" -> source.is(net.minecraft.tags.DamageTypeTags.IS_FALL);
            case "drown", "drowning" -> source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING);
            case "explosion" -> source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
            case "projectile" -> source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE);
            case "magic" -> source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO);
            case "wither" -> source.is(net.minecraft.world.damagesource.DamageTypes.WITHER);
            case "lightning" -> source.is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT);
            case "freeze", "freezing" -> source.is(net.minecraft.tags.DamageTypeTags.IS_FREEZING);
            default -> false;
        };
    }

    /**
     * Handle boss death - remove boss bar, apply loot multipliers.
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!MobLevelingConfig.BOSS_MODULE_ENABLED.get()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!BossData.isBoss(mob)) {
            return;
        }

        // Remove boss bar
        BossManager.getInstance().removeBossBar(mob);

        // Announce death
        BossRule rule = BossManager.getInstance().getBossRule(mob);
        if (rule != null && MobLevelingConfig.BOSS_SPAWN_ANNOUNCEMENT.get()) {
            if (mob.level() instanceof ServerLevel serverLevel) {
                announceDeath(mob, rule, serverLevel, event.getSource());
            }
        }

        if (MobLevelingConfig.DEBUG_MODE.get()) {
            LOGGER.debug("[BossEventHandler] Boss {} died", BossData.getBossUUID(mob));
        }
    }

    private static void announceDeath(Mob boss, BossRule rule, ServerLevel level, DamageSource source) {
        int radius = MobLevelingConfig.BOSS_ANNOUNCEMENT_RADIUS.get();

        String killerName = "unknown forces";
        if (source.getEntity() instanceof Player player) {
            killerName = player.getName().getString();
        }

        var message = net.minecraft.network.chat.Component.literal(
                "§a§l[!] " + rule.getDisplayName() + " §r§ahas been defeated by §e" + killerName + "§a!"
        );

        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().closerThan(boss.blockPosition(), radius)) {
                player.sendSystemMessage(message);
            }
        }
    }

    /**
     * Apply XP multiplier for boss kills.
     */
    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (!MobLevelingConfig.BOSS_MODULE_ENABLED.get()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!BossData.isBoss(mob)) {
            return;
        }

        BossRule rule = BossManager.getInstance().getBossRule(mob);
        if (rule != null && rule.getXpMultiplier() > 1.0) {
            int originalXp = event.getDroppedExperience();
            int newXp = (int) (originalXp * rule.getXpMultiplier());
            event.setDroppedExperience(newXp);

            if (MobLevelingConfig.DEBUG_MODE.get()) {
                LOGGER.debug("[BossEventHandler] XP multiplied from {} to {} for boss {}",
                        originalXp, newXp, rule.getId());
            }
        }
    }

    /**
     * Check if server is ready for boss processing.
     */
    private static boolean isServerReady(ServerLevel level) {
        try {
            var server = level.getServer();
            if (server == null || !server.isRunning()) {
                return false;
            }

            // Wait for world to tick a bit before spawning bosses
            if (level.getGameTime() < 200) {
                return false;
            }

            // Ensure at least one player is in game
            if (server.getPlayerList().getPlayerCount() == 0) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
