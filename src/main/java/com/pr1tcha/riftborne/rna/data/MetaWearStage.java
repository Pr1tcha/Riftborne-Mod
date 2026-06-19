package com.pr1tcha.riftborne.rna.data;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum MetaWearStage {
    STABLE(0, 24, "message.riftborne.rna.stage.stable"),
    STRAIN(25, 49, "message.riftborne.rna.stage.strain"),
    DISTORTION(50, 74, "message.riftborne.rna.stage.distortion"),
    REJECTION(75, 94, "message.riftborne.rna.stage.rejection"),
    ARCHITECTURE_BREAK(95, 100, "message.riftborne.rna.stage.architecture_break");

    private final int minimum;
    private final int maximum;
    private final String messageKey;

    MetaWearStage(int minimum, int maximum, String messageKey) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.messageKey = messageKey;
    }

    public int minimum() {
        return minimum;
    }

    public int maximum() {
        return maximum;
    }

    public MutableComponent transitionMessage() {
        return Component.translatable(messageKey);
    }

    public static MetaWearStage fromWear(int metaWear) {
        int value = Math.max(0, Math.min(100, metaWear));
        for (MetaWearStage stage : values()) {
            if (value >= stage.minimum && value <= stage.maximum) {
                return stage;
            }
        }
        return ARCHITECTURE_BREAK;
    }
}
