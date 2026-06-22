package com.pr1tcha.riftborne.codex.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.codex.data.PocketCodexData;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class PocketCodexDynamicDisplay {
    private static final ResourceLocation BASE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "textures/block/pocket_codex.png");
    private static final ResourceLocation DYNAMIC_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "dynamic/pocket_codex_display");
    private static final String[] SCREEN_NAMES = {"HOME", "SCAN", "NOTES", "SIGNAL", "ACT"};
    private static final int SCREEN_X = 4;
    private static final int SCREEN_Y = 68;
    private static final int SCREEN_WIDTH = 56;
    private static final int SCREEN_HEIGHT = 56;

    private static DynamicTexture texture;
    private static NativeImage image;
    private static int lastSignature = Integer.MIN_VALUE;

    private PocketCodexDynamicDisplay() {
    }

    public static ResourceLocation texture() {
        ensureTexture();
        return DYNAMIC_TEXTURE;
    }

    public static void update(ItemStack stack) {
        ensureTexture();
        int screen = PocketCodexData.selectedScreen(stack);
        int entries = PocketCodexData.shortEntries(stack).size();
        int queued = PocketCodexData.queuedEntries(stack).size();
        int damaged = PocketCodexData.damagedEntries(stack).size();
        int signature = screen | entries << 4 | queued << 12 | damaged << 20;
        if (signature == lastSignature) {
            return;
        }
        lastSignature = signature;

        fill(SCREEN_X, SCREEN_Y, SCREEN_WIDTH, SCREEN_HEIGHT, color(2, 10, 12));
        fill(SCREEN_X + 1, SCREEN_Y + 1, SCREEN_WIDTH - 2, 8, color(9, 34, 35));
        drawText(SCREEN_NAMES[screen], SCREEN_X + 3, SCREEN_Y + 2, color(78, 237, 207));
        drawNumber(entries, SCREEN_X + 45, SCREEN_Y + 2, color(132, 255, 226));
        fill(SCREEN_X + 2, SCREEN_Y + 11, SCREEN_WIDTH - 4, 1, color(33, 105, 96));

        switch (screen) {
            case 1 -> drawScanner(entries);
            case 2 -> drawNotes(entries);
            case 3 -> drawSignals(queued);
            case 4 -> drawActivity(entries, queued, damaged);
            default -> drawHome(entries, queued, damaged);
        }
        drawNavigation(screen);
        texture.upload();
    }

    private static void drawNavigation(int selected) {
        for (int index = 0; index < 5; index++) {
            int x = SCREEN_X + 3 + index * 10;
            fill(x, SCREEN_Y + 50, 7, 3,
                    index == selected ? color(80, 239, 207) : color(18, 65, 62));
        }
    }

    private static void drawHome(int entries, int queued, int damaged) {
        drawText("DATA", SCREEN_X + 4, SCREEN_Y + 16, color(49, 159, 143));
        drawNumber(entries, SCREEN_X + 40, SCREEN_Y + 16, color(106, 255, 218));
        drawText("QUEUE", SCREEN_X + 4, SCREEN_Y + 27, color(49, 159, 143));
        drawNumber(queued, SCREEN_X + 44, SCREEN_Y + 27, color(106, 255, 218));
        drawText("DMG", SCREEN_X + 4, SCREEN_Y + 38,
                damaged > 0 ? color(224, 74, 81) : color(49, 159, 143));
        drawNumber(damaged, SCREEN_X + 40, SCREEN_Y + 38,
                damaged > 0 ? color(255, 99, 105) : color(106, 255, 218));
    }

    private static void drawScanner(int entries) {
        outline(SCREEN_X + 7, SCREEN_Y + 15, 42, 28, color(50, 198, 177));
        fill(SCREEN_X + 27, SCREEN_Y + 15, 2, 28, color(22, 92, 84));
        fill(SCREEN_X + 7, SCREEN_Y + 28, 42, 2, color(22, 92, 84));
        int pulse = 4 + Math.floorMod(entries, 14);
        fill(SCREEN_X + 12 + pulse, SCREEN_Y + 22, 4, 4, color(92, 255, 218));
        drawText("TARGET", SCREEN_X + 15, SCREEN_Y + 44, color(50, 164, 148));
    }

    private static void drawNotes(int entries) {
        int rows = Math.max(1, Math.min(4, entries));
        for (int row = 0; row < rows; row++) {
            int y = SCREEN_Y + 16 + row * 8;
            fill(SCREEN_X + 4, y, 4, 4, color(70, 220, 194));
            fill(SCREEN_X + 11, y, 36 - row * 4, 2, color(28, 100 + row * 18, 94));
            fill(SCREEN_X + 11, y + 4, 25 - row * 2, 1, color(18, 66, 63));
        }
    }

    private static void drawSignals(int queued) {
        int centerX = SCREEN_X + 28;
        int centerY = SCREEN_Y + 29;
        fill(centerX - 1, centerY - 1, 3, 3, color(93, 255, 218));
        for (int ring = 1; ring <= 3; ring++) {
            int strength = queued >= ring ? 220 : 75;
            outline(centerX - ring * 7, centerY - ring * 5, ring * 14 + 2, ring * 10 + 2,
                    color(40, strength, Math.max(0, strength - 20)));
        }
    }

    private static void drawActivity(int entries, int queued, int damaged) {
        int baseline = SCREEN_Y + 44;
        int[] values = {entries + 2, queued + 3, damaged + 1, entries + queued + 2, damaged + 4};
        for (int index = 0; index < values.length; index++) {
            int height = Math.min(28, values[index] * 4);
            fill(SCREEN_X + 5 + index * 10, baseline - height, 6, height,
                    index == 2 && damaged > 0 ? color(226, 77, 83) : color(48, 181, 159));
        }
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
            case 'C' -> new int[]{3, 4, 4, 4, 3};
            case 'D' -> new int[]{6, 5, 5, 5, 6};
            case 'E' -> new int[]{7, 4, 6, 4, 7};
            case 'G' -> new int[]{3, 4, 5, 5, 3};
            case 'H' -> new int[]{5, 5, 7, 5, 5};
            case 'I' -> new int[]{7, 2, 2, 2, 7};
            case 'L' -> new int[]{4, 4, 4, 4, 7};
            case 'M' -> new int[]{5, 7, 7, 5, 5};
            case 'N' -> new int[]{5, 7, 7, 7, 5};
            case 'O' -> new int[]{2, 5, 5, 5, 2};
            case 'Q' -> new int[]{2, 5, 5, 7, 3};
            case 'R' -> new int[]{6, 5, 6, 5, 5};
            case 'S' -> new int[]{3, 4, 2, 1, 6};
            case 'T' -> new int[]{7, 2, 2, 2, 2};
            case 'U' -> new int[]{5, 5, 5, 5, 7};
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
