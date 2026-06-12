package com.pr1tcha.Rifts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pr1tcha.Rifts.RiftData.RiftBlockEntity;
import com.pr1tcha.Rifts.RiftData.RiftStage;
import com.pr1tcha.Rifts.RiftborneRift;
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

    public RiftBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(RiftBlockEntity rift, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        float age = getAge(rift, partialTick);
        RiftStage stage = rift.getData().stage;
        float stageScale = getStageScale(stage);
        float pulse = 1.0F + Mth.sin(age * 0.16F) * getPulseAmount(stage);
        float height = 2.6F * stageScale * pulse;
        float width = 1.05F * stageScale * (1.0F + Mth.sin(age * 0.11F) * 0.06F);
        float alpha = getAlpha(stage);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.05F, 0.5F);

        renderTexturedLayer(poseStack, buffer, width, height, alpha, 0.0F, 255, 70, 255);
        renderTexturedLayer(poseStack, buffer, width * 0.72F, height * 1.08F, alpha * 0.72F, 90.0F, 170, 35, 255);
        renderTexturedLayer(poseStack, buffer, width * 1.18F, height * 0.95F, alpha * 0.42F, 45.0F + age * 0.45F, 95, 20, 180);

        if (stage == RiftStage.UNSTABLE || stage == RiftStage.COLLAPSING) {
            renderLightningSlash(poseStack, buffer, width * 0.42F, height, age);
        }

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
            case OPENING -> 0.55F;
            case ACTIVE -> 1.0F;
            case UNSTABLE -> 1.22F;
            case COLLAPSING -> 1.35F;
            case SCAR -> 0.35F;
        };
    }

    private static float getPulseAmount(RiftStage stage) {
        return switch (stage) {
            case OPENING -> 0.04F;
            case ACTIVE -> 0.08F;
            case UNSTABLE -> 0.16F;
            case COLLAPSING -> 0.24F;
            case SCAR -> 0.02F;
        };
    }

    private static float getAlpha(RiftStage stage) {
        return switch (stage) {
            case OPENING -> 0.55F;
            case ACTIVE -> 0.86F;
            case UNSTABLE -> 1.0F;
            case COLLAPSING -> 0.92F;
            case SCAR -> 0.35F;
        };
    }

    private static void renderTexturedLayer(PoseStack poseStack, MultiBufferSource buffer, float width, float height, float alpha, float rotation, int red, int green, int blue) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(STORY_RIFT_TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        float half = width / 2.0F;

        vertex(consumer, pose, -half, 0.0F, 0.0F, 0.0F, 1.0F, red, green, blue, a);
        vertex(consumer, pose, half, 0.0F, 0.0F, 1.0F, 1.0F, red, green, blue, a);
        vertex(consumer, pose, half, height, 0.0F, 1.0F, 0.0F, red, green, blue, a);
        vertex(consumer, pose, -half, height, 0.0F, 0.0F, 0.0F, red, green, blue, a);

        poseStack.popPose();
    }

    private static void renderLightningSlash(PoseStack poseStack, MultiBufferSource buffer, float width, float height, float age) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        float flicker = 0.75F + Mth.sin(age * 0.7F) * 0.25F;
        int alpha = Mth.clamp((int) (210.0F * flicker), 80, 255);
        float half = width / 2.0F;
        float z = 0.015F;

        vertex(consumer, pose, -half * 0.25F, 0.05F, z, 175, 80, 255, alpha);
        vertex(consumer, pose, half * 0.10F, 0.12F, z, 245, 225, 255, alpha);
        vertex(consumer, pose, half * 0.32F, height * 0.95F, z, 245, 225, 255, alpha);
        vertex(consumer, pose, -half * 0.02F, height, z, 175, 80, 255, alpha);
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
        return 128;
    }
}
