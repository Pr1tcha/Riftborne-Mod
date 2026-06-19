package com.pr1tcha.riftborne.aspects.telekinesis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.aspects.telekinesis.entity.TelekineticBlockEntity;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.RenderTypeHelper;

public class TelekineticBlockRenderer extends EntityRenderer<TelekineticBlockEntity> {
    private final BlockRenderDispatcher dispatcher;

    public TelekineticBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.5F;
        dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(TelekineticBlockEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        BlockState blockState = entity.getBlockState();
        if (blockState.getRenderShape() == RenderShape.MODEL && blockState.getRenderShape() != RenderShape.INVISIBLE) {
            Level level = entity.level();
            poseStack.pushPose();
            BlockPos renderPos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
            poseStack.translate(-0.5D, 0.0D, -0.5D);

            var model = dispatcher.getBlockModel(blockState);
            for (var renderType : model.getRenderTypes(blockState, RandomSource.create(blockState.getSeed(renderPos)), ModelData.EMPTY)) {
                dispatcher.getModelRenderer().tesselateBlock(
                        level,
                        model,
                        blockState,
                        renderPos,
                        poseStack,
                        buffer.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType)),
                        false,
                        RandomSource.create(),
                        blockState.getSeed(renderPos),
                        OverlayTexture.NO_OVERLAY,
                        ModelData.EMPTY,
                        renderType
                );
            }

            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TelekineticBlockEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
