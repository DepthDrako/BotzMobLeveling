package com.botzlabz.mobleveling.level;

import com.botzlabz.mobleveling.BotzMobLeveling;
import com.eidolonreach.eidolon_lib.capability.CapabilityHelper;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Convenience accessor for {@link BotzMobLeveling#MOB_LEVEL_CAP}.
 *
 * <p>Delegates to eidolon_lib's {@link CapabilityHelper} for consistent
 * null-safety across all Eidolon mods.
 */
public final class MobLevelCapability {

    /** Returns the capability data, or {@code null} if absent. */
    @Nullable
    public static MobLevelData get(LivingEntity entity) {
        return CapabilityHelper.get(entity, BotzMobLeveling.MOB_LEVEL_CAP).orElse(null);
    }

    /** Returns an {@link Optional} wrapping the capability (empty if absent). */
    public static Optional<MobLevelData> opt(LivingEntity entity) {
        return CapabilityHelper.get(entity, BotzMobLeveling.MOB_LEVEL_CAP);
    }

    /** Runs {@code action} only if the capability is present and processed. */
    public static void ifProcessed(LivingEntity entity, Consumer<MobLevelData> action) {
        MobLevelData data = get(entity);
        if (data != null && data.isProcessed()) action.accept(data);
    }

    /** Maps the capability to a value, or returns {@code defaultValue} if absent. */
    public static <R> R map(LivingEntity entity, Function<MobLevelData, R> mapper, R defaultValue) {
        return CapabilityHelper.map(entity, BotzMobLeveling.MOB_LEVEL_CAP, mapper, defaultValue);
    }

    private MobLevelCapability() {}
}
