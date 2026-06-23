package com.pr1tcha.riftborne.codex.scan.api;

import net.minecraft.server.level.ServerPlayer;

public interface CodexScannable {
    boolean canScan(ServerPlayer player, CodexScanType type);

    CodexScanResult scan(ServerPlayer player, CodexScanType type);
}
