package com.pr1tcha.riftborne.rna.power.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * Counters for the current adaptation cycle. Growth is never applied the moment something happens —
 * activity accumulates here, and only when the cycle closes does it convert into parameter growth.
 * That is what keeps a burst of spam from being worth more than steady practice.
 *
 * <p>Everything in here is throwaway state that resets when the cycle rolls over; the durable
 * results live in {@link PhysicalProfile} and {@link RNAProfile}.
 */
public record AdaptationCycle(
        long activeTicks,
        Map<String, Double> physicalActivity,
        int recoveryEpisodes,
        Map<String, Double> rnaPractice,
        Map<String, Integer> primitiveUses
) {
    public static final double MAX_ACTIVITY = 100.0D;

    public AdaptationCycle {
        activeTicks = Math.max(0L, activeTicks);
        physicalActivity = physicalActivity == null ? Map.of() : Map.copyOf(physicalActivity);
        recoveryEpisodes = Math.max(0, recoveryEpisodes);
        rnaPractice = rnaPractice == null ? Map.of() : Map.copyOf(rnaPractice);
        primitiveUses = primitiveUses == null ? Map.of() : Map.copyOf(primitiveUses);
    }

    public static final Codec<AdaptationCycle> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.LONG.optionalFieldOf("active_ticks", 0L).forGetter(AdaptationCycle::activeTicks),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)
                    .optionalFieldOf("physical_activity", Map.of()).forGetter(AdaptationCycle::physicalActivity),
            Codec.INT.optionalFieldOf("recovery_episodes", 0).forGetter(AdaptationCycle::recoveryEpisodes),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)
                    .optionalFieldOf("rna_practice", Map.of()).forGetter(AdaptationCycle::rnaPractice),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("primitive_uses", Map.of()).forGetter(AdaptationCycle::primitiveUses)
    ).apply(inst, AdaptationCycle::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdaptationCycle> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static AdaptationCycle empty() {
        return new AdaptationCycle(0L, Map.of(), 0, Map.of(), Map.of());
    }

    public double activity(PhysicalStat stat) {
        return physicalActivity.getOrDefault(stat.id(), 0.0D);
    }

    public double practice(RnaTrack track) {
        return rnaPractice.getOrDefault(track.id(), 0.0D);
    }

    /** Raw sub-counter access, used to cap how much of a norm one source may fill. */
    public double raw(String key) {
        return physicalActivity.getOrDefault(key, 0.0D);
    }

    public AdaptationCycle addRaw(String key, double amount) {
        if (amount <= 0.0D) {
            return this;
        }
        Map<String, Double> updated = new HashMap<>(physicalActivity);
        updated.merge(key, amount, Double::sum);
        return new AdaptationCycle(activeTicks, updated, recoveryEpisodes, rnaPractice, primitiveUses);
    }

    public int primitiveUses(String primitiveId) {
        return primitiveUses.getOrDefault(primitiveId, 0);
    }

    public AdaptationCycle withActiveTicks(long ticks) {
        return new AdaptationCycle(ticks, physicalActivity, recoveryEpisodes, rnaPractice, primitiveUses);
    }

    public AdaptationCycle addActivity(PhysicalStat stat, double amount) {
        if (amount <= 0.0D) {
            return this;
        }
        Map<String, Double> updated = new HashMap<>(physicalActivity);
        updated.merge(stat.id(), amount, (a, b) -> Mth.clamp(a + b, 0.0D, MAX_ACTIVITY));
        return new AdaptationCycle(activeTicks, updated, recoveryEpisodes, rnaPractice, primitiveUses);
    }

    public AdaptationCycle withRecoveryEpisodes(int episodes) {
        return new AdaptationCycle(activeTicks, physicalActivity, episodes, rnaPractice, primitiveUses);
    }

    public AdaptationCycle addPractice(RnaTrack track, double amount) {
        if (amount <= 0.0D) {
            return this;
        }
        Map<String, Double> updated = new HashMap<>(rnaPractice);
        updated.merge(track.id(), amount, Double::sum);
        return new AdaptationCycle(activeTicks, physicalActivity, recoveryEpisodes, updated, primitiveUses);
    }

    /** Raw practice-map access for bookkeeping that is not one of the four tracks. */
    public double rawPractice(String key) {
        return rnaPractice.getOrDefault(key, 0.0D);
    }

    public AdaptationCycle addRawPractice(String key, double amount) {
        if (amount <= 0.0D) {
            return this;
        }
        Map<String, Double> updated = new HashMap<>(rnaPractice);
        updated.merge(key, amount, Double::sum);
        return new AdaptationCycle(activeTicks, physicalActivity, recoveryEpisodes, updated, primitiveUses);
    }

    public AdaptationCycle countPrimitiveUse(String primitiveId) {
        Map<String, Integer> updated = new HashMap<>(primitiveUses);
        updated.merge(primitiveId, 1, Integer::sum);
        return new AdaptationCycle(activeTicks, physicalActivity, recoveryEpisodes, rnaPractice, updated);
    }
}
