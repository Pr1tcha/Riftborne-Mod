package com.pr1tcha.riftborne.codex.data.entry;

import com.pr1tcha.riftborne.codex.data.category.CodexCategory;
import com.pr1tcha.riftborne.codex.data.state.CodexEntryState;
import com.pr1tcha.riftborne.codex.decrypt.DecryptData;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record CodexEntryDefinition(
        String id,
        String title,
        CodexCategory category,
        CodexEntryState initialState,
        String summary,
        String fullContent,
        CodexSourceType sourceType,
        Set<String> flags,
        Map<String, String> metadata,
        List<String> requirements,
        DecryptData decryptData
) {
    public CodexEntryDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Codex entry id cannot be empty");
        }
        title = title == null ? id : title;
        category = category == null ? CodexCategory.SYSTEM : category;
        initialState = initialState == null ? CodexEntryState.LOCKED : initialState;
        summary = summary == null ? "" : summary;
        fullContent = fullContent == null ? "" : fullContent;
        sourceType = sourceType == null ? CodexSourceType.OTHER : sourceType;
        flags = flags == null ? Set.of() : Set.copyOf(flags);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        decryptData = decryptData == null ? DecryptData.NONE : decryptData;
    }
}
