package com.pr1tcha.riftborne.codex.decrypt;

import com.pr1tcha.riftborne.codex.data.state.CodexEntryState;

public record DecryptData(
        DamagedDataType type,
        int requiredFragments,
        CodexEntryState successState
) {
    public static final DecryptData NONE = new DecryptData(
            DamagedDataType.NONE,
            0,
            CodexEntryState.UNLOCKED
    );

    public DecryptData {
        requiredFragments = Math.max(0, requiredFragments);
        if (type == null) {
            type = DamagedDataType.NONE;
        }
        if (successState == null || successState == CodexEntryState.LOCKED) {
            successState = CodexEntryState.UNLOCKED;
        }
    }

    public boolean enabled() {
        return type != DamagedDataType.NONE && requiredFragments > 0;
    }
}
