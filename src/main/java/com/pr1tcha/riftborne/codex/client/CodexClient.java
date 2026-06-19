package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.codex.network.CodexNetwork;
import net.minecraft.client.Minecraft;

public final class CodexClient {
    private CodexClient() {
    }

    public static void open(CodexNetwork.SnapshotPayload snapshot) {
        Minecraft.getInstance().setScreen(new CodexLaptopScreen(snapshot));
    }
}
