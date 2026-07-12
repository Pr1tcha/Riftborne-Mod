package com.pr1tcha.riftborne.rna.combat.barrier;

import net.minecraft.world.entity.HumanoidArm;

public enum BarrierGestureMode {
    TWO_HANDED,
    LEFT_HANDED,
    HANDS_FREE;

    public boolean occupies(HumanoidArm arm) {
        return switch (this) {
            case TWO_HANDED -> true;
            case LEFT_HANDED -> arm == HumanoidArm.LEFT;
            case HANDS_FREE -> false;
        };
    }

    public static BarrierGestureMode fromOrdinal(int ordinal) {
        BarrierGestureMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : TWO_HANDED;
    }
}
