package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.codex.CodexEntries;
import com.pr1tcha.riftborne.codex.data.CodexEntry;
import com.pr1tcha.riftborne.codex.network.CodexNetwork;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PocketCodexScreen extends Screen {
    private static final int WIDTH = 420;
    private static final int HEIGHT = 228;
    private static final int VISIBLE_ENTRIES = 7;
    private static final int ACCENT = 0xFF58E3B4;
    private static final int TEXT = 0xFFE6FFF5;
    private static final int MUTED = 0xFF77998D;
    private CodexNetwork.PocketSnapshotPayload snapshot;
    private int selectedEntry;
    private int entryScroll;

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
        int left = Math.max(6, width - WIDTH - 18);
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
                    left + WIDTH / 2, top + HEIGHT / 2, MUTED);
            return;
        }

        selectedEntry = Math.min(selectedEntry, entries.size() - 1);
        entryScroll = Math.clamp(entryScroll, 0, Math.max(0, entries.size() - VISIBLE_ENTRIES));

        int listX = left + 16;
        int listTop = top + 62;
        int listWidth = 142;
        int listHeight = HEIGHT - 82;
        graphics.fill(listX, listTop - 5, listX + listWidth, listTop + listHeight, 0x7703090B);
        for (int visible = 0; visible < Math.min(entries.size() - entryScroll, VISIBLE_ENTRIES); visible++) {
            int index = entryScroll + visible;
            CodexEntry entry = CodexEntries.get(entries.get(index));
            if (entry != null) {
                if (index == selectedEntry) {
                    graphics.fill(listX + 4, listTop - 2 + visible * 18, listX + listWidth - 4, listTop + 14 + visible * 18,
                            0x66307060);
                }
                graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(entry.titleKey()).getString(), listWidth - 14),
                        listX + 9, listTop + 3 + visible * 18,
                        index == selectedEntry ? ACCENT : MUTED, false);
            }
        }
        if (entries.size() > VISIBLE_ENTRIES) {
            int trackX = listX + listWidth - 6;
            int trackTop = listTop;
            int trackHeight = VISIBLE_ENTRIES * 18 - 4;
            int knobHeight = Math.max(12, trackHeight * VISIBLE_ENTRIES / entries.size());
            int knobY = trackTop + (trackHeight - knobHeight) * entryScroll / Math.max(1, entries.size() - VISIBLE_ENTRIES);
            graphics.fill(trackX, trackTop, trackX + 2, trackTop + trackHeight, 0x66304C4A);
            graphics.fill(trackX - 1, knobY, trackX + 3, knobY + knobHeight, ACCENT);
        }

        CodexEntry entry = CodexEntries.get(entries.get(selectedEntry));
        if (entry == null) {
            return;
        }
        int x = left + 176;
        int detailTop = top + 62;
        int detailWidth = WIDTH - 196;
        graphics.fill(x - 8, detailTop - 5, left + WIDTH - 16, top + HEIGHT - 20, 0x7703090B);
        graphics.drawString(font, Component.translatable(entry.titleKey()), x, detailTop, TEXT, false);
        graphics.drawString(font, Component.translatable("screen.riftborne.pocket_codex.threat", entry.threatLevel()),
                x, detailTop + 16, threatColor(entry.threatLevel()), false);
        graphics.drawWordWrap(font, Component.translatable(entry.shortTextKey()), x, detailTop + 38, detailWidth, 0xFFC8DED5);
        graphics.drawWordWrap(font, Component.translatable(entry.recommendationKey()), x, detailTop + 92, detailWidth, ACCENT);
        String statusKey = CodexNetwork.split(snapshot.damagedEntries()).contains(entry.id())
                ? "screen.riftborne.pocket_codex.status.damaged"
                : CodexNetwork.split(snapshot.queuedEntries()).contains(entry.id())
                        ? "screen.riftborne.pocket_codex.status.queued"
                        : "screen.riftborne.pocket_codex.status.stored";
        graphics.drawWordWrap(font, Component.translatable(statusKey), x, top + HEIGHT - 44, detailWidth, MUTED);
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
        int left = Math.max(6, width - WIDTH - 18);
        int top = (height - HEIGHT) / 2;
        int index = entryScroll + (int) ((mouseY - (top + 60)) / 18);
        List<String> entries = CodexNetwork.split(snapshot.shortEntries());
        if (mouseX >= left + 16 && mouseX < left + 158
                && index >= 0 && index < entries.size()
                && index < entryScroll + VISIBLE_ENTRIES) {
            selectedEntry = index;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<String> entries = CodexNetwork.split(snapshot.shortEntries());
        if (entries.size() > VISIBLE_ENTRIES) {
            entryScroll = Math.clamp(entryScroll - (int) Math.signum(scrollY), 0, entries.size() - VISIBLE_ENTRIES);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
