package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.codex.data.entry.CodexArticleSection;
import com.pr1tcha.riftborne.codex.data.entry.CodexInfobaseSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

final class CodexInfobasePanel {
    private static final int COLOR_PANEL = 0xCC080D14;
    private static final int COLOR_PANEL_RAISED = 0xFF111923;
    private static final int COLOR_BORDER = 0x663D4757;
    private static final int COLOR_ACCENT = 0xFF8C74F7;
    private static final int COLOR_ACCENT_BRIGHT = 0xFFB9ABFF;
    private static final int COLOR_CYAN = 0xFF55D8DC;
    private static final int COLOR_GREEN = 0xFF68D995;
    private static final int COLOR_AMBER = 0xFFE5B768;
    private static final int COLOR_DANGER = 0xFFE56D7C;
    private static final int COLOR_TEXT = 0xFFF0F3F7;
    private static final int COLOR_MUTED = 0xFF89919F;
    private static final int COLOR_FAINT = 0xFF596171;
    private static final String HOME = "HOME";
    private static final int CATEGORY_WIDTH = 112;
    private static final int LIST_WIDTH = 166;
    private static final int GAP = 6;

    private CodexInfobaseSnapshot snapshot = CodexInfobaseSnapshot.EMPTY;
    private String selectedCategory = HOME;
    private String selectedEntryId = "";
    private String search = "";
    private boolean searchFocused;
    private Filter filter = Filter.ALL;
    private int entryScroll;
    private int articleScroll;
    private int articleMaxScroll;
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;
    private final List<LinkHit> linkHits = new ArrayList<>();

    void update(String encoded) {
        snapshot = CodexInfobaseSnapshot.decode(encoded);
        if (!selectedEntryId.isBlank() && entry(selectedEntryId) == null) {
            selectedEntryId = "";
            articleScroll = 0;
        }
    }

    void render(GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY) {
        linkHits.clear();
        graphics.fill(x, y, x + width, y + height, 0xBB060A10);
        renderToolbar(graphics, font, x, y, width, mouseX, mouseY);

        int contentY = y + 29;
        int contentHeight = height - 29;
        int listX = x + CATEGORY_WIDTH + GAP;
        int articleX = listX + LIST_WIDTH + GAP;
        int articleWidth = x + width - articleX;

        renderCategories(graphics, font, x, contentY, CATEGORY_WIDTH, contentHeight, mouseX, mouseY);
        renderEntryList(graphics, font, listX, contentY, LIST_WIDTH, contentHeight, mouseX, mouseY);
        renderArticle(graphics, font, articleX, contentY, articleWidth, contentHeight, mouseX, mouseY);
    }

    private void renderToolbar(GuiGraphics graphics, Font font, int x, int y, int width, int mouseX, int mouseY) {
        graphics.fill(x, y, x + width, y + 24, COLOR_PANEL_RAISED);
        graphics.renderOutline(x, y, width, 24, COLOR_BORDER);
        drawToolbarButton(graphics, font, x + 4, y + 4, 18, "<", historyIndex > 0, mouseX, mouseY);
        drawToolbarButton(graphics, font, x + 25, y + 4, 18, ">", historyIndex >= 0 && historyIndex + 1 < history.size(), mouseX, mouseY);
        drawToolbarButton(graphics, font, x + 46, y + 4, 42, "HOME", true, mouseX, mouseY);

        int searchX = x + 96;
        int searchWidth = Math.max(120, width - 238);
        graphics.fill(searchX, y + 4, searchX + searchWidth, y + 20, 0xFF070B11);
        graphics.renderOutline(searchX, y + 4, searchWidth, 16, searchFocused ? COLOR_CYAN : COLOR_BORDER);
        String shown = search.isEmpty() && !searchFocused
                ? Component.translatable("screen.riftborne.infobase.search").getString()
                : search + (searchFocused && (Util.getMillis() / 350L) % 2 == 0 ? "_" : "");
        graphics.drawString(font, trim(font, shown, searchWidth - 12), searchX + 6, y + 8,
                search.isEmpty() && !searchFocused ? COLOR_FAINT : COLOR_TEXT, false);

        int filterX = x + width - 134;
        boolean hovered = inside(mouseX, mouseY, filterX, y + 4, 130, 16);
        graphics.fill(filterX, y + 4, filterX + 130, y + 20, hovered ? 0xFF252D3A : 0xFF171D27);
        graphics.renderOutline(filterX, y + 4, 130, 16, filter.color());
        graphics.drawCenteredString(font, Component.translatable(filter.key()), filterX + 65, y + 8, filter.color());
    }

