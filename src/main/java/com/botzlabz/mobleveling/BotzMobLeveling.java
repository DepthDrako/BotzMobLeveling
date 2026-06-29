package com.botzlabz.mobleveling;

import com.botzlabz.mobleveling.adaptive.GearScoreCache;
import com.botzlabz.mobleveling.combat.WeakSpotInitializer;
import com.botzlabz.mobleveling.compat.EpicFightIntegration;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.data.MobLevelingDataManager;
import com.botzlabz.mobleveling.display.LevelDisplay;
import com.botzlabz.mobleveling.level.MobLevelData;
import com.botzlabz.mobleveling.level.MobLevelStore;
import com.eidolonreach.eidolon_lib.capability.EidolonLibCapabilities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(BotzMobLeveling.MOD_ID)
public class BotzMobLeveling {

    public static final String MOD_ID = "botzmobleveling";

    public static final EntityCapability<MobLevelData, Void> MOB_LEVEL_CAP =
        EntityCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "mob_level_data"),
            MobLevelData.class
        );

    public BotzMobLeveling(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, MobLevelingConfig.SPEC,
            "botzmobleveling-common.toml");

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        EpicFightIntegration.init();

        // Register weak spots for all known vanilla mob types so eidolon_combat
        // can resolve head-shots, crits, and hit-zone multipliers on leveled mobs.
        event.enqueueWork(WeakSpotInitializer::init);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            registerForType(event, type);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Entity> void registerForType(
            RegisterCapabilitiesEvent event, EntityType<E> type) {
        // Providers run on EVERY getCapability call, so they must return a stable
        // per-entity instance — MobLevelStore caches one per UUID and hydrates it
        // from persistent NBT.  Client-side returns null: level data is
        // server-authoritative, and in singleplayer the client entity shares its
        // UUID with the server entity, so caching it would corrupt the store.
        event.registerEntity(
            MOB_LEVEL_CAP,
            type,
            (entity, ctx) -> entity instanceof Mob mob && !mob.level().isClientSide()
                ? MobLevelStore.getOrCreate(mob) : null
        );

        // Cross-mod stat access: consumers (eidolon_ai, botz_elemental, ...) read
        // this mob's stats through eidolon_lib's IStatHolder token without any
        // compile dependency on this mod.
        event.registerEntity(
            EidolonLibCapabilities.STAT_HOLDER,
            type,
            (entity, ctx) -> entity instanceof Mob mob && !mob.level().isClientSide()
                ? MobLevelStore.getOrCreate(mob).getStatBlock() : null
        );
    }

    // -------------------------------------------------------------------------
    // Game bus
    // -------------------------------------------------------------------------

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.GAME)
    public static class GameEvents {

        /** Register the datapack rule loader (reloads on /reload). */
        @SubscribeEvent
        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            event.addListener(MobLevelingDataManager.INSTANCE);
        }

        /**
         * Secondary name-sync hook after entity is fully in the world.
         * {@link com.botzlabz.mobleveling.event.AttributeRestoreHandler} handles
         * the full data restore; this just ensures the name tag is always current.
         */
        @SubscribeEvent
        public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide()) return;
            if (!(event.getEntity() instanceof Mob mob)) return;

            MobLevelData data = mob.getCapability(MOB_LEVEL_CAP);
            if (data == null || !data.isProcessed()) return;

            LevelDisplay.apply(mob, data);
        }

        /**
         * Flush level data to persistent NBT and drop the cache entry when an
         * entity leaves the level.  Client guard is load-bearing: in singleplayer
         * the client entity shares its UUID with the server entity, and a
         * client-side leave (render-distance unload) must not evict the live
         * server-side entry.
         */
        @SubscribeEvent
        public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
            if (event.getLevel().isClientSide()) return;
            if (event.getEntity() instanceof Mob mob) {
                MobLevelStore.flushAndEvict(mob);
            }
        }

        /** Clear caches when the server shuts down. */
        @SubscribeEvent
        public static void onServerStopping(ServerStoppingEvent event) {
            GearScoreCache.clear();
            MobLevelStore.clear();
        }
    }
}
