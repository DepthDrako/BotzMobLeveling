package com.botzlabz.mobleveling.event;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.level.MobLevelCapability;
import com.botzlabz.mobleveling.level.MobLevelData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Opt-in level-based loot scaling.
 *
 * <p>1.20.1 implemented this through Forge's {@code LootingLevelEvent}, bumping
 * the looting level fed into the loot table. NeoForge 1.21.1 removed that event
 * (looting became an enchantment effect with no level hook), so this instead
 * post-processes the final {@link LivingDropsEvent}: for a leveled mob it grants
 * an effective Looting bonus and grows each <i>stackable</i> drop by a uniform
 * {@code 0..bonus} — the same distribution vanilla's {@code LootingEnchantFunction}
 * uses. Counts are grown in place, so no extra item entities are spawned.
 *
 * <p>Non-stackable drops (tools, armor — a mob's own equipment) are skipped, so
 * unique gear is never duplicated. Off unless {@code lootScaling.enabled}.
 */
@EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class LootScalingHandler {

    private static final RandomSource RANDOM = RandomSource.create();

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!MobLevelingConfig.LOOT_SCALING_ENABLED.get()) return;
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        MobLevelData data = MobLevelCapability.get(mob);
        if (data == null || !data.isProcessed()) return;

        // Effective level = base + kill levels (mirrors the 1.20.1 behaviour).
        int perLevels = MobLevelingConfig.LOOT_BONUS_PER_LEVELS.get();
        int bonus = Math.min(data.getTotalLevel() / perLevels,
                             MobLevelingConfig.LOOT_MAX_BONUS.get());
        if (bonus <= 0) return;

        for (ItemEntity itemEntity : event.getDrops()) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty() || stack.getMaxStackSize() <= 1) continue; // skip equipment/tools

            int extra = RANDOM.nextInt(bonus + 1); // uniform 0..bonus, like vanilla looting
            if (extra <= 0) continue;

            stack.setCount(Math.min(stack.getMaxStackSize(), stack.getCount() + extra));
            itemEntity.setItem(stack);
        }
    }

    private LootScalingHandler() {}
}
