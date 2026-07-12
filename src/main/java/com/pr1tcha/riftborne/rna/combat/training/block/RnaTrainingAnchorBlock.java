package com.pr1tcha.riftborne.rna.combat.training.block;

import com.mojang.serialization.MapCodec;
import com.pr1tcha.riftborne.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class RnaTrainingAnchorBlock extends BaseEntityBlock {
    public static final MapCodec<RnaTrainingAnchorBlock> CODEC = simpleCodec(RnaTrainingAnchorBlock::new);
    public static final BooleanProperty DEPLOYED = BooleanProperty.create("deployed");

    // Compact (undeployed) unit sits low; deployed unit fills the base cell. The 4-block
    // silhouette above is rendered by the GeckoLib model, not occupied by collision yet.
    private static final VoxelShape COMPACT_SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 7.0D, 12.0D);
    private static final VoxelShape DEPLOYED_SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    // Deploy arena: an 11x11 footprint (radius 5, "~10x10") that must be clear at the two
    // training-height layers, plus a 4-block vertical column above the anchor for the structure.
    private static final int ARENA_RADIUS = 5;
    private static final int ARENA_HEIGHT = 2;
    private static final int STRUCTURE_HEIGHT = 4;

    public RnaTrainingAnchorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(DEPLOYED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DEPLOYED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(DEPLOYED, false);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RnaTrainingAnchorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!(level.getBlockEntity(pos) instanceof RnaTrainingAnchorBlockEntity anchor)) {
            return InteractionResult.PASS;
        }

        if (!state.getValue(DEPLOYED)) {
            if (player.isSecondaryUseActive()) {
                return InteractionResult.PASS;
            }
            return tryDeploy(state, (ServerLevel) level, pos, serverPlayer, anchor);
        }

        if (player.isSecondaryUseActive()) {
            collapse(state, (ServerLevel) level, pos, serverPlayer, anchor);
        } else {
            anchor.startTraining(serverPlayer);
        }
        return InteractionResult.CONSUME;
    }

    private InteractionResult tryDeploy(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            RnaTrainingAnchorBlockEntity anchor
    ) {
        if (!hasDeploySpace(level, pos)) {
            player.displayClientMessage(Component.translatable("message.riftborne.training.deploy_no_space"), true);
            level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 0.6F, 1.2F);
            return InteractionResult.CONSUME;
        }
        level.setBlock(pos, state.setValue(DEPLOYED, true), Block.UPDATE_ALL);
        anchor.onDeployed();
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 0.9F);
        player.displayClientMessage(Component.translatable("message.riftborne.training.deployed"), true);
        return InteractionResult.CONSUME;
    }

    private void collapse(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            RnaTrainingAnchorBlockEntity anchor
    ) {
        anchor.stopTraining(player, false);
        level.setBlock(pos, state.setValue(DEPLOYED, false), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.7F, 1.1F);
        player.displayClientMessage(Component.translatable("message.riftborne.training.collapsed"), true);
    }

    private boolean hasDeploySpace(Level level, BlockPos pos) {
        for (int y = 1; y <= STRUCTURE_HEIGHT; y++) {
            if (!isClear(level, pos.above(y))) {
                return false;
            }
        }
        for (int dx = -ARENA_RADIUS; dx <= ARENA_RADIUS; dx++) {
            for (int dz = -ARENA_RADIUS; dz <= ARENA_RADIUS; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                for (int y = 1; y <= ARENA_HEIGHT; y++) {
                    if (!isClear(level, pos.offset(dx, y, dz))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isClear(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty() && (state.isAir() || state.canBeReplaced());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(DEPLOYED) ? DEPLOYED_SHAPE : COMPACT_SHAPE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type,
                        ModContent.RNA_TRAINING_ANCHOR_BE_TYPE.get(),
                        RnaTrainingAnchorBlockEntity::serverTick
                );
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof RnaTrainingAnchorBlockEntity anchor) {
            anchor.stopTraining(null, false);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
