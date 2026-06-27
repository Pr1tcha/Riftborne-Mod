package com.pr1tcha.riftborne.rna.combat.data;

public enum RnaLoadBand {
    NORMAL,
    STRAIN,
    OVERHEAT,
    BREAKDOWN;

    public static RnaLoadBand fromLoad(float load, int warningThreshold, int lockThreshold) {
        if (load >= lockThreshold) {
            return BREAKDOWN;
        }
        if (load >= 75.0F || load >= warningThreshold) {
            return OVERHEAT;
        }
        if (load >= 50.0F) {
            return STRAIN;
        }
        return NORMAL;
    }

    public float cooldownMultiplier() {
        return switch (this) {
            case NORMAL -> 1.0F;
            case STRAIN -> 1.10F;
            case OVERHEAT -> 1.25F;
            case BREAKDOWN -> 1.50F;
        };
    }
}
