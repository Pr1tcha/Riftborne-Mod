package com.pr1tcha.riftborne.codex.data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

public final class CodexData {
    private static final int MAX_FEED_ENTRIES = 8;

    private final Set<String> unlockedEntries = new LinkedHashSet<>();
    private final List<String> notifications = new ArrayList<>();
    private final List<String> recentData = new ArrayList<>();
    private boolean devicePowered = true;
    private int battery = 100;

    public CodexData() {
        unlockedEntries.add("rna_overview");
        unlockedEntries.add("node_density");
        notifications.add("Кодекс готов к работе.");
        recentData.add("Профиль РНА не синхронизирован.");
    }

    public static CodexData load(CompoundTag tag) {
        CodexData data = new CodexData();
        data.unlockedEntries.clear();
        data.notifications.clear();
        data.recentData.clear();

        readStrings(tag, "UnlockedEntries", data.unlockedEntries);
        readStrings(tag, "Notifications", data.notifications);
        readStrings(tag, "RecentData", data.recentData);
        data.devicePowered = !tag.contains("DevicePowered") || tag.getBoolean("DevicePowered");
        data.battery = Mth.clamp(tag.contains("Battery") ? tag.getInt("Battery") : 100, 0, 100);
        return data;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("UnlockedEntries", writeStrings(unlockedEntries));
        tag.put("Notifications", writeStrings(notifications));
        tag.put("RecentData", writeStrings(recentData));
        tag.putBoolean("DevicePowered", devicePowered);
        tag.putInt("Battery", battery);
        return tag;
    }

    private static ListTag writeStrings(Iterable<String> values) {
        ListTag list = new ListTag();
        for (String value : values) {
            list.add(StringTag.valueOf(value));
        }
        return list;
    }

    private static void readStrings(CompoundTag tag, String key, java.util.Collection<String> target) {
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int index = 0; index < list.size(); index++) {
            target.add(list.getString(index));
        }
    }

    public void unlock(String entryId) {
        unlockedEntries.add(entryId);
    }

    public void addNotification(String message) {
        addBounded(notifications, message);
    }

    public void addRecentData(String message) {
        addBounded(recentData, message);
    }

    private static void addBounded(List<String> list, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        list.remove(message);
        list.add(0, message);
        while (list.size() > MAX_FEED_ENTRIES) {
            list.remove(list.size() - 1);
        }
    }

    public Set<String> unlockedEntries() {
        return Set.copyOf(unlockedEntries);
    }

    public List<String> notifications() {
        return List.copyOf(notifications);
    }

    public List<String> recentData() {
        return List.copyOf(recentData);
    }

    public boolean devicePowered() {
        return devicePowered;
    }

    public void setDevicePowered(boolean devicePowered) {
        this.devicePowered = devicePowered;
    }

    public int battery() {
        return battery;
    }
}
