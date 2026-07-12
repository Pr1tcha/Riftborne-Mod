package com.pr1tcha.riftborne.rna.combat.training;

public enum TrainingPulseState {
    IDLE,
    CALIBRATING,
    TELEGRAPH,
    RECOVERY,
    PAUSED;

    public static TrainingPulseState fromOrdinal(int ordinal) {
        TrainingPulseState[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : IDLE;
    }
}
