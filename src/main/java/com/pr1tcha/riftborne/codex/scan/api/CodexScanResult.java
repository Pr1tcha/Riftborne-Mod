package com.pr1tcha.riftborne.codex.scan.api;

import java.util.Map;

public record CodexScanResult(
        CodexScanType type,
        boolean success,
        String summary,
        String codexEntryId,
        Map<String, String> metadata
) {
    public CodexScanResult {
        type = type == null ? CodexScanType.OTHER : type;
        summary = summary == null ? "" : summary;
        codexEntryId = codexEntryId == null ? "" : codexEntryId;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
