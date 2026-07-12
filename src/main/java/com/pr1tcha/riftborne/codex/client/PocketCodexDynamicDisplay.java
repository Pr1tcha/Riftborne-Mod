package com.pr1tcha.riftborne.codex.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.codex.data.PocketCodexData;
import java.io.IOException;
import java.util.Objects;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class PocketCodexDynamicDisplay {
    private static final ResourceLocation BASE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "textures/block/pocket_codex.png");
    private static final ResourceLocation DYNAMIC_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "dynamic/pocket_codex_display");
    private static final String[] SCREEN_NAMES = {"SCAN", "PULSE", "BUFFER"};
    private static final int SCREEN_X = 4;
    private static final int SCREEN_Y = 52;
    private static final int SCREEN_WIDTH = 96;
    private static final int SCREEN_HEIGHT = 72;

    private static DynamicTexture texture;
    private static NativeImage image;
    private static int lastSignature = Integer.MIN_VALUE;
    private static String transientNotice = "";
    private static long noticeUntil;

    private PocketCodexDynamicDisplay() {
    }

    public static ResourceLocation texture() {
        ensureTexture();
        return DYNAMIC_TEXTURE;
    }

    public static void showNotice(String noticeKey) {
        transientNotice = switch (noticeKey) {
            case "codex.riftborne.pocket.notice.discovered" -> "FOUND";
            case "codex.riftborne.pocket.notice.updated" -> "UPDATED";
            case "codex.riftborne.pocket.notice.damaged" -> "DAMAGED";
            case "codex.riftborne.pocket.notice.no_target" -> "NO TARGET";
            case "codex.riftborne.pocket.notice.buffer_full" -> "BUFFER FULL";
            case "codex.riftborne.pocket.notice.signals_found" -> "SIGNAL";
            case "codex.riftborne.pocket.notice.area_clear" -> "CLEAR";
            default -> "";
        };
        noticeUntil = Util.getMillis() + 1800L;
        lastSignature = Integer.MIN_VALUE;
    }

    public static void update(ItemStack stack) {
        ensureTexture();
        int screen = PocketCodexData.selectedScreen(stack);
        int entries = PocketCodexData.shortEntries(stack).size();
        int queued = PocketCodexData.queuedEntries(stack).size();
        int damaged = PocketCodexData.damagedEntries(stack).size();
        int pulseSignals = PocketCodexData.lastPulseCount(stack);
        int observations = PocketCodexData.observationCount(stack, PocketCodexData.lastTarget(stack));
        String visibleNotice = Util.getMillis() < noticeUntil ? transientNotice : "";
        int signature = Objects.hash(screen, entries, queued, damaged, pulseSignals, observations, visibleNotice);
        if (signature == lastSignature) {
            return;
        }
        lastSignature = signature;

        fill(SCREEN_X, SCREEN_Y, SCREEN_WIDTH, SCREEN_HEIGHT, color(2, 10, 12));
        fill(SCREEN_X + 1, SCREEN_Y + 1, SCREEN_WIDTH - 2, 9, color(9, 34, 35));
        drawText(SCREEN_NAMES[screen], SCREEN_X + 3, SCREEN_Y + 2, color(78, 237, 207));
        drawNumber(entries, SCREEN_X + 85, SCREEN_Y + 2, color(132, 255, 226));
        fill(SCREEN_X + 2, SCREEN_Y + 12, SCREEN_WIDTH - 4, 1, color(33, 105, 96));

        switch (screen) {
            case 1 -> drawPulse(pulseSignals);
            case 2 -> drawBuffer(queued, damaged);
            default -> drawScanner(observations);
        }
        if (!visibleNotice.isBlank()) {
            int bannerWidth = Math.min(SCREEN_WIDTH - 12, visibleNotice.length() * 4 + 8);
            int bannerX = SCREEN_X + (SCREEN_WIDTH - bannerWidth) / 2;
            fill(bannerX, SCREEN_Y + 49, bannerWidth, 9, color(8, 39, 39));
            outline(bannerX, SCREEN_Y + 49, bannerWidth, 9, color(79, 238, 207));
            drawText(visibleNotice, bannerX + 4, SCREEN_Y + 51, color(132, 255, 226));
        }
        drawNavigation(screen);
        texture.upload();
    }

    private static void drawNavigation(int selected) {
        for (int index = 0; index < 3; index++) {
            int x = SCREEN_X + 14 + index * 25;
            fill(x, SCREEN_Y + 66, 18, 3,
                    index == selected ? color(80, 239, 207) : color(18, 65, 62));
        }
    }

    private static void drawScanner(int observations) {
        outline(SCREEN_X + 10, SCREEN_Y + 17, 76, 39, color(50, 198, 177));
        fill(SCREEN_X + 47, SCREEN_Y + 17, 2, 39, color(22, 92, 84));
        fill(SCREEN_X + 10, SCREEN_Y + 35, 76, 2, color(22, 92, 84));
        int pulse = 7 + observations * 15;
        fill(SCREEN_X + 16 + pulse, SCREEN_Y + 27, 5, 5, color(92, 255, 218));
        drawText("ACQUIRE", SCREEN_X + 34, SCREEN_Y + 58, color(50, 164, 148));
    }

    private static void drawPulse(int signals) {
        int centerX = SCREEN_X + 48;
        int centerY = SCREEN_Y + 36;
        fill(centerX - 1, centerY - 1, 3, 3, color(93, 255, 218));
        for (int ring = 1; ring <= 3; ring++) {
            int strength = signals >= ring ? 220 : 75;
            outline(centerX - ring * 7, centerY - ring * 5, ring * 14 + 2, ring * 10 + 2,
                    color(40, strength, Math.max(0, strength - 20)));
        }
        drawText("SIG", SCREEN_X + 8, SCREEN_Y + 57, color(50, 164, 148));
        drawNumber(signals, SCREEN_X + 78, SCREEN_Y + 57, color(106, 255, 218));
    }

    private static void drawBuffer(int queued, int damaged) {
        drawText("BUFFER", SCREEN_X + 7, SCREEN_Y + 19, color(49, 159, 143));
        drawNumber(queued + damaged, SCREEN_X + 78, SCREEN_Y + 19, color(106, 255, 218));
        drawText("QUEUE", SCREEN_X + 7, SCREEN_Y + 33, color(49, 159, 143));
        drawNumber(queued, SCREEN_X + 78, SCREEN_Y + 33, color(106, 255, 218));
        drawText("DMG", SCREEN_X + 7, SCREEN_Y + 47,
                damaged > 0 ? color(224, 74, 81) : color(49, 159, 143));
        drawNumber(damaged, SCREEN_X + 78, SCREEN_Y + 47,
                damaged > 0 ? color(255, 99, 105) : color(106, 255, 218));
    }

    private static void drawNumber(int number, int x, int y, int color) {
        drawText(Integer.toString(Math.min(number, 99)), x, y, color);
    }

    private static void drawText(String text, int x, int y, int color) {
        int cursor = x;
        for (int index = 0; index < text.length(); index++) {
            drawGlyph(Character.toUpperCase(text.charAt(index)), cursor, y, color);
            cursor += 4;
        }
    }

    private static void drawGlyph(char character, int x, int y, int color) {
        int[] rows = glyph(character);
        for (int row = 0; row < rows.length; row++) {
            for (int column = 0; column < 3; column++) {
                if ((rows[row] & 1 << (2 - column)) != 0) {
                    fill(x + column, y + row, 1, 1, color);
                }
            }
        }
    }

    private static int[] glyph(char character) {
        return switch (character) {
            case 'A' -> new int[]{2, 5, 7, 5, 5};
            case 'B' -> new int[]{6, 5, 6, 5, 6};
            case 'C' -> new int[]{3, 4, 4, 4, 3};
            case 'D' -> new int[]{6, 5, 5, 5, 6};
            case 'E' -> new int[]{7, 4, 6, 4, 7};
            case 'F' -> new int[]{7, 4, 6, 4, 4};
            case 'G' -> new int[]{3, 4, 5, 5, 3};
            case 'H' -> new int[]{5, 5, 7, 5, 5};
            case 'I' -> new int[]{7, 2, 2, 2, 7};
            case 'K' -> new int[]{5, 5, 6, 5, 5};
            case 'L' -> new int[]{4, 4, 4, 4, 7};
            case 'M' -> new int[]{5, 7, 7, 5, 5};
            case 'N' -> new int[]{5, 7, 7, 7, 5};
            case 'O' -> new int[]{2, 5, 5, 5, 2};
            case 'P' -> new int[]{6, 5, 6, 4, 4};
            case 'Q' -> new int[]{2, 5, 5, 7, 3};
            case 'R' -> new int[]{6, 5, 6, 5, 5};
            case 'S' -> new int[]{3, 4, 2, 1, 6};
            case 'T' -> new int[]{7, 2, 2, 2, 2};
            case 'U' -> new int[]{5, 5, 5, 5, 7};
            case 'V' -> new int[]{5, 5, 5, 5, 2};
            case 'W' -> new int[]{5, 5, 7, 7, 5};
            case 'X' -> new int[]{5, 5, 2, 5, 5};
            case '0' -> new int[]{7, 5, 5, 5, 7};
            case '1' -> new int[]{2, 6, 2, 2, 7};
            case '2' -> new int[]{6, 1, 7, 4, 7};
            case '3' -> new int[]{6, 1, 3, 1, 6};
            case '4' -> new int[]{5, 5, 7, 1, 1};
            case '5' -> new int[]{7, 4, 6, 1, 6};
            case '6' -> new int[]{3, 4, 7, 5, 7};
            case '7' -> new int[]{7, 1, 2, 2, 2};
            case '8' -> new int[]{7, 5, 7, 5, 7};
            case '9' -> new int[]{7, 5, 7, 1, 6};
            default -> new int[]{0, 0, 0, 0, 0};
        };
    }

    private static void outline(int x, int y, int width, int height, int color) {
        fill(x, y, width, 1, color);
        fill(x, y + height - 1, width, 1, color);
        fill(x, y, 1, height, color);
        fill(x + width - 1, y, 1, height, color);
    }

    private static void fill(int x, int y, int width, int height, int color) {
        image.fillRect(x, y, width, height, color);
    }

    private static int color(int red, int green, int blue) {
        return 0xFF000000 | blue << 16 | green << 8 | red;
    }

    private static void ensureTexture() {
        if (texture != null) {
            return;
        }
        try {
            image = Minecraft.getInstance().getResourceManager().getResource(BASE_TEXTURE)
                    .map(resource -> {
                        try (var stream = resource.open()) {
                            return NativeImage.read(stream);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .orElseGet(() -> new NativeImage(128, 128, true));
        } catch (RuntimeException exception) {
            image = new NativeImage(128, 128, true);
        }
        texture = new DynamicTexture(image);
        Minecraft.getInstance().getTextureManager().register(DYNAMIC_TEXTURE, texture);
    }
}