    private void drawToolbarButton(
            GuiGraphics graphics, Font font, int x, int y, int width, String label,
            boolean active, int mouseX, int mouseY
    ) {
        boolean hovered = active && inside(mouseX, mouseY, x, y, width, 16);
        graphics.fill(x, y, x + width, y + 16, hovered ? 0xFF2A3342 : 0xFF171D27);
        graphics.renderOutline(x, y, width, 16, active ? COLOR_BORDER : 0x332B323D);
        graphics.drawCenteredString(font, label, x + width / 2, y + 4, active ? COLOR_TEXT : COLOR_FAINT);
    }

    private void renderCategories(
            GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY
    ) {
        graphics.fill(x, y, x + width, y + height, COLOR_PANEL);
        graphics.renderOutline(x, y, width, height, COLOR_BORDER);
        graphics.drawString(font, trim(font,
                Component.translatable("screen.riftborne.infobase.sections").getString(), width - 16),
                x + 8, y + 8, COLOR_MUTED, false);
        int rowY = y + 24;
        rowY = drawCategoryRow(graphics, font, x, rowY, width, HOME, "screen.riftborne.infobase.home", mouseX, mouseY);
        for (String category : categories()) {
            rowY = drawCategoryRow(graphics, font, x, rowY, width, category,
                    "screen.riftborne.infobase.category." + category.toLowerCase(Locale.ROOT), mouseX, mouseY);
        }
    }

    private int drawCategoryRow(
            GuiGraphics graphics, Font font, int x, int y, int width, String category, String key,
            int mouseX, int mouseY
    ) {
        boolean selected = selectedCategory.equals(category) && search.isBlank();
        boolean hovered = inside(mouseX, mouseY, x + 4, y, width - 8, 19);
        if (selected || hovered) {
            graphics.fill(x + 4, y, x + width - 4, y + 19, selected ? 0x553C326D : 0x332A3342);
        }
        int color = categoryColor(category);
        graphics.fill(x + 5, y + 4, x + 8, y + 15, color);
        graphics.drawString(font, trim(font, Component.translatable(key).getString(), width - 42), x + 13, y + 6,
                selected ? COLOR_TEXT : COLOR_MUTED, false);
        String count = Integer.toString(category.equals(HOME) ? snapshot.entries().size() : count(category));
        graphics.drawString(font, count, x + width - font.width(count) - 8, y + 6, selected ? color : COLOR_FAINT, false);
        return y + 20;
    }

