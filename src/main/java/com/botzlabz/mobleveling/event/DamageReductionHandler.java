package com.botzlabz.mobleveling.event;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.botzlabz.mobleveling.config.MobLevelingConfig;
import com.botzlabz.mobleveling.level.MobLevelCapability;
import com.botzlabz.mobleveling.level.MobLevelData;
import com.eidolonreach.eidolon_lib.stats.StatKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = BotzMobLeveling.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class DamageReductionHandler {

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();

        // ── Boss outgoing damage multiplier (attacker side) ──────────────────
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            MobLevelData attackerData = MobLevelCapability.get(attacker);
            if (attackerData != null && attackerData.isBoss()
                    && attackerData.getBossDamageMul() > 1.0) {
                event.setAmount((float)(event.getAmount() * attackerData.getBossDamageMul()));
            }
        }

        MobLevelData data = MobLevelCapability.get(target);
        if (data == null || !data.isProcessed()) return;

        // ── Boss immunities (from the boss rule's "immunities" list) ─────────
        if (data.isBoss() && isImmune(data, event)) {
            event.setCanceled(true);
            return;
        }

        // ── Endurance damage reduction ───────────────────────────────────────
        float endurance = data.getStatBlock().getValue(StatKey.ENDURANCE);
        if (endurance <= 0) return;

        float cap         = (float)(MobLevelingConfig.ENDURANCE_CAP.get() / 100.0);
        float reduction   = Math.min(endurance / 100f, cap);
        float newDamage   = event.getAmount() * (1f - reduction);
        event.setAmount(newDamage);
    }

    private static boolean isImmune(MobLevelData data, LivingIncomingDamageEvent event) {
        var source = event.getSource();
        if (data.hasBossImmunity("fire")      && source.is(DamageTypeTags.IS_FIRE))      return true;
        if (data.hasBossImmunity("fall")      && source.is(DamageTypeTags.IS_FALL))      return true;
        if (data.hasBossImmunity("explosion") && source.is(DamageTypeTags.IS_EXPLOSION)) return true;
        // Poison has no damage type of its own (the effect deals minecraft:magic);
        // wither damage does. Effect application for both is blocked separately in
        // BossEventHandler.onEffectApplicable.
        if (data.hasBossImmunity("wither")
                && source.is(net.minecraft.world.damagesource.DamageTypes.WITHER))       return true;
        return false;
    }
}
