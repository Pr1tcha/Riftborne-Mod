package com.pr1tcha.riftborne.codex.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.codex.item.CodexLaptopItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class CodexLaptopItemRenderer extends GeoItemRenderer<CodexLaptopItem> {
    private static final ResourceLocation INVENTORY_ICON =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "textures/item/codex_laptop.png");

    public CodexLaptopItemRenderer() {
        super(new CodexLaptopItemModel());
        withScale(0.78F);
        useAlternateGuiLighting();
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
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
        // GUI item transforms use the lower-left corner as their local origin.
        // Centering the quad around zero shifted half of the icon outside its slot.
        poseStack.translate(0.06F, 0.06F, 0.0F);
        poseStack.scale(0.88F, 0.88F, 0.88F);
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
