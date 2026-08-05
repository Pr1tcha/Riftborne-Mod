package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.codex.CodexEntries;
import com.pr1tcha.riftborne.codex.data.CodexData;
import com.pr1tcha.riftborne.codex.data.CodexEntry;
import com.pr1tcha.riftborne.codex.network.CodexNetwork;
import com.pr1tcha.riftborne.rna.power.Primitive;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CodexLaptopScreen extends Screen {
    private static final int PANEL_WIDTH = 600;
    private static final int PANEL_HEIGHT = 340;
    private static final int STATUS_HEIGHT = 24;
    private static final int SCREEN_MARGIN = 12;
    private static final int ICON_HIT_WIDTH = 58;
    private static final int ICON_HIT_HEIGHT = 72;
    private static final int ICON_STEP_X = 76;
    private static final int COLOR_SHELL = 0xF507090D;
    private static final int COLOR_DESKTOP = 0xFF0A1019;
    private static final int COLOR_WINDOW = 0xF70B0F16;
    private static final int COLOR_WINDOW_RAISED = 0xFF121822;
    private static final int COLOR_BORDER = 0xFF303846;
    private static final int COLOR_BORDER_SOFT = 0x663D4757;
    private static final int COLOR_ACCENT = 0xFF8C74F7;
    private static final int COLOR_ACCENT_BRIGHT = 0xFFB9ABFF;
    private static final int COLOR_CYAN = 0xFF55D8DC;
    private static final int COLOR_GREEN = 0xFF68D995;
    private static final int COLOR_AMBER = 0xFFE5B768;
    private static final int COLOR_DANGER = 0xFFE56D7C;
    private static final int COLOR_TEXT = 0xFFF0F3F7;
    private static final int COLOR_MUTED = 0xFF89919F;
    private static final int COLOR_FAINT = 0xFF596171;

    private CodexNetwork.SnapshotPayload snapshot;
    private View view = View.DESKTOP;
    private final CodexInfobasePanel infobase = new CodexInfobasePanel();
    private int selectedDrive;
    private int explorerScroll;
    private final List<DesktopItem> desktopItems = new ArrayList<>();
    private DesktopItem openedFolder;
    private DesktopItem draggingItem;
    private boolean desktopItemsInitialized;
    private int draggingStartX;
    private int draggingStartY;
    private int draggingOffsetX;
    private int draggingOffsetY;
    private boolean draggingMoved;
    private boolean renamingFolder;
    private String folderRenameBuffer = "";
    private boolean systemMenuOpen;
    private boolean confirmingFolderDissolve;

    public CodexLaptopScreen(CodexNetwork.SnapshotPayload snapshot) {
        super(Component.translatable("screen.riftborne.codex_laptop"));
        this.snapshot = snapshot;
        infobase.update(snapshot.infobaseData());
    }

    public void updateSnapshot(CodexNetwork.SnapshotPayload snapshot) {
        this.snapshot = snapshot;
        infobase.update(snapshot.infobaseData());
        if ((selectedDrive == 1 && !snapshot.firstFlashInserted())
                || (selectedDrive == 2 && !snapshot.secondFlashInserted())) {
            selectedDrive = 0;
        }
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
        setupDesktopItems(left, top);

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        drawLaptopShell(graphics, left, top);
        if (snapshot.powered()) {
            drawDesktopWallpaper(graphics, left, top);
            switch (view) {
                case DESKTOP -> renderDesktop(graphics, left, top, scaledMouseX, scaledMouseY);
                case CODEX, SYNC, DECRYPTOR -> renderCodexWindow(graphics, left, top, scaledMouseX, scaledMouseY);
                case EXPLORER -> renderExplorerWindow(graphics, left, top, scaledMouseX, scaledMouseY);
                case DIAGNOSTICS -> renderDiagnosticsWindow(graphics, left, top, scaledMouseX, scaledMouseY);
                case PHYSICAL -> renderPhysicalWindow(graphics, left, top, scaledMouseX, scaledMouseY);
                case FOLDER -> renderFolderWindow(graphics, left, top, scaledMouseX, scaledMouseY);
            }
            if (systemMenuOpen) {
                renderSystemMenu(graphics, left, top, scaledMouseX, scaledMouseY);
            }
        } else {
            renderStandby(graphics, left, top);
        }
        renderStatusBar(graphics, left, top, scaledMouseX, scaledMouseY);
        graphics.pose().popPose();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawLaptopShell(GuiGraphics graphics, int left, int top) {
        graphics.fill(left - 6, top - 5, left + PANEL_WIDTH + 6, top + PANEL_HEIGHT + 7, 0x66000000);
        graphics.fill(left - 4, top - 4, left + PANEL_WIDTH + 4, top + PANEL_HEIGHT + 4, 0xEE030509);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, COLOR_SHELL);
        graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF303744);
        graphics.renderOutline(left + 2, top + 2, PANEL_WIDTH - 4, PANEL_HEIGHT - 4, 0xFF171C25);
        graphics.fill(left + PANEL_WIDTH / 2 - 9, top + 1, left + PANEL_WIDTH / 2 + 9, top + 3, 0xFF141922);
        graphics.fill(left + PANEL_WIDTH / 2, top + 1, left + PANEL_WIDTH / 2 + 2, top + 3, 0xFF596171);
        drawCornerMark(graphics, left + 5, top + 5, false);
        drawCornerMark(graphics, left + PANEL_WIDTH - 6, top + 5, true);
    }

    private void drawDesktopWallpaper(GuiGraphics graphics, int left, int top) {
        int bottom = top + PANEL_HEIGHT - STATUS_HEIGHT;
        graphics.fill(left + 3, top + 3, left + PANEL_WIDTH - 3, bottom, COLOR_DESKTOP);
        graphics.fill(left + 3, top + 3, left + PANEL_WIDTH - 3, top + 52, 0xFF101621);
        graphics.fill(left + 3, top + 52, left + PANEL_WIDTH - 3, bottom, 0xFF090E16);

        for (int x = left + 23; x < left + PANEL_WIDTH - 3; x += 32) {
            graphics.vLine(x, top + 53, bottom - 1, 0x151F2A38);
        }
        for (int y = top + 69; y < bottom; y += 24) {
            graphics.hLine(left + 3, left + PANEL_WIDTH - 4, y, 0x151F2A38);
        }

        int riftCenter = left + PANEL_WIDTH - 126;
        for (int y = top + 4; y < bottom; y += 3) {
            int phase = ((y - top) / 18) % 4;
            int shift = switch (phase) {
                case 0 -> -8;
                case 1 -> 3;
                case 2 -> -3;
                default -> 9;
            };
            int x = riftCenter + shift + (y - top) / 11;
            graphics.fill(x - 10, y, x + 12, y + 3, 0x0D8C74F7);
            graphics.fill(x - 3, y, x + 5, y + 3, 0x338C74F7);
            graphics.fill(x, y, x + 2, y + 3, 0xCCB9ABFF);
            if ((y / 3) % 5 == 0) {
                graphics.fill(x + 2, y, x + 3, y + 3, 0xCC55D8DC);
            }
        }

        graphics.fill(left + 13, top + 12, left + 29, top + 28, 0xFF745FD8);
        graphics.fill(left + 18, top + 15, left + 21, top + 25, COLOR_TEXT);
        graphics.fill(left + 21, top + 18, left + 25, top + 21, COLOR_TEXT);
        graphics.drawString(font, "RIFT OS", left + 36, top + 12, COLOR_TEXT, false);
        graphics.drawString(font, "CODEX WORKSPACE", left + 36, top + 25, COLOR_FAINT, false);
        graphics.fill(left + PANEL_WIDTH - 126, top + 15, left + PANEL_WIDTH - 120, top + 21, COLOR_GREEN);
        graphics.drawString(font, "SYSTEM ONLINE", left + PANEL_WIDTH - 113, top + 14, COLOR_MUTED, false);
        graphics.hLine(left + 13, left + PANEL_WIDTH - 14, top + 43, 0x333D4757);
    }

    private void renderDesktop(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int cardX = left + PANEL_WIDTH - 174;
        int cardY = top + PANEL_HEIGHT - STATUS_HEIGHT - 64;
        graphics.fill(cardX, cardY, left + PANEL_WIDTH - 16, cardY + 42, 0xB20D131C);
        graphics.renderOutline(cardX, cardY, 158, 42, COLOR_BORDER_SOFT);
        graphics.fill(cardX, cardY, cardX + 3, cardY + 42, COLOR_CYAN);
        graphics.drawString(font, "RIFTBORNE NODE", cardX + 10, cardY + 8, COLOR_TEXT, false);
        graphics.drawString(font, snapshot.hasRna() ? stageLabel() : "RNA OFFLINE", cardX + 10, cardY + 23,
                snapshot.hasRna() ? stageColor() : COLOR_DANGER, false);
        graphics.drawString(font, snapshot.battery() + "%", cardX + 129, cardY + 23, batteryColor(), false);

        for (DesktopItem item : desktopItems) {
            if (item == draggingItem && draggingMoved) {
                continue;
            }
            drawDesktopItem(graphics, item, item.x, item.y, mouseX, mouseY, false);
        }

        if (draggingItem != null && draggingMoved) {
            DesktopItem target = desktopItemAt(mouseX, mouseY, draggingItem);
            if (target != null && (target.isFolder() || !draggingItem.isFolder())) {
                graphics.renderOutline(target.x - 8, target.y - 7, ICON_HIT_WIDTH + 4, ICON_HIT_HEIGHT + 4,
                        0xCC55E1D5);
            }
            drawDesktopItem(graphics, draggingItem, draggingItem.x, draggingItem.y, mouseX, mouseY, true);
        }
    }

    private void drawCornerMark(GuiGraphics graphics, int x, int y, boolean mirrored) {
        int direction = mirrored ? -1 : 1;
        graphics.hLine(x, x + direction * 9, y, 0x554F5969);
        graphics.vLine(x, y, y + 8, 0x554F5969);
    }

    private void drawDesktopItem(
            GuiGraphics graphics,
            DesktopItem item,
            int x,
            int y,
            int mouseX,
            int mouseY,
            boolean ghost
    ) {
        boolean hovered = inside(mouseX, mouseY, x - 6, y - 5, ICON_HIT_WIDTH, ICON_HIT_HEIGHT);
        int alphaFill = ghost ? 0x664B426D : 0x55342F4C;
        if (hovered) {
            graphics.fill(x - 6, y - 5, x + 52, y + 67, alphaFill);
            graphics.renderOutline(x - 6, y - 5, ICON_HIT_WIDTH, ICON_HIT_HEIGHT, 0xAA8C74F7);
        }
        if (item.isFolder()) {
            drawFolderShortcutIcon(graphics, x, y);
        } else if (item.view == View.CODEX) {
            drawCodexIcon(graphics, x, y);
        } else if (item.view == View.EXPLORER) {
            drawExplorerIcon(graphics, x, y);
        } else {
            int accent = appColor(item.view);
            drawIconTile(graphics, x, y + 4, accent);
            graphics.drawCenteredString(font, item.glyph, x + 20, y + 20, accent);
        }
        int count = itemBadge(item);
        if (count > 0) {
            graphics.fill(x + 29, y, x + 43, y + 14, COLOR_DANGER);
            graphics.drawCenteredString(font, Integer.toString(count), x + 36, y + 3, COLOR_TEXT);
        }
        drawIconLabel(graphics, item.title(), x + 20, y + 51);
    }

    private void drawCodexIcon(GuiGraphics graphics, int x, int y) {
        drawIconTile(graphics, x, y + 4, COLOR_ACCENT);
        graphics.fill(x + 10, y + 13, x + 30, y + 16, COLOR_ACCENT_BRIGHT);
        graphics.fill(x + 10, y + 21, x + 26, y + 23, 0xFF7769BC);
        graphics.fill(x + 10, y + 28, x + 29, y + 30, 0xFF7769BC);
        graphics.fill(x + 10, y + 35, x + 22, y + 37, 0xFF7769BC);
    }

    private void drawExplorerIcon(GuiGraphics graphics, int x, int y) {
        drawIconTile(graphics, x, y + 4, COLOR_CYAN);
        graphics.fill(x + 7, y + 16, x + 33, y + 35, 0xFF183840);
        graphics.fill(x + 9, y + 13, x + 21, y + 18, 0xFF28636C);
        graphics.renderOutline(x + 7, y + 16, 26, 19, COLOR_CYAN);
        graphics.fill(x + 12, y + 23, x + 28, y + 25, COLOR_CYAN);
    }

    private void drawFolderShortcutIcon(GuiGraphics graphics, int x, int y) {
        drawIconTile(graphics, x, y + 4, COLOR_AMBER);
        graphics.fill(x + 6, y + 17, x + 20, y + 22, 0xFF6A522A);
        graphics.fill(x + 6, y + 21, x + 34, y + 36, 0xFF4E4029);
        graphics.renderOutline(x + 6, y + 21, 28, 15, COLOR_AMBER);
        graphics.fill(x + 11, y + 27, x + 28, y + 29, COLOR_AMBER);
    }

    private void drawIconTile(GuiGraphics graphics, int x, int y, int accent) {
        graphics.fill(x + 2, y, x + 38, y + 40, 0xFF111722);
        graphics.fill(x, y + 2, x + 40, y + 38, 0xFF111722);
        graphics.renderOutline(x, y + 2, 40, 36, COLOR_BORDER);
        graphics.fill(x + 1, y + 3, x + 3, y + 37, accent);
        graphics.fill(x + 4, y + 4, x + 36, y + 6, 0x22111111 | (accent & 0x00FFFFFF));
    }

    private int appColor(View appView) {
        return switch (appView) {
            case CODEX -> COLOR_ACCENT;
            case SYNC -> COLOR_CYAN;
            case DECRYPTOR -> COLOR_AMBER;
            case EXPLORER -> COLOR_CYAN;
            case DIAGNOSTICS -> COLOR_GREEN;
            case PHYSICAL -> COLOR_AMBER;
            default -> COLOR_MUTED;
        };
    }

    private void drawIconLabel(GuiGraphics graphics, Component title, int centerX, int y) {
        List<FormattedCharSequence> lines = font.split(title, 58);
        int lineCount = Math.min(2, lines.size());
        for (int index = 0; index < lineCount; index++) {
            graphics.drawCenteredString(font, lines.get(index), centerX, y + index * 11, COLOR_TEXT);
        }
    }

    private void drawWindowFrame(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            Component title,
            int mouseX,
            int mouseY
    ) {
        graphics.fill(x + 4, y + 5, x + width + 5, y + height + 6, 0x66000000);
        graphics.fill(x, y, x + width, y + height, COLOR_WINDOW);
        graphics.renderOutline(x, y, width, height, COLOR_BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 25, COLOR_WINDOW_RAISED);
        graphics.hLine(x + 1, x + width - 2, y + 25, COLOR_BORDER_SOFT);
        graphics.fill(x + 8, y + 7, x + 20, y + 19, 0xFF2A2342);
        graphics.fill(x + 12, y + 9, x + 15, y + 17, COLOR_ACCENT_BRIGHT);
        graphics.fill(x + 15, y + 11, x + 18, y + 14, COLOR_ACCENT_BRIGHT);
        graphics.drawString(font, title, x + 27, y + 8, COLOR_TEXT, false);
        int closeX = x + width - 21;
        boolean closeHovered = inside(mouseX, mouseY, closeX, y + 4, 16, 16);
        graphics.fill(closeX, y + 4, closeX + 16, y + 20, closeHovered ? COLOR_DANGER : 0xFF202631);
        graphics.drawCenteredString(font, "x", closeX + 8, y + 8, COLOR_TEXT);
    }

    private void renderExplorerWindow(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int windowX = left + 16;
        int windowY = top + 14;
        int windowWidth = PANEL_WIDTH - 32;
        int windowHeight = PANEL_HEIGHT - STATUS_HEIGHT - 26;
        drawWindowFrame(graphics, windowX, windowY, windowWidth, windowHeight,
                Component.translatable("screen.riftborne.codex.explorer_title"), mouseX, mouseY);

        int drivesX = windowX + 8;
        int drivesY = windowY + 31;
        int drivesWidth = 152;
        int contentX = windowX + 168;
        int contentWidth = windowWidth - 176;
        graphics.fill(drivesX, drivesY, drivesX + drivesWidth, windowY + windowHeight - 8, 0x88050A0F);
        graphics.fill(contentX, drivesY, contentX + contentWidth, windowY + windowHeight - 8, 0x88050A0F);

        drawDriveRow(graphics, mouseX, mouseY, drivesX + 4, drivesY + 5, drivesWidth - 8, 0,
                Component.translatable("screen.riftborne.codex.drive_internal"), true);
        drawDriveRow(graphics, mouseX, mouseY, drivesX + 4, drivesY + 29, drivesWidth - 8, 1,
                Component.translatable("screen.riftborne.codex.drive_flash", 1), snapshot.firstFlashInserted());
        drawDriveRow(graphics, mouseX, mouseY, drivesX + 4, drivesY + 53, drivesWidth - 8, 2,
                Component.translatable("screen.riftborne.codex.drive_flash", 2), snapshot.secondFlashInserted());

        Set<String> entries = switch (selectedDrive) {
            case 1 -> new HashSet<>(CodexNetwork.split(snapshot.firstFlashEntries()));
            case 2 -> new HashSet<>(CodexNetwork.split(snapshot.secondFlashEntries()));
            default -> new HashSet<>(CodexNetwork.split(snapshot.unlockedEntries()));
        };
        int visibleRows = Math.max(1, (windowY + windowHeight - 8 - (drivesY + 44)) / 22);
        explorerScroll = Math.clamp(explorerScroll, 0, Math.max(0, explorerEntryCount(entries) - visibleRows));
        Component heading = selectedDrive == 0
                ? Component.translatable("screen.riftborne.codex.internal_records")
                : Component.translatable("screen.riftborne.codex.flash_records");
        graphics.drawString(font, heading, contentX + 9, drivesY + 9, COLOR_ACCENT, false);
        graphics.drawString(font, Component.translatable(selectedDrive == 0
                        ? "screen.riftborne.codex.internal_hint"
                        : "screen.riftborne.codex.flash_hint"),
                contentX + 9, drivesY + 24, COLOR_MUTED, false);

        int rowIndex = 0;
        int rowY = drivesY + 44 - explorerScroll * 22;
        int contentBottom = windowY + windowHeight - 8;
        Set<String> internal = new HashSet<>(CodexNetwork.split(snapshot.unlockedEntries()));
        for (CodexEntry entry : CodexEntries.all()) {
            if (!entries.contains(entry.id())) {
                continue;
            }
            if (rowIndex++ < explorerScroll) {
                rowY += 22;
                continue;
            }
            if (rowY + 20 > contentBottom) {
                break;
            }
            boolean hovered = inside(mouseX, mouseY, contentX + 6, rowY, contentWidth - 12, 20);
            if (hovered) {
                graphics.fill(contentX + 6, rowY, contentX + contentWidth - 6, rowY + 20, 0x44333A4A);
            }
            Component status = selectedDrive == 0 || internal.contains(entry.id())
                    ? Component.translatable("screen.riftborne.codex.file_saved")
                    : Component.translatable("screen.riftborne.codex.file_copy");
            int statusWidth = font.width(status);
            graphics.drawString(font, fit(Component.translatable(entry.titleKey()).getString(),
                            contentWidth - statusWidth - 30), contentX + 11, rowY + 6,
                    COLOR_TEXT, false);
            graphics.drawString(font, status, contentX + contentWidth - statusWidth - 11, rowY + 6,
                    selectedDrive == 0 || internal.contains(entry.id()) ? COLOR_MUTED : COLOR_ACCENT, false);
            rowY += 22;
        }

        int totalRows = explorerEntryCount(entries);
        if (totalRows > visibleRows) {
            int trackX = contentX + contentWidth - 6;
            int trackTop = drivesY + 44;
            int trackHeight = contentBottom - trackTop;
            int knobHeight = Math.max(14, trackHeight * visibleRows / totalRows);
            int knobY = trackTop + (trackHeight - knobHeight) * explorerScroll / Math.max(1, totalRows - visibleRows);
            graphics.fill(trackX, trackTop, trackX + 2, contentBottom, 0x66304C4A);
            graphics.fill(trackX - 1, knobY, trackX + 3, knobY + knobHeight, COLOR_ACCENT);
        }
    }

    private void renderDiagnosticsWindow(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int windowX = left + 16;
        int windowY = top + 14;
        int windowWidth = PANEL_WIDTH - 32;
        int windowHeight = PANEL_HEIGHT - STATUS_HEIGHT - 26;
        drawWindowFrame(graphics, windowX, windowY, windowWidth, windowHeight,
                Component.translatable("screen.riftborne.codex.synapsis_title"), mouseX, mouseY);

        if (!snapshot.diagnosticAvailable()) {
            graphics.drawCenteredString(font, Component.translatable("screen.riftborne.codex.synapsis_waiting"),
                    windowX + windowWidth / 2, windowY + 112, COLOR_ACCENT);
            graphics.drawCenteredString(font, Component.translatable("screen.riftborne.codex.synapsis_hint"),
                    windowX + windowWidth / 2, windowY + 132, COLOR_MUTED);
            return;
        }

        int cardX = windowX + 12;
        int cardY = windowY + 34;
        int cardWidth = 174;
        graphics.fill(cardX, cardY, cardX + cardWidth, windowY + windowHeight - 12, 0x88050A0F);
        graphics.renderOutline(cardX, cardY, cardWidth, windowHeight - 46, 0x4438A9A4);
        int cardTextWidth = cardWidth - 20;
        drawTrimmed(graphics, Component.translatable("screen.riftborne.codex.synapsis_subject"),
                cardX + 10, cardY + 10, cardTextWidth, COLOR_MUTED);
        graphics.drawString(font, fit(snapshot.diagnosticSubjectName(), cardTextWidth),
                cardX + 10, cardY + 25, COLOR_TEXT, false);

        BlockPos capsulePos = BlockPos.of(snapshot.diagnosticCapsulePos());
        drawTrimmed(graphics, Component.translatable("screen.riftborne.codex.synapsis_source"),
                cardX + 10, cardY + 48, cardTextWidth, COLOR_MUTED);
        graphics.drawString(font, fit(capsulePos.toShortString(), cardTextWidth),
                cardX + 10, cardY + 63, COLOR_TEXT, false);
        drawTrimmed(graphics, Component.translatable("screen.riftborne.codex.synapsis_path"),
                cardX + 10, cardY + 86, cardTextWidth, COLOR_MUTED);
        drawTrimmed(graphics, Component.translatable("rna.riftborne.formation_path."
                        + snapshot.diagnosticFormationPath().toLowerCase(Locale.ROOT)),
                cardX + 10, cardY + 101, cardTextWidth, COLOR_TEXT);

        // The card is laid out with a running cursor: the technique notice wraps to an unknown
        // number of lines, so a fixed offset for the block below it either overlaps or overflows.
        int cardBottom = cardY + windowHeight - 46;
        int cursorY = cardY + 130;
        if (!snapshot.diagnosticNotice().isBlank()) {
            drawTrimmed(graphics, Component.translatable("screen.riftborne.codex.technique"),
                    cardX + 10, cursorY, cardTextWidth, COLOR_MUTED);
            cursorY += 15;
            for (var line : font.split(Component.translatable(snapshot.diagnosticNotice()), cardTextWidth)) {
                graphics.drawString(font, line, cardX + 10, cursorY, COLOR_ACCENT, false);
                cursorY += 11;
            }
            cursorY += 8;
        }
        // Never let the summary hang below the card, however long the notice ran.
        int progressionY = Math.min(cursorY, cardBottom - PROGRESSION_HEIGHT - 6);
        renderProgressionSummary(graphics, cardX + 10, progressionY, cardTextWidth);

        int contentX = cardX + cardWidth + 12;
        int contentWidth = windowX + windowWidth - 12 - contentX;
        if (!snapshot.diagnosticHasRna()) {
            graphics.fill(contentX, cardY, contentX + contentWidth, cardY + 56, 0x661C0B12);
            graphics.renderOutline(contentX, cardY, contentWidth, 56, 0xFF9A3543);
            drawTrimmed(graphics, Component.translatable("screen.riftborne.codex.synapsis_no_rna"),
                    contentX + 12, cardY + 20, contentWidth - 24, 0xFFE65A69);
            return;
        }

        int barY = cardY + 4;
        drawDiagnosticBar(graphics, contentX, barY,
                "rna.riftborne.stat.node_density", snapshot.diagnosticNodeDensity(), contentWidth);
        drawDiagnosticBar(graphics, contentX, barY + 38,
                "rna.riftborne.stat.connectivity", snapshot.diagnosticConnectivity(), contentWidth);
        drawDiagnosticBar(graphics, contentX, barY + 76,
                "rna.riftborne.stat.throughput", snapshot.diagnosticThroughput(), contentWidth);
        drawDiagnosticBar(graphics, contentX, barY + 114,
                "rna.riftborne.stat.overload_resistance", snapshot.diagnosticOverloadResistance(), contentWidth);

        int wearY = barY + 158;
        drawLabelValue(graphics, Component.translatable("codex.riftborne.entry.meta_wear.title"),
                snapshot.diagnosticMetaWear() + "%", contentX, wearY, contentWidth,
                COLOR_TEXT, diagnosticStageColor());
        graphics.fill(contentX, wearY + 14, contentX + contentWidth, wearY + 23, 0xFF17252B);
        graphics.fill(contentX, wearY + 14,
                contentX + Math.round(contentWidth * snapshot.diagnosticMetaWear() / 100.0F),
                wearY + 23, diagnosticStageColor());
        graphics.drawString(font, Component.translatable("rna.riftborne.meta_wear_stage."
                        + snapshot.diagnosticMetaWearStage().toLowerCase(Locale.ROOT)),
                contentX, wearY + 30, diagnosticStageColor(), false);
    }

    private void renderPhysicalWindow(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int windowX = left + 16;
        int windowY = top + 14;
        int windowWidth = PANEL_WIDTH - 32;
        int windowHeight = PANEL_HEIGHT - STATUS_HEIGHT - 26;
        drawWindowFrame(graphics, windowX, windowY, windowWidth, windowHeight,
                Component.translatable("screen.riftborne.codex.physical_title"), mouseX, mouseY);

        int summaryY = windowY + 34;
        int summaryWidth = (windowWidth - 36) / 2;
        drawPhysicalSummary(graphics, windowX + 12, summaryY, summaryWidth,
                Component.translatable("screen.riftborne.codex.physical_overall"),
                snapshot.physicalOverallForm(), 100.0F, COLOR_CYAN);
        drawPhysicalSummary(graphics, windowX + 24 + summaryWidth, summaryY, summaryWidth,
                Component.translatable("screen.riftborne.codex.physical_overload"),
                snapshot.physicalOverloadCapacity(), 100.0F, COLOR_ACCENT);

        List<PhysicalSnapshot> stats = physicalSnapshots();
        int cardWidth = (windowWidth - 36) / 2;
        int cardHeight = 82;
        int startY = summaryY + 66;
        for (int index = 0; index < stats.size(); index++) {
            PhysicalSnapshot stat = stats.get(index);
            int cardX = windowX + 12 + index % 2 * (cardWidth + 12);
            int cardY = startY + index / 2 * (cardHeight + 10);
            drawPhysicalCard(graphics, stat, cardX, cardY, cardWidth, cardHeight);
        }
    }

    private void drawPhysicalSummary(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            Component label,
            float value,
            float maximum,
            int color
    ) {
        graphics.fill(x, y, x + width, y + 54, 0x88050A0F);
        graphics.renderOutline(x, y, width, 54, COLOR_BORDER_SOFT);
        graphics.fill(x, y, x + 3, y + 54, color);
        drawTrimmed(graphics, label, x + 10, y + 8, width - 20, COLOR_MUTED);
        String formatted = oneDecimal(value);
        graphics.drawString(font, formatted, x + 10, y + 23, COLOR_TEXT, false);
        drawPhysicalBar(graphics, x + 10, y + 40, width - 20, value / maximum * 100.0F, color);
    }

    private void drawPhysicalCard(
            GuiGraphics graphics,
            PhysicalSnapshot stat,
            int x,
            int y,
            int width,
            int height
    ) {
        graphics.fill(x, y, x + width, y + height, 0x88050A0F);
        graphics.renderOutline(x, y, width, height, COLOR_BORDER_SOFT);
        drawTrimmed(graphics, Component.translatable("physical.riftborne." + stat.id()),
                x + 9, y + 7, width - 18, COLOR_TEXT);

        Component formLabel = Component.translatable("screen.riftborne.codex.physical_form", oneDecimal(stat.value()));
        drawTrimmed(graphics, formLabel, x + 9, y + 22, width - 18, COLOR_MUTED);
        drawPhysicalBar(graphics, x + 9, y + 34, width - 18, stat.value(), COLOR_CYAN);

        int activityColor = stat.activity() >= 100.0F
                ? COLOR_GREEN
                : stat.activity() >= 70.0F ? COLOR_CYAN : COLOR_AMBER;
        Component activityLabel = Component.translatable(
                "screen.riftborne.codex.physical_activity", Math.round(stat.activity()));
        drawTrimmed(graphics, activityLabel, x + 9, y + 46, width - 18, COLOR_MUTED);
        drawPhysicalBar(graphics, x + 9, y + 58, width - 18, stat.activity(), activityColor);

        // The status names which growth step this cycle's activity has actually reached.
        Component status;
        int statusColor;
        if (stat.activity() >= 100.0F) {
            status = Component.translatable("screen.riftborne.codex.physical_step_full");
            statusColor = COLOR_GREEN;
        } else if (stat.activity() >= 70.0F) {
            status = Component.translatable("screen.riftborne.codex.physical_step_high");
            statusColor = COLOR_CYAN;
        } else if (stat.activity() >= 40.0F) {
            status = Component.translatable("screen.riftborne.codex.physical_step_low");
            statusColor = COLOR_AMBER;
        } else {
            status = Component.translatable("screen.riftborne.codex.physical_step_none");
            statusColor = COLOR_FAINT;
        }
        drawTrimmed(graphics, status, x + 9, y + 70, width - 18, statusColor);
    }

    private void drawPhysicalBar(GuiGraphics graphics, int x, int y, int width, float value, int color) {
        float normalized = Math.max(0.0F, Math.min(100.0F, value)) / 100.0F;
        graphics.fill(x, y, x + width, y + 6, 0xFF17212B);
        graphics.fill(x, y, x + Math.round(width * normalized), y + 6, color);
    }

    private List<PhysicalSnapshot> physicalSnapshots() {
        List<PhysicalSnapshot> result = new ArrayList<>();
        for (String encoded : CodexNetwork.split(snapshot.physicalProfile())) {
            String[] fields = encoded.split(",", -1);
            if (fields.length < 3) {
                continue;
            }
            try {
                result.add(new PhysicalSnapshot(
                        fields[0],
                        Float.parseFloat(fields[1]),
                        Float.parseFloat(fields[2])
                ));
            } catch (NumberFormatException ignored) {
                // Ignore a malformed diagnostic row without breaking the entire Rift OS screen.
            }
        }
        return result;
    }

    private static String oneDecimal(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** Height the progression summary occupies, used to keep it inside the card. */
    private static final int PROGRESSION_HEIGHT = 68;

    private void renderProgressionSummary(GuiGraphics graphics, int x, int y, int width) {
        drawTrimmed(graphics, Component.translatable("screen.riftborne.codex.primitive_profile"),
                x, y, width, COLOR_MUTED);
        List<String> primitives = CodexNetwork.split(snapshot.techniqueProgress());
        if (primitives.isEmpty()) {
            drawTrimmed(graphics, Component.translatable("screen.riftborne.codex.primitive_profile.empty"),
                    x, y + 11, width, COLOR_MUTED);
        } else {
            graphics.drawString(font, fit(primitiveLine(primitives), width),
                    x, y + 11, COLOR_ACCENT, false);
        }

        drawTrimmed(graphics, Component.translatable("screen.riftborne.codex.axis_practice"),
                x, y + 24, width, COLOR_MUTED);
        List<String> practice = CodexNetwork.split(snapshot.techniqueReadiness());
        Component practiceLine = practice.isEmpty()
                ? Component.translatable("screen.riftborne.codex.axis_practice.empty")
                : Component.literal(practiceLine(practice));
        graphics.drawString(font, fit(practiceLine.getString(), width),
                x, y + 35, practice.isEmpty() ? COLOR_MUTED : COLOR_CYAN, false);

        drawTrimmed(graphics, Component.translatable("screen.riftborne.codex.facet"),
                x, y + 48, width, COLOR_MUTED);
        String facet = snapshot.aspectResonance();
        Component facetLine = facet == null || facet.isBlank()
                ? Component.translatable("screen.riftborne.codex.facet.empty")
                : Component.translatable("screen.riftborne.codex.facet.value",
                        Component.translatable("facet.riftborne." + facet));
        graphics.drawString(font, fit(facetLine.getString(), width),
                x, y + 59, facet == null || facet.isBlank() ? COLOR_MUTED : COLOR_ACCENT, false);
    }

    /** Top primitives by execution level, e.g. "Shift 3 · Reading 2". */
    private String primitiveLine(List<String> encoded) {
        StringBuilder builder = new StringBuilder();
        int shown = 0;
        for (String entry : encoded) {
            String[] fields = entry.split(",", -1);
            if (fields.length < 2) {
                continue;
            }
            Primitive primitive = Primitive.fromId(fields[0]);
            if (primitive == null) {
                continue;
            }
            if (shown > 0) {
                builder.append(" · ");
            }
            builder.append(Component.translatable(primitive.translationKey()).getString())
                    .append(' ')
                    .append(fields[1]);
            if (++shown >= 3) {
                break;
            }
        }
        return builder.toString();
    }

    /** Most practised Δ-families, e.g. "dG 24 · dS 11". */
    private String practiceLine(List<String> encoded) {
        StringBuilder builder = new StringBuilder();
        int shown = 0;
        for (String entry : encoded) {
            String[] fields = entry.split(",", -1);
            if (fields.length < 2) {
                continue;
            }
            if (shown > 0) {
                builder.append(" · ");
            }
            builder.append(fields[0].toUpperCase(Locale.ROOT).replace("DST", "dSt").replace("D", "d"))
                    .append(' ')
                    .append(fields[1]);
            if (++shown >= 4) {
                break;
            }
        }
        return builder.toString();
    }

    private void renderFolderWindow(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        if (openedFolder == null) {
            view = View.DESKTOP;
            return;
        }

        int windowX = left + 44;
        int windowY = top + 31;
        int windowWidth = PANEL_WIDTH - 88;
        int windowHeight = PANEL_HEIGHT - STATUS_HEIGHT - 60;
        drawWindowFrame(graphics, windowX, windowY, windowWidth, windowHeight,
                Component.translatable("screen.riftborne.codex.folder_title", openedFolder.title()), mouseX, mouseY);

        int renameX = windowX + windowWidth - 123;
        boolean renameHovered = inside(mouseX, mouseY, renameX, windowY + 4, 96, 17);
        graphics.fill(renameX, windowY + 4, renameX + 96, windowY + 21,
                renameHovered || renamingFolder ? 0xFF2A263A : 0xFF202631);
        graphics.drawCenteredString(font, Component.translatable("screen.riftborne.codex.folder_rename"),
                renameX + 48, windowY + 8, renamingFolder ? COLOR_ACCENT : COLOR_TEXT);

        int dissolveX = renameX - 108;
        boolean dissolveHovered = inside(mouseX, mouseY, dissolveX, windowY + 4, 102, 17);
        graphics.fill(dissolveX, windowY + 4, dissolveX + 102, windowY + 21,
                confirmingFolderDissolve
                        ? (dissolveHovered ? 0xFF6E2D3A : 0xFF4B222C)
                        : (dissolveHovered ? 0xFF2A263A : 0xFF202631));
        graphics.drawCenteredString(font, Component.literal(confirmingFolderDissolve
                        ? "CONFIRM"
                        : "UNGROUP"),
                dissolveX + 51, windowY + 8, confirmingFolderDissolve ? COLOR_TEXT : COLOR_MUTED);

        if (renamingFolder) {
            int inputX = windowX + 11;
            int inputY = windowY + 33;
            graphics.fill(inputX, inputY, inputX + windowWidth - 22, inputY + 24, 0xFF050A0F);
            graphics.renderOutline(inputX, inputY, windowWidth - 22, 24, COLOR_ACCENT);
            graphics.drawString(font, folderRenameBuffer + ((Util.getMillis() / 350L) % 2 == 0 ? "_" : ""),
                    inputX + 7, inputY + 8, COLOR_TEXT, false);
            graphics.drawString(font, Component.translatable("screen.riftborne.codex.folder_rename_hint"),
                    inputX + 7, inputY + 31, COLOR_MUTED, false);
        }

        int itemAreaY = renamingFolder ? windowY + 76 : windowY + 43;
        if (openedFolder.children.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.riftborne.codex.folder_empty"),
                    windowX + windowWidth / 2, itemAreaY + 45, COLOR_MUTED);
            return;
        }

        for (int index = 0; index < openedFolder.children.size(); index++) {
            DesktopItem child = openedFolder.children.get(index);
            int x = windowX + 24 + index % 6 * ICON_STEP_X;
            int y = itemAreaY + index / 6 * 76;
            drawDesktopItem(graphics, child, x, y, mouseX, mouseY, false);
        }
    }

    private void drawDiagnosticBar(GuiGraphics graphics, int x, int y, String key, int value, int width) {
        drawLabelValue(graphics, Component.translatable(key), value + "%", x, y, width,
                COLOR_TEXT, COLOR_ACCENT);
        graphics.fill(x, y + 14, x + width, y + 23, 0xFF171D27);
        graphics.fill(x, y + 14, x + Math.round(width * value / 100.0F), y + 23, COLOR_ACCENT);
    }

    private void drawDriveRow(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int drive,
            Component name,
            boolean connected
    ) {
        boolean hovered = connected && inside(mouseX, mouseY, x, y, width, 20);
        if (selectedDrive == drive || hovered) {
            graphics.fill(x, y, x + width, y + 20, selectedDrive == drive ? 0x663E3560 : 0x44333A4A);
        }
        graphics.fill(x + 6, y + 6, x + 16, y + 14, connected ? COLOR_CYAN : 0xFF344145);
        graphics.drawString(font, name, x + 22, y + 6, connected ? COLOR_TEXT : COLOR_MUTED, false);
    }

    private void renderCodexWindow(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int windowX = left + 16;
        int windowY = top + 14;
        int windowWidth = PANEL_WIDTH - 32;
        int windowHeight = PANEL_HEIGHT - STATUS_HEIGHT - 26;

        String titleKey = switch (view) {
            case SYNC -> "screen.riftborne.codex.sync_title";
            case DECRYPTOR -> "screen.riftborne.codex.decryptor_title";
            default -> "screen.riftborne.codex.archive_title";
        };
        drawWindowFrame(graphics, windowX, windowY, windowWidth, windowHeight,
                Component.translatable(titleKey), mouseX, mouseY);

        if (view == View.CODEX) {
            infobase.render(
                    graphics,
                    font,
                    windowX + 8,
                    windowY + 31,
                    windowWidth - 16,
                    windowHeight - 39,
                    mouseX,
                    mouseY
            );
        } else {
            renderQueueWindow(graphics, windowX, windowY, windowWidth, windowHeight);
        }
    }

    private void renderQueueWindow(GuiGraphics graphics, int windowX, int windowY, int windowWidth, int windowHeight) {
        List<String> entries = CodexNetwork.split(view == View.SYNC ? snapshot.queuedEntries() : snapshot.damagedEntries());
        int x = windowX + 14;
        int y = windowY + 38;
        if (entries.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.riftborne.codex.queue_empty"), x, y, COLOR_MUTED, false);
            return;
        }
        for (String entryId : entries) {
            CodexEntry entry = CodexEntries.get(entryId);
            if (entry != null) {
                graphics.drawString(font, Component.translatable(entry.titleKey()), x, y, COLOR_TEXT, false);
                if (view == View.DECRYPTOR) {
                    graphics.fill(windowX + windowWidth - 112, y - 4, windowX + windowWidth - 16, y + 13, 0xFF28233A);
                    graphics.drawCenteredString(font, Component.translatable("screen.riftborne.codex.restore"),
                            windowX + windowWidth - 64, y, COLOR_ACCENT);
                }
                y += 22;
            }
        }
    }

    private void renderStandby(GuiGraphics graphics, int left, int top) {
        int bottom = top + PANEL_HEIGHT - STATUS_HEIGHT;
        graphics.fill(left + 3, top + 3, left + PANEL_WIDTH - 3, bottom, 0xFF030509);
        int centerX = left + PANEL_WIDTH / 2;
        int centerY = top + (PANEL_HEIGHT - STATUS_HEIGHT) / 2;
        graphics.fill(centerX - 18, centerY - 35, centerX + 18, centerY + 1, 0xFF15121F);
        graphics.renderOutline(centerX - 18, centerY - 35, 36, 36, 0xFF3C335F);
        graphics.fill(centerX - 5, centerY - 28, centerX, centerY - 6, COLOR_ACCENT);
        graphics.fill(centerX, centerY - 22, centerX + 7, centerY - 17, COLOR_ACCENT);
        graphics.drawCenteredString(font, Component.translatable("screen.riftborne.codex.standby"), centerX,
                centerY + 13, COLOR_MUTED);
        graphics.drawCenteredString(font, Component.translatable("screen.riftborne.codex.power_hint"), centerX,
                centerY + 29, COLOR_FAINT);
    }

    private void renderStatusBar(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int y = top + PANEL_HEIGHT - STATUS_HEIGHT;
        graphics.fill(left + 3, y, left + PANEL_WIDTH - 3, top + PANEL_HEIGHT - 3, 0xF5080B11);
        graphics.hLine(left + 3, left + PANEL_WIDTH - 4, y, COLOR_BORDER);
        int startX = left + 6;
        boolean startHovered = inside(mouseX, mouseY, startX, y + 3, 48, 18);
        graphics.fill(startX, y + 3, startX + 48, y + 21,
                systemMenuOpen ? 0xFF332A50 : startHovered ? 0xFF242B37 : 0xFF151A23);
        graphics.fill(startX + 5, y + 7, startX + 13, y + 15, snapshot.powered() ? COLOR_ACCENT : COLOR_FAINT);
        graphics.drawString(font, "RIFT", startX + 18, y + 8,
                startHovered ? COLOR_TEXT : COLOR_MUTED, false);

        if (snapshot.powered()) {
            View[] pinned = pinnedViews();
            for (int index = 0; index < pinned.length; index++) {
                int appX = left + 60 + index * 28;
                boolean hovered = inside(mouseX, mouseY, appX, y + 3, 24, 18);
                boolean active = view == pinned[index] || (view == View.FOLDER && pinned[index] == View.EXPLORER);
                if (hovered || active) {
                    graphics.fill(appX, y + 3, appX + 24, y + 21, active ? 0xFF28233A : 0xFF1B202A);
                }
                int color = appColor(pinned[index]);
                graphics.fill(appX + 8, y + 7, appX + 16, y + 15, color);
                if (active) {
                    graphics.fill(appX + 7, y + 19, appX + 17, y + 21, color);
                }
            }
        }

        String rnaStatus = snapshot.hasRna()
                ? Component.translatable("screen.riftborne.codex.rna_status", stageLabel(), snapshot.metaWear()).getString()
                : Component.translatable("screen.riftborne.codex.rna_offline").getString();
        graphics.drawString(font, rnaStatus, left + 232, y + 8,
                snapshot.hasRna() ? stageColor() : 0xFFB24C5B, false);

        String time = minecraftTime();
        int timeWidth = font.width(time);
        graphics.drawString(font, time, left + PANEL_WIDTH - timeWidth - 12, y + 8, COLOR_TEXT, false);

        String battery = Component.translatable("screen.riftborne.codex.energy", snapshot.battery()).getString();
        int batteryWidth = font.width(battery);
        graphics.drawString(font, battery, left + PANEL_WIDTH - timeWidth - batteryWidth - 25, y + 8,
                batteryColor(), false);

        renderTaskbarNotification(graphics, left, y, timeWidth, batteryWidth);
    }

    private void renderTaskbarNotification(GuiGraphics graphics, int left, int y, int timeWidth, int batteryWidth) {
        List<String> notifications = CodexNetwork.split(snapshot.notifications());
        List<String> recent = CodexNetwork.split(snapshot.recentData());
        if (notifications.isEmpty() && recent.isEmpty()) {
            return;
        }

        Component message = !notifications.isEmpty()
                ? feedComponent(notifications.get(0))
                : feedComponent(recent.get(0));
        int notificationX = left + 340;
        int notificationRight = left + PANEL_WIDTH - timeWidth - batteryWidth - 34;
        int notificationWidth = Math.max(118, notificationRight - notificationX);
        if (notificationWidth < 118) {
            return;
        }
        graphics.fill(notificationX, y + 3, notificationX + notificationWidth, y + 21, 0xFF111720);
        graphics.renderOutline(notificationX, y + 3, notificationWidth, 18, COLOR_BORDER_SOFT);
        graphics.fill(notificationX + 4, y + 7, notificationX + 7, y + 17, COLOR_CYAN);
        drawTrimmed(graphics, message, notificationX + 12, y + 8, notificationWidth - 18, COLOR_TEXT);
    }

    private void renderSystemMenu(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int menuX = left + 8;
        int menuY = top + PANEL_HEIGHT - STATUS_HEIGHT - 178;
        int menuWidth = 230;
        int menuHeight = 170;
        graphics.fill(menuX + 4, menuY + 5, menuX + menuWidth + 5, menuY + menuHeight + 6, 0x66000000);
        graphics.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xF70C1017);
        graphics.renderOutline(menuX, menuY, menuWidth, menuHeight, COLOR_BORDER);
        graphics.fill(menuX + 1, menuY + 1, menuX + menuWidth - 1, menuY + 35, COLOR_WINDOW_RAISED);
        graphics.fill(menuX + 10, menuY + 9, menuX + 28, menuY + 27, 0xFF745FD8);
        graphics.drawString(font, "RIFT OS", menuX + 36, menuY + 8, COLOR_TEXT, false);
        graphics.drawString(font, "SYSTEM MENU", menuX + 36, menuY + 20, COLOR_FAINT, false);

        View[] apps = pinnedViews();
        for (int index = 0; index < apps.length; index++) {
            int column = index % 2;
            int row = index / 2;
            int itemX = menuX + 9 + column * 107;
            int itemY = menuY + 43 + row * 30;
            boolean hovered = inside(mouseX, mouseY, itemX, itemY, 102, 25);
            graphics.fill(itemX, itemY, itemX + 102, itemY + 25, hovered ? 0xFF202632 : 0xFF131820);
            if (view == apps[index]) {
                graphics.fill(itemX, itemY, itemX + 3, itemY + 25, appColor(apps[index]));
            }
            graphics.fill(itemX + 9, itemY + 8, itemX + 18, itemY + 17, appColor(apps[index]));
            graphics.drawString(font, fit(menuTitle(apps[index]).getString(), 102 - 31),
                    itemX + 25, itemY + 8, hovered ? COLOR_TEXT : COLOR_MUTED, false);
        }

        int powerY = menuY + menuHeight - 29;
        graphics.hLine(menuX + 8, menuX + menuWidth - 9, powerY - 6, COLOR_BORDER_SOFT);
        boolean powerHovered = inside(mouseX, mouseY, menuX + 9, powerY, menuWidth - 18, 20);
        graphics.fill(menuX + 9, powerY, menuX + menuWidth - 9, powerY + 20,
                powerHovered ? 0xFF34202A : 0xFF151A22);
        graphics.fill(menuX + 17, powerY + 6, menuX + 25, powerY + 14, COLOR_DANGER);
        graphics.drawString(font, "POWER OFF", menuX + 34, powerY + 6,
                powerHovered ? COLOR_TEXT : COLOR_MUTED, false);
        graphics.drawString(font, "CODEX NODE / LOCAL", menuX + 124, powerY + 6, COLOR_FAINT, false);
    }

    private static View[] pinnedViews() {
        return new View[]{View.CODEX, View.EXPLORER, View.SYNC, View.DECRYPTOR, View.DIAGNOSTICS, View.PHYSICAL};
    }

    private Component menuTitle(View appView) {
        return switch (appView) {
            case CODEX -> Component.translatable("screen.riftborne.codex.infobase");
            case EXPLORER -> Component.translatable("screen.riftborne.codex.explorer_icon");
            case SYNC -> Component.translatable("screen.riftborne.codex.sync");
            case DECRYPTOR -> Component.translatable("screen.riftborne.codex.decryptor");
            case DIAGNOSTICS -> Component.translatable("screen.riftborne.codex.synapsis");
            case PHYSICAL -> Component.translatable("screen.riftborne.codex.physical");
            default -> Component.empty();
        };
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
        if (inside(mouseX, mouseY, left + 6, statusY + 3, 48, 18)) {
            if (snapshot.powered()) {
                systemMenuOpen = !systemMenuOpen;
            } else {
                PacketDistributor.sendToServer(new CodexNetwork.TogglePowerPayload(true, snapshot.laptopPos()));
            }
            return true;
        }
        if (!snapshot.powered()) {
            return true;
        }

        if (systemMenuOpen) {
            int menuX = left + 8;
            int menuY = top + PANEL_HEIGHT - STATUS_HEIGHT - 178;
            int menuWidth = 230;
            int menuHeight = 170;
            View[] menuApps = pinnedViews();
            for (int index = 0; index < menuApps.length; index++) {
                int itemX = menuX + 9 + index % 2 * 107;
                int itemY = menuY + 43 + index / 2 * 30;
                if (inside(mouseX, mouseY, itemX, itemY, 102, 25)) {
                    view = menuApps[index];
                    openedFolder = null;
                    renamingFolder = false;
                    systemMenuOpen = false;
                    return true;
                }
            }
            int powerY = menuY + menuHeight - 29;
            if (inside(mouseX, mouseY, menuX + 9, powerY, menuWidth - 18, 20)) {
                systemMenuOpen = false;
                PacketDistributor.sendToServer(new CodexNetwork.TogglePowerPayload(false, snapshot.laptopPos()));
                return true;
            }
            if (inside(mouseX, mouseY, menuX, menuY, menuWidth, menuHeight)) {
                return true;
            }
            systemMenuOpen = false;
        }

        View[] pinned = pinnedViews();
        for (int index = 0; index < pinned.length; index++) {
            int appX = left + 60 + index * 28;
            if (inside(mouseX, mouseY, appX, statusY + 3, 24, 18)) {
                view = pinned[index];
                openedFolder = null;
                renamingFolder = false;
                return true;
            }
        }

        if (view == View.DESKTOP) {
            DesktopItem item = desktopItemAt(mouseX, mouseY, null);
            if (item != null) {
                draggingItem = item;
                draggingStartX = (int) mouseX;
                draggingStartY = (int) mouseY;
                draggingOffsetX = (int) mouseX - item.x;
                draggingOffsetY = (int) mouseY - item.y;
                draggingMoved = false;
                return true;
            }
            return true;
        }

        int windowX = left + 16;
        int windowY = top + 14;
        int windowWidth = PANEL_WIDTH - 32;
        int windowHeight = PANEL_HEIGHT - STATUS_HEIGHT - 26;
        if (view == View.FOLDER) {
            if (openedFolder == null) {
                view = View.DESKTOP;
                return true;
            }
            int folderWindowX = left + 44;
            int folderWindowY = top + 31;
            int folderWindowWidth = PANEL_WIDTH - 88;
            if (inside(mouseX, mouseY, folderWindowX + folderWindowWidth - 21, folderWindowY + 4, 16, 16)) {
                renamingFolder = false;
                confirmingFolderDissolve = false;
                openedFolder = null;
                view = View.DESKTOP;
                return true;
            }
            int renameX = folderWindowX + folderWindowWidth - 123;
            int dissolveX = renameX - 108;
            if (inside(mouseX, mouseY, dissolveX, folderWindowY + 4, 102, 17)) {
                if (confirmingFolderDissolve) {
                    dissolveOpenedFolder();
                } else {
                    confirmingFolderDissolve = true;
                    renamingFolder = false;
                }
                return true;
            }
            if (inside(mouseX, mouseY, renameX, folderWindowY + 4, 96, 17)) {
                confirmingFolderDissolve = false;
                renamingFolder = true;
                folderRenameBuffer = openedFolder.title().getString();
                return true;
            }
            DesktopItem child = folderChildAt(mouseX, mouseY, folderWindowX, folderWindowY);
            if (child != null) {
                confirmingFolderDissolve = false;
                openDesktopItem(child);
                return true;
            }
            confirmingFolderDissolve = false;
            return true;
        }

        if (inside(mouseX, mouseY, windowX + windowWidth - 21, windowY + 4, 16, 16)) {
            renamingFolder = false;
            openedFolder = null;
            view = View.DESKTOP;
            return true;
        }

        if (view == View.DECRYPTOR) {
            List<String> damaged = CodexNetwork.split(snapshot.damagedEntries());
            int rowY = windowY + 34;
            for (String entryId : damaged) {
                if (inside(mouseX, mouseY, windowX + windowWidth - 112, rowY, 96, 17)) {
                    PacketDistributor.sendToServer(new CodexNetwork.RestoreDamagedPayload(
                            snapshot.laptopPos(), entryId));
                    return true;
                }
                rowY += 22;
            }
            return true;
        }
        if (view == View.SYNC) {
            return true;
        }

        if (view == View.CODEX) {
            return infobase.mouseClicked(
                    mouseX, mouseY, button,
                    windowX + 8, windowY + 31, windowWidth - 16, windowHeight - 39
            );
        }

        if (view == View.PHYSICAL || view == View.DIAGNOSTICS) {
            return true;
        }

        if (view == View.EXPLORER) {
            int drivesX = windowX + 8;
            int drivesY = windowY + 31;
            if (inside(mouseX, mouseY, drivesX + 4, drivesY + 5, 144, 20)) {
                selectedDrive = 0;
                explorerScroll = 0;
                return true;
            }
            if (snapshot.firstFlashInserted()
                    && inside(mouseX, mouseY, drivesX + 4, drivesY + 29, 144, 20)) {
                selectedDrive = 1;
                explorerScroll = 0;
                return true;
            }
            if (snapshot.secondFlashInserted()
                    && inside(mouseX, mouseY, drivesX + 4, drivesY + 53, 144, 20)) {
                selectedDrive = 2;
                explorerScroll = 0;
                return true;
            }
            if (selectedDrive > 0) {
                Set<String> entries = new HashSet<>(CodexNetwork.split(
                        selectedDrive == 1 ? snapshot.firstFlashEntries() : snapshot.secondFlashEntries()));
                int contentX = windowX + 168;
                int contentWidth = windowWidth - 176;
                int rowY = drivesY + 44 - explorerScroll * 22;
                int contentBottom = windowY + (PANEL_HEIGHT - STATUS_HEIGHT - 26) - 8;
                for (CodexEntry entry : CodexEntries.all()) {
                    if (!entries.contains(entry.id())) {
                        continue;
                    }
                    if (rowY + 20 > contentBottom) {
                        break;
                    }
                    if (inside(mouseX, mouseY, contentX + 6, rowY, contentWidth - 12, 20)) {
                        PacketDistributor.sendToServer(new CodexNetwork.TransferEntryPayload(
                                snapshot.laptopPos(), selectedDrive - 1, entry.id()));
                        return true;
                    }
                    rowY += 22;
                }
            }
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingItem != null && view == View.DESKTOP) {
            float scale = interfaceScale();
            mouseX /= scale;
            mouseY /= scale;
            if (Math.abs(mouseX - draggingStartX) > 3 || Math.abs(mouseY - draggingStartY) > 3) {
                draggingMoved = true;
            }
            if (draggingMoved) {
                draggingItem.x = clampDesktopX((int) mouseX - draggingOffsetX);
                draggingItem.y = clampDesktopY((int) mouseY - draggingOffsetY);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingItem != null) {
            float scale = interfaceScale();
            mouseX /= scale;
            mouseY /= scale;
            DesktopItem released = draggingItem;
            draggingItem = null;
            if (draggingMoved) {
                handleDesktopDrop(released, mouseX, mouseY);
            } else {
                openDesktopItem(released);
            }
            draggingMoved = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (view == View.CODEX && infobase.keyPressed(keyCode, modifiers)) {
            return true;
        }
        if (systemMenuOpen && keyCode == 256) {
            systemMenuOpen = false;
            return true;
        }
        if (confirmingFolderDissolve && keyCode == 256) {
            confirmingFolderDissolve = false;
            return true;
        }
        if (renamingFolder && openedFolder != null) {
            if (keyCode == 257 || keyCode == 335) {
                String trimmed = folderRenameBuffer.trim();
                if (!trimmed.isEmpty()) {
                    openedFolder.customTitle = trimmed;
                    saveDesktopLayout();
                }
                renamingFolder = false;
                return true;
            }
            if (keyCode == 256) {
                renamingFolder = false;
                return true;
            }
            if (keyCode == 259 && !folderRenameBuffer.isEmpty()) {
                folderRenameBuffer = folderRenameBuffer.substring(0, folderRenameBuffer.length() - 1);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (view == View.CODEX && infobase.charTyped(codePoint)) {
            return true;
        }
        if (renamingFolder && openedFolder != null) {
            if (!Character.isISOControl(codePoint) && folderRenameBuffer.length() < 22) {
                folderRenameBuffer += codePoint;
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float scale = interfaceScale();
        mouseX /= scale;
        mouseY /= scale;
        if (view == View.CODEX) {
            int left = panelLeft();
            int top = panelTop();
            int windowX = left + 16;
            int windowY = top + 14;
            int windowWidth = PANEL_WIDTH - 32;
            int windowHeight = PANEL_HEIGHT - STATUS_HEIGHT - 26;
            if (infobase.mouseScrolled(
                    mouseX, mouseY, scrollY,
                    windowX + 8, windowY + 31, windowWidth - 16, windowHeight - 39
            )) {
                return true;
            }
        }
        if (view == View.EXPLORER) {
            int left = panelLeft();
            int top = panelTop();
            int windowX = left + 16;
            int windowY = top + 14;
            int windowWidth = PANEL_WIDTH - 32;
            int windowHeight = PANEL_HEIGHT - STATUS_HEIGHT - 26;
            int drivesY = windowY + 31;
            int contentX = windowX + 168;
            int contentWidth = windowWidth - 176;
            if (inside(mouseX, mouseY, contentX, drivesY, contentWidth, windowHeight - 39)) {
                Set<String> entries = switch (selectedDrive) {
                    case 1 -> new HashSet<>(CodexNetwork.split(snapshot.firstFlashEntries()));
                    case 2 -> new HashSet<>(CodexNetwork.split(snapshot.secondFlashEntries()));
                    default -> new HashSet<>(CodexNetwork.split(snapshot.unlockedEntries()));
                };
                int visibleRows = Math.max(1, (windowY + windowHeight - 8 - (drivesY + 44)) / 22);
                explorerScroll = Math.clamp(
                        explorerScroll - (int) Math.signum(scrollY),
                        0,
                        Math.max(0, explorerEntryCount(entries) - visibleRows)
                );
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void setupDesktopItems(int left, int top) {
        if (desktopItemsInitialized) {
            return;
        }
        desktopItemsInitialized = true;
        if (loadDesktopLayout(snapshot.desktopLayout(), left, top)) {
            return;
        }
        addDefaultDesktopItems(left, top);
    }

    private void addDefaultDesktopItems(int left, int top) {
        int iconY = top + 62;
        int iconX = left + 24;
        desktopItems.add(appForId("codex", iconX, iconY));
        desktopItems.add(appForId("sync", iconX + ICON_STEP_X, iconY));
        desktopItems.add(appForId("decryptor", iconX + ICON_STEP_X * 2, iconY));
        desktopItems.add(appForId("explorer", iconX + ICON_STEP_X * 3, iconY));
        desktopItems.add(appForId("diagnostics", iconX + ICON_STEP_X * 4, iconY));
        desktopItems.add(appForId("physical", iconX + ICON_STEP_X * 5, iconY));
    }

    private boolean loadDesktopLayout(String layout, int left, int top) {
        if (layout == null || layout.isBlank()) {
            return false;
        }
        List<DesktopItem> loaded = new ArrayList<>();
        Set<String> usedApps = new HashSet<>();
        try {
            for (String line : layout.split("\n")) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 4) {
                    return false;
                }
                if ("A".equals(parts[0])) {
                    DesktopItem app = appForId(parts[1], clampDesktopX(Integer.parseInt(parts[2])),
                            clampDesktopY(Integer.parseInt(parts[3])));
                    if (app != null && usedApps.add(parts[1])) {
                        loaded.add(app);
                    }
                } else if ("F".equals(parts[0]) && parts.length >= 5) {
                    DesktopItem folder = DesktopItem.folder(clampDesktopX(Integer.parseInt(parts[2])),
                            clampDesktopY(Integer.parseInt(parts[3])));
                    String name = decodeLayoutText(parts[1]);
                    if (!name.isBlank()) {
                        folder.customTitle = name;
                    }
                    for (String childId : parts[4].split(",")) {
                        DesktopItem child = appForId(childId, 0, 0);
                        if (child != null && usedApps.add(childId)) {
                            folder.children.add(child);
                        }
                    }
                    loaded.add(folder);
                }
            }
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (loaded.isEmpty()) {
            return false;
        }
        appendMissingApps(loaded, usedApps, left, top);
        desktopItems.clear();
        desktopItems.addAll(loaded);
        return true;
    }

    private void appendMissingApps(List<DesktopItem> loaded, Set<String> usedApps, int left, int top) {
        String[] ids = {"codex", "sync", "decryptor", "explorer", "diagnostics", "physical"};
        int index = 0;
        for (String id : ids) {
            if (usedApps.contains(id)) {
                continue;
            }
            DesktopItem app = appForId(id, left + 24 + index * ICON_STEP_X, top + 142);
            if (app != null) {
                loaded.add(app);
            }
            index++;
        }
    }

    private DesktopItem appForId(String id, int x, int y) {
        return switch (id) {
            case "codex" -> DesktopItem.app(View.CODEX, x, y, "DOC", "screen.riftborne.codex.infobase");
            case "sync" -> DesktopItem.app(View.SYNC, x, y, "SYNC", "screen.riftborne.codex.sync");
            case "decryptor" -> DesktopItem.app(View.DECRYPTOR, x, y, "DEC", "screen.riftborne.codex.decryptor");
            case "explorer" -> DesktopItem.app(View.EXPLORER, x, y, "DRV", "screen.riftborne.codex.explorer_icon");
            case "diagnostics" -> DesktopItem.app(View.DIAGNOSTICS, x, y, "RNA", "screen.riftborne.codex.synapsis");
            case "physical" -> DesktopItem.app(View.PHYSICAL, x, y, "PHY", "screen.riftborne.codex.physical");
            default -> null;
        };
    }

    private DesktopItem desktopItemAt(double mouseX, double mouseY, DesktopItem excluded) {
        for (int index = desktopItems.size() - 1; index >= 0; index--) {
            DesktopItem item = desktopItems.get(index);
            if (item == excluded) {
                continue;
            }
            if (inside(mouseX, mouseY, item.x - 6, item.y - 5, ICON_HIT_WIDTH, ICON_HIT_HEIGHT)) {
                return item;
            }
        }
        return null;
    }

    private DesktopItem folderChildAt(double mouseX, double mouseY, int windowX, int windowY) {
        int itemAreaY = renamingFolder ? windowY + 76 : windowY + 43;
        for (int index = 0; openedFolder != null && index < openedFolder.children.size(); index++) {
            int x = windowX + 24 + index % 6 * ICON_STEP_X;
            int y = itemAreaY + index / 6 * 76;
            if (inside(mouseX, mouseY, x - 6, y - 5, ICON_HIT_WIDTH, ICON_HIT_HEIGHT)) {
                return openedFolder.children.get(index);
            }
        }
        return null;
    }

    private void openDesktopItem(DesktopItem item) {
        if (item.isFolder()) {
            openedFolder = item;
            renamingFolder = false;
            confirmingFolderDissolve = false;
            view = View.FOLDER;
        } else {
            view = item.view;
        }
    }

    private void handleDesktopDrop(DesktopItem item, double mouseX, double mouseY) {
        DesktopItem target = desktopItemAt(mouseX, mouseY, item);
        if (target != null && target != item) {
            if (target.isFolder() && !item.isFolder()) {
                desktopItems.remove(item);
                target.children.add(item);
                saveDesktopLayout();
                return;
            }
            if (!target.isFolder() && !item.isFolder()) {
                createFolderFrom(target, item);
                saveDesktopLayout();
                return;
            }
        }
        item.x = clampDesktopX(item.x);
        item.y = clampDesktopY(item.y);
        saveDesktopLayout();
    }

    private void createFolderFrom(DesktopItem target, DesktopItem dragged) {
        int folderX = target.x;
        int folderY = target.y;
        desktopItems.remove(target);
        desktopItems.remove(dragged);
        DesktopItem folder = DesktopItem.folder(folderX, folderY);
        folder.children.add(target);
        folder.children.add(dragged);
        desktopItems.add(folder);
    }

    private void dissolveOpenedFolder() {
        if (openedFolder == null) {
            return;
        }
        int preferredX = openedFolder.x;
        int preferredY = openedFolder.y;
        List<DesktopItem> children = new ArrayList<>(openedFolder.children);
        desktopItems.remove(openedFolder);
        for (DesktopItem child : children) {
            int[] position = nearestFreeDesktopPosition(preferredX, preferredY);
            child.x = position[0];
            child.y = position[1];
            desktopItems.add(child);
        }
        openedFolder.children.clear();
        openedFolder = null;
        renamingFolder = false;
        confirmingFolderDissolve = false;
        view = View.DESKTOP;
        saveDesktopLayout();
    }

    private int[] nearestFreeDesktopPosition(int preferredX, int preferredY) {
        int gridLeft = panelLeft() + 24;
        int gridTop = panelTop() + 62;
        int gridRight = panelLeft() + PANEL_WIDTH - 58;
        int gridBottom = panelTop() + PANEL_HEIGHT - STATUS_HEIGHT - 78;
        int bestX = clampDesktopX(preferredX);
        int bestY = clampDesktopY(preferredY);
        long bestDistance = Long.MAX_VALUE;
        for (int y = gridTop; y <= gridBottom; y += 76) {
            for (int x = gridLeft; x <= gridRight; x += ICON_STEP_X) {
                if (desktopSlotOccupied(x, y)) {
                    continue;
                }
                long deltaX = x - preferredX;
                long deltaY = y - preferredY;
                long distance = deltaX * deltaX + deltaY * deltaY;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        return new int[]{bestX, bestY};
    }

    private boolean desktopSlotOccupied(int x, int y) {
        for (DesktopItem item : desktopItems) {
            if (Math.abs(item.x - x) < ICON_HIT_WIDTH && Math.abs(item.y - y) < ICON_HIT_HEIGHT) {
                return true;
            }
        }
        return false;
    }

    private int clampDesktopX(int x) {
        int left = panelLeft();
        return Math.clamp(x, left + 12, left + PANEL_WIDTH - 58);
    }

    private int clampDesktopY(int y) {
        int top = panelTop();
        return Math.clamp(y, top + 24, top + PANEL_HEIGHT - STATUS_HEIGHT - 78);
    }

    private int itemBadge(DesktopItem item) {
        if (item.isFolder()) {
            return item.children.size();
        }
        return switch (item.view) {
            case SYNC -> CodexNetwork.split(snapshot.queuedEntries()).size();
            case DECRYPTOR -> CodexNetwork.split(snapshot.damagedEntries()).size();
            case DIAGNOSTICS -> snapshot.diagnosticAvailable() ? 1 : 0;
            default -> 0;
        };
    }

    private void saveDesktopLayout() {
        PacketDistributor.sendToServer(new CodexNetwork.UpdateDesktopLayoutPayload(
                snapshot.laptopPos(),
                encodeDesktopLayout()
        ));
    }

    private String encodeDesktopLayout() {
        StringBuilder builder = new StringBuilder();
        for (DesktopItem item : desktopItems) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            if (item.isFolder()) {
                builder.append("F|")
                        .append(encodeLayoutText(item.customTitle == null ? "" : item.customTitle))
                        .append('|')
                        .append(item.x)
                        .append('|')
                        .append(item.y)
                        .append('|');
                for (int index = 0; index < item.children.size(); index++) {
                    if (index > 0) {
                        builder.append(',');
                    }
                    builder.append(viewId(item.children.get(index)));
                }
            } else {
                builder.append("A|")
                        .append(viewId(item))
                        .append('|')
                        .append(item.x)
                        .append('|')
                        .append(item.y);
            }
        }
        return builder.toString();
    }

    private static String viewId(DesktopItem item) {
        return switch (item.view) {
            case CODEX -> "codex";
            case SYNC -> "sync";
            case DECRYPTOR -> "decryptor";
            case EXPLORER -> "explorer";
            case DIAGNOSTICS -> "diagnostics";
            case PHYSICAL -> "physical";
            default -> "";
        };
    }

    private static String encodeLayoutText(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeLayoutText(String value) {
        if (value.isBlank()) {
            return "";
        }
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static int explorerEntryCount(Set<String> entries) {
        int count = 0;
        for (CodexEntry entry : CodexEntries.all()) {
            if (entries.contains(entry.id())) {
                count++;
            }
        }
        return count;
    }

    private void drawTrimmed(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
        graphics.drawString(font, fit(text.getString(), maxWidth), x, y, color, false);
    }

    /**
     * Cut a string to the given width, marking it with an ellipsis when something was dropped.
     * Translations vary a lot in length, so nearly every label in Rift OS goes through here
     * rather than trusting that it happens to fit its container.
     */
    private String fit(String text, int maxWidth) {
        if (maxWidth <= 0 || font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    /**
     * A label on the left with its value right-aligned. The label is clamped against the value,
     * which is what used to overlap whenever a translation ran long.
     */
    private void drawLabelValue(GuiGraphics graphics, Component label, String value,
                                int x, int y, int width, int labelColor, int valueColor) {
        int valueWidth = font.width(value);
        graphics.drawString(font, fit(label.getString(), width - valueWidth - 8), x, y, labelColor, false);
        graphics.drawString(font, value, x + width - valueWidth, y, valueColor, false);
    }

    private void drawScrollingTitle(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
        String value = text.getString();
        if (font.width(value) <= maxWidth) {
            graphics.drawString(font, value, x, y, color, false);
            return;
        }

        String loop = value + "   \u2022   ";
        int offset = (int) ((Util.getMillis() / 180L) % loop.length());
        String scrolling = loop.substring(offset) + loop.substring(0, offset) + loop;
        graphics.drawString(font, font.plainSubstrByWidth(scrolling, maxWidth), x, y, color, false);
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

    private int diagnosticStageColor() {
        return switch (snapshot.diagnosticMetaWearStage()) {
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
        CODEX,
        SYNC,
        DECRYPTOR,
        EXPLORER,
        DIAGNOSTICS,
        PHYSICAL,
        FOLDER
    }

    private record PhysicalSnapshot(String id, float value, float activity) {
    }

    private static final class DesktopItem {
        private final View view;
        private final String glyph;
        private final String titleKey;
        private final List<DesktopItem> children = new ArrayList<>();
        private String customTitle;
        private int x;
        private int y;

        private DesktopItem(View view, int x, int y, String glyph, String titleKey) {
            this.view = view;
            this.x = x;
            this.y = y;
            this.glyph = glyph;
            this.titleKey = titleKey;
        }

        private static DesktopItem app(View view, int x, int y, String glyph, String titleKey) {
            return new DesktopItem(view, x, y, glyph, titleKey);
        }

        private static DesktopItem folder(int x, int y) {
            return new DesktopItem(null, x, y, "DIR", "screen.riftborne.codex.folder_default");
        }

        private boolean isFolder() {
            return view == null;
        }

        private Component title() {
            return customTitle == null || customTitle.isBlank()
                    ? Component.translatable(titleKey)
                    : Component.literal(customTitle);
        }
    }
}
