package com.pr1tcha.riftborne.rna.combat.progression;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;

public final class RnaTechniqueProgress {
    public static final int DISCOVERY_THRESHOLD = 20;
    public static final int PROVISIONAL_THRESHOLD = 60;
    public static final int STABILIZATION_THRESHOLD = 100;

    private RnaTechniqueStage stage = RnaTechniqueStage.SEALED;
    private int patternProgress;
    private final Map<RnaAcquisitionMethod, Integer> methodContributions = new LinkedHashMap<>();
    private int successfulUses;
    private int discoveryOrder;
    private int stabilizationOrder;
    private long discoveredAtTick;
    private long stabilizedAtTick;
    private long lastUseTick;

    public static RnaTechniqueProgress load(CompoundTag tag) {
        RnaTechniqueProgress progress = new RnaTechniqueProgress();
        progress.stage = RnaTechniqueStage.fromId(tag.getString("Stage"));
        progress.patternProgress = Math.max(0, Math.min(STABILIZATION_THRESHOLD, tag.getInt("PatternProgress")));
        readMethodContributions(tag.getCompound("MethodContributions"), progress.methodContributions);
        progress.successfulUses = Math.max(0, tag.getInt("SuccessfulUses"));
        progress.discoveryOrder = Math.max(0, tag.getInt("DiscoveryOrder"));
        progress.stabilizationOrder = Math.max(0, tag.getInt("StabilizationOrder"));
        progress.discoveredAtTick = Math.max(0L, tag.getLong("DiscoveredAtTick"));
        progress.stabilizedAtTick = Math.max(0L, tag.getLong("StabilizedAtTick"));
        progress.lastUseTick = Math.max(0L, tag.getLong("LastUseTick"));
        return progress;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Stage", stage.id());
        tag.putInt("PatternProgress", patternProgress);
        tag.put("MethodContributions", writeMethodContributions(methodContributions));
        tag.putInt("SuccessfulUses", successfulUses);
        tag.putInt("DiscoveryOrder", discoveryOrder);
        tag.putInt("StabilizationOrder", stabilizationOrder);
        tag.putLong("DiscoveredAtTick", discoveredAtTick);
        tag.putLong("StabilizedAtTick", stabilizedAtTick);
        tag.putLong("LastUseTick", lastUseTick);
        return tag;
    }

    public boolean discover(int order, long gameTime) {
        if (stage != RnaTechniqueStage.SEALED) {
            return false;
        }
        stage = RnaTechniqueStage.DISCOVERED;
        ensurePatternProgress(DISCOVERY_THRESHOLD, RnaAcquisitionMethod.UNTRACKED);
        discoveryOrder = Math.max(1, order);
        discoveredAtTick = Math.max(0L, gameTime);
        return true;
    }

    public boolean stabilize(int order, long gameTime) {
        if (stage == RnaTechniqueStage.MASTERED || stage == RnaTechniqueStage.STABILIZED) {
            return false;
        }
        if (stage == RnaTechniqueStage.SEALED) {
            discover(order, gameTime);
        }
        stage = RnaTechniqueStage.STABILIZED;
        ensurePatternProgress(STABILIZATION_THRESHOLD, RnaAcquisitionMethod.UNTRACKED);
        stabilizationOrder = Math.max(1, order);
        stabilizedAtTick = Math.max(0L, gameTime);
        return true;
    }

    public boolean recordSuccessfulUse(long gameTime, int masteryUses) {
        if (!stage.usable()) {
            return false;
        }
        successfulUses++;
        lastUseTick = Math.max(0L, gameTime);
        if (stage == RnaTechniqueStage.STABILIZED
                && masteryUses > 0
                && successfulUses >= masteryUses) {
            stage = RnaTechniqueStage.MASTERED;
            return true;
        }
        return false;
    }

