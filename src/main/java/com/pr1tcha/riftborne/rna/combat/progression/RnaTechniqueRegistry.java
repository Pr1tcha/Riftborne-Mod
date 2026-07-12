package com.pr1tcha.riftborne.rna.combat.progression;

import com.pr1tcha.riftborne.rna.combat.registry.RnaAbilityRegistry;
import com.pr1tcha.riftborne.rna.data.MetaWearStage;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Design-facing registry for pre-aspect techniques. Effects remain in the ability registry;
 * this registry owns discovery, calibration, mastery and latent Aspect tendencies.
 */
public final class RnaTechniqueRegistry {
    private static final Map<ResourceLocation, RnaTechniqueDefinition> TECHNIQUES = new LinkedHashMap<>();

    static {
        register(new RnaTechniqueDefinition(
                RnaAbilityRegistry.BARRIER_ID,
                "rna_ability.riftborne.barrier",
                25,
                20,
                0,
                0,
                MetaWearStage.DISTORTION,
                Map.of(),
                20,
                Map.of(
                        RnaAspectResonance.SPATIAL_DISTORTION, 3,
                        RnaAspectResonance.TEMPORAL_FIXATION, 1,
                        RnaAspectResonance.ENERGETIC, 1
                )
        ));
    }

    private RnaTechniqueRegistry() {
    }

    public static void register(RnaTechniqueDefinition technique) {
        if (TECHNIQUES.putIfAbsent(technique.id(), technique) != null) {
            throw new IllegalArgumentException("Duplicate RNA technique: " + technique.id());
        }
    }

    public static RnaTechniqueDefinition get(ResourceLocation id) {
        return TECHNIQUES.get(id);
    }

    public static Collection<RnaTechniqueDefinition> all() {
        return TECHNIQUES.values();
    }

    public static Collection<String> ids() {
        return TECHNIQUES.keySet().stream().map(ResourceLocation::toString).toList();
    }
}
