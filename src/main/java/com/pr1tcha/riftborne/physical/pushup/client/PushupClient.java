package com.pr1tcha.riftborne.physical.pushup.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.physical.pushup.PhysicalTrainingNetwork;
import com.pr1tcha.riftborne.physical.pushup.PushupPhase;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class PushupClient {
    private static final float POSE_STEP = 0.18F;
    private static final Map<UUID, ClientState> STATES = new HashMap<>();
    private static final KeyMapping ACTION_KEY = new KeyMapping(
            "key.riftborne.pushup_action",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_SPACE,
            "key.categories.riftborne"
    );
    private static int inputSequence;
    private static boolean exitKeyWasDown;

    private PushupClient() {
    }

    public static void handleState(PhysicalTrainingNetwork.PushupStatePayload payload) {
        if (!payload.active()) {
            STATES.remove(payload.playerId());
            return;
        }
        ClientState state = STATES.computeIfAbsent(payload.playerId(), ignored -> new ClientState());
        state.update(
                PushupPhase.fromOrdinal(payload.phaseOrdinal()),
                Math.max(0, payload.stateTicks()),
                Math.max(0, payload.repetitions()),
                Math.max(0, payload.failures()),
                payload.bodyYaw()
        );
    }

    public static boolean isActive(UUID playerId) {
        return STATES.containsKey(playerId);
    }

    public static PushupPhase phase(UUID playerId) {
        ClientState state = STATES.get(playerId);
        return state == null ? PushupPhase.IDLE : state.phase;
    }

    private static void sendAction(boolean stop) {
        PacketDistributor.sendToServer(new PhysicalTrainingNetwork.PushupInputPayload(++inputSequence, stop));
    }

    public static PoseSample pose(UUID playerId, float partialTick) {
        ClientState state = STATES.get(playerId);
        if (state == null) {
            return PoseSample.NONE;
        }
        float lowering = Mth.lerp(
                Mth.clamp(partialTick, 0.0F, 1.0F),
                state.previousVisualLowering,
                state.visualLowering
        );
        lowering = lowering * lowering * (3.0F - 2.0F * lowering);
        return new PoseSample(true, lowering, state.bodyYaw);
    }

    @EventBusSubscriber(modid = Riftborne.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(ACTION_KEY);
        }
    }

    @EventBusSubscriber(modid = Riftborne.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            STATES.values().forEach(ClientState::tick);
            if (minecraft.player == null || minecraft.screen != null || !isActive(minecraft.player.getUUID())) {
                exitKeyWasDown = false;
                return;
            }
            boolean exitKeyDown = minecraft.options.keyShift.isDown();
            if (exitKeyDown && !exitKeyWasDown) {
                sendAction(true);
            }
            exitKeyWasDown = exitKeyDown;
            while (ACTION_KEY.consumeClick()) {
                sendAction(false);
            }
        }

        @SubscribeEvent
        public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null
                    || minecraft.screen != null
                    || !isActive(minecraft.player.getUUID())
                    || (!event.isAttack() && !event.isUseItem())) {
                return;
            }
            sendAction(false);
            event.setSwingHand(false);
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.options.hideGui) {
                return;
            }
            ClientState state = STATES.get(minecraft.player.getUUID());
            if (state == null) {
                return;
            }

            GuiGraphics graphics = event.getGuiGraphics();
            int width = 176;
            int x = (graphics.guiWidth() - width) / 2;
            int y = 14;
            graphics.fill(x, y, x + width, y + 49, 0xB0040A0D);
            graphics.drawCenteredString(
                    minecraft.font,
                    Component.translatable("hud.riftborne.pushup.title", state.repetitions),
                    graphics.guiWidth() / 2,
                    y + 5,
                    0xFFEAF7F5
            );

            int phaseColor = state.phase == PushupPhase.LOWER ? 0xFF65F0C7 : 0xFFA5C4C8;
            graphics.drawCenteredString(
                    minecraft.font,
                    state.phase == PushupPhase.LOWER
                            ? Component.translatable(state.phase.translationKey())
                            : Component.translatable(state.phase.translationKey(), state.stateTicks),
                    graphics.guiWidth() / 2,
                    y + 18,
                    phaseColor
            );
            graphics.drawCenteredString(
                    minecraft.font,
                    Component.translatable(state.phase == PushupPhase.LOWER
                            ? "hud.riftborne.pushup.input"
                            : "hud.riftborne.pushup.auto"),
                    graphics.guiWidth() / 2,
                    y + 29,
                    0xFF7FAEB6
            );
            graphics.drawCenteredString(
                    minecraft.font,
                    Component.translatable("hud.riftborne.pushup.exit"),
                    graphics.guiWidth() / 2,
                    y + 40,
                    0xFF76858A
            );
        }
    }

    public record PoseSample(boolean active, float lowering, float bodyYaw) {
        public static final PoseSample NONE = new PoseSample(false, 0.0F, 0.0F);
    }

    private static final class ClientState {
        private PushupPhase phase = PushupPhase.PREPARE;
        private int stateTicks;
        private int repetitions;
        private int failures;
        private float bodyYaw;
        private float previousVisualLowering;
        private float visualLowering;

        private void update(PushupPhase phase, int stateTicks, int repetitions, int failures, float bodyYaw) {
            this.phase = phase;
            this.stateTicks = stateTicks;
            this.repetitions = repetitions;
            this.failures = failures;
            this.bodyYaw = bodyYaw;
        }

        private void tick() {
            previousVisualLowering = visualLowering;
            float target = phase == PushupPhase.PREPARE
                    || phase == PushupPhase.LOWER
                    || phase == PushupPhase.REST
                    ? 1.0F
                    : 0.0F;
            visualLowering += Mth.clamp(target - visualLowering, -POSE_STEP, POSE_STEP);
            if (stateTicks > 0) {
                stateTicks--;
            }
        }
    }
}
