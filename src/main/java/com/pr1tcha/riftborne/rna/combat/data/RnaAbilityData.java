package com.pr1tcha.riftborne.rna.combat.data;

import com.pr1tcha.riftborne.rna.data.RnaStat;
import com.pr1tcha.riftborne.rna.combat.progression.RnaAspectResonance;
import com.pr1tcha.riftborne.rna.combat.progression.RnaTechniqueProgress;
import com.pr1tcha.riftborne.rna.combat.progression.RnaTechniqueStage;
import com.pr1tcha.riftborne.rna.combat.progression.RnaTechniqueEvidence;
import com.pr1tcha.riftborne.rna.combat.progression.RnaAcquisitionMethod;
import com.pr1tcha.riftborne.rna.combat.training.RnaTrainingSession;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class RnaAbilityData {
    public static final int CURRENT_VERSION = 5;

    private final Set<String> unlockedAbilities = new LinkedHashSet<>();
    private final Set<String> activeAbilities = new LinkedHashSet<>();
    private final Map<String, Long> cooldowns = new LinkedHashMap<>();
    private final Map<String, Long> lastUseTicks = new LinkedHashMap<>();
    private final Map<String, Long> growthCooldowns = new LinkedHashMap<>();
    private final Map<String, Long> activeStateUntil = new LinkedHashMap<>();
    private final Map<String, Float> activeStrength = new LinkedHashMap<>();
    private final Map<RnaAffinityTag, Integer> affinityCounters = new LinkedHashMap<>();
    private final Map<String, RnaTechniqueProgress> techniqueProgress = new LinkedHashMap<>();
    private final Map<RnaAspectResonance, Integer> aspectResonance = new LinkedHashMap<>();
    private final Map<RnaTechniqueEvidence, Integer> progressionEvidence = new LinkedHashMap<>();
    private int nextDiscoveryOrder = 1;
    private int nextStabilizationOrder = 1;
    private RnaTrainingSession trainingSession;
    private float currentLoad;
    private long lastLoadTick;
    private long instabilityUntilTick;
    private String lastCombatAction = "";
    private int version = CURRENT_VERSION;

    public static RnaAbilityData load(CompoundTag tag) {
        RnaAbilityData data = new RnaAbilityData();
        readStrings(tag, "UnlockedAbilities", data.unlockedAbilities);
        readStrings(tag, "ActiveAbilities", data.activeAbilities);
        readLongMap(tag, "Cooldowns", data.cooldowns);
        readLongMap(tag, "LastUseTicks", data.lastUseTicks);
        readLongMap(tag, "GrowthCooldowns", data.growthCooldowns);
        readLongMap(tag, "ActiveStateUntil", data.activeStateUntil);
        readFloatMap(tag, "ActiveStrength", data.activeStrength);
        readAffinityCounters(tag.getCompound("AffinityCounters"), data.affinityCounters);
        readTechniqueProgress(tag.getCompound("TechniqueProgress"), data.techniqueProgress);
        readAspectResonance(tag.getCompound("AspectResonance"), data.aspectResonance);
        readProgressionEvidence(tag.getCompound("ProgressionEvidence"), data.progressionEvidence);
        data.nextDiscoveryOrder = Math.max(1, tag.getInt("NextDiscoveryOrder"));
        data.nextStabilizationOrder = Math.max(1, tag.getInt("NextStabilizationOrder"));
        data.recalculateTechniqueOrderCounters();
        data.trainingSession = tag.contains("TrainingSession")
                ? RnaTrainingSession.load(tag.getCompound("TrainingSession"))
                : null;
        data.currentLoad = tag.contains("CurrentLoad") ? Mth.clamp(tag.getFloat("CurrentLoad"), 0.0F, 100.0F) : 0.0F;
        data.lastLoadTick = tag.contains("LastLoadTick") ? tag.getLong("LastLoadTick") : 0L;
        data.instabilityUntilTick = tag.contains("InstabilityUntilTick") ? tag.getLong("InstabilityUntilTick") : 0L;
        data.lastCombatAction = tag.getString("LastCombatAction");
        data.version = tag.contains("Version") ? tag.getInt("Version") : CURRENT_VERSION;
        return data;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("UnlockedAbilities", writeStrings(unlockedAbilities));
        tag.put("ActiveAbilities", writeStrings(activeAbilities));
        tag.put("Cooldowns", writeLongMap(cooldowns));
        tag.put("LastUseTicks", writeLongMap(lastUseTicks));
        tag.put("GrowthCooldowns", writeLongMap(growthCooldowns));
        tag.put("ActiveStateUntil", writeLongMap(activeStateUntil));
        tag.put("ActiveStrength", writeFloatMap(activeStrength));
        tag.put("AffinityCounters", writeAffinityCounters(affinityCounters));
        tag.put("TechniqueProgress", writeTechniqueProgress(techniqueProgress));
        tag.put("AspectResonance", writeAspectResonance(aspectResonance));
        tag.put("ProgressionEvidence", writeProgressionEvidence(progressionEvidence));
        tag.putInt("NextDiscoveryOrder", nextDiscoveryOrder);
        tag.putInt("NextStabilizationOrder", nextStabilizationOrder);
        if (trainingSession != null) {
            tag.put("TrainingSession", trainingSession.save());
        }
        tag.putFloat("CurrentLoad", currentLoad);
        tag.putLong("LastLoadTick", lastLoadTick);
        tag.putLong("InstabilityUntilTick", instabilityUntilTick);
        tag.putString("LastCombatAction", lastCombatAction);
        tag.putInt("Version", CURRENT_VERSION);
        return tag;
    }

    public boolean isUnlocked(String id) {
        return unlockedAbilities.contains(id);
    }

    public boolean unlock(String id) {
        return unlockedAbilities.add(id);
    }

    public boolean revoke(String id) {
        activeAbilities.remove(id);
        cooldowns.remove(id);
        lastUseTicks.remove(id);
        activeStateUntil.remove(id);
        activeStrength.remove(id);
        growthCooldowns.keySet().removeIf(key -> key.startsWith(id + "|"));
        RnaTechniqueProgress progress = techniqueProgress.remove(id);
        return unlockedAbilities.remove(id) || progress != null;
    }

    public boolean discoverTechnique(String id, long gameTime) {
        RnaTechniqueProgress progress = techniqueProgress.computeIfAbsent(id, ignored -> new RnaTechniqueProgress());
        if (progress.stage() != RnaTechniqueStage.SEALED) {
            return false;
        }
        return progress.discover(nextDiscoveryOrder++, gameTime);
    }

    public boolean stabilizeTechnique(String id, long gameTime) {
        RnaTechniqueProgress progress = techniqueProgress.computeIfAbsent(id, ignored -> new RnaTechniqueProgress());
        if (progress.stage() == RnaTechniqueStage.SEALED) {
            progress.discover(nextDiscoveryOrder++, gameTime);
        }
        boolean changed = false;
        if (!progress.stage().usable()) {
            changed = progress.stabilize(nextStabilizationOrder++, gameTime);
        }
        changed |= unlockedAbilities.add(id);
        return changed;
    }

    public boolean recordTechniqueUse(String id, long gameTime, int masteryUses) {
        RnaTechniqueProgress progress = techniqueProgress.computeIfAbsent(id, ignored -> new RnaTechniqueProgress());
        if (!progress.stage().usable() && unlockedAbilities.contains(id)) {
            progress.stabilize(nextStabilizationOrder++, gameTime);
        }
        return progress.recordSuccessfulUse(gameTime, masteryUses);
    }

    public int addTechniquePatternProgress(
            String id,
            RnaAcquisitionMethod method,
            int amount
    ) {
        return techniqueProgress
                .computeIfAbsent(id, ignored -> new RnaTechniqueProgress())
                .addPatternProgress(method, amount);
    }

    public boolean hasProvisionalTechnique(String id) {
        RnaTechniqueProgress progress = techniqueProgress.get(id);
        return progress != null && progress.hasProvisionalAccess();
    }

    public RnaTrainingSession trainingSession() {
        return trainingSession;
    }

    public void setTrainingSession(RnaTrainingSession trainingSession) {
        this.trainingSession = trainingSession;
    }

    public void clearTrainingSession() {
        trainingSession = null;
    }

    public void setTechniqueStage(String id, RnaTechniqueStage stage, long gameTime) {
        RnaTechniqueProgress progress = techniqueProgress.computeIfAbsent(id, ignored -> new RnaTechniqueProgress());
        progress.forceStage(stage, Math.max(nextDiscoveryOrder, nextStabilizationOrder), gameTime);
        if (stage != null && stage.usable()) {
            unlockedAbilities.add(id);
        } else {
            unlockedAbilities.remove(id);
            activeAbilities.remove(id);
            activeStateUntil.remove(id);
            activeStrength.remove(id);
        }
        recalculateTechniqueOrderCounters();
    }

    public RnaTechniqueProgress techniqueProgress(String id) {
        return techniqueProgress.get(id);
    }

    public Map<String, RnaTechniqueProgress> techniqueProgress() {
        return Map.copyOf(techniqueProgress);
    }

    public void migrateUnlockedTechniqueProgress(Iterable<String> techniqueIds) {
        for (String id : techniqueIds) {
            RnaTechniqueProgress progress = techniqueProgress.get(id);
            if (unlockedAbilities.contains(id)) {
                progress = techniqueProgress.computeIfAbsent(id, ignored -> new RnaTechniqueProgress());
            }
            if (progress == null) {
                continue;
            }
            if (unlockedAbilities.contains(id) && !progress.stage().usable()) {
                progress.stabilize(nextStabilizationOrder++, 0L);
            } else {
                progress.forceStage(
                        progress.stage(),
                        Math.max(nextDiscoveryOrder, nextStabilizationOrder),
                        0L
                );
            }
        }
        recalculateTechniqueOrderCounters();
    }

    public Set<String> unlockedAbilities() {
        return Set.copyOf(unlockedAbilities);
    }

    public Set<String> activeAbilities() {
        return Set.copyOf(activeAbilities);
    }

    public long cooldownUntil(String id) {
        return cooldowns.getOrDefault(id, 0L);
    }

    public void setCooldownUntil(String id, long tick) {
        if (tick <= 0L) {
            cooldowns.remove(id);
        } else {
            cooldowns.put(id, tick);
        }
    }

    public void clearCooldown(String id) {
        cooldowns.remove(id);
    }

    public void clearCooldowns() {
        cooldowns.clear();
    }

    public long lastUseTick(String id) {
        return lastUseTicks.getOrDefault(id, 0L);
    }

    public void setLastUseTick(String id, long tick) {
        lastUseTicks.put(id, tick);
    }

    public long growthCooldownUntil(String abilityId, RnaStat stat) {
        return growthCooldowns.getOrDefault(growthKey(abilityId, stat), 0L);
    }

    public void setGrowthCooldownUntil(String abilityId, RnaStat stat, long tick) {
        growthCooldowns.put(growthKey(abilityId, stat), tick);
    }

    public float currentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(float currentLoad) {
        this.currentLoad = Mth.clamp(currentLoad, 0.0F, 100.0F);
    }

    public void addLoad(float amount) {
        setCurrentLoad(currentLoad + amount);
    }

    public long lastLoadTick() {
        return lastLoadTick;
    }

    public void setLastLoadTick(long lastLoadTick) {
        this.lastLoadTick = Math.max(0L, lastLoadTick);
    }

    public long instabilityUntilTick() {
        return instabilityUntilTick;
    }

    public void setInstabilityUntilTick(long instabilityUntilTick) {
        this.instabilityUntilTick = Math.max(0L, instabilityUntilTick);
    }

    public String lastCombatAction() {
        return lastCombatAction;
    }

    public void setLastCombatAction(String lastCombatAction) {
        this.lastCombatAction = lastCombatAction == null ? "" : lastCombatAction;
    }

    public long activeStateUntil(String id) {
        return activeStateUntil.getOrDefault(id, 0L);
    }

    public void setActiveStateUntil(String id, long tick) {
        if (tick <= 0L) {
            activeAbilities.remove(id);
            activeStateUntil.remove(id);
            activeStrength.remove(id);
        } else {
            activeAbilities.add(id);
            activeStateUntil.put(id, tick);
        }
    }

    public void expireActiveStates(long gameTime) {
        activeStateUntil.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue() <= gameTime;
            if (expired) {
                activeAbilities.remove(entry.getKey());
                activeStrength.remove(entry.getKey());
            }
            return expired;
        });
    }

    public float activeStrength(String id) {
        return activeStrength.getOrDefault(id, 0.0F);
    }

    public void setActiveStrength(String id, float strength) {
        if (strength <= 0.0F) {
            activeStrength.remove(id);
        } else {
            activeStrength.put(id, strength);
        }
    }

    public int affinity(RnaAffinityTag tag) {
        return affinityCounters.getOrDefault(tag, 0);
    }

    public void addAffinity(RnaAffinityTag tag, int amount) {
        if (tag != null && amount > 0) {
            affinityCounters.merge(tag, amount, Integer::sum);
        }
    }

    public Map<RnaAffinityTag, Integer> affinityCounters() {
        return Map.copyOf(affinityCounters);
    }

    public int aspectResonance(RnaAspectResonance resonance) {
        return aspectResonance.getOrDefault(resonance, 0);
    }

    public void addAspectResonance(RnaAspectResonance resonance, int amount) {
        if (resonance != null && amount > 0) {
            aspectResonance.merge(resonance, amount, Integer::sum);
        }
    }

    public Map<RnaAspectResonance, Integer> aspectResonance() {
        return Map.copyOf(aspectResonance);
    }

    public int progressionEvidence(RnaTechniqueEvidence evidence) {
        return progressionEvidence.getOrDefault(evidence, 0);
    }

    public void addProgressionEvidence(RnaTechniqueEvidence evidence, int amount) {
        if (evidence == null || amount <= 0) {
            return;
        }
        progressionEvidence.compute(evidence, (ignored, current) -> {
            long next = (long) (current == null ? 0 : current) + amount;
            return (int) Math.min(Integer.MAX_VALUE, next);
        });
    }

    public Map<RnaTechniqueEvidence, Integer> progressionEvidence() {
        return Map.copyOf(progressionEvidence);
    }

    public void clearCombatState() {
        activeAbilities.clear();
        cooldowns.clear();
        activeStateUntil.clear();
        activeStrength.clear();
        currentLoad = 0.0F;
        lastLoadTick = 0L;
        instabilityUntilTick = 0L;
        lastCombatAction = "";
    }

    public int version() {
        return version;
    }

    public void markCurrentVersion() {
        version = CURRENT_VERSION;
    }

    private static String growthKey(String abilityId, RnaStat stat) {
        return abilityId + "|" + stat.id();
    }

    private static ListTag writeStrings(Iterable<String> values) {
        ListTag list = new ListTag();
        values.forEach(value -> list.add(StringTag.valueOf(value)));
        return list;
    }

    private static void readStrings(CompoundTag tag, String key, Set<String> target) {
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int index = 0; index < list.size(); index++) {
            target.add(list.getString(index));
        }
    }

    private static CompoundTag writeLongMap(Map<String, Long> values) {
        CompoundTag tag = new CompoundTag();
        values.forEach(tag::putLong);
        return tag;
    }

    private static void readLongMap(CompoundTag parent, String key, Map<String, Long> target) {
        CompoundTag tag = parent.getCompound(key);
        for (String entryKey : tag.getAllKeys()) {
            target.put(entryKey, tag.getLong(entryKey));
        }
    }

    private static CompoundTag writeFloatMap(Map<String, Float> values) {
        CompoundTag tag = new CompoundTag();
        values.forEach(tag::putFloat);
        return tag;
    }

    private static void readFloatMap(CompoundTag parent, String key, Map<String, Float> target) {
        CompoundTag tag = parent.getCompound(key);
        for (String entryKey : tag.getAllKeys()) {
            float value = tag.getFloat(entryKey);
            if (value > 0.0F) {
                target.put(entryKey, value);
            }
        }
    }

    private static CompoundTag writeAffinityCounters(Map<RnaAffinityTag, Integer> values) {
        CompoundTag tag = new CompoundTag();
        values.forEach((affinity, amount) -> tag.putInt(affinity.id(), amount));
        return tag;
    }

    private static void readAffinityCounters(CompoundTag tag, Map<RnaAffinityTag, Integer> target) {
        for (String entryKey : tag.getAllKeys()) {
            RnaAffinityTag affinity = RnaAffinityTag.fromId(entryKey);
            if (affinity != null) {
                target.put(affinity, Math.max(0, tag.getInt(entryKey)));
            }
        }
    }

    private static CompoundTag writeTechniqueProgress(Map<String, RnaTechniqueProgress> values) {
        CompoundTag tag = new CompoundTag();
        values.forEach((id, progress) -> tag.put(id, progress.save()));
        return tag;
    }

    private static void readTechniqueProgress(CompoundTag tag, Map<String, RnaTechniqueProgress> target) {
        for (String id : tag.getAllKeys()) {
            target.put(id, RnaTechniqueProgress.load(tag.getCompound(id)));
        }
    }

    private static CompoundTag writeAspectResonance(Map<RnaAspectResonance, Integer> values) {
        CompoundTag tag = new CompoundTag();
        values.forEach((resonance, amount) -> tag.putInt(resonance.id(), amount));
        return tag;
    }

    private static void readAspectResonance(CompoundTag tag, Map<RnaAspectResonance, Integer> target) {
        for (String id : tag.getAllKeys()) {
            RnaAspectResonance resonance = RnaAspectResonance.fromId(id);
            if (resonance != null) {
                target.put(resonance, Math.max(0, tag.getInt(id)));
            }
        }
    }

    private static CompoundTag writeProgressionEvidence(Map<RnaTechniqueEvidence, Integer> values) {
        CompoundTag tag = new CompoundTag();
        values.forEach((evidence, amount) -> tag.putInt(evidence.id(), amount));
        return tag;
    }

    private static void readProgressionEvidence(
            CompoundTag tag,
            Map<RnaTechniqueEvidence, Integer> target
    ) {
        for (String id : tag.getAllKeys()) {
            RnaTechniqueEvidence evidence = RnaTechniqueEvidence.fromId(id);
            if (evidence != null) {
                target.put(evidence, Math.max(0, tag.getInt(id)));
            }
        }
    }

    private void recalculateTechniqueOrderCounters() {
        int maxDiscovery = 0;
        int maxStabilization = 0;
        for (RnaTechniqueProgress progress : techniqueProgress.values()) {
            maxDiscovery = Math.max(maxDiscovery, progress.discoveryOrder());
            maxStabilization = Math.max(maxStabilization, progress.stabilizationOrder());
        }
        nextDiscoveryOrder = Math.max(nextDiscoveryOrder, maxDiscovery + 1);
        nextStabilizationOrder = Math.max(nextStabilizationOrder, maxStabilization + 1);
    }
}
