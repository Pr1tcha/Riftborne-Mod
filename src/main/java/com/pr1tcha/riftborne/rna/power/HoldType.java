package com.pr1tcha.riftborne.rna.power;

/**
 * How a technique is sustained, feeding K_hold in the load formula (spec §4).
 * MOMENT is a one-shot cast; HOLD/LONG scale with sustained duration and are grown further
 * per-second by the tick service.
 */
public enum HoldType {
    MOMENT(1.0F),
    HOLD(1.6F),
    LONG(2.4F);

    private final float multiplier;

    HoldType(float multiplier) {
        this.multiplier = multiplier;
    }

    public float multiplier() {
        return multiplier;
    }
}
