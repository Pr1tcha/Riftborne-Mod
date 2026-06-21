package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.codex.CodexEntries;
import com.pr1tcha.riftborne.codex.data.CodexData;
import com.pr1tcha.riftborne.codex.data.CodexEntry;
import com.pr1tcha.riftborne.codex.network.CodexNetwork;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CodexLaptopScreen extends Screen {
    private static final int PANEL_WIDTH = 600;
    private static final int PANEL_HEIGHT = 340;
    private static final int STATUS_HEIGHT = 22;
    private static final int SCREEN_MARGIN = 12;
    private static final int TOAST_MIN_WIDTH = 220;
    private static final int TOAST_MAX_WIDTH = 300;
    private static final int COLOR_SHELL = 0xF2080C11;
    private static final int COLOR_DESKTOP = 0xFF0B1520;
    private static final int COLOR_WINDOW = 0xF20C121A;
    private static final int COLOR_BORDER = 0xFF2FA9A4;
    private static final int COLOR_ACCENT = 0xFF55E1D5;
    private static final int COLOR_TEXT = 0xFFE5F5F3;
    private static final int COLOR_MUTED = 0xFF819193;

    private final CodexNetwork.SnapshotPayload snapshot;
    private View view = View.DESKTOP;
    private String selectedEntryId = "rna_overview";

    public CodexLaptopScreen(CodexNetwork.SnapshotPayload snapshot) {
        super(Component.translatable("screen.riftborne.codex_laptop"));
        this.snapshot = snapshot;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The laptop is an in-world display. Keep the world sharp behind it.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float scale = interfaceScale();
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);
        int left = panelLeft();
        int top = panelTop();

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        drawLaptopShell(graphics, left, top);
        if (snapshot.powered()) {
            drawDesktopWallpaper(graphics, left, top);
            if (view == View.DESKTOP) {
                renderDesktop(graphics, left, top, scaledMouseX, scaledMouseY);
            } else {
                renderCodexWindow(graphics, left, top, scaledMouseX, scaledMouseY);
            }
        } else {
            renderStandby(graphics, left, top);
        }
        renderStatusBar(graphics, left, top, scaledMouseX, scaledMouseY);
        graphics.pose().popPose();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawLaptopShell(GuiGraphics graphics, int left, int top) {
        graphics.fill(left - 3, top - 3, left + PANEL_WIDTH + 3, top + PANEL_HEIGHT + 3, 0xCC020407);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_SHELL);
        graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF17343C);
        graphics.renderOutline(left + 2, top + 2, PANEL_WIDTH - 4, PANEL_HEIGHT - 4, 0xFF10242A);
    }

    private void drawDesktopWallpaper(GuiGraphics graphics, int left, int top) {
        int bottom = top + PANEL_HEIGHT - STATUS_HEIGHT;
        graphics.fill(left + 3, top + 3, left + PANEL_WIDTH - 3, bottom, COLOR_DESKTOP);
        graphics.fill(left + 3, top + 3, left + PANEL_WIDTH - 3, top + 44, 0xFF101D28);
        graphics.fill(left + 3, top + 44, left + PANEL_WIDTH - 3, bottom, 0xFF08111A);

        for (int index = 0; index < 11; index++) {
            int x = left + 24 + index * 31;
            int y = top + 24 + (index * 37 % 135);
            graphics.fill(x, y, x + 2, y + 2, index % 3 == 0 ? 0x8848CFC5 : 0x55306B76);
        }
        graphics.hLine(left + 18, left + PANEL_WIDTH - 19, top + 47, 0x4425656B);
        graphics.drawString(font, "RIFT/OS", left + 12, top + 12, 0x886DB6B4, false);
    }

    private void renderDesktop(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int iconX = left + 24;
        int iconY = top + 35;
        boolean hovered = inside(mouseX, mouseY, iconX - 6, iconY - 5, 58, 72);
        if (hovered) {
            graphics.fill(iconX - 6, iconY - 5, iconX + 52, iconY + 67, 0x55345D66);
            graphics.renderOutline(iconX - 6, iconY - 5, 58, 72, 0x8855E1D5);
        }

        drawCodexIcon(graphics, iconX, iconY);
        graphics.drawCenteredString(font, Component.translatable("screen.riftborne.codex.icon"), iconX + 22, iconY + 51, COLOR_TEXT);

        List<String> notifications = CodexNetwork.split(snapshot.notifications());
        List<String> recent = CodexNetwork.split(snapshot.recentData());
        if (!notifications.isEmpty() || !recent.isEmpty()) {
            Component systemText = Component.translatable("screen.riftborne.codex.system");
            Component notificationText = notifications.isEmpty() ? Component.empty() : feedComponent(notifications.get(0));
            Component recentText = recent.isEmpty() ? Component.empty() : feedComponent(recent.get(0));
            int toastWidth = Math.clamp(
                    Math.max(font.width(systemText), Math.max(font.width(notificationText), font.width(recentText))) + 14,
                    TOAST_MIN_WIDTH,
                    TOAST_MAX_WIDTH
            );
            int toastX = left + PANEL_WIDTH - toastWidth - 18;
            int toastY = top + 18;
            graphics.fill(toastX, toastY, toastX + toastWidth, toastY + 53, 0xB5091119);
            graphics.renderOutline(toastX, toastY, toastWidth, 53, 0x6638A9A4);
            graphics.drawString(font, systemText, toastX + 7, toastY + 7, COLOR_ACCENT, false);
            if (!notifications.isEmpty()) {
                drawTrimmed(graphics, notificationText, toastX + 7, toastY + 20, toastWidth - 14, COLOR_TEXT);
            }
            if (!recent.isEmpty()) {
                drawTrimmed(graphics, recentText, toastX + 7, toastY + 34, toastWidth - 14, COLOR_MUTED);
            }
        }
    }

    private void drawCodexIcon(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 3, y, x + 39, y + 44, 0xFF172631);
        graphics.fill(x, y + 4, x + 36, y + 48, 0xFF203A43);
        graphics.renderOutline(x, y + 4, 36, 44, COLOR_ACCENT);
        graphics.fill(x + 7, y + 11, x + 29, y + 14, 0xFF55E1D5);
        graphics.fill(x + 7, y + 19, x + 25, y + 21, 0xFF3B8D90);
        graphics.fill(x + 7, y + 27, x + 28, y + 29, 0xFF3B8D90);
        graphics.fill(x + 7, y + 35, x + 20, y + 37, 0xFF3B8D90);
    }

    private void renderCodexWindow(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int windowX = left + 16;
        int windowY = top + 14;
        int windowWidth = PANEL_WIDTH - 32;
        int windowHeight = PANEL_HEIGHT - STATUS_HEIGHT - 26;

        graphics.fill(windowX, windowY, windowX + windowWidth, windowY + windowHeight, COLOR_WINDOW);
        graphics.renderOutline(windowX, windowY, windowWidth, windowHeight, COLOR_BORDER);
        graphics.fill(windowX + 1, windowY + 1, windowX + windowWidth - 1, windowY + 24, 0xFF12232D);
        graphics.drawString(font, Component.translatable("screen.riftborne.codex.archive_title"), windowX + 9, windowY + 8, COLOR_TEXT, false);

        int closeX = windowX + windowWidth - 21;
        boolean closeHovered = inside(mouseX, mouseY, closeX, windowY + 4, 16, 16);
        graphics.fill(closeX, windowY + 4, closeX + 16, windowY + 20, closeHovered ? 0xFF9A3543 : 0xFF263943);
        graphics.drawCenteredString(font, "×", closeX + 8, windowY + 8, COLOR_TEXT);

        int listX = windowX + 8;
        int listY = windowY + 31;
        int listWidth = 137;
        int contentX = windowX + 154;
        int contentWidth = windowWidth - 162;
        graphics.fill(listX, listY, listX + listWidth, windowY + windowHeight - 8, 0x88050A0F);
        graphics.fill(contentX, listY, contentX + contentWidth, windowY + windowHeight - 8, 0x88050A0F);

        Set<String> unlocked = new HashSet<>(CodexNetwork.split(snapshot.unlockedEntries()));
        int rowY = listY + 4;
        for (CodexEntry entry : CodexEntries.all()) {
            boolean isUnlocked = unlocked.contains(entry.id());
            boolean selected = entry.id().equals(selectedEntryId);
            boolean hovered = inside(mouseX, mouseY, listX + 4, rowY, listWidth - 8, 19);
            if (selected || hovered) {
                graphics.fill(listX + 4, rowY, listX + listWidth - 4, rowY + 19,
                        selected ? 0x88317D7D : 0x442B5960);
            }
            Component label = isUnlocked
                    ? Component.translatable(entry.titleKey())
                    : Component.translatable("screen.riftborne.codex.locked_entry");
            drawTrimmed(graphics, label, listX + 9, rowY + 6, listWidth - 18,
                    isUnlocked ? COLOR_TEXT : 0xFF4D5A5C);
            rowY += 22;
        }

        CodexEntry selected = unlocked.contains(selectedEntryId) ? CodexEntries.get(selectedEntryId) : null;
        if (selected == null) {
            graphics.drawString(font, Component.translatable("screen.riftborne.codex.select_entry"), contentX + 9, listY + 10, COLOR_MUTED, false);
            return;
        }

        String category = Component.translatable(selected.categoryKey()).getString().toUpperCase(Locale.ROOT);
        graphics.drawString(font, category, contentX + 9, listY + 9, COLOR_ACCENT, false);
        graphics.drawString(font, Component.translatable(selected.titleKey()), contentX + 9, listY + 25, COLOR_TEXT, false);
        List<FormattedCharSequence> lines = font.split(Component.translatable(selected.textKey()), contentWidth - 18);
        int textY = listY + 45;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, contentX + 9, textY, 0xFFC1D1D0, false);
            textY += 11;
        }
    }

    private void renderStandby(GuiGraphics graphics, int left, int top) {
        graphics.fill(left + 3, top + 3, left + PANEL_WIDTH - 3, top + PANEL_HEIGHT - STATUS_HEIGHT, 0xFF020406);
        graphics.drawCenteredString(font, Component.translatable("screen.riftborne.codex.standby"), left + PANEL_WIDTH / 2,
                top + PANEL_HEIGHT / 2 - 10, 0xFF41696C);
        graphics.drawCenteredString(font, Component.translatable("screen.riftborne.codex.power_hint"), left + PANEL_WIDTH / 2,
                top + PANEL_HEIGHT / 2 + 6, 0xFF273F43);
    }

    private void renderStatusBar(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int y = top + PANEL_HEIGHT - STATUS_HEIGHT;
        graphics.fill(left + 3, y, left + PANEL_WIDTH - 3, top + PANEL_HEIGHT - 3, 0xF205090D);
        graphics.hLine(left + 3, left + PANEL_WIDTH - 4, y, 0xFF1B363D);
        int powerX = left + 6;
        boolean powerHovered = inside(mouseX, mouseY, powerX, y + 3, 34, 16);
        if (powerHovered) {
            graphics.fill(powerX, y + 3, powerX + 34, y + 19, 0x55365E66);
        }
        graphics.drawString(font, "PWR", left + 10, y + 7,
                powerHovered ? 0xFFFFFFFF : COLOR_ACCENT, false);

        String rnaStatus = snapshot.hasRna()
                ? Component.translatable("screen.riftborne.codex.rna_status", stageLabel(), snapshot.metaWear()).getString()
                : Component.translatable("screen.riftborne.codex.rna_offline").getString();
        graphics.drawString(font, rnaStatus, left + 48, y + 7,
                snapshot.hasRna() ? stageColor() : 0xFFB24C5B, false);

        String time = minecraftTime();
        int timeWidth = font.width(time);
        graphics.drawString(font, time, left + PANEL_WIDTH - timeWidth - 12, y + 7, COLOR_TEXT, false);

        String battery = Component.translatable("screen.riftborne.codex.energy", snapshot.battery()).getString();
        int batteryWidth = font.width(battery);
        graphics.drawString(font, battery, left + PANEL_WIDTH - timeWidth - batteryWidth - 25, y + 7,
                batteryColor(), false);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        float scale = interfaceScale();
        mouseX /= scale;
        mouseY /= scale;
        int left = panelLeft();
        int top = panelTop();
        int statusY = top + PANEL_HEIGHT - STATUS_HEIGHT;
        if (inside(mouseX, mouseY, left + 6, statusY + 3, 34, 16)) {
            PacketDistributor.sendToServer(new CodexNetwork.TogglePowerPayload(!snapshot.powered()));
            return true;
        }
        if (!snapshot.powered()) {
            return true;
        }

        if (view == View.DESKTOP) {
            if (inside(mouseX, mouseY, left + 18, top + 30, 58, 72)) {
                view = View.CODEX;
                return true;
            }
            return true;
        }

        int windowX = left + 16;
        int windowY = top + 14;
        int windowWidth = PANEL_WIDTH - 32;
        if (inside(mouseX, mouseY, windowX + windowWidth - 21, windowY + 4, 16, 16)) {
            view = View.DESKTOP;
            return true;
        }

        Set<String> unlocked = new HashSet<>(CodexNetwork.split(snapshot.unlockedEntries()));
        int listX = windowX + 8;
        int rowY = windowY + 35;
        for (CodexEntry entry : CodexEntries.all()) {
            if (inside(mouseX, mouseY, listX + 4, rowY, 129, 19)) {
                if (unlocked.contains(entry.id())) {
                    selectedEntryId = entry.id();
                }
                return true;
            }
            rowY += 22;
        }
        return true;
    }

    private void drawTrimmed(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
        graphics.drawString(font, font.plainSubstrByWidth(text.getString(), maxWidth), x, y, color, false);
    }

    private static Component feedComponent(String encoded) {
        if (!CodexData.isTranslation(encoded)) {
            return Component.literal(encoded);
        }
        String[] parts = CodexData.translationParts(encoded);
        Object[] arguments = new Object[Math.max(0, parts.length - 1)];
        for (int index = 0; index < arguments.length; index++) {
            String argument = parts[index + 1];
            arguments[index] = CodexData.isTranslationArgument(argument)
                    ? Component.translatable(CodexData.translationArgumentKey(argument))
                    : argument;
        }
        return Component.translatable(parts[0], arguments);
    }

    private String minecraftTime() {
        if (minecraft == null || minecraft.level == null) {
            return "--:--";
        }
        long dayTicks = Math.floorMod(minecraft.level.getDayTime(), 24000L);
        int totalMinutes = (int) (((dayTicks + 6000L) * 1440L / 24000L) % 1440L);
        return String.format("%02d:%02d", totalMinutes / 60, totalMinutes % 60);
    }

    private int panelLeft() {
        return (virtualWidth() - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return (virtualHeight() - PANEL_HEIGHT) / 2;
    }

    private float interfaceScale() {
        float widthScale = Math.max(1, width - SCREEN_MARGIN) / (float) PANEL_WIDTH;
        float heightScale = Math.max(1, height - SCREEN_MARGIN) / (float) PANEL_HEIGHT;
        return Math.min(1.0F, Math.min(widthScale, heightScale));
    }

    private int virtualWidth() {
        return (int) (width / interfaceScale());
    }

    private int virtualHeight() {
        return (int) (height / interfaceScale());
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int batteryColor() {
        return snapshot.battery() > 50 ? 0xFF5AE68A : snapshot.battery() > 20 ? 0xFFE6C75A : 0xFFE65A69;
    }

    private int stageColor() {
        return switch (snapshot.metaWearStage()) {
            case "STABLE" -> 0xFF5AE68A;
            case "STRAIN" -> 0xFFE6D65A;
            case "DISTORTION" -> 0xFFE69C5A;
            case "REJECTION" -> 0xFFE65A69;
            default -> 0xFFFF3658;
        };
    }

    private String stageLabel() {
        return Component.translatable("screen.riftborne.codex.stage."
                + snapshot.metaWearStage().toLowerCase(Locale.ROOT)).getString();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum View {
        DESKTOP,
        CODEX
    }
}
