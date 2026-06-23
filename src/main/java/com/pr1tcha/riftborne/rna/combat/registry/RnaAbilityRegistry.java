package com.pr1tcha.riftborne.rna.combat.registry;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.rna.combat.ability.RnaAbility;
import com.pr1tcha.riftborne.rna.combat.ability.RnaAbilityType;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityCost;
import com.pr1tcha.riftborne.rna.combat.requirement.RnaAbilityRequirement;
import com.pr1tcha.riftborne.rna.data.RnaStat;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class RnaAbilityRegistry {
    public static final ResourceLocation TELEKINESIS_ID =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "telekinesis");

    private static final Map<ResourceLocation, RnaAbility> ABILITIES = new LinkedHashMap<>();

    static {
        register(new RnaAbility(
                TELEKINESIS_ID,
                "rna_ability.riftborne.telekinesis",
                RnaAbilityType.CONTROL,
                RnaAbilityRequirement.activeRna(),
                Map.of(
                        "grab", new RnaAbilityCost(1, RnaStat.CONNECTIVITY, 1, 240),
                        "block_grab", new RnaAbilityCost(2, RnaStat.NODE_DENSITY, 1, 400),
                        "push", new RnaAbilityCost(2, RnaStat.THROUGHPUT, 1, 240)
                ),
                0,
                false
        ));
    }

    private RnaAbilityRegistry() {
    }

    public static void register(RnaAbility ability) {
        if (ABILITIES.putIfAbsent(ability.id(), ability) != null) {
            throw new IllegalArgumentException("Duplicate RNA ability: " + ability.id());
        }
    }

    public static RnaAbility get(ResourceLocation id) {
        return ABILITIES.get(id);
    }

    public static Collection<RnaAbility> all() {
        return ABILITIES.values();
    }

    public static Collection<String> ids() {
        return ABILITIES.keySet().stream().map(ResourceLocation::toString).toList();
    }
}
