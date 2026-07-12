package com.pr1tcha.riftborne.codex.scan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public final class CodexScanTargetReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final CodexScanTargetReloadListener INSTANCE = new CodexScanTargetReloadListener();

    private CodexScanTargetReloadListener() {
        super(GSON, "codex_scan_targets");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> resources,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        List<CodexScanTargetDefinition> loaded = new ArrayList<>();
        resources.forEach((resourceId, json) -> {
            try {
                loaded.add(parse(resourceId, json.getAsJsonObject()));
            } catch (RuntimeException exception) {
                LOGGER.error("Unable to load Pocket Codex scan target {}", resourceId, exception);
            }
        });
        CodexScanTargetRegistry.replace(loaded);
        LOGGER.info("Loaded {} data-driven Pocket Codex scan targets", loaded.size());
    }

    private static CodexScanTargetDefinition parse(ResourceLocation resourceId, JsonObject json) {
        String id = string(json, "id", resourceId.toString());
        String entryId = requiredString(json, "entry");
        CodexScanTargetDefinition.TargetType type = CodexScanTargetDefinition.TargetType.fromString(
                requiredString(json, "type")
        );
        List<String> targets = strings(json.getAsJsonArray("targets"));
        boolean damaged = json.has("damaged") && json.get("damaged").getAsBoolean();
        int priority = json.has("priority") ? json.get("priority").getAsInt() : 0;
        return new CodexScanTargetDefinition(id, entryId, type, targets, damaged, priority);
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

    private static List<String> strings(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                result.add(element.getAsString());
            }
        }
        return result;
    }
}
