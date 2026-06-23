package com.pr1tcha.riftborne.riftwalker.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pr1tcha.riftborne.riftwalker.item.RiftwalkerArmorItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class RiftwalkerGlowLayer extends AutoGlowingGeoLayer<RiftwalkerArmorItem> {
    public RiftwalkerGlowLayer(RiftwalkerArmorRenderer renderer) {
        super(renderer);
    }

    @Override
    public void render(
            PoseStack poseStack,
            RiftwalkerArmorItem animatable,
            BakedGeoModel bakedModel,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay
    ) {
        RiftwalkerArmorRenderer renderer = (RiftwalkerArmorRenderer) getRenderer();
        if (RiftwalkerArmorItem.maskLightsEnabled(renderer.getCurrentStack())) {
            super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer,
                    partialTick, packedLight, packedOverlay);
        }
    }
}