    private void renderEntryList(
            GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY
    ) {
        graphics.fill(x, y, x + width, y + height, COLOR_PANEL);
        graphics.renderOutline(x, y, width, height, COLOR_BORDER);
        List<CodexInfobaseSnapshot.Entry> entries = visibleEntries();
        graphics.drawString(font, Component.translatable(search.isBlank()
                        ? "screen.riftborne.infobase.records"
                        : "screen.riftborne.infobase.search_results", entries.size()),
                x + 8, y + 8, COLOR_MUTED, false);
        int visibleRows = Math.max(1, (height - 28) / 29);
        entryScroll = Math.clamp(entryScroll, 0, Math.max(0, entries.size() - visibleRows));
        int rowY = y + 24;
        for (int index = entryScroll; index < Math.min(entries.size(), entryScroll + visibleRows); index++) {
            CodexInfobaseSnapshot.Entry entry = entries.get(index);
            boolean selected = entry.id().equals(selectedEntryId);
            boolean hovered = inside(mouseX, mouseY, x + 4, rowY, width - 8, 27);
            if (selected || hovered) {
                graphics.fill(x + 4, rowY, x + width - 4, rowY + 27, selected ? 0x553C326D : 0x332A3342);
            }
            int stateColor = stateColor(entry);
            graphics.fill(x + 8, rowY + 6, x + 11, rowY + 21, stateColor);
            String title = isLocked(entry)
                    ? Component.translatable("screen.riftborne.infobase.unknown").getString()
                    : entry.title();
            graphics.drawString(font, trim(font, title, width - 28), x + 16, rowY + 5,
                    selected ? COLOR_TEXT : 0xFFD0D6DF, false);
            graphics.drawString(font, stateLabel(entry), x + 16, rowY + 16, stateColor, false);
            rowY += 29;
        }
        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.riftborne.infobase.empty"),
                    x + width / 2, y + 48, COLOR_FAINT);
        }
        drawScrollbar(graphics, x + width - 5, y + 25, height - 29, entries.size(), visibleRows, entryScroll);
    }

    private void renderArticle(
            GuiGraphics graphics, Font font, int x, int y, int width, int height, int mouseX, int mouseY
    ) {
        graphics.fill(x, y, x + width, y + height, COLOR_PANEL);
        graphics.renderOutline(x, y, width, height, COLOR_BORDER);
        if (selectedEntryId.isBlank()) {
            renderDashboard(graphics, font, x, y, width, height);
            return;
        }
        CodexInfobaseSnapshot.Entry entry = entry(selectedEntryId);
        if (entry == null) {
            selectedEntryId = "";
            renderDashboard(graphics, font, x, y, width, height);
            return;
        }

        int inset = 10;
        int contentX = x + inset;
        int contentWidth = width - inset * 2 - 3;
        int contentTop = y + 8;
        int contentBottom = y + height - 7;
        int cursor = contentTop - articleScroll;
        graphics.enableScissor(x + 2, y + 2, x + width - 3, y + height - 2);
        if (isLocked(entry)) {
            graphics.drawString(font, Component.translatable("screen.riftborne.infobase.unknown"), contentX, cursor,
                    COLOR_FAINT, false);
            cursor += 22;
            graphics.renderOutline(contentX, cursor, contentWidth, 54, 0x55596171);
            graphics.drawWordWrap(font, Component.translatable("screen.riftborne.infobase.locked_hint"),
                    contentX + 8, cursor + 10, contentWidth - 16, COLOR_MUTED);
            cursor += 68;
        } else {
            graphics.drawString(font, trim(font, entry.title(), contentWidth), contentX, cursor, COLOR_TEXT, false);
            cursor += 13;
            graphics.drawString(font,
                    Component.translatable("screen.riftborne.infobase.category."
                            + entry.category().toLowerCase(Locale.ROOT)),
                    contentX, cursor, categoryColor(entry.category()), false);
            cursor += 15;
            cursor = renderStatusStrip(graphics, font, entry, contentX, cursor, contentWidth);
            cursor += 8;
            cursor = renderParagraph(graphics, font, Component.translatable("screen.riftborne.infobase.summary").getString(),
                    entry.summary(), contentX, cursor, contentWidth, COLOR_ACCENT_BRIGHT);
            if (!entry.fullContent().isBlank()) {
                cursor = renderParagraph(graphics, font, Component.translatable("screen.riftborne.infobase.details").getString(),
                        entry.fullContent(), contentX, cursor, contentWidth, 0xFFC7CFD8);
            }
            for (CodexArticleSection section : entry.sections()) {
                cursor = renderParagraph(graphics, font, section.heading(), section.body(), contentX, cursor,
                        contentWidth, 0xFFC7CFD8);
            }
            if (!entry.metadata().isEmpty()) {
                graphics.drawString(font, Component.translatable("screen.riftborne.infobase.metadata"),
                        contentX, cursor, COLOR_CYAN, false);
                cursor += 13;
                for (var metadata : entry.metadata().entrySet()) {
                    graphics.drawString(font, trim(font, metadata.getKey() + ": " + metadata.getValue(), contentWidth),
                            contentX + 4, cursor, COLOR_MUTED, false);
                    cursor += 11;
                }
                cursor += 6;
            }
            if (!entry.relatedEntries().isEmpty()) {
                graphics.drawString(font, Component.translatable("screen.riftborne.infobase.related"),
                        contentX, cursor, COLOR_CYAN, false);
                cursor += 14;
                for (String relatedId : entry.relatedEntries()) {
                    CodexInfobaseSnapshot.Entry related = entry(relatedId);
                    if (related == null) {
                        continue;
                    }
                    int linkY = cursor;
                    boolean hovered = inside(mouseX, mouseY, contentX + 4, linkY, contentWidth - 8, 12);
                    graphics.drawString(font, "→ " + trim(font, related.title(), contentWidth - 14), contentX + 4,
                            linkY, hovered ? COLOR_ACCENT_BRIGHT : COLOR_ACCENT, false);
                    linkHits.add(new LinkHit(contentX + 4, linkY, contentWidth - 8, 12, related.id()));
                    cursor += 13;
                }
            }
        }
        graphics.disableScissor();
        articleMaxScroll = Math.max(0, cursor + articleScroll - contentBottom);
        articleScroll = Math.clamp(articleScroll, 0, articleMaxScroll);
        drawScrollbar(graphics, x + width - 5, y + 4, height - 8,
                Math.max(height, articleMaxScroll + height), height, articleScroll);
    }

    private void renderDashboard(GuiGraphics graphics, Font font, int x, int y, int width, int height) {
        int known = (int) snapshot.entries().stream().filter(entry -> !isLocked(entry)).count();
        int alerts = (int) snapshot.entries().stream().filter(this::isAlert).count();
        graphics.drawString(font, trim(font,
                Component.translatable("screen.riftborne.infobase.dashboard").getString(), width - 20),
                x + 10, y + 9, COLOR_TEXT, false);
        graphics.drawString(font, trim(font,
                Component.translatable("screen.riftborne.infobase.dashboard_hint").getString(), width - 20),
                x + 10, y + 23, COLOR_MUTED, false);
        int cardY = y + 44;
        int cardWidth = (width - 32) / 3;
        renderMetric(graphics, font, x + 8, cardY, cardWidth,
                "screen.riftborne.infobase.metric.total", snapshot.entries().size(), COLOR_CYAN);
        renderMetric(graphics, font, x + 16 + cardWidth, cardY, cardWidth,
                "screen.riftborne.infobase.metric.known", known, COLOR_GREEN);
        renderMetric(graphics, font, x + 24 + cardWidth * 2, cardY, cardWidth,
                "screen.riftborne.infobase.metric.alerts", alerts, alerts > 0 ? COLOR_DANGER : COLOR_MUTED);
        graphics.drawString(font, trim(font,
                Component.translatable("screen.riftborne.infobase.dashboard_navigation").getString(), width - 20),
                x + 10, cardY + 55, COLOR_ACCENT_BRIGHT, false);
        graphics.drawWordWrap(font, Component.translatable("screen.riftborne.infobase.dashboard_navigation_hint"),
                x + 10, cardY + 70, width - 20, COLOR_MUTED);
    }

    private void renderMetric(GuiGraphics graphics, Font font, int x, int y, int width, String key, int value, int color) {
        graphics.fill(x, y, x + width, y + 42, COLOR_PANEL_RAISED);
        graphics.renderOutline(x, y, width, 42, COLOR_BORDER);
        graphics.fill(x, y, x + 3, y + 42, color);
        graphics.drawString(font, trim(font, Component.translatable(key).getString(), width - 16),
                x + 8, y + 7, COLOR_MUTED, false);
        graphics.drawString(font, Integer.toString(value), x + 8, y + 23, color, false);
    }

    private int renderStatusStrip(
            GuiGraphics graphics, Font font, CodexInfobaseSnapshot.Entry entry,
            int x, int y, int width
    ) {
        graphics.fill(x, y, x + width, y + 24, COLOR_PANEL_RAISED);
        graphics.renderOutline(x, y, width, 24, COLOR_BORDER);
        graphics.fill(x, y, x + 3, y + 24, stateColor(entry));
        String threat = Component.translatable("screen.riftborne.infobase.threat", entry.threatLevel()).getString();
        graphics.drawString(font, trim(font, stateLabel(entry), width - font.width(threat) - 22),
                x + 8, y + 5, stateColor(entry), false);
        graphics.drawString(font, threat, x + width - font.width(threat) - 7, y + 5,
                threatColor(entry.threatLevel()), false);
        return y + 24;
    }

    private int renderParagraph(
            GuiGraphics graphics, Font font, String heading, String body,
            int x, int y, int width, int bodyColor
    ) {
        if (body == null || body.isBlank()) {
            return y;
        }
        if (heading != null && !heading.isBlank()) {
            graphics.drawString(font, heading.toUpperCase(Locale.ROOT), x, y, COLOR_CYAN, false);
            y += 14;
        }
        for (FormattedCharSequence line : font.split(Component.literal(body), width)) {
            graphics.drawString(font, line, x, y, bodyColor, false);
            y += 11;
        }
        return y + 9;
    }

    boolean mouseClicked(double mouseX, double mouseY, int button, int x, int y, int width, int height) {
        if (button != 0) {
            return false;
        }
        int searchX = x + 96;
        int searchWidth = Math.max(120, width - 238);
        searchFocused = inside(mouseX, mouseY, searchX, y + 4, searchWidth, 16);
        if (searchFocused) {
            return true;
        }
        if (inside(mouseX, mouseY, x + 4, y + 4, 18, 16) && historyIndex > 0) {
            historyIndex--;
            selectFromHistory();
            return true;
        }
        if (inside(mouseX, mouseY, x + 25, y + 4, 18, 16)
                && historyIndex >= 0 && historyIndex + 1 < history.size()) {
            historyIndex++;
            selectFromHistory();
            return true;
        }
        if (inside(mouseX, mouseY, x + 46, y + 4, 42, 16)) {
            selectedCategory = HOME;
            selectedEntryId = "";
            articleScroll = 0;
            return true;
        }
        if (inside(mouseX, mouseY, x + width - 134, y + 4, 130, 16)) {
            filter = filter.next();
            entryScroll = 0;
            return true;
        }

        int contentY = y + 29;
        int contentHeight = height - 29;
        int rowY = contentY + 24;
        List<String> categories = new ArrayList<>();
        categories.add(HOME);
        categories.addAll(categories());
        for (String category : categories) {
            if (inside(mouseX, mouseY, x + 4, rowY, CATEGORY_WIDTH - 8, 19)) {
                selectedCategory = category;
                search = "";
                selectedEntryId = "";
                entryScroll = 0;
                articleScroll = 0;
                return true;
            }
            rowY += 20;
        }

        int listX = x + CATEGORY_WIDTH + GAP;
        if (inside(mouseX, mouseY, listX, contentY, LIST_WIDTH, contentHeight)) {
            List<CodexInfobaseSnapshot.Entry> entries = visibleEntries();
            int index = entryScroll + (int) ((mouseY - (contentY + 24)) / 29);
            if (index >= entryScroll && index < entries.size()) {
                select(entries.get(index).id(), true);
            }
            return true;
        }
        for (LinkHit hit : linkHits) {
            if (inside(mouseX, mouseY, hit.x(), hit.y(), hit.width(), hit.height())) {
                select(hit.entryId(), true);
                return true;
            }
        }
        return inside(mouseX, mouseY, x, y, width, height);
    }

    boolean mouseScrolled(double mouseX, double mouseY, double scrollY, int x, int y, int width, int height) {
        int contentY = y + 29;
        int contentHeight = height - 29;
        int listX = x + CATEGORY_WIDTH + GAP;
        int articleX = listX + LIST_WIDTH + GAP;
        if (inside(mouseX, mouseY, listX, contentY, LIST_WIDTH, contentHeight)) {
            int visibleRows = Math.max(1, (contentHeight - 28) / 29);
            entryScroll = Math.clamp(entryScroll - (int) Math.signum(scrollY), 0,
                    Math.max(0, visibleEntries().size() - visibleRows));
            return true;
        }
        if (inside(mouseX, mouseY, articleX, contentY, x + width - articleX, contentHeight)) {
            articleScroll = Math.clamp(articleScroll - (int) Math.signum(scrollY) * 22, 0, articleMaxScroll);
            return true;
        }
        return false;
    }

    boolean keyPressed(int keyCode, int modifiers) {
        if ((modifiers & 2) != 0 && keyCode == 70) {
            searchFocused = true;
            return true;
        }
        if (!searchFocused) {
            return false;
        }
        if (keyCode == 256 || keyCode == 257 || keyCode == 335) {
            searchFocused = false;
            return true;
        }
        if (keyCode == 259 && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            entryScroll = 0;
            selectedEntryId = "";
            return true;
        }
        return true;
    }

    boolean charTyped(char codePoint) {
        if (!searchFocused) {
            return false;
        }
        if (!Character.isISOControl(codePoint) && search.length() < 48) {
            search += codePoint;
            entryScroll = 0;
            selectedEntryId = "";
        }
        return true;
    }

    private void select(String id, boolean addHistory) {
        CodexInfobaseSnapshot.Entry entry = entry(id);
        if (entry == null) {
            return;
        }
        selectedEntryId = id;
        selectedCategory = entry.category();
        articleScroll = 0;
        if (!addHistory) {
            return;
        }
        while (history.size() > historyIndex + 1) {
            history.removeLast();
        }
        if (history.isEmpty() || !history.getLast().equals(id)) {
            history.add(id);
        }
        historyIndex = history.size() - 1;
    }

    private void selectFromHistory() {
        if (historyIndex >= 0 && historyIndex < history.size()) {
            select(history.get(historyIndex), false);
        }
    }

    private List<CodexInfobaseSnapshot.Entry> visibleEntries() {
        String query = search.trim().toLowerCase(Locale.ROOT);
        return snapshot.entries().stream()
                .filter(entry -> query.isBlank() ? selectedCategory.equals(HOME) || entry.category().equals(selectedCategory)
                        : matches(entry, query))
                .filter(filter::accepts)
                .sorted(Comparator.comparingInt(CodexInfobaseSnapshot.Entry::sortOrder)
                        .thenComparing(CodexInfobaseSnapshot.Entry::title))
                .toList();
    }

    private boolean matches(CodexInfobaseSnapshot.Entry entry, String query) {
        if (entry.title().toLowerCase(Locale.ROOT).contains(query)
                || entry.summary().toLowerCase(Locale.ROOT).contains(query)
                || entry.category().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        return entry.tags().stream().anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains(query));
    }

    private List<String> categories() {
        Set<String> categories = new LinkedHashSet<>();
        snapshot.entries().stream()
                .sorted(Comparator.comparingInt(entry -> categoryOrder(entry.category())))
                .forEach(entry -> categories.add(entry.category()));
        return List.copyOf(categories);
    }

    private int count(String category) {
        return (int) snapshot.entries().stream().filter(entry -> entry.category().equals(category)).count();
    }

    private CodexInfobaseSnapshot.Entry entry(String id) {
        return snapshot.entries().stream().filter(entry -> entry.id().equals(id)).findFirst().orElse(null);
    }

    private boolean isLocked(CodexInfobaseSnapshot.Entry entry) {
        return !entry.known() && "LOCKED".equals(entry.state());
    }

    private boolean isAlert(CodexInfobaseSnapshot.Entry entry) {
        return entry.threatLevel() >= 4 || Set.of("DAMAGED", "ENCRYPTED", "NEEDS_DECRYPTION").contains(entry.state());
    }

    private String stateLabel(CodexInfobaseSnapshot.Entry entry) {
        return Component.translatable("screen.riftborne.infobase.state."
                + entry.state().toLowerCase(Locale.ROOT)).getString();
    }

    private int stateColor(CodexInfobaseSnapshot.Entry entry) {
        return switch (entry.state()) {
            case "UNLOCKED" -> COLOR_GREEN;
            case "PARTIAL" -> COLOR_CYAN;
            case "DAMAGED", "NEEDS_DECRYPTION" -> COLOR_DANGER;
            case "ENCRYPTED" -> COLOR_AMBER;
            default -> COLOR_FAINT;
        };
    }

    private static int threatColor(int threat) {
        return threat >= 4 ? COLOR_DANGER : threat >= 3 ? COLOR_AMBER : threat > 0 ? COLOR_GREEN : COLOR_MUTED;
    }

    private static int categoryColor(String category) {
        return switch (category) {
            case "RIFTS" -> 0xFFA57AF2;
            case "MOBS" -> 0xFFE87884;
            case "DIMENSIONS" -> 0xFF6AB7F5;
            case "RNA" -> 0xFF63D8C5;
            case "ASPECTS" -> 0xFFB997FF;
            case "TECHNIQUES" -> 0xFFFFC76C;
            case "ITEMS", "DEVICES" -> 0xFF7CD8E8;
            case "SIGNALS", "FIELD_ARCHIVE", "ARCHIVE" -> 0xFF70A7C4;
            default -> COLOR_ACCENT;
        };
    }

    private static int categoryOrder(String category) {
        return switch (category) {
            case "RIFTS" -> 10;
            case "DIMENSIONS" -> 20;
            case "MOBS" -> 30;
            case "RNA" -> 40;
            case "ASPECTS" -> 50;
            case "TECHNIQUES" -> 60;
            case "ITEMS", "DEVICES" -> 70;
            case "SIGNALS", "FIELD_ARCHIVE", "ARCHIVE" -> 80;
            default -> 90;
        };
    }

    private static void drawScrollbar(
            GuiGraphics graphics, int x, int y, int height, int total, int visible, int scroll
    ) {
        if (total <= visible || total <= 0) {
            return;
        }
        int knob = Math.max(12, height * visible / total);
        int maxScroll = Math.max(1, total - visible);
        int knobY = y + (height - knob) * scroll / maxScroll;
        graphics.fill(x, y, x + 2, y + height, 0x66304C4A);
        graphics.fill(x - 1, knobY, x + 3, knobY + knob, COLOR_ACCENT);
    }

    private static String trim(Font font, String value, int width) {
        return font.plainSubstrByWidth(value == null ? "" : value, Math.max(1, width));
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private enum Filter {
        ALL("screen.riftborne.infobase.filter.all", COLOR_ACCENT),
        KNOWN("screen.riftborne.infobase.filter.known", COLOR_GREEN),
        ALERTS("screen.riftborne.infobase.filter.alerts", COLOR_DANGER);

        private final String key;
        private final int color;

        Filter(String key, int color) {
            this.key = key;
            this.color = color;
        }

        String key() {
            return key;
        }

        int color() {
            return color;
        }

        boolean accepts(CodexInfobaseSnapshot.Entry entry) {
            return switch (this) {
                case ALL -> true;
                case KNOWN -> entry.known() || !"LOCKED".equals(entry.state());
                case ALERTS -> entry.threatLevel() >= 4
                        || Set.of("DAMAGED", "ENCRYPTED", "NEEDS_DECRYPTION").contains(entry.state());
            };
        }

        Filter next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private record LinkHit(int x, int y, int width, int height, String entryId) {
    }
}
