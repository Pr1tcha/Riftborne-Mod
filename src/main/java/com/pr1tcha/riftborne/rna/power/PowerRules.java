package com.pr1tcha.riftborne.rna.power;

import com.pr1tcha.riftborne.rna.power.data.RNAProfile;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.util.Mth;

/**
 * PS V2.5 rule helpers that need the player profile but no world side effects: the admissibility
 * window, the default Δ-axis footprint of each primitive, and the meta-wear a cast inflicts.
 *
 * <p>The per-primitive axis footprints are working defaults (the lore leaves exact tagging to
 * context/Ogranki). Kept here so {@link PowerCast} reads cleanly; candidates for config/datapack.
 */
public final class PowerRules {
    private static final float MIN_WINDOW = 10.0F;

    private PowerRules() {
    }

    /** Working window shrinks as meta-wear grows: 0 wear → 100, high wear → {@value MIN_WINDOW}. */
    public static float admissibilityWindow(RNAProfile profile) {
        return Math.max(MIN_WINDOW, RNAProfile.MAX_META_WEAR - profile.metaWear());
    }

    /** Default Δ-families a primitive touches at its base use. */
    public static Set<DeltaAxis> defaultAxes(Primitive primitive) {
        return switch (primitive) {
            case P1_READING -> EnumSet.of(DeltaAxis.DS);
            case P2_ANCHOR -> EnumSet.of(DeltaAxis.DR);
            case P3_SHIFT -> EnumSet.of(DeltaAxis.DG);
            case P4_SHAPE -> EnumSet.of(DeltaAxis.DS);
            case P5_STABILIZE -> EnumSet.of(DeltaAxis.DR);
            case V1_TEMPO_READ -> EnumSet.of(DeltaAxis.DV);
            case V2_PHASE_ENTRY -> EnumSet.of(DeltaAxis.DP);
            case V3_TEMPO_SHIFT -> EnumSet.of(DeltaAxis.DV);
            case V4_PHASE_HOLD -> EnumSet.of(DeltaAxis.DST);
        };
    }

    /** Meta-wear a cast inflicts, scaled by its load. Minimum 1 per meaningful cast. */
    public static float metaWearForLoad(float load) {
        return Math.max(1.0F, load * 0.05F);
    }

    /** Depth level (1-4) of a cast = the player's execution level for that primitive. */
    public static int depthLevel(RNAProfile profile, Primitive primitive) {
        return Mth.clamp(profile.level(primitive), RNAProfile.MIN_PRIMITIVE_LEVEL, RNAProfile.MAX_PRIMITIVE_LEVEL);
    }
}
