package com.pr1tcha.riftborne.rna.power.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * Long-term physical condition of the organism. Separate from {@link RNAProfile} on purpose: the
 * body and the resonant architecture are different contours, and recovery of one must never repair
 * the other.
 *
 * <p>{@code physicalLoad} is the live exertion level (0-100) that drives the Recovery adaptation
 * episode: load has to actually climb and then come back down for the body to learn anything.
 */
public record PhysicalProfile(
        double strength,
        double endurance,
        double resilience,
        double recovery,
        double physicalLoad
) {
    public static final double MIN_VALUE = 0.0D;
    public static final double MAX_VALUE = 100.0D;
    public static final double DEFAULT_VALUE = 40.0D;

    public PhysicalProfile {
        strength = clamp(strength);
        endurance = clamp(endurance);
        resilience = clamp(resilience);
        recovery = clamp(recovery);
        physicalLoad = clamp(physicalLoad);
    }

    public static final Codec<PhysicalProfile> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.optionalFieldOf("strength", DEFAULT_VALUE).forGetter(PhysicalProfile::strength),
            Codec.DOUBLE.optionalFieldOf("endurance", DEFAULT_VALUE).forGetter(PhysicalProfile::endurance),
            Codec.DOUBLE.optionalFieldOf("resilience", DEFAULT_VALUE).forGetter(PhysicalProfile::resilience),
            Codec.DOUBLE.optionalFieldOf("recovery", DEFAULT_VALUE).forGetter(PhysicalProfile::recovery),
            Codec.DOUBLE.optionalFieldOf("physical_load", 0.0D).forGetter(PhysicalProfile::physicalLoad)
    ).apply(inst, PhysicalProfile::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PhysicalProfile> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /** A save with no physical section starts every parameter at the baseline, not at zero. */
    public static PhysicalProfile initial() {
        return new PhysicalProfile(DEFAULT_VALUE, DEFAULT_VALUE, DEFAULT_VALUE, DEFAULT_VALUE, 0.0D);
    }

    public double get(PhysicalStat stat) {
        return switch (stat) {
            case STRENGTH -> strength;
            case ENDURANCE -> endurance;
            case RESILIENCE -> resilience;
            case RECOVERY -> recovery;
        };
    }

    public PhysicalProfile with(PhysicalStat stat, double value) {
        return switch (stat) {
            case STRENGTH -> new PhysicalProfile(value, endurance, resilience, recovery, physicalLoad);
            case ENDURANCE -> new PhysicalProfile(strength, value, resilience, recovery, physicalLoad);
            case RESILIENCE -> new PhysicalProfile(strength, endurance, value, recovery, physicalLoad);
            case RECOVERY -> new PhysicalProfile(strength, endurance, resilience, value, physicalLoad);
        };
    }

    public PhysicalProfile withPhysicalLoad(double value) {
        return new PhysicalProfile(strength, endurance, resilience, recovery, value);
    }

    /** Mean condition — the value the future physical organism gate will be derived from. */
    public double overall() {
        return (strength + endurance + resilience + recovery) / 4.0D;
    }

    private static double clamp(double value) {
        return Mth.clamp(value, MIN_VALUE, MAX_VALUE);
    }
}
