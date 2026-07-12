package com.pr1tcha.riftborne.rna.combat.training;

import java.util.Locale;

public enum RnaTrainingPhase {
    FORMATION(3, 20, 34, 0.55D),
    ALIGNMENT(4, 40, 26, 0.72D),
    STABILIZATION(5, 40, 20, 0.84D);

    private final int requiredSuccesses;
    private final int completionPatternGain;
    private final int telegraphTicks;
    private final double facingThreshold;

    RnaTrainingPhase(
            int requiredSuccesses,
            int completionPatternGain,
            int telegraphTicks,
            double facingThreshold
    ) {
        this.requiredSuccesses = requiredSuccesses;
        this.completionPatternGain = completionPatternGain;
        this.telegraphTicks = telegraphTicks;
        this.facingThreshold = facingThreshold;
    }

    public int requiredSuccesses() {
        return requiredSuccesses;
    }

    public int completionPatternGain() {
        return completionPatternGain;
    }

    public int telegraphTicks() {
        return telegraphTicks;
    }

    public double facingThreshold() {
        return facingThreshold;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "rna.riftborne.training_phase." + id();
    }

    public RnaTrainingPhase next() {
        return switch (this) {
            case FORMATION -> ALIGNMENT;
            case ALIGNMENT -> STABILIZATION;
            case STABILIZATION -> null;
        };
    }

    public static RnaTrainingPhase fromId(String id) {
        if (id != null) {
            for (RnaTrainingPhase phase : values()) {
                if (phase.id().equalsIgnoreCase(id)) {
                    return phase;
                }
            }
        }
        return FORMATION;
    }
}
