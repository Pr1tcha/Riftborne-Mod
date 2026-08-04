package com.pr1tcha.riftborne.rna.combat.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pr1tcha.riftborne.Riftborne;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.forge.event.ForgeVeilPostProcessingEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Veil post effect for the finite, gaze-aligned pre-aspect barrier plane.
 * Gameplay remains server-authoritative; this class only visualizes synchronized state.
 */
public final class VeilBarrierField {
    private static final ResourceLocation PIPELINE =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "barrier_field");
    private static final double MAX_RENDER_DISTANCE = 72.0D;
    private static final float FORWARD_DISTANCE = 1.25F;
    private static final float HALF_WIDTH = 1.48F;
    private static final float HALF_HEIGHT = 1.16F;
    private static final Map<UUID, BarrierVisualState> STATES = new HashMap<>();

    private static boolean registered;
    private static boolean pipelineRequested;
    private static float centerX;
    private static float centerY;
    private static float axisXx;
    private static float axisXy;
    private static float axisYx;
    private static float axisYy;
    private static float strength;
    private static float pulse;
    private static float time;

    private VeilBarrierField() {
    }

    public static void registerIfPresent() {
        if (registered || !ModList.get().isLoaded("veil")) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.register(VeilBarrierField.class);
    }

    public static void handleState(UUID playerId, float integrity) {
        if (playerId == null) {
            return;
        }
        if (integrity <= 0.0F) {
            BarrierVisualState state = STATES.get(playerId);
            if (state != null) {
                state.integrity = 0.0F;
                state.impactPulse = 1.0F;
                state.fading = true;
            }
            return;
        }

        BarrierVisualState state = STATES.get(playerId);
        if (state == null) {
            STATES.put(playerId, new BarrierVisualState(integrity, 0.38F));
            return;
        }
        if (integrity < state.integrity) {
            state.impactPulse = 1.0F;
        }
        state.integrity = integrity;
        state.fading = false;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            STATES.clear();
            strength = 0.0F;
            return;
        }

        time += 0.05F;
        ensurePipeline();
        STATES.values().forEach(state -> state.impactPulse *= 0.82F);
        STATES.entrySet().removeIf(entry -> entry.getValue().fading && entry.getValue().impactPulse < 0.025F);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || STATES.isEmpty()) {
            strength = 0.0F;
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPosition = camera.getPosition();
        Matrix4f modelView = event.getModelViewMatrix();
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Candidate best = null;

        for (Map.Entry<UUID, BarrierVisualState> entry : STATES.entrySet()) {
            Player player = minecraft.level.getPlayerByUUID(entry.getKey());
            if (player == null || !player.isAlive()) {
                continue;
            }
            double distance = cameraPosition.distanceTo(player.position());
            if (distance > MAX_RENDER_DISTANCE) {
                continue;
            }

            Candidate candidate = projectBarrier(
                    player,
                    entry.getValue(),
                    camera,
                    cameraPosition,
                    modelView,
                    projection,
                    partialTick,
                    distance
            );
            if (candidate != null && (best == null || candidate.score < best.score)) {
                best = candidate;
            }
        }

        if (best == null) {
            strength = 0.0F;
            return;
        }

        centerX = best.center.x;
        centerY = best.center.y;
        axisXx = best.axisX.x;
        axisXy = best.axisX.y;
        axisYx = best.axisY.x;
        axisYy = best.axisY.y;
        pulse = best.pulse;
        float integrityFactor = Mth.clamp(best.integrity / 16.0F, 0.15F, 1.0F);
        float distanceFade = 1.0F - Mth.clamp((float) (best.distance / MAX_RENDER_DISTANCE), 0.0F, 1.0F);
        strength = (0.10F + integrityFactor * 0.14F + pulse * 0.42F) * (0.35F + distanceFade * 0.65F);
    }

    @SubscribeEvent
    public static void onVeilPostProcessing(ForgeVeilPostProcessingEvent.Pre event) {
        if (!PIPELINE.equals(event.getName())) {
            return;
        }
        PostPipeline pipeline = event.getPipeline();
        pipeline.getUniformSafe("BarrierFrame0").setVector(centerX, centerY, axisXx, axisXy);
        pipeline.getUniformSafe("BarrierFrame1").setVector(axisYx, axisYy, strength, pulse);
        pipeline.getUniformSafe("BarrierTime").setFloat(time);
    }

    private static Candidate projectBarrier(
            Player player,
            BarrierVisualState state,
            Camera camera,
            Vec3 cameraPosition,
            Matrix4f modelView,
            Matrix4f projection,
            float partialTick,
            double distance
    ) {
        boolean cameraBound = camera.getEntity() == player && !camera.isDetached();
        Vec3 forward;
        Vec3 right;
        Vec3 up;
        Vec3 origin;
        if (cameraBound) {
            forward = new Vec3(camera.getLookVector()).normalize();
            right = new Vec3(camera.getLeftVector()).scale(-1.0D).normalize();
            up = new Vec3(camera.getUpVector()).normalize();
            origin = cameraPosition;
        } else {
            forward = player.getViewVector(partialTick).normalize();
            right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (right.lengthSqr() < 0.001D) {
                Vec3 yawForward = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick));
                right = yawForward.cross(new Vec3(0.0D, 1.0D, 0.0D));
            }
            right = right.normalize();
            up = right.cross(forward).normalize();
            origin = player.getEyePosition(partialTick);
        }
        Vec3 center = origin.add(forward.scale(FORWARD_DISTANCE));
        if (!hasLineOfSight(cameraPosition, center)) {
            return null;
        }

        ScreenPoint projectedCenter = project(center, cameraPosition, modelView, projection);
        ScreenPoint projectedRight = project(center.add(right.scale(HALF_WIDTH)), cameraPosition, modelView, projection);
        ScreenPoint projectedTop = project(center.add(up.scale(HALF_HEIGHT)), cameraPosition, modelView, projection);
        if (projectedCenter == null || projectedRight == null || projectedTop == null) {
            return null;
        }

        ScreenPoint axisX = projectedRight.subtract(projectedCenter);
        ScreenPoint axisY = projectedTop.subtract(projectedCenter);
        float determinant = axisX.x * axisY.y - axisX.y * axisY.x;
        if (Math.abs(determinant) < 0.00008F) {
            return null;
        }
        if (projectedCenter.x < -1.2F || projectedCenter.x > 2.2F
                || projectedCenter.y < -1.2F || projectedCenter.y > 2.2F) {
            return null;
        }

        float screenDistance = (float) Mth.length(projectedCenter.x - 0.5F, projectedCenter.y - 0.5F);
        float score = (float) distance + screenDistance * 12.0F;
        return new Candidate(projectedCenter, axisX, axisY, state.integrity, state.impactPulse, distance, score);
    }

    private static boolean hasLineOfSight(Vec3 cameraPosition, Vec3 center) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.cameraEntity == null) {
            return false;
        }
        HitResult hit = minecraft.level.clip(new ClipContext(
                cameraPosition,
                center,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                minecraft.cameraEntity
        ));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(center) < 0.12D;
    }

    private static ScreenPoint project(
            Vec3 worldPosition,
            Vec3 cameraPosition,
            Matrix4f modelView,
            Matrix4f projection
    ) {
        Vec3 relative = worldPosition.subtract(cameraPosition);
        Vector4f projected = new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1.0F);
        modelView.transform(projected);
        projection.transform(projected);
        if (projected.w <= 0.0001F) {
            return null;
        }
        float ndcX = projected.x / projected.w;
        float ndcY = projected.y / projected.w;
        return new ScreenPoint(ndcX * 0.5F + 0.5F, ndcY * 0.5F + 0.5F);
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

    private static final class BarrierVisualState {
        private float integrity;
        private float impactPulse;
        private boolean fading;

        private BarrierVisualState(float integrity, float impactPulse) {
            this.integrity = integrity;
            this.impactPulse = impactPulse;
        }
    }

    private record ScreenPoint(float x, float y) {
        private ScreenPoint subtract(ScreenPoint other) {
            return new ScreenPoint(x - other.x, y - other.y);
        }
    }

    private record Candidate(
            ScreenPoint center,
            ScreenPoint axisX,
            ScreenPoint axisY,
            float integrity,
            float pulse,
            double distance,
            float score
    ) {
    }
}
