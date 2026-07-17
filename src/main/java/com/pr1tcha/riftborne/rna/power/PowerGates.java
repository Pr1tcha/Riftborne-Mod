package com.pr1tcha.riftborne.rna.power;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * The four organism gates of PS V2.5 (spec §5.5) that cap how far RNA parameters can be trained.
 * All four are placeholder defaults for now. The physical-condition source that fed {@code phys}
 * was removed pending a training rework, so {@code phys} is a neutral default until the reworked
 * training system supplies it; {@code mental/psych/genetic} become stored innate values when growth
 * caps are enforced (progression phase).
 */
public final class PowerGates {
    // Placeholder defaults until the reworked training + per-player generation land.
    private static final float DEFAULT_PHYS = 60.0F;
    private static final int DEFAULT_MENTAL = 50;
    private static final int DEFAULT_PSYCH = 50;
    private static final float DEFAULT_GENETIC = 1.0F;

    private PowerGates() {
    }

    /** Physical condition gate (0-100). Neutral placeholder until reworked training supplies it. */
    public static float phys(ServerPlayer player) {
        return DEFAULT_PHYS;
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
