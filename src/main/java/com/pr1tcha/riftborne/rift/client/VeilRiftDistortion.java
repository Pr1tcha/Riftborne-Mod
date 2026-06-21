package com.pr1tcha.riftborne.rift.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pr1tcha.riftborne.config.Config;
import com.pr1tcha.riftborne.rift.block.RiftBlockEntity;
import com.pr1tcha.riftborne.rift.data.RiftData;
import com.pr1tcha.riftborne.rift.data.RiftStage;
import com.pr1tcha.riftborne.rift.RiftType;
import com.pr1tcha.riftborne.Riftborne;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.forge.event.ForgeVeilPostProcessingEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public final class VeilRiftDistortion {
    private static final ResourceLocation PIPELINE = ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rift_distortion");
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
    private static float mode = 0.0F;
    private static float time;
    private static final List<DeferredRift> DEFERRED_RIFTS = new ArrayList<>();

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
        if (!hasLineOfSight(minecraft, camera.getPosition(), riftCenter, height)) {
            return;
        }

        double distance = camera.getPosition().distanceTo(riftCenter);
        float distanceFade = distanceFade(distance);
        if (distanceFade <= 0.0F) {
            return;
        }

        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.pose());
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        ScreenBounds bounds = new ScreenBounds();
        boolean contourRift = RiftData.isContourRift(rift.getData().riftType);

        if (contourRift) {
            includeContourRiftBounds(bounds, modelView, projection, height);
        } else {
            for (int i = 0; i <= 8; i++) {
                float t = i / 8.0F;
                float envelope = Mth.clamp(0.16F + Mth.sin(t * Mth.PI) * 1.08F, 0.16F, 1.0F);
                float y = height * t;
                float halfWidth = baseWidth * envelope * 1.55F;

                includeProjected(bounds, modelView, projection, -halfWidth, y, 0.0F);
                includeProjected(bounds, modelView, projection, halfWidth, y, 0.0F);
                includeProjected(bounds, modelView, projection, 0.0F, y, 0.0F);
            }
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
            float openingPulse = openingPulse(rift);
            float contourPulse = contourRift ? openingPulse : 0.0F;
            float padX = contourRift ? Math.max(0.018F, (bounds.maxX() - bounds.minX()) * (0.06F + contourPulse * 0.22F)) : (bounds.maxX() - bounds.minX()) * 0.34F * openingPulse;
            float padY = contourRift ? Math.max(0.014F, (bounds.maxY() - bounds.minY()) * (0.06F + contourPulse * 0.22F)) : (bounds.maxY() - bounds.minY()) * 0.34F * openingPulse;
            visibleFrame = frame;
            visibleScore = score;
            minX = Mth.clamp(bounds.minX() - padX, 0.0F, 1.0F);
            minY = Mth.clamp(bounds.minY() - padY, 0.0F, 1.0F);
            maxX = Mth.clamp(bounds.maxX() + padX, 0.0F, 1.0F);
            maxY = Mth.clamp(bounds.maxY() + padY, 0.0F, 1.0F);
            strength = Math.min(alpha * distanceFade * (contourRift ? 1.08F + contourPulse * 0.34F : 0.95F + openingPulse * 0.45F), 1.0F);
            mode = contourRift ? 1.0F : 0.0F;
        }
    }

    public static boolean deferRenderedRift(RiftBlockEntity rift, float partialTick) {
        if (!registered || !pipelineRequested) {
            return false;
        }

        DEFERRED_RIFTS.add(new DeferredRift(rift, partialTick));
        return true;
    }

    private static boolean hasLineOfSight(Minecraft minecraft, Vec3 cameraPos, Vec3 riftCenter, float height) {
        if (minecraft.level == null || cameraPos.distanceToSqr(riftCenter) < 1.0D) {
            return true;
        }

        double halfHeight = height * 0.5D;
        return isPointVisible(minecraft, cameraPos, riftCenter)
                || isPointVisible(minecraft, cameraPos, riftCenter.add(0.0D, halfHeight * 0.62D, 0.0D))
                || isPointVisible(minecraft, cameraPos, riftCenter.add(0.0D, -halfHeight * 0.62D, 0.0D));
    }

    private static boolean isPointVisible(Minecraft minecraft, Vec3 from, Vec3 to) {
        HitResult hit = minecraft.level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                minecraft.cameraEntity
        ));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(to) < 0.09D;
    }

    private static float openingPulse(RiftBlockEntity rift) {
        if (rift.getData().stage != RiftStage.OPENING) {
            return 0.0F;
        }

        float progress = Mth.clamp(rift.getData().stageTicks / (float) Config.riftOpeningDurationTicks.get(), 0.0F, 1.0F);
        float fadeIn = smoothstep(Mth.clamp((progress - 0.58F) / 0.08F, 0.0F, 1.0F));
        float fadeOut = 1.0F - smoothstep(Mth.clamp((progress - 0.8F) / 0.16F, 0.0F, 1.0F));
        return fadeIn * fadeOut;
    }

    private static float smoothstep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static void includeContourRiftBounds(ScreenBounds bounds, Matrix4f modelView, Matrix4f projection, float height) {
        ScreenPoint bottom = project(modelView, projection, 0.0F, 0.0F, 0.0F);
        ScreenPoint middle = project(modelView, projection, 0.0F, height * 0.5F, 0.0F);
        ScreenPoint top = project(modelView, projection, 0.0F, height, 0.0F);
        if (bottom == null || middle == null || top == null) {
            return;
        }

        float screenHeight = Math.max(0.001F, Math.abs(top.y() - bottom.y()));
        float halfWidth = screenHeight * 0.34F;
        float halfHeight = screenHeight * 0.53F;
        bounds.include(middle.x() - halfWidth, middle.y() - halfHeight);
        bounds.include(middle.x() + halfWidth, middle.y() + halfHeight);
        bounds.include(middle.x(), middle.y());
    }

    private static void includeProjected(ScreenBounds bounds, Matrix4f modelView, Matrix4f projection, float x, float y, float z) {
        ScreenPoint point = project(modelView, projection, x, y, z);
        if (point != null) {
            bounds.include(point.x(), point.y());
        }
    }

    private static ScreenPoint project(Matrix4f modelView, Matrix4f projection, float x, float y, float z) {
        Vector4f projected = new Vector4f(x, y, z, 1.0F);
        modelView.transform(projected);
        projection.transform(projected);
        if (projected.w() <= 0.0001F) {
            return null;
        }

        float ndcX = projected.x() / projected.w();
        float ndcY = projected.y() / projected.w();
        return new ScreenPoint(ndcX * 0.5F + 0.5F, ndcY * 0.5F + 0.5F);
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
        if (minecraft.level.getGameTime() - visibleFrame > 0L) {
            strength *= 0.45F;
            if (strength < 0.01F) {
                strength = 0.0F;
                mode = 0.0F;
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
        pipeline.getUniformSafe("RiftMode").setFloat(mode);
        pipeline.getUniformSafe("RiftTime").setFloat(time);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || DEFERRED_RIFTS.isEmpty()) {
            return;
        }

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        Vec3 cameraPos = event.getCamera().getPosition();
        var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.set(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        try {
            for (DeferredRift deferred : DEFERRED_RIFTS) {
                if (deferred.rift().isRemoved() || deferred.rift().getLevel() != Minecraft.getInstance().level) {
                    continue;
                }

                PoseStack poseStack = new PoseStack();
                poseStack.translate(
                        deferred.rift().getBlockPos().getX() - cameraPos.x,
                        deferred.rift().getBlockPos().getY() - cameraPos.y,
                        deferred.rift().getBlockPos().getZ() - cameraPos.z
                );
                ProceduralRiftRenderer.renderDeferred(deferred.rift(), deferred.partialTick(), poseStack, buffer);
            }
            buffer.endBatch();
        } finally {
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
            DEFERRED_RIFTS.clear();
        }
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

    private record ScreenPoint(float x, float y) {
    }

    private record DeferredRift(RiftBlockEntity rift, float partialTick) {
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
