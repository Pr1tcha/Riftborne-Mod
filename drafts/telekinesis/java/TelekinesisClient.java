package com.pr1tcha.riftborne.aspects.telekinesis.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.aspects.telekinesis.TelekinesisNetwork;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class TelekinesisClient {
    private static final KeyMapping TELEKINESIS_KEY = new KeyMapping(
            "key.riftborne.telekinesis",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.riftborne"
    );
    private static boolean holdSent;
    private static boolean hasActiveGrab;

    private TelekinesisClient() {
    }

    public static void setHasActiveGrab(boolean active) {
        hasActiveGrab = active;
    }

    @EventBusSubscriber(modid = Riftborne.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TELEKINESIS_KEY);
        }
    }

    @EventBusSubscriber(modid = Riftborne.MODID, value = Dist.CLIENT)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null) {
                holdSent = false;
                hasActiveGrab = false;
                return;
            }

            boolean holding = minecraft.screen == null && TELEKINESIS_KEY.isDown();
            if (holding != holdSent) {
                PacketDistributor.sendToServer(new TelekinesisNetwork.HoldPayload(holding));
                holdSent = holding;
            }
        }

        @SubscribeEvent
        public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null || minecraft.screen != null || !TELEKINESIS_KEY.isDown()) {
                return;
            }

            double scroll = event.getScrollDeltaY();
            if (Math.abs(scroll) < 0.01D) {
                return;
            }

            PacketDistributor.sendToServer(new TelekinesisNetwork.DistancePayload((float) scroll));
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null || minecraft.getConnection() == null || minecraft.screen != null || !TELEKINESIS_KEY.isDown()) {
                return;
            }

            if (event.isAttack()) {
                PacketDistributor.sendToServer(new TelekinesisNetwork.PushPayload());
                event.setSwingHand(true);
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null || minecraft.screen != null || !TELEKINESIS_KEY.isDown() || hasActiveGrab) {
                return;
            }

            BlockHitResult hit = event.getTarget();
            BlockPos pos = hit.getBlockPos();
            BlockState state = minecraft.level.getBlockState(pos);
            if (state.isAir()
                    || state.getRenderShape() != RenderShape.MODEL
                    || minecraft.level.getBlockEntity(pos) != null
                    || state.getDestroySpeed(minecraft.level, pos) < 0.0F) {
                return;
            }

            Camera camera = event.getCamera();
            Vec3 cameraPos = camera.getPosition();
            VoxelShape shape = state.getShape(minecraft.level, pos);
            AABB bounds = shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);
            AABB outline = bounds.inflate(0.005D).move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            LevelRenderer.renderLineBox(
                    event.getPoseStack(),
                    event.getMultiBufferSource().getBuffer(RenderType.lines()),
                    outline,
                    0.05F,
                    1.0F,
                    0.9F,
                    1.0F
            );
            event.setCanceled(true);
        }
    }
}
