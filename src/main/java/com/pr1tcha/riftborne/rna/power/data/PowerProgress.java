package com.pr1tcha.riftborne.rna.power.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Slow-changing progression state, kept in its own attachment so the fast {@link RNAProfile} stat
 * record does not churn every phase. Holds Δ-axis practice counts (the raw material for facet
 * crystallization) and the crystallized {@link Facet}. Ogranki/Apex are added here in later phases
 * as optional Codec fields.
 */
public record PowerProgress(Map<String, Integer> practiceByAxis, Optional<Facet> facet) {

    public PowerProgress {
        practiceByAxis = practiceByAxis == null ? Map.of() : Map.copyOf(practiceByAxis);
        facet = facet == null ? Optional.empty() : facet;
    }

    public static final Codec<PowerProgress> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("practice_by_axis", Map.of())
                    .forGetter(PowerProgress::practiceByAxis),
            Facet.CODEC.optionalFieldOf("facet").forGetter(PowerProgress::facet)
    ).apply(inst, PowerProgress::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PowerProgress> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static PowerProgress empty() {
        return new PowerProgress(Map.of(), Optional.empty());
    }

    public int practice(String axisId) {
        return practiceByAxis.getOrDefault(axisId, 0);
    }

    public int totalPractice() {
        int sum = 0;
        for (int v : practiceByAxis.values()) {
            sum += v;
        }
        return sum;
    }

    public PowerProgress withPractice(String axisId, int delta) {
        Map<String, Integer> updated = new HashMap<>(practiceByAxis);
        updated.merge(axisId, delta, Integer::sum);
        return new PowerProgress(updated, facet);
    }

    public PowerProgress withFacet(Facet value) {
        return new PowerProgress(practiceByAxis, Optional.ofNullable(value));
    }
}
