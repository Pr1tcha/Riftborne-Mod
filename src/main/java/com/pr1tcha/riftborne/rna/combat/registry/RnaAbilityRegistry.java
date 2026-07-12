package com.pr1tcha.riftborne.rna.combat.registry;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.config.Config;
import com.pr1tcha.riftborne.rna.combat.ability.RnaAbility;
import com.pr1tcha.riftborne.rna.combat.ability.RnaAbilityInputType;
import com.pr1tcha.riftborne.rna.combat.ability.RnaAbilityType;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityCost;
import com.pr1tcha.riftborne.rna.combat.data.RnaAffinityTag;
import com.pr1tcha.riftborne.rna.combat.requirement.RnaAbilityRequirement;
import com.pr1tcha.riftborne.rna.data.RnaStat;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class RnaAbilityRegistry {
    public static final ResourceLocation TELEKINESIS_ID =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "telekinesis");
    public static final ResourceLocation RNA_FOCUS_ID =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_focus");
    public static final ResourceLocation BARRIER_ID =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "barrier");
    public static final ResourceLocation LEGACY_RNA_GUARD_ID =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_guard");
    public static final ResourceLocation RNA_STRIKE_ID =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_strike");
    public static final ResourceLocation RNA_IMPULSE_STEP_ID =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_impulse_step");
    public static final ResourceLocation RNA_ANCHOR_HOLD_ID =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_anchor_hold");
    public static final ResourceLocation RNA_RECOVERY_ID =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_recovery");
    public static final ResourceLocation RNA_PULSE_PUSH_ID =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_pulse_push");
    public static final ResourceLocation RNA_DEFLECT_ID =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_deflect");
    public static final ResourceLocation RNA_REACTION_SPIKE_ID =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_reaction_spike");
    public static final ResourceLocation RNA_OVERLOAD_VENT_ID =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_overload_vent");

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
        registerBasic(
                RNA_FOCUS_ID,
                "rna_ability.riftborne.rna_focus",
                RnaAbilityType.SUPPORT,
                RnaAbilityInputType.HOLD,
                Config.basicRnaFocusCooldown.get(),
                5.0F,
                24,
                Set.of(RnaAffinityTag.STABILITY, RnaAffinityTag.CONTROL)
        );
        registerBasic(
                BARRIER_ID,
                "rna_ability.riftborne.barrier",
                RnaAbilityType.DEFENSE,
                RnaAbilityInputType.PRESS,
                80,
                7.0F,
                60,
                Set.of(RnaAffinityTag.STABILITY, RnaAffinityTag.STRUCTURE, RnaAffinityTag.CONTROL)
        );
        registerBasic(
                RNA_STRIKE_ID,
                "rna_ability.riftborne.rna_strike",
                RnaAbilityType.OFFENSE,
                RnaAbilityInputType.PRESS,
                45,
                10.0F,
                60,
                Set.of(RnaAffinityTag.INTENSITY, RnaAffinityTag.BODY_CONTROL)
        );
        registerBasic(
                RNA_IMPULSE_STEP_ID,
                "rna_ability.riftborne.rna_impulse_step",
                RnaAbilityType.MOBILITY,
                RnaAbilityInputType.PRESS,
                Config.basicRnaImpulseStepCooldown.get(),
                12.0F,
                6,
                Set.of(RnaAffinityTag.POSITION, RnaAffinityTag.TEMPO, RnaAffinityTag.BODY_CONTROL)
        );
        registerBasic(
                RNA_ANCHOR_HOLD_ID,
                "rna_ability.riftborne.rna_anchor_hold",
                RnaAbilityType.DEFENSE,
                RnaAbilityInputType.HOLD,
                70,
                9.0F,
                60,
                Set.of(RnaAffinityTag.STABILITY, RnaAffinityTag.STRUCTURE)
        );
        registerBasic(
                RNA_RECOVERY_ID,
                "rna_ability.riftborne.rna_recovery",
                RnaAbilityType.SUPPORT,
                RnaAbilityInputType.PASSIVE_TRIGGER,
                100,
                6.0F,
                20,
                Set.of(RnaAffinityTag.STABILITY, RnaAffinityTag.CONTROL)
        );
        registerBasic(
                RNA_PULSE_PUSH_ID,
                "rna_ability.riftborne.rna_pulse_push",
                RnaAbilityType.CONTROL,
                RnaAbilityInputType.PRESS,
                80,
                14.0F,
                8,
                Set.of(RnaAffinityTag.INTENSITY, RnaAffinityTag.CONTROL, RnaAffinityTag.SPACE_BASIC)
        );
        registerBasic(
                RNA_DEFLECT_ID,
                "rna_ability.riftborne.rna_deflect",
                RnaAbilityType.DEFENSE,
                RnaAbilityInputType.PRESS,
                55,
                11.0F,
                8,
                Set.of(RnaAffinityTag.POSITION, RnaAffinityTag.CONTROL, RnaAffinityTag.TEMPO)
        );
        registerBasic(
                RNA_REACTION_SPIKE_ID,
                "rna_ability.riftborne.rna_reaction_spike",
                RnaAbilityType.SUPPORT,
                RnaAbilityInputType.PRESS,
                160,
                18.0F,
                80,
                Set.of(RnaAffinityTag.TEMPO, RnaAffinityTag.BODY_CONTROL)
        );
        registerBasic(
                RNA_OVERLOAD_VENT_ID,
                "rna_ability.riftborne.rna_overload_vent",
                RnaAbilityType.SUPPORT,
                RnaAbilityInputType.HOLD,
                Config.basicRnaOverloadVentCooldown.get(),
                0.0F,
                40,
                Set.of(RnaAffinityTag.STABILITY, RnaAffinityTag.CONTROL, RnaAffinityTag.OVERLOAD_MANAGEMENT)
        );
    }

    private RnaAbilityRegistry() {
    }

    public static void register(RnaAbility ability) {
        if (ABILITIES.putIfAbsent(ability.id(), ability) != null) {
            throw new IllegalArgumentException("Duplicate RNA ability: " + ability.id());
        }
    }

    private static void registerBasic(
            ResourceLocation id,
            String titleKey,
            RnaAbilityType type,
            RnaAbilityInputType inputType,
            int cooldownTicks,
            float baseLoad,
            int durationTicks,
            Set<RnaAffinityTag> affinityTags
    ) {
        register(new RnaAbility(
                id,
                titleKey,
                type,
                RnaAbilityRequirement.activeRna(),
                Map.of(),
                cooldownTicks,
                false,
                inputType,
                baseLoad,
                durationTicks,
                affinityTags,
                id.getPath()
        ));
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

    public static Collection<ResourceLocation> basicCombatIds() {
        return ABILITIES.values().stream()
                .filter(RnaAbility::isBasicCombatSkill)
                .map(RnaAbility::id)
                .toList();
    }
}
