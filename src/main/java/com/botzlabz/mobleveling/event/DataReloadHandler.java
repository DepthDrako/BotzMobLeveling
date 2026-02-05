package com.botzlabz.mobleveling.event;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.data.MobLevelingDataManager;
import com.mojang.logging.LogUtils;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DataReloadHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        MobLevelingDataManager dataManager = new MobLevelingDataManager();
        event.addListener(dataManager);
        LOGGER.info("[{}] Registered mob leveling data reload listener", BotzMobLeveling.MOD_ID);
    }
}
