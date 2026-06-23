package com.pr1tcha.riftborne.codex.data.entry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CodexEntryRegistry {
    private static volatile Map<String, CodexEntryDefinition> entries = Map.of();

    private CodexEntryRegistry() {
    }

    public static CodexEntryDefinition get(String id) {
        return entries.get(id);
    }

    public static Collection<CodexEntryDefinition> all() {
        return entries.values();
    }

    public static Collection<String> ids() {
        return entries.keySet();
    }

    public static void replace(Map<String, CodexEntryDefinition> loadedEntries) {
        entries = Map.copyOf(new LinkedHashMap<>(loadedEntries));
    }
}
