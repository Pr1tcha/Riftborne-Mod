package com.pr1tcha.riftborne.physical.pushup.block;

import com.mojang.serialization.MapCodec;
import com.pr1tcha.riftborne.physical.pushup.PushupTrainingManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PushupMatBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PushupMatBlock> CODEC = simpleCodec(PushupMatBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final BooleanProperty ROLLED = BooleanProperty.create("rolled");
    private static final VoxelShape UNROLLED_SHAPE = Block.box(1.0D, 0.0D, 0.0D, 15.0D, 1.0D, 16.0D);
    private static final VoxelShape ROLLED_SHAPE = Block.box(1.0D, 0.0D, 3.0D, 15.0D, 7.0D, 13.0D);

    public PushupMatBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BedPart.FOOT)
                .setValue(ROLLED, true));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(PART, BedPart.FOOT)
                .setValue(ROLLED, true);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && !state.getValue(ROLLED)) {
            BlockPos secondPos = pos.relative(state.getValue(FACING));
            level.setBlock(secondPos, state.setValue(PART, BedPart.HEAD).setValue(ROLLED, false), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, ROLLED);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide) {
                toggleFoldedState(state, level, pos, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.getValue(ROLLED)) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable("message.riftborne.pushup.unfold_hint"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockPos root = rootPos(pos, state);
            PushupTrainingManager.start(serverPlayer, root, state.getValue(FACING));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide) {
                toggleFoldedState(state, level, pos, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.getValue(ROLLED)) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable("message.riftborne.pushup.unfold_hint"), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockPos root = rootPos(pos, state);
            PushupTrainingManager.start(serverPlayer, root, state.getValue(FACING));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(ROLLED) ? ROLLED_SHAPE : UNROLLED_SHAPE;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            BlockPos root = rootPos(pos, state);
            PushupTrainingManager.stopAt(serverLevel, root);
            if (!state.getValue(ROLLED)) {
                BlockPos otherPos = otherPos(pos, state);
                BlockState otherState = level.getBlockState(otherPos);
                if (otherState.is(this)
                        && !otherState.getValue(ROLLED)
                        && otherState.getValue(FACING) == state.getValue(FACING)
                        && otherState.getValue(PART) != state.getValue(PART)) {
                    level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static BlockPos rootPos(BlockPos pos, BlockState state) {
        return state.getValue(PART) == BedPart.FOOT
                ? pos
                : pos.relative(state.getValue(FACING).getOpposite());
    }

    public static boolean isComplete(Level level, BlockPos root, Direction facing) {
        BlockState foot = level.getBlockState(root);
        BlockState head = level.getBlockState(root.relative(facing));
        return foot.getBlock() instanceof PushupMatBlock
                && foot.getValue(FACING) == facing
                && foot.getValue(PART) == BedPart.FOOT
                && !foot.getValue(ROLLED)
                && head.getBlock() == foot.getBlock()
                && head.getValue(FACING) == facing
                && head.getValue(PART) == BedPart.HEAD
                && !head.getValue(ROLLED);
    }

    private static BlockPos otherPos(BlockPos pos, BlockState state) {
        return state.getValue(PART) == BedPart.FOOT
                ? pos.relative(state.getValue(FACING))
                : pos.relative(state.getValue(FACING).getOpposite());
    }

    private void toggleFoldedState(BlockState state, Level level, BlockPos pos, Player player) {
        BlockPos root = rootPos(pos, state);
        BlockState rootState = level.getBlockState(root);
        if (!rootState.is(this)) {
            return;
        }
        if (rootState.getValue(ROLLED)) {
            unfold(rootState, level, root, player);
        } else {
            fold(rootState, level, root);
        }
    }

    private void unfold(BlockState rootState, Level level, BlockPos root, Player player) {
        Direction facing = rootState.getValue(FACING);
        BlockPos headPos = root.relative(facing);
        if (!level.getWorldBorder().isWithinBounds(headPos) || !level.getBlockState(headPos).canBeReplaced()) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable("message.riftborne.pushup.unfold_blocked"), true);
            }
            return;
        }
        BlockState foot = rootState
                .setValue(PART, BedPart.FOOT)
                .setValue(ROLLED, false);
        level.setBlock(root, foot, Block.UPDATE_ALL);
        level.setBlock(headPos, foot.setValue(PART, BedPart.HEAD), Block.UPDATE_ALL);
        level.playSound(null, root, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.65F, 1.05F);
    }

    private void fold(BlockState rootState, Level level, BlockPos root) {
        Direction facing = rootState.getValue(FACING);
        BlockPos headPos = root.relative(facing);
        if (level instanceof ServerLevel serverLevel) {
            PushupTrainingManager.stopAt(serverLevel, root);
        }
        level.setBlock(root, rootState
                .setValue(PART, BedPart.FOOT)
                .setValue(ROLLED, true), Block.UPDATE_ALL);
        BlockState headState = level.getBlockState(headPos);
        if (headState.is(this)
                && headState.getValue(FACING) == facing
                && headState.getValue(PART) == BedPart.HEAD) {
            level.setBlock(headPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
        level.playSound(null, root, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.55F, 1.2F);
    }
}
