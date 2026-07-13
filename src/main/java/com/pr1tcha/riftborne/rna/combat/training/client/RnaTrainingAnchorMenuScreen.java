package com.pr1tcha.riftborne.rna.combat.training.client;

import com.pr1tcha.riftborne.rna.combat.RnaCombatNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RnaTrainingAnchorMenuScreen extends Screen {
    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 24;
    private static final int BUTTON_GAP = 8;

    private final long anchorPos;
    private final boolean hasActiveRna;

    public RnaTrainingAnchorMenuScreen(long anchorPos, boolean hasActiveRna) {
        super(Component.translatable("screen.riftborne.rna_training_anchor"));
        this.anchorPos = anchorPos;
        this.hasActiveRna = hasActiveRna;
    }

    @Override
    protected void init() {
        int x = (width - BUTTON_WIDTH) / 2;
        int y = height / 2 - BUTTON_HEIGHT;

        if (hasActiveRna) {
            addRenderableWidget(Button.builder(
                    Component.translatable("screen.riftborne.rna_training_anchor.barrier_training"),
                    button -> select(RnaCombatNetwork.AnchorMenuSelectPayload.OPTION_BARRIER_TRAINING)
            ).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        } else {
            addRenderableWidget(Button.builder(
                    Component.translatable("screen.riftborne.rna_training_anchor.formation"),
                    button -> select(RnaCombatNetwork.AnchorMenuSelectPayload.OPTION_FORMATION)
            ).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        }

        y += BUTTON_HEIGHT + BUTTON_GAP;
        addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                button -> onClose()
        ).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private void select(String option) {
        PacketDistributor.sendToServer(new RnaCombatNetwork.AnchorMenuSelectPayload(anchorPos, option));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - BUTTON_HEIGHT - 24, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
