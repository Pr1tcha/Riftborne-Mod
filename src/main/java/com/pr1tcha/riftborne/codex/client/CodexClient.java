package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.codex.network.CodexNetwork;
import net.minecraft.client.Minecraft;

public final class CodexClient {
    private static CodexNetwork.PocketSnapshotPayload pocketSnapshot;

    private CodexClient() {
    }

    public static void open(CodexNetwork.SnapshotPayload snapshot) {
        Minecraft.getInstance().setScreen(new CodexLaptopScreen(snapshot));
    }

    public static void openPocket(CodexNetwork.PocketSnapshotPayload snapshot) {
        pocketSnapshot = snapshot;
        Minecraft minecraft = Minecraft.getInstance();
        if (!snapshot.noticeKey().isBlank() && minecraft.player != null) {
            minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable(snapshot.noticeKey()), true);
        }
        if (snapshot.openScreen()) {
            minecraft.setScreen(new PocketCodexScreen(snapshot));
        } else if (minecraft.screen instanceof PocketCodexScreen screen) {
            screen.updateSnapshot(snapshot);
        }
    }

    public static CodexNetwork.PocketSnapshotPayload pocketSnapshot() {
        return pocketSnapshot;
    }
}
