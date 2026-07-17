package com.pr1tcha.riftborne.rna.power;

import com.pr1tcha.riftborne.physical.PhysicalTrainingManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * The four organism gates of PS V2.5 (spec §5.5) that cap how far RNA parameters can be trained.
 * The {@code phys} gate is driven live by the existing physical-condition system, giving physical
 * training a real purpose; {@code mental/psych/genetic} are innate. They are placeholder defaults
 * for now and become stored per-player innate values when growth caps are enforced (progression phase).
 */
public final class PowerGates {
    // Placeholder innate defaults until per-player generation lands.
    private static final int DEFAULT_MENTAL = 50;
    private static final int DEFAULT_PSYCH = 50;
    private static final float DEFAULT_GENETIC = 1.0F;

    private PowerGates() {
    }

    /** Physical condition gate (0-100), live from the physical-training system. */
    public static float phys(ServerPlayer player) {
        return PhysicalTrainingManager.overallForm(player);
    }

    public static int mental(ServerPlayer player) {
        return DEFAULT_MENTAL;
    }

    public static int psych(ServerPlayer player) {
        return DEFAULT_PSYCH;
    }

    public static float genetic(ServerPlayer player) {
        return DEFAULT_GENETIC;
    }

    /** Cap on node density and overload resistance = physical form × genetic multiplier. */
    public static int physicalStatCap(ServerPlayer player) {
        return Mth.clamp(Math.round(phys(player) * genetic(player)), 0, 100);
    }

    /** Highest connectivity C-class the mental gate allows (1-3). */
    public static int connectivityCap(ServerPlayer player) {
        float scaled = mental(player) * genetic(player);
        if (scaled >= 70.0F) {
            return 3;
        }
        if (scaled >= 40.0F) {
            return 2;
        }
        return 1;
    }
}
