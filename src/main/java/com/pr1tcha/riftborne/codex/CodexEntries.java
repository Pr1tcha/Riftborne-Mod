package com.pr1tcha.riftborne.codex;

import com.pr1tcha.riftborne.codex.data.CodexEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CodexEntries {
    private static final Map<String, CodexEntry> ENTRIES = new LinkedHashMap<>();

    static {
        register(entry("rna_overview", "rna", 1, false));
        register(entry("node_density", "rna", 1, false));
        register(entry("connectivity", "rna", 1, true));
        register(entry("throughput", "rna", 2, true));
        register(entry("overload_resistance", "rna", 2, true));
        register(entry("meta_wear", "meta_wear", 3, true));
        register(entry("rift_basic", "rifts", 3, true));
        register(entry("rift_splinter", "entities", 2, true));
        register(entry("discard_contour", "dimensions", 4, true));
        register(entry("rna_interspace", "dimensions", 2, true));
        register(entry("riftwalker_interspace", "dimensions", 3, true));
    }

    private CodexEntries() {
    }

    private static void register(CodexEntry entry) {
        ENTRIES.put(entry.id(), entry);
    }

    private static CodexEntry entry(String id, String category, int threatLevel, boolean hiddenByDefault) {
        String baseKey = "codex.riftborne.entry." + id;
        return new CodexEntry(
                id,
                baseKey + ".title",
                "codex.riftborne.category." + category,
                baseKey + ".short",
                baseKey + ".text",
                baseKey + ".recommendation",
                threatLevel,
                hiddenByDefault
        );
    }

    public static List<CodexEntry> all() {
        return List.copyOf(ENTRIES.values());
    }

    public static CodexEntry get(String id) {
        return ENTRIES.get(id);
    }
}
