package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.codex.CodexEntries;
import com.pr1tcha.riftborne.codex.data.CodexEntry;
import com.pr1tcha.riftborne.codex.network.CodexNetwork;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PocketCodexScreen extends Screen {
    private static final int WIDTH = 214;
    private static final int HEIGHT = 332;
    private static final int ACCENT = 0xFF58E3B4;
    private static final int TEXT = 0xFFE6FFF5;
    private static final int MUTED = 0xFF77998D;
    private CodexNetwork.PocketSnapshotPayload snapshot;
    private int selectedEntry;

    public PocketCodexScreen(CodexNetwork.PocketSnapshotPayload snapshot) {
        super(Component.translatable("screen.riftborne.pocket_codex"));
        this.snapshot = snapshot;
    }

    public void updateSnapshot(CodexNetwork.PocketSnapshotPayload snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xF20A1013);
        graphics.renderOutline(left, top, WIDTH, HEIGHT, 0xFF285A4C);
        graphics.fill(left + 10, top + 10, left + WIDTH - 10, top + HEIGHT - 10, 0xFF071914);
        graphics.drawString(font, title, left + 18, top + 18, ACCENT, false);
        graphics.drawString(font, screenTitle(), left + 18, top + 34, MUTED, false);
        graphics.hLine(left + 16, left + WIDTH - 17, top + 51, 0xFF1C5145);
        renderContent(graphics, left, top);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderContent(GuiGraphics graphics, int left, int top) {
        List<String> entries = CodexNetwork.split(snapshot.shortEntries());
        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.riftborne.pocket_codex.empty"),
                    left + WIDTH / 2, top + 154, MUTED);
            return;
        }

        selectedEntry = Math.min(selectedEntry, entries.size() - 1);
        int listTop = top + 62;
        graphics.fill(left + 16, listTop - 5, left + WIDTH - 16, listTop + 92, 0x7703090B);
        for (int index = 0; index < Math.min(entries.size(), 5); index++) {
            CodexEntry entry = CodexEntries.get(entries.get(index));
            if (entry != null) {
                if (index == selectedEntry) {
                    graphics.fill(left + 20, listTop - 2 + index * 18, left + WIDTH - 20, listTop + 14 + index * 18,
                            0x66307060);
                }
                graphics.drawString(font, Component.translatable(entry.titleKey()), left + 25, listTop + 3 + index * 18,
                        index == selectedEntry ? ACCENT : MUTED, false);
            }
        }

        CodexEntry entry = CodexEntries.get(entries.get(selectedEntry));
        if (entry == null) {
            return;
        }
        int x = left + 20;
        int detailTop = top + 171;
        graphics.fill(left + 16, detailTop - 8, left + WIDTH - 16, top + HEIGHT - 18, 0x7703090B);
        graphics.drawString(font, Component.translatable(entry.titleKey()), x, detailTop, TEXT, false);
        graphics.drawString(font, Component.translatable("screen.riftborne.pocket_codex.threat", entry.threatLevel()),
                x, detailTop + 16, threatColor(entry.threatLevel()), false);
        graphics.drawWordWrap(font, Component.translatable(entry.shortTextKey()), x, detailTop + 38, WIDTH - 40, 0xFFC8DED5);
        graphics.drawWordWrap(font, Component.translatable(entry.recommendationKey()), x, detailTop + 91, WIDTH - 40, ACCENT);
        String statusKey = CodexNetwork.split(snapshot.damagedEntries()).contains(entry.id())
                ? "screen.riftborne.pocket_codex.status.damaged"
                : CodexNetwork.split(snapshot.queuedEntries()).contains(entry.id())
                        ? "screen.riftborne.pocket_codex.status.queued"
                        : "screen.riftborne.pocket_codex.status.stored";
        graphics.drawWordWrap(font, Component.translatable(statusKey), x, top + HEIGHT - 48, WIDTH - 40, MUTED);
    }

    private Component screenTitle() {
        String name = switch (snapshot.selectedScreen()) {
            case 1 -> "scanner";
            case 2 -> "notes";
            case 3 -> "signals";
            case 4 -> "activity";
            default -> "home";
        };
        return Component.translatable("screen.riftborne.pocket_codex." + name);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        int index = (int) ((mouseY - (top + 60)) / 18);
        List<String> entries = CodexNetwork.split(snapshot.shortEntries());
        if (mouseX >= left + 16 && mouseX < left + WIDTH - 16
                && index >= 0 && index < Math.min(entries.size(), 5)) {
            selectedEntry = index;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int threatColor(int threat) {
        return switch (threat) {
            case 0, 1 -> 0xFF65E59D;
            case 2 -> 0xFFE6CE68;
            case 3 -> 0xFFE8915D;
            default -> 0xFFF05E68;
        };
    }
}
