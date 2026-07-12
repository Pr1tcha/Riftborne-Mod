package com.pr1tcha.riftborne.codex.data.entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pr1tcha.riftborne.codex.data.CodexData;
import com.pr1tcha.riftborne.codex.data.state.CodexEntryState;
import com.pr1tcha.riftborne.codex.storage.CodexEntryProgress;
import com.pr1tcha.riftborne.codex.storage.CodexPlayerProgress;
import com.pr1tcha.riftborne.codex.storage.CodexStorage;
import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

public record CodexInfobaseSnapshot(List<Entry> entries) {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    public static final CodexInfobaseSnapshot EMPTY = new CodexInfobaseSnapshot(List.of());

    public CodexInfobaseSnapshot {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static String encode(ServerPlayer player) {
        CodexPlayerProgress progress = CodexStorage.get(player);
        CodexData legacyProgress = RiftbornePlayerData.getCodex(player);
        List<Entry> entries = CodexEntryRegistry.all().stream()
                .sorted(Comparator.comparingInt(CodexEntryDefinition::sortOrder)
                        .thenComparing(CodexEntryDefinition::id))
                .map(definition -> Entry.from(definition, effectiveProgress(definition, progress, legacyProgress)))
                .toList();
        return GSON.toJson(new CodexInfobaseSnapshot(entries));
    }

    private static CodexEntryProgress effectiveProgress(
            CodexEntryDefinition definition,
            CodexPlayerProgress progress,
            CodexData legacyProgress
    ) {
        CodexEntryProgress saved = progress.get(definition.id());
        if (saved != null) {
            return saved;
        }
        String legacyId = definition.id().contains(":")
                ? definition.id().substring(definition.id().indexOf(':') + 1)
                : definition.id();
        if (legacyProgress.damagedEntries().contains(legacyId)) {
            return new CodexEntryProgress(true, CodexEntryState.DAMAGED, 0);
        }
        if (legacyProgress.unlockedEntries().contains(legacyId)) {
            return new CodexEntryProgress(true, CodexEntryState.UNLOCKED, 0);
        }
        return CodexStorage.status(progress, definition);
    }

    public static CodexInfobaseSnapshot decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return EMPTY;
        }
        try {
            CodexInfobaseSnapshot snapshot = GSON.fromJson(encoded, CodexInfobaseSnapshot.class);
            return snapshot == null ? EMPTY : snapshot;
        } catch (RuntimeException ignored) {
            return EMPTY;
        }
    }

    public record Entry(
            String id,
            String title,
            String category,
            String state,
            String summary,
            String fullContent,
            String sourceType,
            Set<String> flags,
            Map<String, String> metadata,
            List<String> requirements,
            int decryptRequired,
            int decryptProgress,
            int threatLevel,
            int sortOrder,
            List<String> tags,
            List<String> relatedEntries,
            List<CodexArticleSection> sections,
            boolean known
    ) {
        public Entry {
            id = id == null ? "" : id;
            title = title == null ? id : title;
            category = category == null ? "SYSTEM" : category;
            state = state == null ? "LOCKED" : state;
            summary = summary == null ? "" : summary;
            fullContent = fullContent == null ? "" : fullContent;
            sourceType = sourceType == null ? "OTHER" : sourceType;
            flags = flags == null ? Set.of() : Set.copyOf(flags);
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
            requirements = requirements == null ? List.of() : List.copyOf(requirements);
            tags = tags == null ? List.of() : List.copyOf(tags);
            relatedEntries = relatedEntries == null ? List.of() : List.copyOf(relatedEntries);
            sections = sections == null ? List.of() : List.copyOf(sections);
        }

        private static Entry from(CodexEntryDefinition definition, CodexEntryProgress progress) {
            return new Entry(
                    definition.id(),
                    definition.title(),
                    definition.category().name(),
                    progress.state().name(),
                    definition.summary(),
                    definition.fullContent(),
                    definition.sourceType().name(),
                    definition.flags(),
                    definition.metadata(),
                    definition.requirements(),
                    definition.decryptData().requiredFragments(),
                    progress.decryptProgress(),
                    definition.threatLevel(),
                    definition.sortOrder(),
                    definition.tags(),
                    definition.relatedEntries(),
                    definition.sections(),
                    progress.known()
            );
        }
    }
}
