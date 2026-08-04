package com.pr1tcha.riftborne.rna.power;

import com.pr1tcha.riftborne.config.Config;
import com.pr1tcha.riftborne.rna.power.data.AdaptationCycle;
import com.pr1tcha.riftborne.rna.power.data.ModPowerAttachments;
import com.pr1tcha.riftborne.rna.power.data.PhysicalProfile;
import com.pr1tcha.riftborne.rna.power.data.PhysicalStat;
import com.pr1tcha.riftborne.rna.power.data.RNAProfile;
import com.pr1tcha.riftborne.rna.power.data.RnaTrack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * Owner of the adaptation cycle: it counts the player's active ticks, and when a cycle closes it
 * converts the accumulated activity into actual parameter growth.
 *
 * <p>Two deliberate properties. First, nothing grows at the moment it happens — a burst of spam and
 * a steady hour both end up capped by the same per-cycle ceiling. Second, growth is throttled by
 * diminishing returns on the current value, so ordinary play carries a character to a competent
 * baseline and then slows to a crawl rather than stopping outright.
 */
public final class AdaptationService {
    private AdaptationService() {
    }

    public static PhysicalProfile physical(ServerPlayer player) {
        return player.getData(ModPowerAttachments.PHYSICAL_PROFILE.get());
    }

    public static void setPhysical(ServerPlayer player, PhysicalProfile profile) {
        player.setData(ModPowerAttachments.PHYSICAL_PROFILE.get(), profile);
    }

    public static AdaptationCycle cycle(ServerPlayer player) {
        return player.getData(ModPowerAttachments.ADAPTATION_CYCLE.get());
    }

    public static void setCycle(ServerPlayer player, AdaptationCycle cycle) {
        player.setData(ModPowerAttachments.ADAPTATION_CYCLE.get(), cycle);
    }

    /** Record physical activity toward a stat for the current cycle. */
    public static void addActivity(ServerPlayer player, PhysicalStat stat, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        setCycle(player, cycle(player).addActivity(stat, amount));
    }

    /** Record RNA practice toward a training channel for the current cycle. */
    public static void addPractice(ServerPlayer player, RnaTrack track, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        setCycle(player, cycle(player).addPractice(track, amount));
    }

    /**
     * Advance the cycle by one active tick. Called only for players who are actually in the world
     * and able to act, so AFK, sleep, pause and offline time never move it.
     */
    public static void tickActive(ServerPlayer player) {
        AdaptationCycle current = cycle(player);
        long ticks = current.activeTicks() + 1L;
        if (ticks < Config.adaptationCycleTicks.get()) {
            setCycle(player, current.withActiveTicks(ticks));
            return;
        }
        completeCycle(player, current);
    }

    /** Apply everything the closed cycle earned and start a fresh one. */
    public static void completeCycle(ServerPlayer player, AdaptationCycle closed) {
        applyPhysicalGrowth(player, closed);
        applyRnaGrowth(player, closed);
        setCycle(player, AdaptationCycle.empty());
    }

    private static void applyPhysicalGrowth(ServerPlayer player, AdaptationCycle closed) {
        PhysicalProfile profile = physical(player);
        for (PhysicalStat stat : PhysicalStat.values()) {
            double current = profile.get(stat);
            double growth = basePhysicalGrowth(closed.activity(stat)) * diminishing(current);
            if (growth > 0.0D) {
                profile = profile.with(stat, current + growth);
            }
        }
        setPhysical(player, profile);
    }

    /** Activity below the first threshold teaches the body nothing at all. */
    public static double basePhysicalGrowth(double activityScore) {
        if (activityScore >= AdaptationCycle.MAX_ACTIVITY) {
            return Config.adaptationGrowthFull.get();
        }
        if (activityScore >= Config.adaptationActivityHigh.get()) {
            return Config.adaptationGrowthHigh.get();
        }
        if (activityScore >= Config.adaptationActivityLow.get()) {
            return Config.adaptationGrowthLow.get();
        }
        return 0.0D;
    }

    /** The higher a parameter already is, the less an ordinary cycle moves it. */
    public static double diminishing(double currentValue) {
        if (currentValue >= 90.0D) {
            return 0.05D;
        }
        if (currentValue >= 75.0D) {
            return 0.20D;
        }
        if (currentValue >= 60.0D) {
            return 0.50D;
        }
        return 1.0D;
    }

    private static void applyRnaGrowth(ServerPlayer player, AdaptationCycle closed) {
        RNAProfile profile = PowerApi.get(player);
        if (!profile.active()) {
            return;
        }
        double required = Math.max(1.0D, Config.adaptationRequiredPractice.get());

        double bandwidth = grown(profile.throughput(), closed.practice(RnaTrack.BANDWIDTH), required);
        double nodeDensity = grown(profile.nodeDensity(), closed.practice(RnaTrack.NODE_DENSITY), required);
        double overloadRes = grown(profile.overloadRes(), closed.practice(RnaTrack.OVERLOAD_RESISTANCE), required);
        profile = profile.withStats(bandwidth, profile.connectivity(), nodeDensity, overloadRes);

        profile = advanceConnectivity(profile, PowerPractice.connectivityPractice(closed));
        PowerApi.set(player, profile);
    }

    /**
     * Growth toward one numeric RNA parameter: the per-cycle ceiling for its current value, scaled
     * by how much of the required quality practice the player actually delivered.
     */
    private static double grown(double current, double practice, double required) {
        double cap = rnaCycleCap(current);
        double growth = cap * Mth.clamp(practice / required, 0.0D, 1.0D);
        return Mth.clamp(current + growth, 0.0D, RNAProfile.MAX_STAT);
    }

    public static double rnaCycleCap(double currentValue) {
        if (currentValue >= 85.0D) {
            return 0.02D;
        }
        if (currentValue >= 70.0D) {
            return 0.08D;
        }
        if (currentValue >= 50.0D) {
            return 0.20D;
        }
        return 0.40D;
    }

    /**
     * Connectivity is a discrete C-class, so it advances on a hidden progress track instead of a
     * visible number. C2 -> C3 is intentionally not wired yet: it will additionally require a
     * crystallized Facet and convergence practice.
     */
    private static RNAProfile advanceConnectivity(RNAProfile profile, double cyclePractice) {
        if (profile.connectivity() >= RNAProfile.MAX_CONNECTIVITY) {
            return profile;
        }
        double progress = profile.connectivityProgress() + Mth.clamp(cyclePractice, 0.0D, 1.0D);
        if (profile.connectivity() == 1 && progress >= Config.adaptationConnectivityC2.get()) {
            return profile
                    .withStats(profile.throughput(), 2, profile.nodeDensity(), profile.overloadRes())
                    .withConnectivityProgress(0.0D);
        }
        return profile.withConnectivityProgress(progress);
    }
}
