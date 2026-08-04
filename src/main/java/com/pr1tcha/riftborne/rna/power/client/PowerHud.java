package com.pr1tcha.riftborne.rna.power.client;

import com.pr1tcha.riftborne.rna.power.DeltaAxis;
import com.pr1tcha.riftborne.rna.power.HoldType;
import com.pr1tcha.riftborne.rna.power.LoadCalculator;
import com.pr1tcha.riftborne.rna.power.PowerRules;
import com.pr1tcha.riftborne.rna.power.Primitive;
import com.pr1tcha.riftborne.rna.power.data.ModPowerAttachments;
import com.pr1tcha.riftborne.rna.power.data.RNAProfile;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The power HUD. Its job is to answer one question at a glance: <em>can I afford the next cast?</em>
 *
 * <p>A single bar carries the whole cost model — the filled part is accumulated meta-wear (paid
 * forever), so the empty part <em>is</em> the admissibility window. On top of it sit two markers: a
 * projection of where the armed primitive's load would land, and the throughput line past which a
 * cast starts overloading the nodes. If the projection crosses the end of the window, the cast will
 * be compensated, and the bar says so before the key is pressed.
 */
public final class PowerHud {
    private static final int BAR_WIDTH = 132;
    private static final int BAR_HEIGHT = 7;
    private static final int MARGIN_X = 10;
    private static final int BOTTOM_OFFSET = 58;

    private static final int COLOR_FRAME = 0xFF2A3340;
    private static final int COLOR_TRACK = 0xCC0B1017;
    private static final int COLOR_WINDOW = 0xFF2FA8B4;
    private static final int COLOR_WEAR = 0xFFC2506A;
    private static final int COLOR_WEAR_CRIT = 0xFFE8556E;
    private static final int COLOR_PROJECTION_OK = 0xFFE8D48A;
    private static final int COLOR_PROJECTION_BAD = 0xFFE8556E;
    private static final int COLOR_THROUGHPUT = 0x88FFFFFF;
    private static final int COLOR_TEXT = 0xFFE6ECF2;
    private static final int COLOR_MUTED = 0xFF7C8797;
    private static final int COLOR_ACCENT = 0xFF9B87F5;

    private PowerHud() {
    }

    public static void render(GuiGraphics graphics, Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        RNAProfile profile = minecraft.player.getData(ModPowerAttachments.RNA_PROFILE.get());
        if (!profile.active()) {
            return;
        }

        Primitive armed = PowerClientState.selected();
        Set<DeltaAxis> axes = PowerRules.defaultAxes(armed);
        int depth = PowerRules.depthLevel(profile, armed);
        float projected = LoadCalculator.totalLoad(axes, depth, HoldType.MOMENT);
        float window = PowerRules.admissibilityWindow(profile);
        boolean fits = projected <= window
                && LoadCalculator.connectivityAllows(axes.size(), profile.connectivity());

        int x = MARGIN_X;
        int y = graphics.guiHeight() - BOTTOM_OFFSET;

        drawHeader(graphics, minecraft, profile, x, y - 11);
        drawBar(graphics, profile, projected, window, x, y);
        drawArmed(graphics, minecraft, armed, axes, projected, depth, fits, x, y + BAR_HEIGHT + 4);

        if (PowerClientState.hasFeedback()) {
            drawFeedback(graphics, minecraft, x, y + BAR_HEIGHT + 15);
        }
    }

    private static void drawHeader(GuiGraphics graphics, Minecraft minecraft, RNAProfile profile, int x, int y) {
        graphics.drawString(minecraft.font, Component.translatable("hud.riftborne.power.title"),
                x, y, COLOR_MUTED, false);
        Component cClass = Component.translatable("hud.riftborne.power.c_class", profile.connectivity());
        int cWidth = minecraft.font.width(cClass);
        graphics.drawString(minecraft.font, cClass, x + BAR_WIDTH - cWidth, y, COLOR_ACCENT, false);
    }

    private static void drawBar(GuiGraphics graphics, RNAProfile profile, float projected, float window,
                                int x, int y) {
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, COLOR_FRAME);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, COLOR_TRACK);

        // Window occupies everything the wear has not eaten yet.
        int windowEnd = x + scale(window);
        graphics.fill(x, y, windowEnd, y + BAR_HEIGHT, COLOR_WINDOW);

        // Wear is charged from the right: what is gone stays gone.
        int wearStart = x + BAR_WIDTH - scale(profile.metaWear());
        boolean critical = profile.metaWear() >= 75.0F;
        graphics.fill(Math.max(x, wearStart), y, x + BAR_WIDTH, y + BAR_HEIGHT,
                critical ? COLOR_WEAR_CRIT : COLOR_WEAR);

        // Throughput line: past this a cast overloads the nodes even when it is admissible.
        int throughputX = x + scale((float) profile.throughput());
        graphics.fill(throughputX, y - 2, throughputX + 1, y + BAR_HEIGHT + 2, COLOR_THROUGHPUT);

        // Projection of the armed primitive.
        int projectionX = x + Math.min(BAR_WIDTH - 1, scale(projected));
        int projectionColor = projected <= window ? COLOR_PROJECTION_OK : COLOR_PROJECTION_BAD;
        graphics.fill(projectionX - 1, y - 3, projectionX + 2, y + BAR_HEIGHT + 3, projectionColor);
    }

    private static void drawArmed(GuiGraphics graphics, Minecraft minecraft, Primitive armed,
                                  Set<DeltaAxis> axes, float projected, int depth, boolean fits,
                                  int x, int y) {
        Component name = Component.translatable(armed.translationKey());
        graphics.drawString(minecraft.font, name, x, y, fits ? COLOR_TEXT : COLOR_PROJECTION_BAD, false);

        StringBuilder axisLabel = new StringBuilder();
        for (DeltaAxis axis : axes) {
            if (axisLabel.length() > 0) {
                axisLabel.append('+');
            }
            axisLabel.append(axis.name().replace("DST", "dSt").replace("D", "d"));
        }
        Component right = Component.translatable("hud.riftborne.power.cast_info",
                axisLabel.toString(), depth, Math.round(projected));
        int rightWidth = minecraft.font.width(right);
        graphics.drawString(minecraft.font, right, x + BAR_WIDTH - rightWidth, y, COLOR_MUTED, false);
    }

    private static void drawFeedback(GuiGraphics graphics, Minecraft minecraft, int x, int y) {
        String key = "hud.riftborne.power.result." + PowerClientState.lastResult().toLowerCase(java.util.Locale.ROOT);
        int alpha = (int) (PowerClientState.feedbackAlpha() * 255.0F) << 24;
        boolean bad = !"OK".equals(PowerClientState.lastResult());
        int color = alpha | ((bad ? COLOR_PROJECTION_BAD : COLOR_WINDOW) & 0x00FFFFFF);
        graphics.drawString(minecraft.font, Component.translatable(key), x, y, color, false);
    }

    private static int scale(float value) {
        float clamped = Math.max(0.0F, Math.min(RNAProfile.MAX_META_WEAR, value));
        return Math.round(clamped / RNAProfile.MAX_META_WEAR * BAR_WIDTH);
    }
}
