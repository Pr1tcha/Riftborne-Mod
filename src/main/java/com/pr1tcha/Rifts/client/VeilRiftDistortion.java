package com.pr1tcha.Rifts.client;

import com.pr1tcha.Rifts.RiftData.RiftBlockEntity;
import com.pr1tcha.Rifts.RiftborneRift;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.forge.event.ForgeVeilPostProcessingEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

public final class VeilRiftDistortion {
    private static final ResourceLocation PIPELINE = ResourceLocation.fromNamespaceAndPath(RiftborneRift.MODID, "rift_distortion");
    private static final double VERTICAL_FOV_RADIANS = Math.toRadians(70.0D);
    private static boolean registered;
    private static boolean pipelineRequested;
    private static long visibleFrame = Long.MIN_VALUE;
    private static float centerX = 0.5F;
    private static float centerY = 0.5F;
    private static float radius = 0.0F;
    private static float strength = 0.0F;
    private static float time;

    private VeilRiftDistortion() {
    }

    public static void registerIfPresent() {
        if (registered || !ModList.get().isLoaded("veil")) {
            return;
        }

        registered = true;
        NeoForge.EVENT_BUS.register(VeilRiftDistortion.class);
    }

    public static void recordRift(RiftBlockEntity rift, float height, float baseWidth, float alpha) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.getWindow().getHeight() <= 0) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vec3 world = Vec3.atLowerCornerOf(rift.getBlockPos()).add(0.5D, 0.1D + height * 0.52D, 0.5D);
        Vec3 delta = world.subtract(cameraPos);
        Vector3f look = camera.getLookVector();
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        double depth = delta.x * look.x() + delta.y * look.y() + delta.z * look.z();
        if (depth <= 0.35D) {
            return;
        }

        double leftDistance = delta.x * left.x() + delta.y * left.y() + delta.z * left.z();
        double upDistance = delta.x * up.x() + delta.y * up.y() + delta.z * up.z();
        double aspect = minecraft.getWindow().getWidth() / (double) minecraft.getWindow().getHeight();
        double tan = Math.tan(VERTICAL_FOV_RADIANS * 0.5D);
        float projectedX = (float) (0.5D - leftDistance / (depth * tan * aspect) * 0.5D);
        float projectedY = (float) (0.5D - upDistance / (depth * tan) * 0.5D);
        if (projectedX < -0.35F || projectedX > 1.35F || projectedY < -0.35F || projectedY > 1.35F) {
            return;
        }

        float projectedRadius = (float) ((height + baseWidth * 2.0F) / (depth * tan) * 0.18D);
        projectedRadius = Math.max(0.035F, Math.min(projectedRadius, 0.42F));
        long frame = minecraft.level.getGameTime();
        if (frame != visibleFrame || projectedRadius > radius) {
            visibleFrame = frame;
            centerX = projectedX;
            centerY = projectedY;
            radius = projectedRadius;
            strength = Math.min(alpha * 0.82F, 0.92F);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            strength = 0.0F;
            radius = 0.0F;
            return;
        }

        time += 0.05F;
        ensurePipeline();
        if (minecraft.level.getGameTime() - visibleFrame > 2L) {
            strength *= 0.82F;
            radius *= 0.9F;
            if (strength < 0.01F) {
                strength = 0.0F;
                radius = 0.0F;
            }
        }
    }

    @SubscribeEvent
    public static void onVeilPostProcessing(ForgeVeilPostProcessingEvent.Pre event) {
        if (!PIPELINE.equals(event.getName())) {
            return;
        }

        PostPipeline pipeline = event.getPipeline();
        pipeline.getUniformSafe("RiftParams").setVector(centerX, centerY, radius, strength);
        pipeline.getUniformSafe("RiftTime").setFloat(time);
    }

    private static void ensurePipeline() {
        if (pipelineRequested) {
            return;
        }

        try {
            PostProcessingManager manager = VeilRenderSystem.renderer().getPostProcessingManager();
            pipelineRequested = manager.add(PIPELINE);
        } catch (RuntimeException ignored) {
            pipelineRequested = false;
        }
    }
}
