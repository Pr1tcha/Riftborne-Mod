package com.pr1tcha.Rifts;

import net.minecraft.world.level.GameRules;

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
}
