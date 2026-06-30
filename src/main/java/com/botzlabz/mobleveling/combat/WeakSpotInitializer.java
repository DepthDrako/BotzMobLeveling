package com.botzlabz.mobleveling.combat;

import com.botzlabz.lib.combat.WeakSpotRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Registers vanilla mob weak spots in botz_lib's {@link WeakSpotRegistry}.
 *
 * <p>Called once from {@link com.botzlabz.mobleveling.BotzMobLeveling#commonSetup}.
 * {@code eidolon_combat} reads from {@link WeakSpotRegistry} during hit resolution —
 * these registrations are what allow leveled mobs to participate in the full
 * hit-zone pipeline (crits, weak-spot multipliers, throw vectors, visual tiers).
 *
 * <h3>Offset convention (from {@link WeakSpotRegistry})</h3>
 * Offsets are relative to the entity AABB center, normalised to half-extents:
 * <ul>
 *   <li>(0, +1, 0) → top of hitbox (head for bipeds)</li>
 *   <li>(0,  0, 0) → body centre</li>
 *   <li>(0, -1, 0) → feet</li>
 *   <li>(0,  0,+1) → front face</li>
 * </ul>
 */
public final class WeakSpotInitializer {

    public static void init() {

        // ------------------------------------------------------------------
        // Bipedal / undead — head is the weak spot
        // ------------------------------------------------------------------
        headSpot("zombie");
        headSpot("husk");
        headSpot("drowned");
        headSpot("zombie_villager");
        headSpot("skeleton");
        headSpot("stray");
        headSpot("wither_skeleton");
        headSpot("piglin");
        headSpot("piglin_brute");
        headSpot("zombified_piglin");
        headSpot("pillager");
        headSpot("vindicator");
        headSpot("evoker");
        headSpot("witch");
        headSpot("illusioner");
        headSpot("bogged");

        // ------------------------------------------------------------------
        // Creeper — expose torso front as weak spot (classic sneaky shot)
        // ------------------------------------------------------------------
        WeakSpotRegistry.register(mc("creeper"),
            new Vec3(0,  0.1, 0.9),   // upper-front torso
            new Vec3(0, -0.2, 0.9));  // lower-front torso

        // ------------------------------------------------------------------
        // Spiders — top of cephalothorax
        // ------------------------------------------------------------------
        WeakSpotRegistry.register(mc("spider"),
            new Vec3(0, 0.85, 0));
        WeakSpotRegistry.register(mc("cave_spider"),
            new Vec3(0, 0.85, 0));

        // ------------------------------------------------------------------
        // Enderman — upper neck / head (very tall hitbox so slightly lower)
        // ------------------------------------------------------------------
        WeakSpotRegistry.register(mc("enderman"),
            new Vec3(0, 0.82, 0));

        // ------------------------------------------------------------------
        // Blaze — flame core in upper body
        // ------------------------------------------------------------------
        WeakSpotRegistry.register(mc("blaze"),
            new Vec3(0, 0.4, 0));

        // ------------------------------------------------------------------
        // Phantoms — wing joint (back of body)
        // ------------------------------------------------------------------
        WeakSpotRegistry.register(mc("phantom"),
            new Vec3(0, 0, -0.85));

        // ------------------------------------------------------------------
        // Slimes / Magma Cubes — center core
        // ------------------------------------------------------------------
        WeakSpotRegistry.register(mc("slime"),
            new Vec3(0, 0, 0));
        WeakSpotRegistry.register(mc("magma_cube"),
            new Vec3(0, 0, 0));

        // ------------------------------------------------------------------
        // Guardians — eye
        // ------------------------------------------------------------------
        WeakSpotRegistry.register(mc("guardian"),
            new Vec3(0.3, 0.2, 0.7));
        WeakSpotRegistry.register(mc("elder_guardian"),
            new Vec3(0.3, 0.2, 0.7));

        // ------------------------------------------------------------------
        // Shulker — opening when shell is open (top)
        // ------------------------------------------------------------------
        WeakSpotRegistry.register(mc("shulker"),
            new Vec3(0, 0.95, 0));

        // ------------------------------------------------------------------
        // Ravager — exposed neck
        // ------------------------------------------------------------------
        WeakSpotRegistry.register(mc("ravager"),
            new Vec3(0, 0.7, 0.6));

        // ------------------------------------------------------------------
        // Vex — small and fast; body-centre only
        // ------------------------------------------------------------------
        WeakSpotRegistry.register(mc("vex"),
            new Vec3(0, 0, 0));
    }

    // ------------------------------------------------------------------

    /** Convenience: bipedal head weak spot at normalised (0, 0.88, 0). */
    private static void headSpot(String path) {
        WeakSpotRegistry.register(mc(path), new Vec3(0, 0.88, 0));
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    private WeakSpotInitializer() {}
}
