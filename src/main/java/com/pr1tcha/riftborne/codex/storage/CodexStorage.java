package com.pr1tcha.riftborne.codex.storage;

import com.pr1tcha.riftborne.codex.data.entry.CodexEntryDefinition;
import com.pr1tcha.riftborne.codex.data.entry.CodexEntryRegistry;
import com.pr1tcha.riftborne.codex.data.state.CodexEntryState;
import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import net.minecraft.server.level.ServerPlayer;

public final class CodexStorage {
    private CodexStorage() {
    }

    public static CodexPlayerProgress get(ServerPlayer player) {
        return RiftbornePlayerData.getCodexBackend(player);
    }

    public static void save(ServerPlayer player, CodexPlayerProgress progress) {
        RiftbornePlayerData.saveCodexBackend(player, progress);
    }

    public static CodexEntryProgress status(CodexPlayerProgress progress, CodexEntryDefinition entry) {
        CodexEntryProgress saved = progress.get(entry.id());
        if (saved != null) {
            return saved;
        }
        return new CodexEntryProgress(
                entry.initialState() != CodexEntryState.LOCKED,
                entry.initialState(),
                0
        );
    }

    public static boolean unlock(ServerPlayer player, String id) {
        return setState(player, id, CodexEntryState.UNLOCKED);
    }

    public static boolean lock(ServerPlayer player, String id) {
        CodexEntryDefinition entry = CodexEntryRegistry.get(id);
        if (entry == null) {
            return false;
        }
        CodexPlayerProgress progress = get(player);
        progress.put(id, new CodexEntryProgress(false, CodexEntryState.LOCKED, 0));
        save(player, progress);
        return true;
    }

    public static boolean setState(ServerPlayer player, String id, CodexEntryState state) {
        CodexEntryDefinition entry = CodexEntryRegistry.get(id);
        if (entry == null) {
            return false;
        }
        CodexPlayerProgress progress = get(player);
        CodexEntryProgress current = status(progress, entry);
        progress.put(id, new CodexEntryProgress(
                state != CodexEntryState.LOCKED,
                state,
                current.decryptProgress()
        ));
        save(player, progress);
        return true;
    }

    public static int giveFragments(ServerPlayer player, String id, int amount) {
        CodexEntryDefinition entry = CodexEntryRegistry.get(id);
        if (entry == null || !entry.decryptData().enabled()) {
            return -1;
        }
        CodexPlayerProgress progress = get(player);
        CodexEntryProgress current = status(progress, entry);
        int total = Math.min(
                entry.decryptData().requiredFragments(),
                current.decryptProgress() + Math.max(0, amount)
        );
        CodexEntryState state = total >= entry.decryptData().requiredFragments()
                ? entry.decryptData().successState()
                : CodexEntryState.NEEDS_DECRYPTION;
        progress.put(id, new CodexEntryProgress(true, state, total));
        save(player, progress);
        return total;
    }

    public static boolean completeDecryption(ServerPlayer player, String id) {
        CodexEntryDefinition entry = CodexEntryRegistry.get(id);
        if (entry == null || !entry.decryptData().enabled()) {
            return false;
        }
        CodexPlayerProgress progress = get(player);
        progress.put(id, new CodexEntryProgress(
                true,
                entry.decryptData().successState(),
                entry.decryptData().requiredFragments()
        ));
        save(player, progress);
        return true;
    }

    public static void reset(ServerPlayer player) {
        CodexPlayerProgress progress = new CodexPlayerProgress();
        save(player, progress);
    }
}
