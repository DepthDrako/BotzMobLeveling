package com.botzlabz.mobleveling.adaptive;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Scores a player's gear quality on a continuous scale used by {@link ThreatTier}.
 *
 * <h3>Thread safety</h3>
 * {@link #scorePlayer} must run on the <b>main server thread</b> — it reads live
 * player inventory state.  Use {@link #snapshotArmor}, {@link #snapshotWeapon},
 * and {@link #snapshotOffhand} to copy item stacks on the main thread, then pass
 * those snapshots to {@link #scoreFromSnapshots} on any thread (including
 * {@link com.botzlabz.lib.async.BotzAsync}).
 *
 * <h3>Scoring</h3>
 * <ul>
 *   <li>Each armor piece: material tier (1–8) + 2 per enchantment</li>
 *   <li>Main-hand weapon: material tier (0–10) + 3 per enchantment</li>
 *   <li>Off-hand shield: +5 flat + 1.5 per enchantment</li>
 * </ul>
 */
public final class GearAnalyzer {

    // ------------------------------------------------------------------
    // Main-thread scoring (live player)
    // ------------------------------------------------------------------

    /** Main-thread only. Scores the player's current gear directly. */
    public static double scorePlayer(Player player) {
        return scoreFromSnapshots(
            snapshotArmor(player),
            snapshotWeapon(player),
            snapshotOffhand(player)
        );
    }

    // ------------------------------------------------------------------
    // Snapshot helpers (call on main thread, pass results to async)
    // ------------------------------------------------------------------

    /** Copies all four armor stacks. Call on the main thread. */
    public static List<ItemStack> snapshotArmor(Player player) {
        List<ItemStack> copies = new ArrayList<>(4);
        for (ItemStack stack : player.getArmorSlots()) {
            copies.add(stack.copy());
        }
        return copies;
    }

    /** Copies the main-hand item. Call on the main thread. */
    public static ItemStack snapshotWeapon(Player player) {
        return player.getMainHandItem().copy();
    }

    /** Copies the off-hand item. Call on the main thread. */
    public static ItemStack snapshotOffhand(Player player) {
        return player.getOffhandItem().copy();
    }

    // ------------------------------------------------------------------
    // Async-safe scoring (immutable ItemStack copies only)
    // ------------------------------------------------------------------

    /**
     * Scores gear from copied stacks. Safe to call on any thread.
     * All input stacks must be independent copies — never live references.
     */
    public static double scoreFromSnapshots(List<ItemStack> armor,
                                            ItemStack weapon,
                                            ItemStack offhand) {
        double score = 0;

        for (ItemStack stack : armor) {
            if (stack.isEmpty()) continue;
            score += armorMaterialScore(stack);
            score += stack.getEnchantments().size() * 2.0;
        }

        if (!weapon.isEmpty()) {
            score += weaponMaterialScore(weapon);
            score += weapon.getEnchantments().size() * 3.0;
        }

        if (!offhand.isEmpty() && offhand.getItem() instanceof ShieldItem) {
            score += 5.0;
            score += offhand.getEnchantments().size() * 1.5;
        }

        return score;
    }

    // ------------------------------------------------------------------
    // Material scoring (item registry path — namespace-safe)
    // ------------------------------------------------------------------

    private static double armorMaterialScore(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if      (id.startsWith("netherite")) return 8.0;
        else if (id.startsWith("diamond"))   return 6.0;
        else if (id.startsWith("iron"))      return 3.0;
        else if (id.startsWith("chainmail")) return 2.5;
        else if (id.startsWith("gold"))      return 2.0;
        else if (id.startsWith("leather"))   return 1.0;
        return 1.5; // modded armor
    }

    private static double weaponMaterialScore(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if      (id.startsWith("netherite"))                       return 10.0;
        else if (id.startsWith("diamond"))                         return 7.0;
        else if (id.startsWith("iron"))                            return 4.0;
        else if (id.startsWith("stone"))                           return 2.0;
        else if (id.startsWith("gold"))                            return 1.5;
        else if (id.startsWith("wooden") || id.startsWith("wood")) return 1.0;
        else if (id.contains("bow") || id.contains("crossbow"))    return 3.0;
        return 2.0; // modded weapons
    }

    private GearAnalyzer() {}
}
