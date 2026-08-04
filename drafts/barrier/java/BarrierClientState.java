package com.pr1tcha.riftborne.rna.combat.client;

import com.pr1tcha.riftborne.rna.combat.RnaCombatNetwork;
import com.pr1tcha.riftborne.rna.combat.barrier.BarrierGestureMode;
import com.pr1tcha.riftborne.rna.combat.barrier.BarrierPhase;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/** Shared client state for barrier gameplay poses. Veil remains an optional consumer. */
public final class BarrierClientState {
    public static final int DEPLOY_TICKS = 5;
    private static final int RELEASE_TICKS = 4;
    private static final Map<UUID, State> STATES = new HashMap<>();

    private BarrierClientState() {
    }

    public static void handleState(RnaCombatNetwork.BarrierStatePayload payload) {
        UUID playerId = payload.playerId();
        BarrierPhase phase = BarrierPhase.fromOrdinal(payload.phaseOrdinal());
        BarrierGestureMode gesture = BarrierGestureMode.fromOrdinal(payload.gestureOrdinal());
        if (phase == BarrierPhase.INACTIVE) {
            State state = STATES.get(playerId);
            if (state != null) {
                state.phase = BarrierPhase.INACTIVE;
                state.phaseTick = 0;
                state.integrity = 0.0F;
            }
            return;
        }

        State state = STATES.computeIfAbsent(playerId, ignored -> new State());
        if (state.phase != phase) {
            state.phaseTick = Math.max(0, DEPLOY_TICKS - payload.phaseTicks());
        }
        state.phase = phase;
        state.gesture = gesture;
        state.integrity = Math.max(0.0F, payload.integrity());
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            STATES.clear();
            return;
        }
        STATES.values().forEach(state -> state.phaseTick++);
        STATES.entrySet().removeIf(entry -> {
            State state = entry.getValue();
            return state.phase == BarrierPhase.INACTIVE && state.phaseTick > RELEASE_TICKS;
        });
    }

    public static boolean occupiesArm(UUID playerId, HumanoidArm arm) {
        State state = STATES.get(playerId);
        return state != null
                && state.phase != BarrierPhase.INACTIVE
                && state.gesture.occupies(arm);
    }

    public static PoseSample sample(UUID playerId, float partialTick) {
        State state = STATES.get(playerId);
        if (state == null || state.gesture == BarrierGestureMode.HANDS_FREE) {
            return PoseSample.NONE;
        }

        float tick = state.phaseTick + partialTick;
        if (state.phase == BarrierPhase.DEPLOYING) {
            float pull = smoothStep(Mth.clamp(tick / 2.0F, 0.0F, 1.0F));
            if (tick < 2.0F) {
                return new PoseSample(state.gesture, pull, -pull);
            }
            float thrust = smoothStep(Mth.clamp((tick - 2.0F) / 3.0F, 0.0F, 1.0F));
            return new PoseSample(state.gesture, 1.0F, Mth.lerp(thrust, -1.0F, 1.0F));
        }
        if (state.phase == BarrierPhase.ACTIVE) {
            return new PoseSample(state.gesture, 1.0F, 1.0F);
        }

        float release = 1.0F - smoothStep(Mth.clamp(tick / RELEASE_TICKS, 0.0F, 1.0F));
        return new PoseSample(state.gesture, release, 1.0F);
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    public record PoseSample(BarrierGestureMode gesture, float weight, float drive) {
        public static final PoseSample NONE = new PoseSample(BarrierGestureMode.HANDS_FREE, 0.0F, 0.0F);

        public boolean occupies(HumanoidArm arm) {
            return weight > 0.0F && gesture.occupies(arm);
        }
    }

    private static final class State {
        private BarrierPhase phase = BarrierPhase.INACTIVE;
        private BarrierGestureMode gesture = BarrierGestureMode.TWO_HANDED;
        private int phaseTick;
        private float integrity;
    }
}
