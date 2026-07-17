package com.pr1tcha.riftborne.rna.power;

import com.pr1tcha.riftborne.rna.power.data.ModPowerAttachments;
import com.pr1tcha.riftborne.rna.power.data.RNAProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Server-side entry point for the PS V2.5 power system. All reads/writes of {@link RNAProfile}
 * go through here so persistence and rules stay in one place. This is the new authoritative API,
 * separate from the legacy {@code RnaApi}; the two coexist until the migration phase (F9).
 */
public final class PowerApi {
    private PowerApi() {
    }

    public static RNAProfile get(Player player) {
        return player.getData(ModPowerAttachments.RNA_PROFILE.get());
    }

    public static void set(ServerPlayer player, RNAProfile profile) {
        player.setData(ModPowerAttachments.RNA_PROFILE.get(), profile);
    }

    public static boolean hasActive(Player player) {
        return get(player).active();
    }

    /** Activate RNA on a formation path, seeding starting stats per spec §1.3 skews. */
    public static void activate(ServerPlayer player, String formationPath) {
        RNAProfile seeded = switch (formationPath) {
            // throughput, connectivity(C-class), nodeDensity, overloadRes
            case "stress" -> baseProfile(35, 2, 35, 22, formationPath);
            case "artificial" -> baseProfile(32, 1, 45, 25, formationPath);
            case "tech" -> baseProfile(45, 1, 20, 30, formationPath);
            case "interspace" -> baseProfile(30, 1, 30, 20, formationPath);
            default -> baseProfile(25, 1, 25, 35, "training");
        };
        set(player, seeded.withActive(true));
    }

    public static void reset(ServerPlayer player) {
        set(player, RNAProfile.empty());
    }

    /**
     * Add meta-wear, reduced by overload resistance. Meta-wear is the "pay forever" regulator:
     * it accumulates and narrows the admissibility window (see {@link PowerRules}).
     */
    public static void addMetaWear(ServerPlayer player, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        RNAProfile profile = get(player);
        if (!profile.active()) {
            return;
        }
        float reduction = 1.0F - profile.overloadRes() * 0.003F;
        float adjusted = amount * Mth.clamp(reduction, 0.1F, 1.0F);
        set(player, profile.withMetaWear(profile.metaWear() + adjusted));
    }

    public static void reduceMetaWear(ServerPlayer player, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        RNAProfile profile = get(player);
        if (!profile.active() || profile.metaWear() <= 0.0F) {
            return;
        }
        set(player, profile.withMetaWear(profile.metaWear() - amount));
    }

    private static RNAProfile baseProfile(int throughput, int connectivity, int nodeDensity,
                                          int overloadRes, String path) {
        return new RNAProfile(false, throughput, connectivity, nodeDensity, overloadRes,
                0.0F, path, java.util.Map.of());
    }
}
