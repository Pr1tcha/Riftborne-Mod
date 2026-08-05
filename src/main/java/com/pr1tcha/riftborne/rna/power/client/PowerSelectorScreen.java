package com.pr1tcha.riftborne.rna.power.client;

import com.pr1tcha.riftborne.rna.power.DeltaAxis;
import com.pr1tcha.riftborne.rna.power.HoldType;
import com.pr1tcha.riftborne.rna.power.LoadCalculator;
import com.pr1tcha.riftborne.rna.power.PowerRules;
import com.pr1tcha.riftborne.rna.power.Primitive;
import com.pr1tcha.riftborne.rna.power.data.ModPowerAttachments;
import com.pr1tcha.riftborne.rna.power.data.RNAProfile;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Radial picker for the nine primitives. Beyond choosing, it doubles as the system's readout: each
 * spoke is coloured by whether that primitive is castable right now, and the centre spells out the
 * hovered one's cost — Δ-families, depth and projected load against the current window.
 */
public final class PowerSelectorScreen extends Screen {
    // Laid out on an ellipse rather than a circle: nine spokes need horizontal room for the
    // canonical primitive names, but the vertical extent has to stay inside a scaled-up GUI.
    private static final int RADIUS_X = 170;
    private static final int RADIUS_Y = 110;
    private static final int SPOKE_BOX_W = 104;
    private static final int SPOKE_BOX_H = 18;

    private static final int COLOR_BACKDROP = 0xC0060910;
    private static final int COLOR_PANEL = 0xE00A0F16;
    private static final int COLOR_BORDER = 0xFF2A3340;
    private static final int COLOR_BORDER_SEL = 0xFF9B87F5;
    private static final int COLOR_TEXT = 0xFFE6ECF2;
    private static final int COLOR_MUTED = 0xFF7C8797;
    private static final int COLOR_OK = 0xFF2FA8B4;
    private static final int COLOR_BAD = 0xFFE8556E;

    private Primitive hovered = PowerClientState.selected();

    public PowerSelectorScreen() {
        super(Component.translatable("screen.riftborne.power_selector"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // A flat dark backdrop instead of the vanilla screen background: the default path runs the
        // blur post-effect over the world, which read as a mushy, out-of-focus mess behind the
        // spokes. The selector has no child widgets, so it draws everything itself.
        graphics.fill(0, 0, width, height, COLOR_BACKDROP);

        int cx = width / 2;
        int cy = height / 2;
        Primitive[] all = Primitive.values();
        hovered = pick(mouseX, mouseY, cx, cy, all);

        RNAProfile profile = minecraft == null || minecraft.player == null
                ? RNAProfile.empty()
                : minecraft.player.getData(ModPowerAttachments.RNA_PROFILE.get());
        float window = PowerRules.admissibilityWindow(profile);

        for (int i = 0; i < all.length; i++) {
            double angle = angleFor(i, all.length);
            int bx = cx + (int) Math.round(Math.cos(angle) * RADIUS_X) - SPOKE_BOX_W / 2;
            int by = cy + (int) Math.round(Math.sin(angle) * RADIUS_Y) - SPOKE_BOX_H / 2;
            drawSpoke(graphics, all[i], profile, window, bx, by, all[i] == hovered);
        }

        drawCentre(graphics, profile, window, cx, cy);
    }

    private void drawSpoke(GuiGraphics graphics, Primitive primitive, RNAProfile profile, float window,
                           int x, int y, boolean selected) {
        boolean castable = castable(primitive, profile, window);
        graphics.fill(x - 1, y - 1, x + SPOKE_BOX_W + 1, y + SPOKE_BOX_H + 1,
                selected ? COLOR_BORDER_SEL : COLOR_BORDER);
        graphics.fill(x, y, x + SPOKE_BOX_W, y + SPOKE_BOX_H, COLOR_PANEL);

        String name = font.plainSubstrByWidth(
                Component.translatable(primitive.translationKey()).getString(), SPOKE_BOX_W - 10);
        graphics.drawString(font, name, x + 5, y + 5,
                castable ? (selected ? COLOR_TEXT : COLOR_MUTED) : COLOR_BAD, false);
    }

    private void drawCentre(GuiGraphics graphics, RNAProfile profile, float window, int cx, int cy) {
        Set<DeltaAxis> axes = PowerRules.defaultAxes(hovered);
        int depth = PowerRules.depthLevel(profile, hovered);
        float load = LoadCalculator.totalLoad(axes, depth, HoldType.MOMENT);
        boolean castable = castable(hovered, profile, window);

        Component title = Component.translatable(hovered.translationKey());
        Component cost = Component.translatable("hud.riftborne.power.cast_info_full",
                axisLabel(axes), depth, Math.round(load));
        Component windowLine = Component.translatable("screen.riftborne.power_selector.window",
                Math.round(window), profile.connectivity());

        graphics.drawCenteredString(font, title, cx, cy - 14, castable ? COLOR_TEXT : COLOR_BAD);
        graphics.drawCenteredString(font, cost, cx, cy - 2, castable ? COLOR_OK : COLOR_BAD);
        graphics.drawCenteredString(font, windowLine, cx, cy + 10, COLOR_MUTED);
    }

    private boolean castable(Primitive primitive, RNAProfile profile, float window) {
        Set<DeltaAxis> axes = PowerRules.defaultAxes(primitive);
        if (!LoadCalculator.connectivityAllows(axes.size(), profile.connectivity())) {
            return false;
        }
        int depth = PowerRules.depthLevel(profile, primitive);
        return LoadCalculator.totalLoad(axes, depth, HoldType.MOMENT) <= window;
    }

    private static String axisLabel(Set<DeltaAxis> axes) {
        StringBuilder builder = new StringBuilder();
        for (DeltaAxis axis : axes) {
            if (builder.length() > 0) {
                builder.append('+');
            }
            builder.append(axis.name().replace("DST", "dSt").replace("D", "d"));
        }
        return builder.toString();
    }

    /**
     * Nearest spoke to the cursor, measured against where the spokes are actually drawn. The
     * layout is elliptical, so matching on angle alone would drift away from the visible boxes.
     * Resting near the centre keeps the current pick rather than snapping around.
     */
    private Primitive pick(int mouseX, int mouseY, int cx, int cy, Primitive[] all) {
        int dx = mouseX - cx;
        int dy = mouseY - cy;
        if (dx * dx + dy * dy < 30 * 30) {
            return hovered;
        }
        Primitive best = all[0];
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < all.length; i++) {
            double angle = angleFor(i, all.length);
            double sx = cx + Math.cos(angle) * RADIUS_X;
            double sy = cy + Math.sin(angle) * RADIUS_Y;
            double distance = (mouseX - sx) * (mouseX - sx) + (mouseY - sy) * (mouseY - sy);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = all[i];
            }
        }
        return best;
    }

    private static double angleFor(int index, int count) {
        return -Math.PI / 2.0D + index * (2.0D * Math.PI / count);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            confirm();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            int index = keyCode - GLFW.GLFW_KEY_1;
            Primitive[] all = Primitive.values();
            if (index < all.length) {
                hovered = all[index];
                confirm();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void confirm() {
        PowerClientState.select(hovered);
        onClose();
    }
}
