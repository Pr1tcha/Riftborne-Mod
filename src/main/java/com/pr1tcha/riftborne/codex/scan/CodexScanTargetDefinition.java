package com.pr1tcha.riftborne.codex.scan;

import java.util.List;

public record CodexScanTargetDefinition(
        String id,
        String entryId,
        TargetType type,
        List<String> targets,
        boolean damaged,
        int priority
) {
    public CodexScanTargetDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Scan target id cannot be empty");
        }
        if (entryId == null || entryId.isBlank()) {
            throw new IllegalArgumentException("Scan target entry cannot be empty");
        }
        type = type == null ? TargetType.BLOCK : type;
        targets = targets == null ? List.of() : List.copyOf(targets);
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("Scan target must contain at least one block, entity or tag");
        }
    }

    public enum TargetType {
        BLOCK,
        ENTITY;

        public static TargetType fromString(String value) {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        }
    }
}
