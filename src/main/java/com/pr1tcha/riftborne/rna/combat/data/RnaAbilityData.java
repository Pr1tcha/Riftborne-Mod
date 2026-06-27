package com.pr1tcha.riftborne.rna.combat.data;

import com.pr1tcha.riftborne.rna.data.RnaStat;
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
    public static final int CURRENT_VERSION = 1;

    private final Set<String> unlockedAbilities = new LinkedHashSet<>();
    private final Set<String> activeAbilities = new LinkedHashSet<>();
    private final Map<String, Long> cooldowns = new LinkedHashMap<>();
    private final Map<String, Long> lastUseTicks = new LinkedHashMap<>();
    private final Map<String, Long> growthCooldowns = new LinkedHashMap<>();
    private final Map<String, Long> activeStateUntil = new LinkedHashMap<>();
    private final Map<RnaAffinityTag, Integer> affinityCounters = new LinkedHashMap<>();
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
        readAffinityCounters(tag.getCompound("AffinityCounters"), data.affinityCounters);
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
        tag.put("AffinityCounters", writeAffinityCounters(affinityCounters));
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
        growthCooldowns.keySet().removeIf(key -> key.startsWith(id + "|"));
        return unlockedAbilities.remove(id);
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
            }
            return expired;
        });
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

    public void clearCombatState() {
        activeAbilities.clear();
        cooldowns.clear();
        activeStateUntil.clear();
        currentLoad = 0.0F;
        lastLoadTick = 0L;
        instabilityUntilTick = 0L;
        lastCombatAction = "";
    }

    public int version() {
        return version;
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
}
