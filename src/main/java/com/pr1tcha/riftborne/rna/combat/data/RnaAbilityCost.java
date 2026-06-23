package com.pr1tcha.riftborne.rna.combat.data;

import com.pr1tcha.riftborne.rna.data.RnaStat;

public record RnaAbilityCost(
        int baseMetaWear,
        RnaStat growthStat,
        int growthAmount,
        int growthCooldownTicks
) {
    public static final RnaAbilityCost NONE = new RnaAbilityCost(0, null, 0, 0);

    public RnaAbilityCost {
        baseMetaWear = Math.max(0, baseMetaWear);
        growthAmount = Math.max(0, growthAmount);
        growthCooldownTicks = Math.max(0, growthCooldownTicks);
    }
}
