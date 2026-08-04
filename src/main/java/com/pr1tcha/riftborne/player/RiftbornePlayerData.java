package com.pr1tcha.riftborne.player;

import com.pr1tcha.riftborne.codex.data.CodexData;
import com.pr1tcha.riftborne.codex.storage.CodexPlayerProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Persistent player state that still lives in raw NBT under one root tag. The power system moved to
 * Data Attachments in the PS V2.5 rework, so only the Codex sections remain here.
 */
public final class RiftbornePlayerData {
    public static final String ROOT_TAG = "Riftborne";
    private static final String CODEX_TAG = "Codex";
    private static final String CODEX_BACKEND_TAG = "CodexBackend";

    private RiftbornePlayerData() {
    }

    public static CodexData getCodex(Player player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG);
        return root.contains(CODEX_TAG) ? CodexData.load(root.getCompound(CODEX_TAG)) : new CodexData();
    }

    public static void saveCodex(Player player, CodexData data) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG);
        root.put(CODEX_TAG, data.save());
        player.getPersistentData().put(ROOT_TAG, root);
    }

    public static CodexPlayerProgress getCodexBackend(Player player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG);
        return root.contains(CODEX_BACKEND_TAG)
                ? CodexPlayerProgress.load(root.getCompound(CODEX_BACKEND_TAG))
                : new CodexPlayerProgress();
    }

    public static void saveCodexBackend(Player player, CodexPlayerProgress data) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG);
        root.put(CODEX_BACKEND_TAG, data.save());
        player.getPersistentData().put(ROOT_TAG, root);
    }

    public static void copy(Player original, Player replacement) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG)) {
            replacement.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
        }
    }
}
