package com.pr1tcha.riftborne.rna.power.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pr1tcha.riftborne.rna.power.Primitive;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * The PS V2.5 player power profile — the single authoritative model stored as a NeoForge Data
 * Attachment (see {@link com.pr1tcha.riftborne.rna.power.data.ModPowerAttachments}). Immutable
 * record; mutate by rebuilding via the {@code with*} helpers and calling {@code setData}.
 *
 * <p>Facet / Ogranki / Apex are added in later phases (F6/F7) as optional Codec fields so existing
 * saves keep loading.
 */
public record RNAProfile(
        boolean active,
        double throughput,
        int connectivity,
        double nodeDensity,
        double overloadRes,
        float metaWear,
        String formationPath,
        Map<String, Integer> primitiveLevels,
        double connectivityProgress
) {
    public static final double MAX_STAT = 100.0D;
    public static final int MAX_META_WEAR = 100;
    public static final int MIN_CONNECTIVITY = 1;
    public static final int MAX_CONNECTIVITY = 3;
    public static final int MIN_PRIMITIVE_LEVEL = 1;
    public static final int MAX_PRIMITIVE_LEVEL = 4;

    public RNAProfile {
        throughput = Mth.clamp(throughput, 0.0D, MAX_STAT);
        connectivity = Mth.clamp(connectivity, MIN_CONNECTIVITY, MAX_CONNECTIVITY);
        nodeDensity = Mth.clamp(nodeDensity, 0.0D, MAX_STAT);
        overloadRes = Mth.clamp(overloadRes, 0.0D, MAX_STAT);
        metaWear = Mth.clamp(metaWear, 0.0F, MAX_META_WEAR);
        formationPath = formationPath == null ? "none" : formationPath;
        primitiveLevels = primitiveLevels == null ? Map.of() : Map.copyOf(primitiveLevels);
        connectivityProgress = Math.max(0.0D, connectivityProgress);
    }

    public static final Codec<RNAProfile> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.fieldOf("active").forGetter(RNAProfile::active),
            Codec.DOUBLE.fieldOf("throughput").forGetter(RNAProfile::throughput),
            Codec.INT.fieldOf("connectivity").forGetter(RNAProfile::connectivity),
            Codec.DOUBLE.fieldOf("node_density").forGetter(RNAProfile::nodeDensity),
            Codec.DOUBLE.fieldOf("overload_res").forGetter(RNAProfile::overloadRes),
            Codec.FLOAT.fieldOf("meta_wear").forGetter(RNAProfile::metaWear),
            Codec.STRING.fieldOf("formation_path").forGetter(RNAProfile::formationPath),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("primitive_levels", Map.of())
                    .forGetter(RNAProfile::primitiveLevels),
            Codec.DOUBLE.optionalFieldOf("connectivity_progress", 0.0D)
                    .forGetter(RNAProfile::connectivityProgress)
    ).apply(inst, RNAProfile::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RNAProfile> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static RNAProfile empty() {
        return new RNAProfile(false, 10.0D, 1, 10.0D, 10.0D, 0.0F, "none", Map.of(), 0.0D);
    }

    /** Execution level (1-4) of a primitive; absent = base level 1. */
    public int level(Primitive primitive) {
        return Mth.clamp(primitiveLevels.getOrDefault(primitive.id(), MIN_PRIMITIVE_LEVEL),
                MIN_PRIMITIVE_LEVEL, MAX_PRIMITIVE_LEVEL);
    }

    public RNAProfile withActive(boolean value) {
        return new RNAProfile(value, throughput, connectivity, nodeDensity, overloadRes,
                metaWear, formationPath, primitiveLevels, connectivityProgress);
    }

    public RNAProfile withMetaWear(float value) {
        return new RNAProfile(active, throughput, connectivity, nodeDensity, overloadRes,
                value, formationPath, primitiveLevels, connectivityProgress);
    }

    public RNAProfile withFormationPath(String value) {
        return new RNAProfile(active, throughput, connectivity, nodeDensity, overloadRes,
                metaWear, value, primitiveLevels, connectivityProgress);
    }

    public RNAProfile withStats(double throughput, int connectivity, double nodeDensity, double overloadRes) {
        return new RNAProfile(active, throughput, connectivity, nodeDensity, overloadRes,
                metaWear, formationPath, primitiveLevels, connectivityProgress);
    }

    /** Hidden Connectivity progress; crossing the threshold is what promotes the C-class. */
    public RNAProfile withConnectivityProgress(double value) {
        return new RNAProfile(active, throughput, connectivity, nodeDensity, overloadRes,
                metaWear, formationPath, primitiveLevels, value);
    }

    public RNAProfile withPrimitiveLevel(Primitive primitive, int level) {
        Map<String, Integer> updated = new HashMap<>(primitiveLevels);
        updated.put(primitive.id(), Mth.clamp(level, MIN_PRIMITIVE_LEVEL, MAX_PRIMITIVE_LEVEL));
        return new RNAProfile(active, throughput, connectivity, nodeDensity, overloadRes,
                metaWear, formationPath, updated, connectivityProgress);
    }
}
