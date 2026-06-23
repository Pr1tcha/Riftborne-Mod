package com.pr1tcha.riftborne.rna.combat.cooldown;

public record RnaCooldown(long untilTick) {
    public long remaining(long gameTime) {
        return Math.max(0L, untilTick - gameTime);
    }

    public boolean active(long gameTime) {
        return remaining(gameTime) > 0L;
    }
}
