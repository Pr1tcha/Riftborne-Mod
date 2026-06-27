package com.pr1tcha.riftborne.rna.combat.ability;

import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityCost;
import com.pr1tcha.riftborne.rna.combat.data.RnaAffinityTag;
import com.pr1tcha.riftborne.rna.combat.requirement.RnaAbilityRequirement;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public record RnaAbility(
        ResourceLocation id,
        String titleKey,
        RnaAbilityType type,
        RnaAbilityRequirement requirement,
        Map<String, RnaAbilityCost> actionCosts,
        int cooldownTicks,
        boolean heavy,
        RnaAbilityInputType inputType,
        float baseLoad,
        int durationTicks,
        Set<RnaAffinityTag> affinityTags,
        String handlerId
) {
    public RnaAbility(
            ResourceLocation id,
            String titleKey,
            RnaAbilityType type,
            RnaAbilityRequirement requirement,
            Map<String, RnaAbilityCost> actionCosts,
            int cooldownTicks,
            boolean heavy
    ) {
        this(
                id,
                titleKey,
                type,
                requirement,
                actionCosts,
                cooldownTicks,
                heavy,
                RnaAbilityInputType.PRESS,
                0.0F,
                0,
                Set.of(),
                ""
        );
    }

    public RnaAbility {
        if (id == null) {
            throw new IllegalArgumentException("RNA ability id cannot be null");
        }
        titleKey = titleKey == null || titleKey.isBlank() ? id.toString() : titleKey;
        type = type == null ? RnaAbilityType.UTILITY : type;
        requirement = requirement == null ? RnaAbilityRequirement.activeRna() : requirement;
        actionCosts = actionCosts == null ? Map.of() : Map.copyOf(actionCosts);
        cooldownTicks = Math.max(0, cooldownTicks);
        inputType = inputType == null ? RnaAbilityInputType.PRESS : inputType;
        baseLoad = Math.max(0.0F, baseLoad);
        durationTicks = Math.max(0, durationTicks);
        affinityTags = affinityTags == null ? Set.of() : Set.copyOf(affinityTags);
        handlerId = handlerId == null ? "" : handlerId;
    }

    public RnaAbilityCost costFor(String action) {
        return actionCosts.getOrDefault(action, RnaAbilityCost.NONE);
    }

    public boolean isBasicCombatSkill() {
        return baseLoad > 0.0F || !affinityTags.isEmpty();
    }
}
