package com.pr1tcha.riftborne.rna.combat.progression;

import com.pr1tcha.riftborne.rna.data.MetaWearStage;
import com.pr1tcha.riftborne.rna.data.RnaData;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record RnaTechniqueDefinition(
        ResourceLocation id,
        String titleKey,
        int minNodeDensity,
        int minConnectivity,
        int minThroughput,
        int minOverloadResistance,
        MetaWearStage maximumCalibrationStage,
        Map<ResourceLocation, RnaTechniqueStage> prerequisiteTechniques,
        int masteryUses,
        Map<RnaAspectResonance, Integer> resonanceWeights
) {
    public RnaTechniqueDefinition {
        if (id == null) {
            throw new IllegalArgumentException("Technique id cannot be null");
        }
        titleKey = titleKey == null || titleKey.isBlank() ? id.toString() : titleKey;
        minNodeDensity = Math.max(0, minNodeDensity);
        minConnectivity = Math.max(0, minConnectivity);
        minThroughput = Math.max(0, minThroughput);
        minOverloadResistance = Math.max(0, minOverloadResistance);
        maximumCalibrationStage = maximumCalibrationStage == null
                ? MetaWearStage.DISTORTION
                : maximumCalibrationStage;
        prerequisiteTechniques = prerequisiteTechniques == null ? Map.of() : Map.copyOf(prerequisiteTechniques);
        masteryUses = Math.max(1, masteryUses);
        resonanceWeights = resonanceWeights == null ? Map.of() : Map.copyOf(resonanceWeights);
    }

    public boolean isStructurallyReady(RnaData rna) {
        return rna.nodeDensity() >= minNodeDensity
                && rna.connectivity() >= minConnectivity
                && rna.throughput() >= minThroughput
                && rna.overloadResistance() >= minOverloadResistance;
    }

    public boolean isStableEnough(RnaData rna) {
        return rna.metaWearStage().ordinal() <= maximumCalibrationStage.ordinal();
    }
}
