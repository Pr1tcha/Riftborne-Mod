package com.pr1tcha.riftborne.rna.combat.ability;

import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityCost;
import com.pr1tcha.riftborne.rna.combat.requirement.RnaAbilityRequirement;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record RnaAbility(
        ResourceLocation id,
        String titleKey,
        RnaAbilityType type,
        RnaAbilityRequirement requirement,
        Map<String, RnaAbilityCost> actionCosts,
        int cooldownTicks,
        boolean heavy
) {
    public RnaAbility {
        if (id == null) {
            throw new IllegalArgumentException("RNA ability id cannot be null");
        }
        titleKey = titleKey == null || titleKey.isBlank() ? id.toString() : titleKey;
        type = type == null ? RnaAbilityType.UTILITY : type;
        requirement = requirement == null ? RnaAbilityRequirement.activeRna() : requirement;
        actionCosts = actionCosts == null ? Map.of() : Map.copyOf(actionCosts);
        cooldownTicks = Math.max(0, cooldownTicks);
    }

    public RnaAbilityCost costFor(String action) {
        return actionCosts.getOrDefault(action, RnaAbilityCost.NONE);
    }
}
