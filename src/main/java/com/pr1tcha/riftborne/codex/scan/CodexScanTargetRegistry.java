package com.pr1tcha.riftborne.codex.scan;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class CodexScanTargetRegistry {
    private static volatile List<CodexScanTargetDefinition> targets = List.of();

    private CodexScanTargetRegistry() {
    }

    public static CodexScanTargetDefinition resolve(BlockState state) {
        for (CodexScanTargetDefinition definition : targets) {
            if (definition.type() != CodexScanTargetDefinition.TargetType.BLOCK) {
                continue;
            }
            for (String target : definition.targets()) {
                if (matchesBlock(state, target)) {
                    return definition;
                }
            }
        }
        return null;
    }

    public static CodexScanTargetDefinition resolve(Entity entity) {
        for (CodexScanTargetDefinition definition : targets) {
            if (definition.type() != CodexScanTargetDefinition.TargetType.ENTITY) {
                continue;
            }
            for (String target : definition.targets()) {
                if (matchesEntity(entity, target)) {
                    return definition;
                }
            }
        }
        return null;
    }

    public static void replace(List<CodexScanTargetDefinition> loadedTargets) {
        targets = loadedTargets.stream()
                .sorted(Comparator.comparingInt(CodexScanTargetDefinition::priority).reversed()
                        .thenComparing(CodexScanTargetDefinition::id))
                .toList();
    }

    public static List<CodexScanTargetDefinition> all() {
        return targets;
    }

    private static boolean matchesBlock(BlockState state, String rawTarget) {
        boolean tag = rawTarget.startsWith("#");
        ResourceLocation id = ResourceLocation.tryParse(tag ? rawTarget.substring(1) : rawTarget);
        if (id == null) {
            return false;
        }
        if (tag) {
            return state.is(TagKey.create(Registries.BLOCK, id));
        }
        Block block = BuiltInRegistries.BLOCK.get(id);
        return block != null && state.is(block);
    }

    private static boolean matchesEntity(Entity entity, String rawTarget) {
        boolean tag = rawTarget.startsWith("#");
        ResourceLocation id = ResourceLocation.tryParse(tag ? rawTarget.substring(1) : rawTarget);
        if (id == null) {
            return false;
        }
        if (tag) {
            return entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, id));
        }
        return id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }
}
