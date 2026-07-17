package com.pr1tcha.riftborne.rna.power;

import com.pr1tcha.riftborne.rna.power.data.Facet;
import com.pr1tcha.riftborne.rna.power.data.PowerProgress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Facet crystallization (spec §5.3): a Facet is not chosen but forms once, from the dominant Δ-axis
 * in the Synchron's practice, at a stress spike (low HP while RNA is active). Requires a minimum of
 * accumulated practice so it reflects a real habit, not a single cast.
 */
public final class PowerCrystallization {
    public static final int MIN_PRACTICE = 20;

    private PowerCrystallization() {
    }

    /** True if the player is eligible to crystallize a facet right now (no facet, enough practice). */
    public static boolean canCrystallize(ServerPlayer player) {
        if (!PowerApi.hasActive(player)) {
            return false;
        }
        PowerProgress progress = PowerApi.getProgress(player);
        return progress.facet().isEmpty() && progress.totalPractice() >= MIN_PRACTICE;
    }

    /** Attempt to crystallize; returns true if a facet was formed. */
    public static boolean tryCrystallize(ServerPlayer player) {
        if (!canCrystallize(player)) {
            return false;
        }
        PowerProgress progress = PowerApi.getProgress(player);
        List<String> dominant = dominantAxes(progress.practiceByAxis());
        if (dominant.isEmpty()) {
            return false;
        }
        Facet facet = new Facet(dominant, Facet.signatureForAxis(dominant.get(0)));
        PowerApi.setProgress(player, progress.withFacet(facet));

        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(),
                60, 0.4D, 0.8D, 0.4D, 0.05D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.PLAYERS, 1.0F, 0.7F);
        player.sendSystemMessage(Component.translatable(
                "message.riftborne.power.facet_crystallized",
                Component.translatable("rna.riftborne.facet.signature." + facet.signature())
        ).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** The one or two axes with the highest practice count (ties keep both). */
    private static List<String> dominantAxes(Map<String, Integer> practice) {
        int max = 0;
        for (int v : practice.values()) {
            max = Math.max(max, v);
        }
        List<String> result = new ArrayList<>();
        if (max <= 0) {
            return result;
        }
        for (Map.Entry<String, Integer> entry : practice.entrySet()) {
            if (entry.getValue() == max) {
                result.add(entry.getKey());
            }
        }
        // Keep at most two dominant axes (spec: convergence is max 2 families).
        return result.size() > 2 ? result.subList(0, 2) : result;
    }
}
