package com.pr1tcha.riftborne.codex.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.codex.item.PocketCodexItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class PocketCodexItemRenderer extends GeoItemRenderer<PocketCodexItem> {
    private static final ResourceLocation INVENTORY_ICON =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "textures/item/pocket_codex.png");

    public PocketCodexItemRenderer() {
        super(new PocketCodexItemModel());
        useAlternateGuiLighting();
        addRenderLayer(new PocketCodexEmissiveLayer(this));
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

    private static final class PocketCodexEmissiveLayer extends GeoRenderLayer<PocketCodexItem> {
        private PocketCodexEmissiveLayer(PocketCodexItemRenderer renderer) {
            super(renderer);
        }

        @Override
        public void renderForBone(
                PoseStack poseStack,
                PocketCodexItem animatable,
                GeoBone bone,
                RenderType renderType,
                MultiBufferSource bufferSource,
                VertexConsumer buffer,
                float partialTick,
                int packedLight,
                int packedOverlay
        ) {
            if (!bone.getName().equals("screen_glow")
                    && !bone.getName().equals("indicator_glow")
                    && !bone.getName().equals("scan_bar_glow")) {
                return;
            }
            RenderType emissive = RenderType.eyes(PocketCodexDynamicDisplay.texture());
            getRenderer().renderCubesOfBone(
                    poseStack,
                    bone,
                    bufferSource.getBuffer(emissive),
                    LightTexture.FULL_BRIGHT,
                    packedOverlay,
                    0xFFFFFFFF
            );
        }
    }
}
