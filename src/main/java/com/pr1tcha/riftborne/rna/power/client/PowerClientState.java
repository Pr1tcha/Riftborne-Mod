package com.pr1tcha.riftborne.rna.power.client;

import com.pr1tcha.riftborne.rna.power.Primitive;
import com.pr1tcha.riftborne.rna.power.network.PowerNetwork;

/**
 * Client-side view state for the power HUD: which primitive is armed, and the outcome of the last
 * cast (kept briefly so the HUD can explain a refusal). The profile itself is read straight from the
 * synced Data Attachment, so nothing is mirrored here.
 */
public final class PowerClientState {
    private static final int FEEDBACK_TICKS = 60;

    private static Primitive selected = Primitive.P1_READING;
    private static String lastResult = "";
    private static float lastLoad;
    private static boolean lastOverload;
    private static int feedbackTicks;

    private PowerClientState() {
    }

    public static Primitive selected() {
        return selected;
    }

    public static void select(Primitive primitive) {
        if (primitive != null) {
            selected = primitive;
        }
    }

    /** Move the armed primitive by {@code delta} positions through the fixed primitive order. */
    public static void cycle(int delta) {
        Primitive[] all = Primitive.values();
        int index = Math.floorMod(selected.ordinal() + delta, all.length);
        selected = all[index];
    }

    public static void handleFeedback(PowerNetwork.CastFeedbackPayload payload) {
        Primitive primitive = Primitive.fromId(payload.primitiveId());
        if (primitive != null) {
            selected = primitive;
        }
        lastResult = payload.result();
        lastLoad = payload.load();
        lastOverload = payload.overload();
        feedbackTicks = FEEDBACK_TICKS;
    }

    public static void tick() {
        if (feedbackTicks > 0) {
            feedbackTicks--;
        }
    }

    public static void clear() {
        lastResult = "";
        feedbackTicks = 0;
    }

    public static boolean hasFeedback() {
        return feedbackTicks > 0 && !lastResult.isBlank();
    }

    public static String lastResult() {
        return lastResult;
    }

    public static float lastLoad() {
        return lastLoad;
    }

    public static boolean lastOverload() {
        return lastOverload;
    }

    /** 0..1 fade used to soften the feedback line as it expires. */
    public static float feedbackAlpha() {
        return Math.min(1.0F, feedbackTicks / 20.0F);
    }
}
