package com.pr1tcha.riftborne.codex.data;

import com.pr1tcha.riftborne.codex.CodexEntries;
import com.pr1tcha.riftborne.codex.data.entry.CodexEntryRegistry;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class PocketCodexData {
    private static final String ROOT_KEY = "RiftbornePocketCodex";
    private static final String SHORT_ENTRIES_KEY = "ShortEntries";
    private static final String QUEUED_ENTRIES_KEY = "QueuedEntries";
    private static final String DAMAGED_ENTRIES_KEY = "DamagedEntries";
    private static final String SCREEN_KEY = "SelectedScreen";
    private static final String MODE_KEY = "FieldMode";
    private static final String OBSERVATIONS_KEY = "Observations";
    private static final String LAST_TARGET_KEY = "LastTarget";
    private static final String LAST_RESULT_KEY = "LastResult";
    private static final String LAST_PULSE_COUNT_KEY = "LastPulseCount";
    private static final String LAST_PULSE_NEAREST_KEY = "LastPulseNearest";
    private static final String LAST_PULSE_DISTANCE_KEY = "LastPulseDistance";
    public static final int BUFFER_CAPACITY = 8;
    public static final int MAX_OBSERVATIONS = 3;
    private static final int HISTORY_LIMIT = 6;

    private PocketCodexData() {
    }

    public static List<String> shortEntries(ItemStack stack) {
        return readEntries(root(stack), SHORT_ENTRIES_KEY);
    }

    public static List<String> queuedEntries(ItemStack stack) {
        return readEntries(root(stack), QUEUED_ENTRIES_KEY);
    }

    public static List<String> damagedEntries(ItemStack stack) {
        return readEntries(root(stack), DAMAGED_ENTRIES_KEY);
    }

    public static RecordResult recordObservation(ItemStack stack, String entryId, boolean damaged) {
        if (!isKnownEntry(entryId)) {
            return RecordResult.INVALID;
        }

        Set<String> queued = new LinkedHashSet<>(queuedEntries(stack));
        Set<String> damagedEntries = new LinkedHashSet<>(damagedEntries(stack));
        boolean alreadyBuffered = queued.contains(entryId) || damagedEntries.contains(entryId);
        if (!alreadyBuffered && queued.size() + damagedEntries.size() >= BUFFER_CAPACITY) {
            return RecordResult.BUFFER_FULL;
        }

        List<String> history = new ArrayList<>(shortEntries(stack));
        boolean newlyDiscovered = !history.remove(entryId);
        history.add(0, entryId);
        while (history.size() > HISTORY_LIMIT) {
            history.removeLast();
        }

        int previousObservations = observationCount(stack, entryId);
        int observations = Math.min(MAX_OBSERVATIONS, previousObservations + 1);
        if (damaged) {
            queued.remove(entryId);
            damagedEntries.add(entryId);
        } else if (!damagedEntries.contains(entryId)) {
            queued.add(entryId);
        }
        update(stack, root -> {
            root.put(SHORT_ENTRIES_KEY, writeEntries(history));
            root.put(QUEUED_ENTRIES_KEY, writeEntries(queued));
            root.put(DAMAGED_ENTRIES_KEY, writeEntries(damagedEntries));
            CompoundTag observationsTag = root.getCompound(OBSERVATIONS_KEY);
            observationsTag.putInt(entryId, observations);
            root.put(OBSERVATIONS_KEY, observationsTag);
            root.putString(LAST_TARGET_KEY, entryId);
            root.putString(LAST_RESULT_KEY, damaged ? "DAMAGED" : newlyDiscovered ? "DISCOVERED" : "UPDATED");
        });
        if (damaged) {
            return RecordResult.DAMAGED;
        }
        return newlyDiscovered ? RecordResult.DISCOVERED : RecordResult.UPDATED;
    }

    public static boolean discover(ItemStack stack, String entryId, boolean damaged) {
        return recordObservation(stack, entryId, damaged) == RecordResult.DISCOVERED;
    }

    public static void clearTransferQueue(ItemStack stack) {
        update(stack, root -> {
            root.remove(QUEUED_ENTRIES_KEY);
            root.remove(DAMAGED_ENTRIES_KEY);
        });
    }

    public static PocketCodexMode mode(ItemStack stack) {
        CompoundTag root = root(stack);
        int index = root.contains(MODE_KEY) ? root.getInt(MODE_KEY) : Math.floorMod(root.getInt(SCREEN_KEY), 3);
        return PocketCodexMode.byIndex(index);
    }

    public static PocketCodexMode cycleMode(ItemStack stack, int direction) {
        PocketCodexMode selected = PocketCodexMode.byIndex(mode(stack).ordinal() + direction);
        update(stack, root -> root.putInt(MODE_KEY, selected.ordinal()));
        return selected;
    }

    public static int selectedScreen(ItemStack stack) {
        return mode(stack).ordinal();
    }

    public static int cycleScreen(ItemStack stack, int direction) {
        return cycleMode(stack, direction).ordinal();
    }

    public static int bufferSize(ItemStack stack) {
        Set<String> buffered = new LinkedHashSet<>(queuedEntries(stack));
        buffered.addAll(damagedEntries(stack));
        return buffered.size();
    }

    public static int observationCount(ItemStack stack, String entryId) {
        return Math.clamp(root(stack).getCompound(OBSERVATIONS_KEY).getInt(entryId), 0, MAX_OBSERVATIONS);
    }

    public static String lastTarget(ItemStack stack) {
        return root(stack).getString(LAST_TARGET_KEY);
    }

    public static String lastResult(ItemStack stack) {
        return root(stack).getString(LAST_RESULT_KEY);
    }

    public static void recordPulse(ItemStack stack, int signalCount, String nearestEntry, int nearestDistance) {
        update(stack, root -> {
            root.putInt(LAST_PULSE_COUNT_KEY, Math.max(0, signalCount));
            root.putString(LAST_PULSE_NEAREST_KEY, nearestEntry == null ? "" : nearestEntry);
            root.putInt(LAST_PULSE_DISTANCE_KEY, nearestDistance);
            root.putString(LAST_RESULT_KEY, signalCount > 0 ? "SIGNAL" : "CLEAR");
        });
    }

    public static int lastPulseCount(ItemStack stack) {
        return Math.max(0, root(stack).getInt(LAST_PULSE_COUNT_KEY));
    }

    public static String lastPulseNearest(ItemStack stack) {
        return root(stack).getString(LAST_PULSE_NEAREST_KEY);
    }

    public static int lastPulseDistance(ItemStack stack) {
        return root(stack).contains(LAST_PULSE_DISTANCE_KEY) ? root(stack).getInt(LAST_PULSE_DISTANCE_KEY) : -1;
    }

    private static List<String> readEntries(CompoundTag root, String key) {
        ListTag list = root.getList(key, Tag.TAG_STRING);
        List<String> entries = new ArrayList<>(list.size());
        for (int index = 0; index < list.size(); index++) {
            String entryId = list.getString(index);
            if (isKnownEntry(entryId)) {
                entries.add(entryId);
            }
        }
        return entries;
    }

    private static ListTag writeEntries(Iterable<String> entries) {
        ListTag list = new ListTag();
        entries.forEach(entry -> list.add(StringTag.valueOf(entry)));
        return list;
    }

    private static CompoundTag root(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(ROOT_KEY, Tag.TAG_COMPOUND) ? tag.getCompound(ROOT_KEY) : new CompoundTag();
    }

    private static void update(ItemStack stack, java.util.function.Consumer<CompoundTag> updater) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag root = tag.contains(ROOT_KEY, Tag.TAG_COMPOUND) ? tag.getCompound(ROOT_KEY) : new CompoundTag();
        updater.accept(root);
        tag.put(ROOT_KEY, root);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static boolean isKnownEntry(String entryId) {
        if (CodexEntries.get(entryId) != null) {
            return true;
        }
        String qualified = entryId.contains(":") ? entryId : "riftborne:" + entryId;
        return CodexEntryRegistry.get(qualified) != null;
    }

    public enum RecordResult {
        DISCOVERED,
        UPDATED,
        DAMAGED,
        BUFFER_FULL,
        INVALID
    }
}
