package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.codex.data.PocketCodexMode;
import com.pr1tcha.riftborne.codex.network.CodexNetwork;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class PocketCodexScreen extends Screen {
    private static final int WIDTH = 304;
    private static final int HEIGHT = 176;
    private static final int ACCENT = 0xFF57E6C2;
    private static final int CYAN = 0xFF64CFEA;
    private static final int TEXT = 0xFFE7FFF7;
    private static final int MUTED = 0xFF789B91;
    private static final int FAINT = 0xFF3E5D57;
    private static final int DANGER = 0xFFF06C72;
    private static final int AMBER = 0xFFE7BC62;
    private static final String FIELD_SEPARATOR = "\u001D";
    private CodexNetwork.PocketSnapshotPayload snapshot;
    private int bufferScroll;

    public PocketCodexScreen(CodexNetwork.PocketSnapshotPayload snapshot) {
        super(Component.translatable("screen.riftborne.pocket_codex.field_recorder"));
        this.snapshot = snapshot;
    }

    public void updateSnapshot(CodexNetwork.PocketSnapshotPayload snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Field equipment must leave the world visible.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        graphics.fill(left + 4, top + 5, left + WIDTH + 5, top + HEIGHT + 6, 0x66000000);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xF4091112);
        graphics.renderOutline(left, top, WIDTH, HEIGHT, 0xFF28685B);
        graphics.fill(left + 1, top + 1, left + WIDTH - 1, top + 23, 0xFF102322);
        graphics.fill(left + 8, top + 7, left + 13, top + 16, ACCENT);
        graphics.drawString(font, title, left + 19, top + 8, TEXT, false);
        graphics.drawString(font, "FIELD LINK", left + WIDTH - 71, top + 8, MUTED, false);

        renderTabs(graphics, left, top, mouseX, mouseY);
        switch (mode()) {
            case SCANNER -> renderScanner(graphics, left, top);
            case PULSE -> renderPulse(graphics, left, top);
            case BUFFER -> renderBuffer(graphics, left, top);
        }
        renderAction(graphics, left, top, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTabs(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        PocketCodexMode[] modes = PocketCodexMode.values();
        int width = (WIDTH - 16) / modes.length;
        for (int index = 0; index < modes.length; index++) {
            int x = left + 8 + index * width;
            boolean selected = mode().ordinal() == index;
            boolean hovered = inside(mouseX, mouseY, x, top + 29, width - 3, 19);
            graphics.fill(x, top + 29, x + width - 3, top + 48,
                    selected ? 0xFF173F37 : hovered ? 0xFF132A28 : 0xFF0C1919);
            graphics.renderOutline(x, top + 29, width - 3, 19, selected ? ACCENT : FAINT);
            graphics.drawCenteredString(font,
                    Component.translatable("screen.riftborne.pocket_codex.mode."
                            + modes[index].name().toLowerCase()),
                    x + (width - 3) / 2, top + 35, selected ? TEXT : MUTED);
        }
    }

    private void renderScanner(GuiGraphics graphics, int left, int top) {
        int boxX = left + 10;
        int boxY = top + 57;
        int boxWidth = 112;
        int boxHeight = 73;
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xAA030B0C);
        graphics.renderOutline(boxX, boxY, boxWidth, boxHeight, 0xFF2D7D6D);
        graphics.hLine(boxX + 8, boxX + boxWidth - 9, boxY + boxHeight / 2, FAINT);
        graphics.vLine(boxX + boxWidth / 2, boxY + 8, boxY + boxHeight - 9, FAINT);
        graphics.renderOutline(boxX + 38, boxY + 22, 36, 29, ACCENT);
        graphics.drawCenteredString(font, Component.translatable("screen.riftborne.pocket_codex.acquire"),
                boxX + boxWidth / 2, boxY + 58, MUTED);

        int infoX = left + 134;
        int infoWidth = WIDTH - 144;
        graphics.drawString(font, Component.translatable("screen.riftborne.pocket_codex.last_contact"),
                infoX, boxY, MUTED, false);
        if (snapshot.lastTargetTitle().isBlank()) {
            graphics.drawWordWrap(font, Component.translatable("screen.riftborne.pocket_codex.no_contact"),
                    infoX, boxY + 15, infoWidth, FAINT);
            return;
        }
        graphics.drawString(font, trim(snapshot.lastTargetTitle(), infoWidth), infoX, boxY + 15, TEXT, false);
        graphics.drawString(font, Component.translatable("screen.riftborne.pocket_codex.threat", snapshot.threat()),
                infoX, boxY + 31, threatColor(snapshot.threat()), false);
        graphics.drawString(font, Component.translatable("screen.riftborne.pocket_codex.observation",
                        snapshot.observations(), snapshot.maximumObservations()),
                infoX, boxY + 46, CYAN, false);
        int barWidth = infoWidth - 4;
        graphics.fill(infoX, boxY + 61, infoX + barWidth, boxY + 67, 0xFF142825);
        int fill = snapshot.maximumObservations() <= 0 ? 0
                : barWidth * snapshot.observations() / snapshot.maximumObservations();
        graphics.fill(infoX, boxY + 61, infoX + fill, boxY + 67, ACCENT);
    }

    private void renderPulse(GuiGraphics graphics, int left, int top) {
        int centerX = left + 74;
        int centerY = top + 94;
        for (int ring = 3; ring >= 1; ring--) {
            int width = ring * 36;
            int height = ring * 22;
            graphics.renderOutline(centerX - width / 2, centerY - height / 2, width, height,
                    ring == 1 ? ACCENT : 0xFF24584F);
        }
        graphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, ACCENT);
        if (snapshot.pulseSignals() > 0) {
            graphics.fill(centerX + 25, centerY - 17, centerX + 30, centerY - 12, AMBER);
        }

        int infoX = left + 145;
        graphics.drawString(font, Component.translatable("screen.riftborne.pocket_codex.signatures"),
                infoX, top + 63, MUTED, false);
        graphics.drawString(font, Integer.toString(snapshot.pulseSignals()), infoX, top + 78,
                snapshot.pulseSignals() > 0 ? ACCENT : FAINT, false);
        graphics.drawString(font, Component.translatable("screen.riftborne.pocket_codex.nearest"),
                infoX, top + 96, MUTED, false);
        String nearest = snapshot.pulseNearestTitle().isBlank()
                ? Component.translatable("screen.riftborne.pocket_codex.none").getString()
                : snapshot.pulseNearestTitle();
        graphics.drawString(font, trim(nearest, WIDTH - 157), infoX, top + 110,
                snapshot.pulseNearestTitle().isBlank() ? FAINT : TEXT, false);
        if (snapshot.pulseNearestDistance() >= 0) {
            graphics.drawString(font, Component.translatable("screen.riftborne.pocket_codex.distance",
                            snapshot.pulseNearestDistance()),
                    infoX, top + 122, CYAN, false);
        }
    }

    private void renderBuffer(GuiGraphics graphics, int left, int top) {
        List<String> rows = CodexNetwork.split(snapshot.bufferRows());
        int queued = CodexNetwork.split(snapshot.queuedEntries()).size();
        int damaged = CodexNetwork.split(snapshot.damagedEntries()).size();
        int used = Math.min(snapshot.bufferCapacity(), queued + damaged);
        graphics.drawString(font, Component.translatable("screen.riftborne.pocket_codex.buffer_usage",
                        used, snapshot.bufferCapacity()),
                left + 10, top + 57, TEXT, false);
        int barWidth = WIDTH - 20;
        graphics.fill(left + 10, top + 70, left + 10 + barWidth, top + 76, 0xFF142825);
        graphics.fill(left + 10, top + 70,
                left + 10 + (snapshot.bufferCapacity() == 0 ? 0 : barWidth * used / snapshot.bufferCapacity()),
                top + 76, used >= snapshot.bufferCapacity() ? DANGER : ACCENT);

        int visible = 4;
        bufferScroll = Math.clamp(bufferScroll, 0, Math.max(0, rows.size() - visible));
        int rowY = top + 83;
        for (int index = bufferScroll; index < Math.min(rows.size(), bufferScroll + visible); index++) {
            String[] fields = rows.get(index).split(FIELD_SEPARATOR, -1);
            if (fields.length < 4) {
                continue;
            }
            int color = "DAMAGED".equals(fields[3]) ? DANGER : "QUEUED".equals(fields[3]) ? CYAN : MUTED;
            graphics.fill(left + 10, rowY, left + WIDTH - 10, rowY + 18, 0x99102020);
            graphics.fill(left + 10, rowY, left + 13, rowY + 18, color);
            graphics.drawString(font, trim(fields[1], WIDTH - 92), left + 18, rowY + 5, TEXT, false);
            graphics.drawString(font, fields[2] + "/3", left + WIDTH - 36, rowY + 5, color, false);
            rowY += 20;
        }
        if (rows.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.riftborne.pocket_codex.buffer_empty"),
                    left + WIDTH / 2, top + 104, FAINT);
        }
    }

    private void renderAction(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int x = left + 10;
        int y = top + HEIGHT - 31;
        int width = WIDTH - 20;
        boolean hovered = inside(mouseX, mouseY, x, y, width, 21);
        graphics.fill(x, y, x + width, y + 21, hovered ? 0xFF1C4B40 : 0xFF12332D);
        graphics.renderOutline(x, y, width, 21, ACCENT);
        String key = switch (mode()) {
            case SCANNER -> "screen.riftborne.pocket_codex.action.scan";
            case PULSE -> "screen.riftborne.pocket_codex.action.pulse";
            case BUFFER -> "screen.riftborne.pocket_codex.action.buffer";
        };
        graphics.drawCenteredString(font, Component.translatable(key), x + width / 2, y + 7, TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int left = panelLeft();
        int top = panelTop();
        int tabWidth = (WIDTH - 16) / 3;
        for (int index = 0; index < 3; index++) {
            int x = left + 8 + index * tabWidth;
            if (inside(mouseX, mouseY, x, top + 29, tabWidth - 3, 19)) {
                PacketDistributor.sendToServer(new CodexNetwork.PocketCyclePayload(index - mode().ordinal()));
                return true;
            }
        }
        if (inside(mouseX, mouseY, left + 10, top + HEIGHT - 31, WIDTH - 20, 21)) {
            PacketDistributor.sendToServer(new CodexNetwork.PocketActionPayload());
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_V) {
            PacketDistributor.sendToServer(new CodexNetwork.PocketCyclePayload(1));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_G) {
            PacketDistributor.sendToServer(new CodexNetwork.PocketActionPayload());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mode() == PocketCodexMode.BUFFER) {
            int size = CodexNetwork.split(snapshot.bufferRows()).size();
            bufferScroll = Math.clamp(bufferScroll - (int) Math.signum(scrollY), 0, Math.max(0, size - 4));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private PocketCodexMode mode() {
        return PocketCodexMode.byIndex(snapshot.mode());
    }

    private int threatColor(int threat) {
        return threat >= 4 ? DANGER : threat >= 3 ? AMBER : threat > 0 ? ACCENT : MUTED;
    }

    private String trim(String value, int width) {
        return font.plainSubstrByWidth(value == null ? "" : value, Math.max(1, width));
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int panelLeft() {
        return (width - WIDTH) / 2;
    }

    private int panelTop() {
        return (height - HEIGHT) / 2;
    }
}
