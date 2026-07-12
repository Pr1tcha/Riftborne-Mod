package com.pr1tcha.riftborne.rna.combat.progression;

import com.pr1tcha.riftborne.rna.RnaApi;
import com.pr1tcha.riftborne.rna.combat.RnaAbilityManager;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityData;
import com.pr1tcha.riftborne.rna.data.RnaData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-authoritative progression boundary for pre-aspect techniques.
 * Acquisition routes form the pattern; the Codex may only observe the result.
 */
public final class RnaTechniqueProgression {
    private RnaTechniqueProgression() {
    }

    public static CalibrationResult inspectAndStabilize(ServerPlayer player, ResourceLocation techniqueId) {
        RnaTechniqueDefinition technique = RnaTechniqueRegistry.get(techniqueId);
        if (technique == null) {
            return CalibrationResult.UNKNOWN_TECHNIQUE;
        }
        if (!RnaApi.hasActiveRna(player)) {
            return CalibrationResult.NO_ACTIVE_RNA;
        }

        RnaTechniqueReadiness readiness = evaluate(player, technique);
        if (!readiness.discoveryReady()) {
            return CalibrationResult.PATTERN_NOT_DISCOVERED;
        }
        RnaAbilityManager.discoverTechnique(player, techniqueId);
        if (RnaAbilityManager.isUnlocked(player, techniqueId)) {
            return CalibrationResult.ALREADY_UNLOCKED;
        }

        RnaTechniqueReadiness.Condition missing = readiness.firstMissing();
        if (missing != null) {
            if ("meta_stability".equals(missing.id())) {
                return CalibrationResult.UNSTABLE;
            }
            if (missing.id().startsWith("stat.")) {
                return CalibrationResult.NOT_READY;
            }
            if (missing.id().startsWith("prerequisite.")) {
                return CalibrationResult.MISSING_PREREQUISITE;
            }
            return CalibrationResult.PATTERN_NOT_STABILIZED;
        }

        return RnaAbilityManager.grant(player, techniqueId)
                ? CalibrationResult.UNLOCKED
                : CalibrationResult.ALREADY_UNLOCKED;
    }

    public static RnaTechniqueReadiness evaluate(
            ServerPlayer player,
            RnaTechniqueDefinition technique
    ) {
        return evaluate(RnaApi.get(player), RnaAbilityManager.getData(player), technique);
    }

    public static RnaTechniqueReadiness evaluate(
            RnaData rna,
            RnaAbilityData data,
            RnaTechniqueDefinition technique
    ) {
        List<RnaTechniqueReadiness.Condition> discovery = new ArrayList<>();
        discovery.add(condition("active_rna", rna.hasRNA() ? 1 : 0, 1));
        RnaTechniqueProgress ownProgress = data.techniqueProgress(technique.id().toString());
        int patternProgress = ownProgress == null ? 0 : ownProgress.patternProgress();
        discovery.add(condition(
                "pattern_discovery",
                patternProgress,
                RnaTechniqueProgress.DISCOVERY_THRESHOLD
        ));

        List<RnaTechniqueReadiness.Condition> calibration = new ArrayList<>();
        calibration.add(new RnaTechniqueReadiness.Condition(
                "meta_stability",
                rna.metaWearStage().ordinal(),
                technique.maximumCalibrationStage().ordinal(),
                technique.isStableEnough(rna)
        ));
        addStatCondition(calibration, "node_density", rna.nodeDensity(), technique.minNodeDensity());
        addStatCondition(calibration, "connectivity", rna.connectivity(), technique.minConnectivity());
        addStatCondition(calibration, "throughput", rna.throughput(), technique.minThroughput());
        addStatCondition(calibration, "overload_resistance", rna.overloadResistance(),
                technique.minOverloadResistance());
        technique.prerequisiteTechniques().forEach((id, requiredStage) -> {
            RnaTechniqueProgress progress = data.techniqueProgress(id.toString());
            RnaTechniqueStage stage = progress == null ? RnaTechniqueStage.SEALED : progress.stage();
            calibration.add(new RnaTechniqueReadiness.Condition(
                    "prerequisite." + id + "." + requiredStage.id(),
                    stage.ordinal(),
                    requiredStage.ordinal(),
                    stage.ordinal() >= requiredStage.ordinal()
            ));
        });
        calibration.add(condition(
                "pattern_stabilization",
                patternProgress,
                RnaTechniqueProgress.STABILIZATION_THRESHOLD
        ));
        return new RnaTechniqueReadiness(discovery, calibration);
    }

