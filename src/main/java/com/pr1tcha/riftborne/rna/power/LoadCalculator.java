package com.pr1tcha.riftborne.rna.power;

import java.util.Set;

/**
 * Pure implementation of the PS V2.5 load formula (spec §4). No Minecraft dependency, so it can be
 * unit-tested standalone. All balance numbers are the spec defaults; they are candidates to move
 * into config without touching callers.
 *
 * <pre>
 * L_total = (Σ base cost of active Δ) × K_complex × K_depth × K_hold
 * </pre>
 */
public final class LoadCalculator {
    /** Active Δ-family count at or above which the cast is anomalous and blocked for player Synchrons. */
    public static final int ANOMALY_AXES = 4;

    private LoadCalculator() {
    }

    /** Non-linear complexity multiplier by number of converged Δ-families. {@code Float.MAX_VALUE} = anomaly. */
    public static float complexityMultiplier(int activeAxes) {
        return switch (activeAxes) {
            case 0, 1 -> 1.0F;
            case 2 -> 1.8F;
            case 3 -> 3.2F;
            default -> Float.MAX_VALUE;
        };
    }

    /** Depth multiplier by execution level 1-4 (PF09: surface / stream / structural / principle). */
    public static float depthMultiplier(int depthLevel) {
        return switch (depthLevel) {
            case 1 -> 1.0F;
            case 2 -> 1.5F;
            case 3 -> 2.3F;
            default -> 4.0F;
        };
    }

    public static float totalLoad(Set<DeltaAxis> activeAxes, int depthLevel, HoldType hold) {
        float sum = 0.0F;
        for (DeltaAxis axis : activeAxes) {
            sum += axis.baseCost();
        }
        float kComplex = complexityMultiplier(activeAxes.size());
        if (kComplex == Float.MAX_VALUE) {
            return Float.MAX_VALUE;
        }
        return sum * kComplex * depthMultiplier(depthLevel) * hold.multiplier();
    }

    /** True if the cast's Δ-family count fits the player's connectivity (C-class 1-3). */
    public static boolean connectivityAllows(int activeAxes, int connectivity) {
        return activeAxes <= connectivity;
    }

    /** True if the Δ-family count enters the anomalous regime (blocked for player Synchrons). */
    public static boolean isAnomalous(int activeAxes) {
        return activeAxes >= ANOMALY_AXES;
    }
}
