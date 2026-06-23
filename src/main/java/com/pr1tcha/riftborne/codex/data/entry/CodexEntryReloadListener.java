package com.pr1tcha.riftborne.codex.data.entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.pr1tcha.riftborne.codex.data.category.CodexCategory;
import com.pr1tcha.riftborne.codex.data.state.CodexEntryState;
import com.pr1tcha.riftborne.codex.decrypt.DamagedDataType;
import com.pr1tcha.riftborne.codex.decrypt.DecryptData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public final class CodexEntryReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final CodexEntryReloadListener INSTANCE = new CodexEntryReloadListener();

    private CodexEntryReloadListener() {
        super(GSON, "codex_entries");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<String, CodexEntryDefinition> loaded = new LinkedHashMap<>();
        resources.forEach((resourceId, json) -> {
            try {
                CodexEntryDefinition entry = parse(resourceId, json.getAsJsonObject());
                CodexEntryDefinition previous = loaded.put(entry.id(), entry);
                if (previous != null) {
                    throw new IllegalArgumentException("Duplicate codex entry id: " + entry.id());
                }
            } catch (RuntimeException exception) {
                LOGGER.error("Unable to load Codex entry {}", resourceId, exception);
            }
        });
        CodexEntryRegistry.replace(loaded);
        LOGGER.info("Loaded {} data-driven Codex entries", loaded.size());
    }

    private static CodexEntryDefinition parse(ResourceLocation resourceId, JsonObject json) {
        String id = string(json, "id", resourceId.toString());
        String title = requiredString(json, "title");
        CodexCategory category = CodexCategory.fromString(requiredString(json, "category"));
        CodexEntryState state = CodexEntryState.fromString(string(json, "state", "LOCKED"));
        String summary = string(json, "summary", "");
        String content = string(json, "fullContent", string(json, "content", ""));
        CodexSourceType sourceType = CodexSourceType.fromString(string(json, "sourceType", "OTHER"));
        Set<String> flags = new LinkedHashSet<>(strings(json.getAsJsonArray("flags")));
        Map<String, String> metadata = stringMap(json.getAsJsonObject("metadata"));
        List<String> requirements = strings(json.getAsJsonArray("requirements"));
        DecryptData decryptData = parseDecryptData(json.getAsJsonObject("decryptData"));
        return new CodexEntryDefinition(
                id,
                title,
                category,
                state,
                summary,
                content,
                sourceType,
                flags,
                metadata,
                requirements,
                decryptData
        );
    }

    private static DecryptData parseDecryptData(JsonObject json) {
        if (json == null) {
            return DecryptData.NONE;
        }
        return new DecryptData(
                DamagedDataType.fromString(string(json, "type", "NONE")),
                integer(json, "requiredFragments", 0),
                CodexEntryState.fromString(string(json, "successState", "UNLOCKED"))
        );
    }

    private static String requiredString(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing required string field: " + key);
        }
        return json.get(key).getAsString();
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : fallback;
    }

    private static int integer(JsonObject json, String key, int fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsInt() : fallback;
    }

    private static List<String> strings(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                values.add(element.getAsString());
            }
        }
        return values;
    }

    private static Map<String, String> stringMap(JsonObject object) {
        if (object == null) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        object.entrySet().forEach(entry -> values.put(
                entry.getKey(),
                entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : GSON.toJson(entry.getValue())
        ));
        return values;
    }
}
