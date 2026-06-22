package com.pr1tcha.riftborne.codex.data;

import com.pr1tcha.riftborne.codex.CodexEntries;
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
    private static final int SCREEN_COUNT = 5;

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

    public static boolean discover(ItemStack stack, String entryId, boolean damaged) {
        if (CodexEntries.get(entryId) == null) {
            return false;
        }

        Set<String> shortEntries = new LinkedHashSet<>(shortEntries(stack));
        boolean newlyDiscovered = shortEntries.add(entryId);
        Set<String> target = new LinkedHashSet<>(damaged ? damagedEntries(stack) : queuedEntries(stack));
        target.add(entryId);
        update(stack, root -> {
            root.put(SHORT_ENTRIES_KEY, writeEntries(shortEntries));
            root.put(damaged ? DAMAGED_ENTRIES_KEY : QUEUED_ENTRIES_KEY, writeEntries(target));
        });
        return newlyDiscovered;
    }

    public static void clearTransferQueue(ItemStack stack) {
        update(stack, root -> {
            root.remove(QUEUED_ENTRIES_KEY);
            root.remove(DAMAGED_ENTRIES_KEY);
        });
    }

    public static int selectedScreen(ItemStack stack) {
        return Math.floorMod(root(stack).getInt(SCREEN_KEY), SCREEN_COUNT);
    }

    public static int cycleScreen(ItemStack stack, int direction) {
        int selected = Math.floorMod(selectedScreen(stack) + Integer.signum(direction), SCREEN_COUNT);
        update(stack, root -> root.putInt(SCREEN_KEY, selected));
        return selected;
    }

    private static List<String> readEntries(CompoundTag root, String key) {
        ListTag list = root.getList(key, Tag.TAG_STRING);
        List<String> entries = new ArrayList<>(list.size());
        for (int index = 0; index < list.size(); index++) {
            String entryId = list.getString(index);
            if (CodexEntries.get(entryId) != null) {
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
}
