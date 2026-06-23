package com.pr1tcha.riftborne.codex.storage;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class CodexPlayerProgress {
    private final Map<String, CodexEntryProgress> entries = new LinkedHashMap<>();

    public static CodexPlayerProgress load(CompoundTag tag) {
        CodexPlayerProgress progress = new CodexPlayerProgress();
        ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entryTag = list.getCompound(index);
            String id = entryTag.getString("Id");
            if (!id.isBlank()) {
                progress.entries.put(id, CodexEntryProgress.load(entryTag));
            }
        }
        return progress;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        entries.forEach((id, progress) -> list.add(progress.save(id)));
        tag.put("Entries", list);
        return tag;
    }

    public CodexEntryProgress get(String id) {
        return entries.get(id);
    }

    public void put(String id, CodexEntryProgress progress) {
        entries.put(id, progress);
    }

    public Map<String, CodexEntryProgress> entries() {
        return Map.copyOf(entries);
    }

    public void clear() {
        entries.clear();
    }
}
