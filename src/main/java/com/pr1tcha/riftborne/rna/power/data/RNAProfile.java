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
        int throughput,
        int connectivity,
        int nodeDensity,
        int overloadRes,
        float metaWear,
        String formationPath,
        Map<String, Integer> primitiveLevels
) {
    public static final int MAX_STAT = 100;
    public static final int MAX_META_WEAR = 100;
    public static final int MIN_CONNECTIVITY = 1;
    public static final int MAX_CONNECTIVITY = 3;
    public static final int MIN_PRIMITIVE_LEVEL = 1;
    public static final int MAX_PRIMITIVE_LEVEL = 4;

    public RNAProfile {
        throughput = Mth.clamp(throughput, 0, MAX_STAT);
        connectivity = Mth.clamp(connectivity, MIN_CONNECTIVITY, MAX_CONNECTIVITY);
        nodeDensity = Mth.clamp(nodeDensity, 0, MAX_STAT);
        overloadRes = Mth.clamp(overloadRes, 0, MAX_STAT);
        metaWear = Mth.clamp(metaWear, 0.0F, MAX_META_WEAR);
        formationPath = formationPath == null ? "none" : formationPath;
        primitiveLevels = primitiveLevels == null ? Map.of() : Map.copyOf(primitiveLevels);
    }

    public static final Codec<RNAProfile> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.fieldOf("active").forGetter(RNAProfile::active),
            Codec.INT.fieldOf("throughput").forGetter(RNAProfile::throughput),
            Codec.INT.fieldOf("connectivity").forGetter(RNAProfile::connectivity),
            Codec.INT.fieldOf("node_density").forGetter(RNAProfile::nodeDensity),
            Codec.INT.fieldOf("overload_res").forGetter(RNAProfile::overloadRes),
            Codec.FLOAT.fieldOf("meta_wear").forGetter(RNAProfile::metaWear),
            Codec.STRING.fieldOf("formation_path").forGetter(RNAProfile::formationPath),
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("primitive_levels", Map.of())
                    .forGetter(RNAProfile::primitiveLevels)
    ).apply(inst, RNAProfile::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RNAProfile> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public static RNAProfile empty() {
        return new RNAProfile(false, 10, 1, 10, 10, 0.0F, "none", Map.of());
    }

    /** Execution level (1-4) of a primitive; absent = base level 1. */
    public int level(Primitive primitive) {
        return Mth.clamp(primitiveLevels.getOrDefault(primitive.id(), MIN_PRIMITIVE_LEVEL),
                MIN_PRIMITIVE_LEVEL, MAX_PRIMITIVE_LEVEL);
    }

    public RNAProfile withActive(boolean value) {
        return new RNAProfile(value, throughput, connectivity, nodeDensity, overloadRes,
                metaWear, formationPath, primitiveLevels);
    }

    public RNAProfile withMetaWear(float value) {
        return new RNAProfile(active, throughput, connectivity, nodeDensity, overloadRes,
                value, formationPath, primitiveLevels);
    }

    public RNAProfile withFormationPath(String value) {
        return new RNAProfile(active, throughput, connectivity, nodeDensity, overloadRes,
                metaWear, value, primitiveLevels);
    }

    public RNAProfile withStats(int throughput, int connectivity, int nodeDensity, int overloadRes) {
        return new RNAProfile(active, throughput, connectivity, nodeDensity, overloadRes,
                metaWear, formationPath, primitiveLevels);
    }

    public RNAProfile withPrimitiveLevel(Primitive primitive, int level) {
        Map<String, Integer> updated = new HashMap<>(primitiveLevels);
        updated.put(primitive.id(), Mth.clamp(level, MIN_PRIMITIVE_LEVEL, MAX_PRIMITIVE_LEVEL));
        return new RNAProfile(active, throughput, connectivity, nodeDensity, overloadRes,
                metaWear, formationPath, updated);
    }
}
