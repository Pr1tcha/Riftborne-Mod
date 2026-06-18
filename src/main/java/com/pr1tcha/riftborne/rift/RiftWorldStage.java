package com.pr1tcha.riftborne.rift;

import com.pr1tcha.riftborne.Riftborne;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public final class RiftWorldStage {
    public static final int STAGE_RIFTS_DISABLED = 0;
    public static final int STAGE_WEAK_RIFTS_ENABLED = 1;

    public static final GameRules.Key<GameRules.IntegerValue> RIFT_STAGE = GameRules.register(
            "riftborneRiftStage",
            GameRules.Category.MISC,
            GameRules.IntegerValue.create(STAGE_RIFTS_DISABLED)
    );

    private RiftWorldStage() {
    }

    public static void init() {
        // Forces static registration during mod construction.
    }

    public static int getStage(Level level) {
        GameRules.IntegerValue rule = level.getGameRules().getRule(RIFT_STAGE);
        return rule == null ? STAGE_RIFTS_DISABLED : rule.get();
    }

    public static boolean setStage(Level level, int stage, net.minecraft.server.MinecraftServer server) {
        GameRules.IntegerValue rule = level.getGameRules().getRule(RIFT_STAGE);
        if (rule == null) {
            return false;
        }

        rule.set(stage, server);
        return true;
    }
}
