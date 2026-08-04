package com.pr1tcha.riftborne.rna.power;

import com.pr1tcha.riftborne.rna.power.data.PhysicalProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * The four organism gates of PS V2.5 (spec §5.5) that cap how far RNA parameters can be trained.
 * {@code phys} is live — it is the mean of the four trained body parameters. The other three are
 * still placeholders and become stored innate values once per-player generation lands.
 */
public final class PowerGates {
    // Placeholders until per-player generation lands; phys is live again from the body itself.
    private static final int DEFAULT_MENTAL = 50;
    private static final int DEFAULT_PSYCH = 50;
    private static final float DEFAULT_GENETIC = 1.0F;

    private PowerGates() {
    }

    /** Physical condition gate (0-100): the mean of the four body parameters. */
    public static float phys(ServerPlayer player) {
        PhysicalProfile profile = AdaptationService.physical(player);
        return (float) profile.overall();
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
