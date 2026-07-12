package com.pr1tcha.riftborne.rna.combat.barrier;

public enum BarrierPhase {
    INACTIVE,
    DEPLOYING,
    ACTIVE;

    public static BarrierPhase fromOrdinal(int ordinal) {
        BarrierPhase[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : INACTIVE;
    }
}
