package com.pr1tcha.riftborne.flight.client;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.flight.FlightNetwork;
import com.pr1tcha.riftborne.flight.RiftFlightController;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FlightClient {
    private static final int DOUBLE_JUMP_WINDOW_TICKS = 7;

    private static final Map<Integer, VisualState> VISUAL_STATES = new HashMap<>();
    private static boolean active;
    private static boolean jumpWasDown;
    private static int lastJumpTapTick = -DOUBLE_JUMP_WINDOW_TICKS;
    private static int lastSentInput = -1;

    private FlightClient() {
    }

    public static void setActiveFromServer(boolean activeFromServer) {
        active = activeFromServer;
        lastSentInput = -1;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            if (!activeFromServer) {
                setVisualState(minecraft.player.getId(), FlightNetwork.VISUAL_INACTIVE, 0.0F, 0.0F, 0.0F, 0.0F);
            }
            minecraft.player.displayClientMessage(Component.translatable(
                    active ? "message.riftborne.flight_enabled" : "message.riftborne.flight_disabled"
            ), true);
        }
    }

    public static void setVisualState(int entityId, byte phase, float boost, float motionX, float motionY, float motionZ) {
        if (phase != FlightNetwork.VISUAL_INACTIVE) {
            VISUAL_STATES.computeIfAbsent(entityId, id -> new VisualState())
                    .setTarget(phase, boost, new Vec3(motionX, motionY, motionZ));
        } else {
            VisualState state = VISUAL_STATES.get(entityId);
            if (state != null) {
                state.setTarget(FlightNetwork.VISUAL_INACTIVE, 0.0F, Vec3.ZERO);
            }
        }
    }

    public static VisualSample getVisualSample(Player player, float partialTick) {
        VisualState state = VISUAL_STATES.get(player.getId());
        return state == null ? VisualSample.NONE : state.sample(partialTick);
    }

    @EventBusSubscriber(modid = Riftborne.MODID, value = Dist.CLIENT)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null) {
                active = false;
                jumpWasDown = false;
                lastJumpTapTick = -DOUBLE_JUMP_WINDOW_TICKS;
                lastSentInput = -1;
                VISUAL_STATES.clear();
                return;
            }

            VISUAL_STATES.entrySet().removeIf(entry ->
                    minecraft.level.getEntity(entry.getKey()) == null || entry.getValue().tick()
            );
            handleDoubleJumpToggle(minecraft);

            if (!active) {
                return;
            }

            int input = minecraft.screen == null ? collectInput(minecraft) : 0;
            if (input != lastSentInput || minecraft.player.tickCount % 5 == 0) {
                PacketDistributor.sendToServer(new FlightNetwork.InputPayload(input));
                lastSentInput = input;
            }
        }
    }

    private static void handleDoubleJumpToggle(Minecraft minecraft) {
        boolean jumpDown = minecraft.screen == null && minecraft.options.keyJump.isDown();
        if (jumpDown && !jumpWasDown) {
            int now = minecraft.player.tickCount;
            if (now - lastJumpTapTick <= DOUBLE_JUMP_WINDOW_TICKS) {
                requestToggle(!active);
                lastJumpTapTick = -DOUBLE_JUMP_WINDOW_TICKS;
            } else {
                lastJumpTapTick = now;
            }
        }
        jumpWasDown = jumpDown;
    }

    private static void requestToggle(boolean nextActive) {
        active = nextActive;
        lastSentInput = -1;
        PacketDistributor.sendToServer(new FlightNetwork.TogglePayload(nextActive));
    }

    private static int collectInput(Minecraft minecraft) {
        int input = 0;
        if (minecraft.options.keyUp.isDown()) {
            input |= RiftFlightController.INPUT_FORWARD;
        }
        if (minecraft.options.keyDown.isDown()) {
            input |= RiftFlightController.INPUT_BACKWARD;
        }
        if (minecraft.options.keyLeft.isDown()) {
            input |= RiftFlightController.INPUT_LEFT;
        }
        if (minecraft.options.keyRight.isDown()) {
            input |= RiftFlightController.INPUT_RIGHT;
        }
        if (minecraft.options.keyJump.isDown()) {
            input |= RiftFlightController.INPUT_JUMP;
        }
        if (minecraft.options.keyShift.isDown()) {
            input |= RiftFlightController.INPUT_CROUCH;
        }
        if (minecraft.options.keySprint.isDown() || minecraft.player.isSprinting()) {
            input |= RiftFlightController.INPUT_SPRINT;
        }
        return input;
    }

    public record VisualSample(byte phase, float activity, float boost, Vec3 motion, float flightBlend) {
        public static final VisualSample NONE = new VisualSample(
                FlightNetwork.VISUAL_INACTIVE,
                0.0F,
                0.0F,
                Vec3.ZERO,
                0.0F
        );

        public boolean active() {
            return this.phase != FlightNetwork.VISUAL_INACTIVE && this.activity > 0.01F;
        }
    }

    private static final class VisualState {
        private byte phase = FlightNetwork.VISUAL_INACTIVE;
        private byte targetPhase = FlightNetwork.VISUAL_INACTIVE;
        private float prevActivity;
        private float activity;
        private float targetActivity;
        private float prevBoost;
        private float boost;
        private float targetBoost;
        private Vec3 prevMotion = Vec3.ZERO;
        private Vec3 motion = Vec3.ZERO;
        private Vec3 targetMotion = Vec3.ZERO;
        private float prevFlightBlend;
        private float flightBlend;

        private void setTarget(byte targetPhase, float targetBoost, Vec3 targetMotion) {
            this.targetPhase = targetPhase;
            this.targetActivity = targetPhase == FlightNetwork.VISUAL_INACTIVE ? 0.0F : 1.0F;
            this.targetBoost = targetBoost;
            this.targetMotion = targetMotion;
        }

        private boolean tick() {
            this.prevActivity = this.activity;
            this.prevBoost = this.boost;
            this.prevMotion = this.motion;
            this.prevFlightBlend = this.flightBlend;
            if (this.targetPhase != FlightNetwork.VISUAL_INACTIVE) {
                this.phase = this.targetPhase;
            }
            this.activity += (this.targetActivity - this.activity) * 0.35F;
            this.boost += (this.targetBoost - this.boost) * 0.35F;
            this.motion = this.motion.lerp(this.targetMotion, 0.35D);
            float targetFlightBlend = this.targetPhase == FlightNetwork.VISUAL_FLIGHT ? 1.0F : 0.0F;
            this.flightBlend += (targetFlightBlend - this.flightBlend) * 0.24F;
            if (this.targetPhase == FlightNetwork.VISUAL_INACTIVE && this.activity < 0.03F && this.flightBlend < 0.03F) {
                this.phase = FlightNetwork.VISUAL_INACTIVE;
                return true;
            }
            return false;
        }

        private VisualSample sample(float partialTick) {
            float sampledActivity = this.prevActivity + (this.activity - this.prevActivity) * partialTick;
            float sampledBoost = this.prevBoost + (this.boost - this.prevBoost) * partialTick;
            Vec3 sampledMotion = this.prevMotion.lerp(this.motion, partialTick);
            float sampledFlightBlend = this.prevFlightBlend + (this.flightBlend - this.prevFlightBlend) * partialTick;
            return new VisualSample(this.phase, sampledActivity, sampledBoost, sampledMotion, sampledFlightBlend);
        }
    }
}
