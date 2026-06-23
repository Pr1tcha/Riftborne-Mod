package com.pr1tcha.riftborne.rna.combat.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public record RnaAbilityUseContext(
        ServerPlayer player,
        ServerLevel level,
        Entity target,
        BlockPos targetPos,
        String action,
        String source,
        boolean meaningful
) {
    public RnaAbilityUseContext {
        if (player == null) {
            throw new IllegalArgumentException("RNA ability context requires a player");
        }
        level = level == null ? player.serverLevel() : level;
        action = action == null ? "" : action;
        source = source == null || source.isBlank() ? "ability_debug" : source;
    }

    public static RnaAbilityUseContext action(
            ServerPlayer player,
            Entity target,
            BlockPos targetPos,
            String action,
            String source
    ) {
        return new RnaAbilityUseContext(
                player,
                player.serverLevel(),
                target,
                targetPos,
                action,
                source,
                true
        );
    }
}
