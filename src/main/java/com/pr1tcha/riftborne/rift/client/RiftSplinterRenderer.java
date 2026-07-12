package com.pr1tcha.riftborne.rift.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.rift.entity.RiftSplinterEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

public class RiftSplinterRenderer extends GeoEntityRenderer<RiftSplinterEntity> {
    private boolean renderingOverlay;

    public RiftSplinterRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rift_splinter")
        ));
        withScale(0.512F);
        this.shadowRadius = 0.35F;
        addRenderLayer(new TranslucentOverlayLayer(this));
    }

    @Override
    public RenderType getRenderType(RiftSplinterEntity entity, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, int packedLight, int packedOverlay, int renderColor) {
        if (bone.isHidden()) {
            return;
        }

        for (GeoCube cube : bone.getCubes()) {
            boolean overlayCube = cube.inflate() > 0.0D;
            if (overlayCube != renderingOverlay) {
                continue;
            }

            poseStack.pushPose();
            renderCube(poseStack, cube, buffer, packedLight, packedOverlay, renderColor);
            poseStack.popPose();
        }
    }

    private static final class TranslucentOverlayLayer extends GeoRenderLayer<RiftSplinterEntity> {
        private final RiftSplinterRenderer renderer;

        private TranslucentOverlayLayer(RiftSplinterRenderer renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void render(PoseStack poseStack, RiftSplinterEntity entity, BakedGeoModel bakedModel,
                           RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                           float partialTick, int packedLight, int packedOverlay) {
            RenderType translucent = RenderType.entityTranslucent(getTextureResource(entity));
            renderer.renderingOverlay = true;

            try {
                renderer.reRender(
                        bakedModel,
                        poseStack,
                        bufferSource,
                        entity,
                        translucent,
                        bufferSource.getBuffer(translucent),
                        partialTick,
                        packedLight,
                        packedOverlay,
                        Color.WHITE.argbInt()
                );
            } finally {
                renderer.renderingOverlay = false;
            }
        }
    }
}
