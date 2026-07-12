package com.pr1tcha.riftborne.rna.combat.progression;

import java.util.List;

public record RnaTechniqueReadiness(
        List<Condition> discoveryConditions,
        List<Condition> calibrationConditions
) {
    public RnaTechniqueReadiness {
        discoveryConditions = discoveryConditions == null ? List.of() : List.copyOf(discoveryConditions);
        calibrationConditions = calibrationConditions == null ? List.of() : List.copyOf(calibrationConditions);
    }

    public boolean discoveryReady() {
        return discoveryConditions.stream().allMatch(Condition::satisfied);
    }

    public boolean calibrationReady() {
        return discoveryReady() && calibrationConditions.stream().allMatch(Condition::satisfied);
    }

    public boolean structuralReady() {
        return discoveryReady() && calibrationConditions.stream()
                .filter(condition -> !"pattern_stabilization".equals(condition.id()))
                .allMatch(Condition::satisfied);
    }

    public int satisfiedConditions() {
        return (int) allConditions().stream().filter(Condition::satisfied).count();
    }

    public int totalConditions() {
        return allConditions().size();
    }

    public Condition firstMissing() {
        return allConditions().stream().filter(condition -> !condition.satisfied()).findFirst().orElse(null);
    }

    public List<Condition> allConditions() {
        return java.util.stream.Stream.concat(
                discoveryConditions.stream(),
                calibrationConditions.stream()
        ).toList();
    }

    public record Condition(String id, int current, int required, boolean satisfied) {
        public Condition {
            id = id == null ? "unknown" : id;
            current = Math.max(0, current);
            required = Math.max(0, required);
        }
    }
}
