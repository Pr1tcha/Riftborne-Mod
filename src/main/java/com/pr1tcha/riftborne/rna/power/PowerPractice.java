package com.pr1tcha.riftborne.rna.power;

import com.pr1tcha.riftborne.config.Config;
import com.pr1tcha.riftborne.rna.power.data.AdaptationCycle;
import com.pr1tcha.riftborne.rna.power.data.RNAProfile;
import com.pr1tcha.riftborne.rna.power.data.RnaTrack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Converts a successful cast into RNA training value.
 *
 * <p>RNA is a control contour, not a muscle: it does not grow from running or mining, only from
 * meaningful intervention. Two guards keep that honest. Repeating one primitive decays to nothing
 * within a cycle, so a macro on a single block teaches the architecture no more than a few honest
 * attempts. And each parameter answers only to its own kind of work — cheap taps never train
 * Bandwidth, and deliberately overloading never trains Overload Resistance.
 */
public final class PowerPractice {
    /** Cycle counter for casts that ended in compensation; any of them spoils Connectivity. */
    public static final String COMPENSATION_KEY = "compensations";

    private PowerPractice() {
    }

    /** Record a successful, meaningful cast against every track it legitimately trains. */
    public static void recordSuccess(ServerPlayer player, Primitive primitive, float load) {
        RNAProfile profile = PowerApi.get(player);
        AdaptationCycle cycle = AdaptationService.cycle(player);

        int priorUses = cycle.primitiveUses(primitive.id());
        double quality = repetitionQuality(priorUses);
        AdaptationService.setCycle(player, cycle.countPrimitiveUse(primitive.id()));
        if (quality <= 0.0D) {
            return;
        }

        double loadShare = profile.throughput() <= 0 ? 0.0D : load / profile.throughput();

        if (loadShare >= Config.adaptationBandwidthLoadShare.get()) {
            AdaptationService.addPractice(player, RnaTrack.BANDWIDTH, quality);
        }
        if (trainsNodeDensity(primitive)) {
            AdaptationService.addPractice(player, RnaTrack.NODE_DENSITY, quality);
        }
        if (loadShare >= Config.adaptationOverloadBandMin.get()
                && loadShare <= Config.adaptationOverloadBandMax.get()) {
            AdaptationService.addPractice(player, RnaTrack.OVERLOAD_RESISTANCE, quality);
        }
    }

    /** A cast that collapsed under compensation; the cycle's Connectivity work is spoiled. */
    public static void recordCompensation(ServerPlayer player) {
        AdaptationCycle cycle = AdaptationService.cycle(player);
        AdaptationService.setCycle(player, cycle.addRawPractice(COMPENSATION_KEY, 1.0D));
    }

    /** The ninth repetition is worth half, the seventeenth nothing at all. */
    public static double repetitionQuality(int priorUses) {
        int full = Config.adaptationPrimitiveFullUses.get();
        int half = Config.adaptationPrimitiveHalfUses.get();
        if (priorUses < full) {
            return 1.0D;
        }
        if (priorUses < half) {
            return 0.5D;
        }
        return 0.0D;
    }

    /** Node density answers to reach and scale, not to repeating a minimal effect in place. */
    private static boolean trainsNodeDensity(Primitive primitive) {
        return switch (primitive) {
            case P2_ANCHOR, P4_SHAPE, V3_TEMPO_SHIFT, V4_PHASE_HOLD -> true;
            default -> false;
        };
    }

    /**
     * Connectivity is judged over the whole cycle rather than per cast: it is the ability to hold a
     * clean sequence together, so it needs breadth (more than one primitive), volume, and no
     * compensation anywhere in the cycle.
     */
    public static double connectivityPractice(AdaptationCycle cycle) {
        if (cycle.rawPractice(COMPENSATION_KEY) > 0.0D) {
            return 0.0D;
        }
        int distinct = 0;
        int total = 0;
        for (int uses : cycle.primitiveUses().values()) {
            if (uses > 0) {
                distinct++;
                total += uses;
            }
        }
        if (distinct < 2 || total < 8) {
            return 0.0D;
        }
        return Math.min(1.0D, total / 8.0D);
    }
}
