package com.pr1tcha.riftborne.codex.data;

public record CodexEntry(
        String id,
        String titleKey,
        String categoryKey,
        String shortTextKey,
        String textKey,
        String recommendationKey,
        int threatLevel,
        boolean hiddenByDefault
) {
}
