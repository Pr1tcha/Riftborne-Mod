package com.pr1tcha.Rifts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pr1tcha.Rifts.RiftData.RiftBlockEntity;
import com.pr1tcha.Rifts.RiftData.RiftStage;
import com.pr1tcha.Rifts.RiftborneRift;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RiftBlockEntityRenderer implements BlockEntityRenderer<RiftBlockEntity> {
    private static final ResourceLocation STORY_RIFT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RiftborneRift.MODID,
            "textures/effect/story_rift.png"
    );
    private static final ResourceLocation HAZE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RiftborneRift.MODID,
            "textures/effect/story_rift_haze.png"
    );
    public RiftBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(RiftBlockEntity rift, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (rift.getData().useProceduralVisual) {
            ProceduralRiftRenderer.render(rift, partialTick, poseStack, buffer);
            return;
        }

        float age = getAge(rift, partialTick);
        RiftStage stage = rift.getData().stage;
        float stageScale = getStageScale(stage);
        float pulse = 1.0F + Mth.sin(age * 0.14F) * getPulseAmount(stage);
        float shiver = Mth.sin(age * 0.47F) * getTurbulence(stage);
        float height = 2.72F * stageScale * pulse;
        float width = 0.92F * stageScale * (1.0F + Mth.sin(age * 0.09F) * 0.05F);
        float alpha = getAlpha(stage);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.02F, 0.5F);

        poseStack.translate(0.0F, 0.08F, 0.0F);
        faceCamera(poseStack);
        renderDistortionHaze(poseStack, buffer, width, height, alpha, age, shiver, stage);
        renderVoidCore(poseStack, buffer, width, height * 1.04F, alpha, shiver);
        renderEnergySkin(poseStack, buffer, width, height, alpha, age, shiver);
        renderFilaments(poseStack, buffer, width, height, alpha, age, stage);

        poseStack.popPose();
    }

    private static float getAge(RiftBlockEntity rift, float partialTick) {
        if (rift.getLevel() == null) {
            return partialTick;
        }

        return rift.getLevel().getGameTime() + partialTick;
    }

    private static float getStageScale(RiftStage stage) {
        return switch (stage) {
            case OPENING -> 0.62F;
            case ACTIVE -> 1.0F;
            case UNSTABLE -> 1.18F;
            case COLLAPSING -> 1.34F;
            case SCAR -> 0.36F;
        };
    }

    private static float getPulseAmount(RiftStage stage) {
        return switch (stage) {
            case OPENING -> 0.05F;
            case ACTIVE -> 0.09F;
            case UNSTABLE -> 0.17F;
            case COLLAPSING -> 0.24F;
            case SCAR -> 0.025F;
        };
    }

    private static float getTurbulence(RiftStage stage) {
        return switch (stage) {
            case OPENING -> 0.025F;
            case ACTIVE -> 0.045F;
            case UNSTABLE -> 0.085F;
            case COLLAPSING -> 0.13F;
            case SCAR -> 0.015F;
        };
    }

    private static float getAlpha(RiftStage stage) {
        return switch (stage) {
            case OPENING -> 0.62F;
            case ACTIVE -> 0.9F;
            case UNSTABLE -> 1.0F;
            case COLLAPSING -> 0.96F;
            case SCAR -> 0.34F;
        };
    }

    private static void renderVoidCore(PoseStack poseStack, MultiBufferSource buffer, float width, float height, float alpha, float shiver) {
        renderTexturedLayer(poseStack, buffer, solidRiftRenderType(), width, height, 1.0F, 0.0F, shiver, 18, 3, 31);
    }

    private static void renderEnergySkin(PoseStack poseStack, MultiBufferSource buffer, float width, float height, float alpha, float age, float shiver) {
        RenderType glow = RenderType.entityTranslucentEmissive(STORY_RIFT_TEXTURE);
        renderTexturedLayer(poseStack, buffer, glow, width * 0.92F, height * 1.02F, alpha * 0.42F, 0.0F, shiver, 224, 68, 255);
        renderTexturedLayer(poseStack, buffer, glow, width * 0.58F, height * 1.08F, alpha * 0.22F, 0.0F, -shiver * 0.65F, 170, 40, 255);
        renderTexturedLayer(poseStack, buffer, glow, width * 1.08F, height * 0.98F, alpha * 0.1F, 0.0F, shiver * 0.35F + Mth.sin(age * 0.11F) * 0.025F, 95, 18, 180);
    }

    private static void renderDistortionHaze(PoseStack poseStack, MultiBufferSource buffer, float width, float height, float alpha, float age, float shiver, RiftStage stage) {
        float stageAlpha = switch (stage) {
            case OPENING -> 0.38F;
            case ACTIVE -> 0.48F;
            case UNSTABLE -> 0.72F;
            case COLLAPSING -> 0.85F;
            case SCAR -> 0.18F;
        };
        RenderType haze = RenderType.entityTranslucentEmissive(HAZE_TEXTURE);
        float breath = 1.0F + Mth.sin(age * 0.08F) * 0.04F;

        renderTexturedLayer(poseStack, buffer, haze, width * 1.9F * breath, height * 1.12F, alpha * stageAlpha * 1.35F, 0.0F, shiver * 0.4F, 142, 122, 210);
        renderTexturedLayer(poseStack, buffer, haze, width * 1.48F, height * 1.18F, alpha * stageAlpha * 0.78F, 0.0F, -shiver * 0.5F, 205, 190, 255);
    }

    private static void renderFilaments(PoseStack poseStack, MultiBufferSource buffer, float width, float height, float alpha, float age, RiftStage stage) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        float half = width / 2.0F;
        int filaments = stage == RiftStage.OPENING || stage == RiftStage.SCAR ? 3 : 6;
        float stageBoost = stage == RiftStage.UNSTABLE || stage == RiftStage.COLLAPSING ? 1.3F : 1.0F;

        for (int i = 0; i < filaments; i++) {
            float side = i % 2 == 0 ? -1.0F : 1.0F;
            float seed = i * 1.73F;
            float y0 = height * (0.08F + i * 0.105F);
            float y1 = Mth.clamp(y0 + height * (0.26F + (i % 3) * 0.035F), height * 0.12F, height * 0.98F);
            float x0 = side * (half * (0.42F + (i % 3) * 0.08F)) + Mth.sin(age * 0.18F + seed) * 0.045F;
            float x1 = side * (half * (0.24F + (i % 2) * 0.1F)) + Mth.sin(age * 0.31F + seed) * 0.075F;
            float flicker = 0.55F + Mth.sin(age * (0.55F + i * 0.08F) + seed) * 0.35F;
            int filamentAlpha = Mth.clamp((int) (alpha * flicker * 180.0F), 35, 210);

            renderFilamentSegment(consumer, pose, x0, y0, x1, y1, 0.014F * stageBoost, 140, 48, 255, filamentAlpha);

            if (i % 2 == 0 && stage != RiftStage.OPENING && stage != RiftStage.SCAR) {
                float branchY = Mth.lerp(0.56F, y0, y1);
                float branchX = Mth.lerp(0.56F, x0, x1);
                renderFilamentSegment(consumer, pose, branchX, branchY, branchX - side * half * 0.22F, branchY + height * 0.08F, 0.009F * stageBoost, 235, 220, 255, filamentAlpha);
            }
        }
    }

    private static void renderFilamentSegment(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float thickness, int red, int green, int blue, int alpha) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length <= 0.0001F) {
            return;
        }

        float nx = -dy / length * thickness;
        float ny = dx / length * thickness;
        float z = 0.028F;

        vertex(consumer, pose, x0 - nx, y0 - ny, z, red, green, blue, alpha);
        vertex(consumer, pose, x0 + nx, y0 + ny, z, 245, 225, 255, alpha);
        vertex(consumer, pose, x1 + nx, y1 + ny, z, 245, 225, 255, alpha);
        vertex(consumer, pose, x1 - nx, y1 - ny, z, red, green, blue, alpha);
    }

    private static void renderTexturedLayer(PoseStack poseStack, MultiBufferSource buffer, RenderType renderType, float width, float height, float alpha, float rotation, float shiver, int red, int green, int blue) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        VertexConsumer consumer = buffer.getBuffer(renderType);
        PoseStack.Pose pose = poseStack.last();
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        float half = width / 2.0F;
        float topLean = shiver * 0.65F;
        float waist = shiver * -0.35F;

        vertex(consumer, pose, -half + waist, 0.0F, 0.0F, 0.0F, 1.0F, red, green, blue, a);
        vertex(consumer, pose, half + waist, 0.0F, 0.0F, 1.0F, 1.0F, red, green, blue, a);
        vertex(consumer, pose, half * 0.72F + topLean, height, 0.0F, 1.0F, 0.0F, red, green, blue, a);
        vertex(consumer, pose, -half * 0.72F + topLean, height, 0.0F, 0.0F, 0.0F, red, green, blue, a);

        poseStack.popPose();
    }

    private static RenderType solidRiftRenderType() {
        return RenderType.entityCutoutNoCull(STORY_RIFT_TEXTURE);
    }

    private static void faceCamera(PoseStack poseStack) {
        poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha);
    }

    @Override
    public boolean shouldRenderOffScreen(RiftBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 160;
    }

}
