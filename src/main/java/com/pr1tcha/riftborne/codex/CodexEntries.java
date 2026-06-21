package com.pr1tcha.riftborne.codex;

import com.pr1tcha.riftborne.codex.data.CodexEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CodexEntries {
    private static final Map<String, CodexEntry> ENTRIES = new LinkedHashMap<>();

    static {
        register(entry("rna_overview", "rna", false));
        register(entry("node_density", "rna", false));
        register(entry("connectivity", "rna", true));
        register(entry("throughput", "rna", true));
        register(entry("overload_resistance", "rna", true));
        register(entry("meta_wear", "meta_wear", true));
    }

    private CodexEntries() {
    }

    private static void register(CodexEntry entry) {
        ENTRIES.put(entry.id(), entry);
    }

    private static CodexEntry entry(String id, String category, boolean hiddenByDefault) {
        String baseKey = "codex.riftborne.entry." + id;
        return new CodexEntry(
                id,
                baseKey + ".title",
                "codex.riftborne.category." + category,
                baseKey + ".text",
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
