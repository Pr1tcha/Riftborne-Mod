package com.pr1tcha.riftborne.rna.power.anchor.client;

import com.pr1tcha.riftborne.rna.power.network.PowerNetwork;
import net.minecraft.client.Minecraft;

public final class RnaTrainingAnchorClient {
    private RnaTrainingAnchorClient() {
    }

    public static void openMenu(PowerNetwork.AnchorMenuPayload payload) {
        Minecraft.getInstance().setScreen(
                new RnaTrainingAnchorMenuScreen(payload.anchorPos(), payload.hasActiveRna())
        );
    }
}
