package com.pr1tcha.Rifts.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pr1tcha.Rifts.RiftData.RiftBlockEntity;
import com.pr1tcha.Rifts.RiftData.RiftStage;
import com.pr1tcha.Rifts.RiftborneRift;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.forge.event.ForgeVeilPostProcessingEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class VeilRiftDistortion {
    private static final ResourceLocation PIPELINE = ResourceLocation.fromNamespaceAndPath(RiftborneRift.MODID, "rift_distortion");
    private static final double FADE_START_DISTANCE = 42.0D;
    private static final double FADE_END_DISTANCE = 72.0D;
    private static boolean registered;
    private static boolean pipelineRequested;
    private static long visibleFrame = Long.MIN_VALUE;
    private static float visibleScore = Float.POSITIVE_INFINITY;
    private static float minX = 0.0F;
    private static float minY = 0.0F;
    private static float maxX = 0.0F;
    private static float maxY = 0.0F;
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

    public static void recordRenderedRift(RiftBlockEntity rift, PoseStack.Pose pose, float height, float baseWidth, float alpha) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.getWindow().getHeight() <= 0) {
            return;
        }
        if (rift.getData().stage == RiftStage.SCAR) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 riftCenter = Vec3.atLowerCornerOf(rift.getBlockPos()).add(0.5D, 0.1D + height * 0.5D, 0.5D);
        double distance = camera.getPosition().distanceTo(riftCenter);
        float distanceFade = distanceFade(distance);
        if (distanceFade <= 0.0F) {
            return;
        }

        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.pose());
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        ScreenBounds bounds = new ScreenBounds();

        for (int i = 0; i <= 8; i++) {
            float t = i / 8.0F;
            float envelope = Mth.clamp(0.16F + Mth.sin(t * Mth.PI) * 1.08F, 0.16F, 1.0F);
            float y = height * t;
            float halfWidth = baseWidth * envelope * 1.55F;

            includeProjected(bounds, modelView, projection, -halfWidth, y, 0.0F);
            includeProjected(bounds, modelView, projection, halfWidth, y, 0.0F);
            includeProjected(bounds, modelView, projection, 0.0F, y, 0.0F);
        }

        if (!bounds.hasPoints() || bounds.maxX() < -0.04F || bounds.minX() > 1.04F || bounds.maxY() < -0.04F || bounds.minY() > 1.04F) {
            return;
        }

        long frame = minecraft.level.getGameTime();
        if (frame != visibleFrame) {
            visibleScore = Float.POSITIVE_INFINITY;
        }

        float projectedX = (bounds.minX() + bounds.maxX()) * 0.5F;
        float projectedY = (bounds.minY() + bounds.maxY()) * 0.5F;
        float projectedSize = Math.max(bounds.maxX() - bounds.minX(), bounds.maxY() - bounds.minY());
        float screenDistance = (float) Mth.length(projectedX - 0.5F, projectedY - 0.5F);
        float score = screenDistance - projectedSize * 0.35F;
        if (score < visibleScore) {
            visibleFrame = frame;
            visibleScore = score;
            minX = Mth.clamp(bounds.minX(), 0.0F, 1.0F);
            minY = Mth.clamp(bounds.minY(), 0.0F, 1.0F);
            maxX = Mth.clamp(bounds.maxX(), 0.0F, 1.0F);
            maxY = Mth.clamp(bounds.maxY(), 0.0F, 1.0F);
            strength = Math.min(alpha * distanceFade * 0.95F, 0.95F);
        }
    }

    private static void includeProjected(ScreenBounds bounds, Matrix4f modelView, Matrix4f projection, float x, float y, float z) {
        Vector4f projected = new Vector4f(x, y, z, 1.0F);
        modelView.transform(projected);
        projection.transform(projected);
        if (projected.w() <= 0.0001F) {
            return;
        }

        float ndcX = projected.x() / projected.w();
        float ndcY = projected.y() / projected.w();
        bounds.include(ndcX * 0.5F + 0.5F, ndcY * 0.5F + 0.5F);
    }

    private static float distanceFade(double distance) {
        if (distance <= FADE_START_DISTANCE) {
            return 1.0F;
        }
        if (distance >= FADE_END_DISTANCE) {
            return 0.0F;
        }

        float t = (float) ((distance - FADE_START_DISTANCE) / (FADE_END_DISTANCE - FADE_START_DISTANCE));
        t = Mth.clamp(t, 0.0F, 1.0F);
        return 1.0F - t * t * (3.0F - 2.0F * t);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            strength = 0.0F;
            return;
        }

        time += 0.05F;
        ensurePipeline();
        if (minecraft.level.getGameTime() - visibleFrame > 2L) {
            strength *= 0.82F;
            if (strength < 0.01F) {
                strength = 0.0F;
            }
        }
    }

    @SubscribeEvent
    public static void onVeilPostProcessing(ForgeVeilPostProcessingEvent.Pre event) {
        if (!PIPELINE.equals(event.getName())) {
            return;
        }

        PostPipeline pipeline = event.getPipeline();
        pipeline.getUniformSafe("RiftParams").setVector(minX, minY, maxX, maxY);
        pipeline.getUniformSafe("RiftStrength").setFloat(strength);
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

    private static final class ScreenBounds {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private boolean hasPoints;

        void include(float x, float y) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            hasPoints = true;
        }

        boolean hasPoints() {
            return hasPoints;
        }

        float minX() {
            return minX;
        }

        float minY() {
            return minY;
        }

        float maxX() {
            return maxX;
        }

        float maxY() {
            return maxY;
        }
    }
}
