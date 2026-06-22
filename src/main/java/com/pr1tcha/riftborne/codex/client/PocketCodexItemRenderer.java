package com.pr1tcha.riftborne.codex.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.codex.item.PocketCodexItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class PocketCodexItemRenderer extends GeoItemRenderer<PocketCodexItem> {
    private static final ResourceLocation INVENTORY_ICON =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "textures/item/pocket_codex.png");

    public PocketCodexItemRenderer() {
        super(new PocketCodexItemModel());
        withScale(0.68F);
        useAlternateGuiLighting();
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        PocketCodexDynamicDisplay.update(stack);
        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
    }

    @Override
    public void preRender(
            PoseStack poseStack,
            PocketCodexItem animatable,
            BakedGeoModel model,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int renderColor
    ) {
        if (renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            poseStack.translate(-0.2F, 0.2F, -0.18F);
            poseStack.scale(0.62F, 0.62F, 0.62F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-8.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-31.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(7.0F));
        } else if (renderPerspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            poseStack.translate(0.2F, 0.2F, -0.18F);
            poseStack.scale(0.62F, 0.62F, 0.62F);
            poseStack.mulPose(Axis.YP.rotationDegrees(8.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-31.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-7.0F));
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, renderColor);
    }

    @Override
    protected void renderInGui(
            ItemDisplayContext transformType,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            float partialTick
    ) {
        poseStack.pushPose();
        poseStack.translate(0.08F, 0.05F, 0.0F);
        poseStack.scale(0.84F, 0.9F, 0.84F);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(INVENTORY_ICON));
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, 0.0F, 0.0F, 0.1F, 0.0F, 1.0F, packedLight);
        vertex(consumer, pose, 1.0F, 0.0F, 0.1F, 1.0F, 1.0F, packedLight);
        vertex(consumer, pose, 1.0F, 1.0F, 0.1F, 1.0F, 0.0F, packedLight);
        vertex(consumer, pose, 0.0F, 1.0F, 0.1F, 0.0F, 0.0F, packedLight);
        poseStack.popPose();
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int packedLight
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }
}
