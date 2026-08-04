package com.pr1tcha.riftborne.rna.combat.requirement;

import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityResult;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityUseContext;
import com.pr1tcha.riftborne.rna.combat.scaling.RnaStageModifiers;
import com.pr1tcha.riftborne.rna.data.FormationPath;
import com.pr1tcha.riftborne.rna.data.MetaWearStage;
import com.pr1tcha.riftborne.rna.data.RnaData;
import com.pr1tcha.riftborne.rna.data.RnaStat;
import java.util.Map;
import java.util.Set;

public record RnaAbilityRequirement(
        boolean requiresActiveRna,
        Map<RnaStat, Integer> minimumStats,
        Set<FormationPath> allowedPaths,
        MetaWearStage maximumStage
) {
    public RnaAbilityRequirement {
        minimumStats = minimumStats == null ? Map.of() : Map.copyOf(minimumStats);
        allowedPaths = allowedPaths == null ? Set.of() : Set.copyOf(allowedPaths);
        maximumStage = maximumStage == null ? MetaWearStage.ARCHITECTURE_BREAK : maximumStage;
    }

    public static RnaAbilityRequirement activeRna() {
        return new RnaAbilityRequirement(true, Map.of(), Set.of(), MetaWearStage.ARCHITECTURE_BREAK);
    }

    public RnaAbilityResult check(
            RnaData data,
            RnaAbilityUseContext context,
            RnaStageModifiers modifiers,
            boolean heavy
    ) {
        if (context.level() == null || context.player().isRemoved()) {
            return RnaAbilityResult.FAIL_CONTEXT;
        }
        if (data.metaWearStage().ordinal() > maximumStage.ordinal()) {
            return RnaAbilityResult.FAIL_STAGE;
        }
        if (heavy && !modifiers.canUseHeavyAbilities()) {
            return RnaAbilityResult.FAIL_STAGE;
        }
        if (!allowedPaths.isEmpty() && !allowedPaths.contains(data.formationPath())) {
            return RnaAbilityResult.FAIL_REQUIREMENT;
        }
        for (Map.Entry<RnaStat, Integer> entry : minimumStats.entrySet()) {
            if (data.getStat(entry.getKey()) < entry.getValue()) {
                return RnaAbilityResult.FAIL_REQUIREMENT;
            }
        }
        return RnaAbilityResult.SUCCESS;
    }
}
