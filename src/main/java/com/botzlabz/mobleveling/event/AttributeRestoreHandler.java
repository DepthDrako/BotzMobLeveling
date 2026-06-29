package com.botzlabz.mobleveling.event;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.attribute.AttributeScalingManager;
import com.botzlabz.mobleveling.display.LevelDisplay;
import com.botzlabz.mobleveling.level.MobLevelCapability;
import com.botzlabz.mobleveling.level.MobLevelData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Restores level data and transient attribute modifiers when an entity
 * re-enters the world (after chunk reload or server restart).
 *
 * <p>NBT is stored in {@code entity.getPersistentData()} under the key
 * {@value MobLevelData#NBT_KEY}.  When that key is present and the
 * capability instance is unprocessed, we reload it.  If it is already
 * processed (e.g. a just-spawned mob in the same session), we skip the
 * restore so we don't overwrite fresh data.
 *
 * <p>After restoring, we re-apply transient attribute modifiers because
 * {@link AttributeScalingManager} uses {@code addTransientModifier} which
 * is NOT saved in entity NBT.
 */
@EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class AttributeRestoreHandler {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        MobLevelData data = MobLevelCapability.get(mob);
        if (data == null) return;

        // If already processed in this session, just re-apply transient attributes and name.
        if (data.isProcessed()) {
            AttributeScalingManager.remove(mob, data);
            AttributeScalingManager.apply(mob, data);
            LevelDisplay.apply(mob, data);
            return;
        }

        // Try to reload from persistent NBT (cross-restart persistence)
        CompoundTag persistentData = mob.getPersistentData();
        if (!persistentData.contains(MobLevelData.NBT_KEY)) return;

        data.load(persistentData.getCompound(MobLevelData.NBT_KEY));
        if (!data.isProcessed()) return;

        // Re-apply attributes (transient = not in entity NBT, must re-add)
        AttributeScalingManager.remove(mob, data);
        AttributeScalingManager.apply(mob, data);

        // Refresh name tag
        LevelDisplay.apply(mob, data);
    }

    private AttributeRestoreHandler() {}
}