    public void forceStage(RnaTechniqueStage newStage, int order, long gameTime) {
        stage = newStage == null ? RnaTechniqueStage.SEALED : newStage;
        if (stage == RnaTechniqueStage.SEALED) {
            successfulUses = 0;
            discoveryOrder = 0;
            stabilizationOrder = 0;
            discoveredAtTick = 0L;
            stabilizedAtTick = 0L;
            lastUseTick = 0L;
            patternProgress = 0;
            methodContributions.clear();
        } else if (stage == RnaTechniqueStage.DISCOVERED) {
            ensurePatternProgress(DISCOVERY_THRESHOLD, RnaAcquisitionMethod.UNTRACKED);
            discoveryOrder = discoveryOrder == 0 ? Math.max(1, order) : discoveryOrder;
            discoveredAtTick = discoveredAtTick == 0L ? Math.max(0L, gameTime) : discoveredAtTick;
            stabilizationOrder = 0;
            stabilizedAtTick = 0L;
        } else {
            ensurePatternProgress(STABILIZATION_THRESHOLD, RnaAcquisitionMethod.UNTRACKED);
            discoveryOrder = discoveryOrder == 0 ? Math.max(1, order) : discoveryOrder;
            stabilizationOrder = stabilizationOrder == 0 ? Math.max(1, order) : stabilizationOrder;
            discoveredAtTick = discoveredAtTick == 0L ? Math.max(0L, gameTime) : discoveredAtTick;
            stabilizedAtTick = stabilizedAtTick == 0L ? Math.max(0L, gameTime) : stabilizedAtTick;
        }
    }

    public RnaTechniqueStage stage() {
        return stage;
    }

    public int addPatternProgress(RnaAcquisitionMethod method, int amount) {
        if (method == null || amount <= 0 || patternProgress >= STABILIZATION_THRESHOLD) {
            return 0;
        }
        int accepted = Math.min(amount, STABILIZATION_THRESHOLD - patternProgress);
        patternProgress += accepted;
        methodContributions.merge(method, accepted, Integer::sum);
        return accepted;
    }

    public boolean hasDiscoveryThreshold() {
        return patternProgress >= DISCOVERY_THRESHOLD;
    }

    public boolean hasProvisionalAccess() {
        return patternProgress >= PROVISIONAL_THRESHOLD;
    }

    public boolean hasStabilizationThreshold() {
        return patternProgress >= STABILIZATION_THRESHOLD;
    }

    public int patternProgress() {
        return patternProgress;
    }

    public int contribution(RnaAcquisitionMethod method) {
        return methodContributions.getOrDefault(method, 0);
    }

    public Map<RnaAcquisitionMethod, Integer> methodContributions() {
        return Map.copyOf(methodContributions);
    }

    public int successfulUses() {
        return successfulUses;
    }

    public int discoveryOrder() {
        return discoveryOrder;
    }

    public int stabilizationOrder() {
        return stabilizationOrder;
    }

    public long discoveredAtTick() {
        return discoveredAtTick;
    }

    public long stabilizedAtTick() {
        return stabilizedAtTick;
    }

    public long lastUseTick() {
        return lastUseTick;
    }

    private void ensurePatternProgress(int target, RnaAcquisitionMethod fallbackMethod) {
        if (patternProgress >= target) {
            return;
        }
        int missing = target - patternProgress;
        patternProgress = target;
        methodContributions.merge(fallbackMethod, missing, Integer::sum);
    }

    private static CompoundTag writeMethodContributions(Map<RnaAcquisitionMethod, Integer> values) {
        CompoundTag tag = new CompoundTag();
        values.forEach((method, amount) -> tag.putInt(method.id(), amount));
        return tag;
    }

    private static void readMethodContributions(
            CompoundTag tag,
            Map<RnaAcquisitionMethod, Integer> target
    ) {
        for (String id : tag.getAllKeys()) {
            RnaAcquisitionMethod method = RnaAcquisitionMethod.fromId(id);
            if (method != null) {
                target.put(method, Math.max(0, tag.getInt(id)));
            }
        }
    }
}
