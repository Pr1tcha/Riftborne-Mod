package com.pr1tcha.riftborne.player;

import com.pr1tcha.riftborne.codex.data.CodexData;
import com.pr1tcha.riftborne.rna.data.RnaData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class RiftbornePlayerData {
    public static final String ROOT_TAG = "Riftborne";
    private static final String RNA_TAG = "RNA";
    private static final String CODEX_TAG = "Codex";

    private RiftbornePlayerData() {
    }

    public static RnaData getRna(Player player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG);
        return RnaData.load(root.getCompound(RNA_TAG));
    }

    public static void saveRna(Player player, RnaData data) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG);
        root.put(RNA_TAG, data.save());
        player.getPersistentData().put(ROOT_TAG, root);
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

    public static void copy(Player original, Player replacement) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(ROOT_TAG)) {
            replacement.getPersistentData().put(ROOT_TAG, originalData.getCompound(ROOT_TAG).copy());
        }
    }
}
