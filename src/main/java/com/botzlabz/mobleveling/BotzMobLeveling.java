package com.botzlabz.mobleveling;

import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(BotzMobLeveling.MOD_ID)
public class BotzMobLeveling {
    public static final String MOD_ID = "botzmobleveling";
    private static final Logger LOGGER = LogUtils.getLogger();

    public BotzMobLeveling() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MobLevelingConfig.SPEC);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[{}] Mob leveling system initialized!", MOD_ID);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[{}] Common setup complete - data-driven mob leveling ready", MOD_ID);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("[{}] Client setup complete", MOD_ID);
    }
}
