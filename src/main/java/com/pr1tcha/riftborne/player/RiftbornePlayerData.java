package com.pr1tcha.riftborne.player;

import com.pr1tcha.riftborne.codex.data.CodexData;
import com.pr1tcha.riftborne.codex.storage.CodexPlayerProgress;
import com.pr1tcha.riftborne.rna.data.RnaData;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class RiftbornePlayerData {
    public static final String ROOT_TAG = "Riftborne";
    private static final String RNA_TAG = "RNA";
    private static final String RNA_COMBAT_TAG = "Combat";
    private static final String CODEX_TAG = "Codex";
    private static final String CODEX_BACKEND_TAG = "CodexBackend";

    private RiftbornePlayerData() {
    }

    public static RnaData getRna(Player player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG);
        return RnaData.load(root.getCompound(RNA_TAG));
    }

    public static void saveRna(Player player, RnaData data) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG);
        CompoundTag existingRna = root.getCompound(RNA_TAG);
        CompoundTag savedRna = data.save();
        if (existingRna.contains(RNA_COMBAT_TAG)) {
            savedRna.put(RNA_COMBAT_TAG, existingRna.getCompound(RNA_COMBAT_TAG).copy());
        }
        root.put(RNA_TAG, savedRna);
        player.getPersistentData().put(ROOT_TAG, root);
    }

    public static RnaAbilityData getRnaCombat(Player player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG);
        CompoundTag rna = root.getCompound(RNA_TAG);
        return rna.contains(RNA_COMBAT_TAG)
                ? RnaAbilityData.load(rna.getCompound(RNA_COMBAT_TAG))
                : new RnaAbilityData();
    }

    public static void saveRnaCombat(Player player, RnaAbilityData data) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT_TAG);
        CompoundTag rna = root.getCompound(RNA_TAG);
        rna.put(RNA_COMBAT_TAG, data.save());
        root.put(RNA_TAG, rna);
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
