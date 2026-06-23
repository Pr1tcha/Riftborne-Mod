package com.pr1tcha.riftborne.codex;

import com.pr1tcha.riftborne.codex.data.CodexEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CodexEntries {
    private static final Map<String, CodexEntry> ENTRIES = new LinkedHashMap<>();

    static {
        register(basicEntry("rna_overview", "rna", 1, false));
        register(basicEntry("node_density", "rna", 1, false));
        register(basicEntry("connectivity", "rna", 1, true));
        register(basicEntry("throughput", "rna", 2, true));
        register(basicEntry("overload_resistance", "rna", 2, true));
        register(basicEntry("meta_wear", "meta_wear", 3, true));
        register(fieldEntry("rift_basic", "rifts", 3, true));
        register(fieldEntry("rift_splinter", "entities", 2, true));
        register(fieldEntry("discard_contour", "dimensions", 4, true));
        register(fieldEntry("rna_interspace", "dimensions", 2, true));
        register(fieldEntry("riftwalker_interspace", "dimensions", 3, true));
    }

    private CodexEntries() {
    }

    private static void register(CodexEntry entry) {
        ENTRIES.put(entry.id(), entry);
    }

    private static CodexEntry basicEntry(String id, String category, int threatLevel, boolean hiddenByDefault) {
        String baseKey = "codex.riftborne.entry." + id;
        return new CodexEntry(
                id,
                baseKey + ".title",
                "codex.riftborne.category." + category,
                baseKey + ".brief",
                baseKey + ".text",
                null,
                threatLevel,
                baseKey + ".brief",
                hiddenByDefault
        );
    }

    private static CodexEntry fieldEntry(String id, String category, int threatLevel, boolean hiddenByDefault) {
        String baseKey = "codex.riftborne.entry." + id;
        return new CodexEntry(
                id,
                baseKey + ".title",
                "codex.riftborne.category." + category,
                baseKey + ".short",
                baseKey + ".text",
                baseKey + ".recommendation",
                threatLevel,
                baseKey + ".short",
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
