package com.pr1tcha.Rifts.client;

import com.pr1tcha.Rifts.RiftborneRift;
import com.pr1tcha.Rifts.entity.RiftSplinterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class RiftSplinterRenderer extends EntityRenderer<RiftSplinterEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RiftborneRift.MODID,
            "textures/entity/rift_splinter.png"
    );

    public RiftSplinterRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.35F;
    }

    @Override
    public void render(RiftSplinterEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.translate(-0.275D, 0.0D, -0.275D);
        poseStack.scale(0.55F, 1.15F, 0.55F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.CRYING_OBSIDIAN.defaultBlockState(),
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();

        renderShard(entity.tickCount + partialTick, 0.28D, 0.95D, 0.0D, 0.16F, 0.48F, poseStack, buffer, packedLight);
        renderShard(-(entity.tickCount + partialTick), -0.22D, 0.72D, 0.14D, 0.12F, 0.38F, poseStack, buffer, packedLight);
        renderShard((entity.tickCount + partialTick) * 0.6F, 0.0D, 1.28D, -0.18D, 0.10F, 0.32F, poseStack, buffer, packedLight);

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void renderShard(float rotation, double x, double y, double z, float width, float height, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation * 6.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(18.0F));
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        poseStack.scale(width, height, width);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.TINTED_GLASS.defaultBlockState(),
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(RiftSplinterEntity entity) {
        return TEXTURE;
    }
}
