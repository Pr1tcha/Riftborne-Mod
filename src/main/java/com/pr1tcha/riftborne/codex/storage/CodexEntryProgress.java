package com.pr1tcha.riftborne.codex.storage;

import com.pr1tcha.riftborne.codex.data.state.CodexEntryState;
import net.minecraft.nbt.CompoundTag;

public record CodexEntryProgress(
        boolean known,
        CodexEntryState state,
        int decryptProgress
) {
    public CodexEntryProgress {
        state = state == null ? CodexEntryState.LOCKED : state;
        decryptProgress = Math.max(0, decryptProgress);
    }

    public CompoundTag save(String id) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putBoolean("Known", known);
        tag.putString("State", state.name());
        tag.putInt("DecryptProgress", decryptProgress);
        return tag;
    }

    public static CodexEntryProgress load(CompoundTag tag) {
        CodexEntryState state;
        try {
            state = CodexEntryState.fromString(tag.getString("State"));
        } catch (RuntimeException ignored) {
            state = CodexEntryState.LOCKED;
        }
        return new CodexEntryProgress(tag.getBoolean("Known"), state, tag.getInt("DecryptProgress"));
    }
}
