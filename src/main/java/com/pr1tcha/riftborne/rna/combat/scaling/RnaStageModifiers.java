package com.pr1tcha.riftborne.rna.combat.scaling;

import com.pr1tcha.riftborne.rna.data.MetaWearStage;

public record RnaStageModifiers(
        double cooldownMultiplier,
        double metaWearMultiplier,
        boolean canUseHeavyAbilities,
        double failureChance
) {
    public static RnaStageModifiers forStage(MetaWearStage stage) {
        return switch (stage) {
            case STABLE -> new RnaStageModifiers(1.0D, 1.0D, true, 0.0D);
            case STRAIN -> new RnaStageModifiers(1.15D, 1.10D, true, 0.0D);
            case DISTORTION -> new RnaStageModifiers(1.35D, 1.20D, true, 0.0D);
            case REJECTION -> new RnaStageModifiers(1.75D, 1.35D, false, 0.0D);
            case ARCHITECTURE_BREAK -> new RnaStageModifiers(2.0D, 1.5D, false, 0.0D);
        };
    }
}
