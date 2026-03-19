package com.botzlabz.mobleveling.event;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.kills.KillLevelManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobKillHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!MobLevelingConfig.ENABLED.get()) {
            return;
        }

        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // We need the direct attacker to be a Mob
        Entity killerEntity = event.getSource().getDirectEntity();
        if (!(killerEntity instanceof Mob killer)) {
            return;
        }

        if (!(killer.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        KillLevelManager.onMobKill(killer, event.getEntity(), serverLevel);
    }
}
