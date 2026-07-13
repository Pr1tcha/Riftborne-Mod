package com.pr1tcha.riftborne.rna.combat.training.client;

import com.pr1tcha.riftborne.rna.combat.RnaCombatNetwork;
import net.minecraft.client.Minecraft;

public final class RnaTrainingAnchorClient {
    private RnaTrainingAnchorClient() {
    }

    public static void openMenu(RnaCombatNetwork.AnchorMenuPayload payload) {
        Minecraft.getInstance().setScreen(
                new RnaTrainingAnchorMenuScreen(payload.anchorPos(), payload.hasActiveRna())
        );
    }
}
