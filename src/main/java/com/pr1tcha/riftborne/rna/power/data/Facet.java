package com.pr1tcha.riftborne.rna.power.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/**
 * A crystallized Facet (Грань) — the Synchron's personal specialization. Not chosen from a catalog;
 * it crystallizes once from dominant Δ-practice × organism × stress condition (spec §5.3). Stores the
 * dominant Δ-axes it formed around and a derived signature name. Ogranki are later re-cut through it.
 */
public record Facet(List<String> dominantAxes, String signature) {
    public static final Codec<Facet> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.listOf().fieldOf("dominant_axes").forGetter(Facet::dominantAxes),
            Codec.STRING.fieldOf("signature").forGetter(Facet::signature)
    ).apply(inst, Facet::new));

    public static String signatureForAxis(String axisId) {
        return switch (axisId) {
            case "dr" -> "distribution";
            case "de" -> "environment";
            case "ds" -> "structure";
            case "dg" -> "geometry";
            case "dv" -> "tempo";
            case "dp" -> "sequence";
            case "dst" -> "fixation";
            default -> "undefined";
        };
    }
}