    public static List<String> encodeTechniqueProgress(RnaAbilityData data) {
        List<String> encoded = new ArrayList<>();
        for (RnaTechniqueDefinition technique : RnaTechniqueRegistry.all()) {
            RnaTechniqueProgress progress = data.techniqueProgress(technique.id().toString());
            if (progress == null || progress.stage() == RnaTechniqueStage.SEALED) {
                continue;
            }
            encoded.add(String.join(",",
                    technique.id().toString(),
                    progress.stage().id(),
                    Integer.toString(progress.successfulUses()),
                    Integer.toString(technique.masteryUses()),
                    Integer.toString(progress.stabilizationOrder()),
                    Integer.toString(progress.patternProgress()),
                    encodeMethodContributions(progress)
            ));
        }
        encoded.sort(Comparator.comparingInt(RnaTechniqueProgression::encodedOrder));
        return encoded;
    }

    public static List<String> encodeAspectResonance(RnaAbilityData data) {
        return data.aspectResonance().entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<RnaAspectResonance, Integer>comparingByValue().reversed())
                .map(entry -> entry.getKey().id() + "," + entry.getValue())
                .toList();
    }

    public static List<String> encodeTechniqueReadiness(RnaData rna, RnaAbilityData data) {
        List<String> encoded = new ArrayList<>();
        for (RnaTechniqueDefinition technique : RnaTechniqueRegistry.all()) {
            RnaTechniqueReadiness readiness = evaluate(rna, data, technique);
            RnaTechniqueReadiness.Condition missing = readiness.firstMissing();
            encoded.add(String.join(",",
                    technique.id().toString(),
                    Integer.toString(readiness.satisfiedConditions()),
                    Integer.toString(readiness.totalConditions()),
                    missing == null ? "" : missing.id(),
                    missing == null ? "0" : Integer.toString(missing.current()),
                    missing == null ? "0" : Integer.toString(missing.required())
            ));
        }
        return encoded;
    }

    private static RnaTechniqueReadiness.Condition condition(String id, int current, int required) {
        return new RnaTechniqueReadiness.Condition(id, current, required, current >= required);
    }

    private static void addStatCondition(
            List<RnaTechniqueReadiness.Condition> conditions,
            String stat,
            int current,
            int required
    ) {
        if (required > 0) {
            conditions.add(condition("stat." + stat, current, required));
        }
    }

    private static int encodedOrder(String value) {
        String[] fields = value.split(",", -1);
        if (fields.length < 5) {
            return Integer.MAX_VALUE;
        }
        try {
            int order = Integer.parseInt(fields[4]);
            return order <= 0 ? Integer.MAX_VALUE : order;
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private static String encodeMethodContributions(RnaTechniqueProgress progress) {
        return progress.methodContributions().entrySet().stream()
                .sorted(Map.Entry.<RnaAcquisitionMethod, Integer>comparingByValue().reversed())
                .map(entry -> entry.getKey().id() + ":" + entry.getValue())
                .collect(java.util.stream.Collectors.joining("|"));
    }

    public enum CalibrationResult {
        UNKNOWN_TECHNIQUE("screen.riftborne.codex.technique.unknown", false),
        NO_ACTIVE_RNA("screen.riftborne.codex.technique.barrier.no_rna", false),
        PATTERN_NOT_DISCOVERED("screen.riftborne.codex.technique.pattern_not_discovered", false),
        UNSTABLE("screen.riftborne.codex.technique.barrier.unstable", false),
        NOT_READY("screen.riftborne.codex.technique.barrier.not_ready", false),
        MISSING_PREREQUISITE("screen.riftborne.codex.technique.missing_prerequisite", false),
        PATTERN_NOT_STABILIZED("screen.riftborne.codex.technique.pattern_not_stabilized", false),
        ALREADY_UNLOCKED("screen.riftborne.codex.technique.barrier.unlocked", false),
        UNLOCKED("screen.riftborne.codex.technique.barrier.calibrated", true);

        private final String translationKey;
        private final boolean newlyUnlocked;

        CalibrationResult(String translationKey, boolean newlyUnlocked) {
            this.translationKey = translationKey;
            this.newlyUnlocked = newlyUnlocked;
        }

        public String translationKey() {
            return translationKey;
        }

        public boolean newlyUnlocked() {
            return newlyUnlocked;
        }
    }
}
