package com.pr1tcha.riftborne.codex.block;

import com.mojang.serialization.MapCodec;
import com.pr1tcha.riftborne.codex.network.CodexNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CodexLaptopBlock extends BaseEntityBlock {
    public static final MapCodec<CodexLaptopBlock> CODEC = simpleCodec(CodexLaptopBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    private static final VoxelShape BASE_SHAPE = Block.box(1.0D, 0.0D, 2.0D, 15.0D, 3.0D, 14.0D);
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            BASE_SHAPE,
            Block.box(1.5D, 3.0D, 12.0D, 14.5D, 14.0D, 14.0D)
    );
    private static final VoxelShape SOUTH_SHAPE = Shapes.or(
            BASE_SHAPE,
            Block.box(1.5D, 3.0D, 2.0D, 14.5D, 14.0D, 4.0D)
    );
    private static final VoxelShape WEST_SHAPE = Shapes.or(
            BASE_SHAPE,
            Block.box(12.0D, 3.0D, 1.5D, 14.0D, 14.0D, 14.5D)
    );
    private static final VoxelShape EAST_SHAPE = Shapes.or(
            Block.box(1.0D, 0.0D, 2.0D, 15.0D, 3.0D, 14.0D),
            Block.box(2.0D, 3.0D, 1.5D, 4.0D, 14.0D, 14.5D)
    );

    public CodexLaptopBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CodexLaptopBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isSecondaryUseActive()) {
            toggleLaptop(state, level, pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (state.getValue(OPEN) && !level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            CodexNetwork.open(serverPlayer);
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
        if (!player.isSecondaryUseActive()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        toggleLaptop(state, level, pos);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void toggleLaptop(BlockState state, Level level, BlockPos pos) {
        if (!level.isClientSide) {
            boolean opening = !state.getValue(OPEN);
            level.setBlock(pos, state.setValue(OPEN, opening), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof CodexLaptopBlockEntity laptop) {
                laptop.triggerLidAnimation(opening);
            }
        }
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!player.isSecondaryUseActive() || level.isClientSide) {
            return;
        }

        Item laptopItem = state.getBlock().asItem();
        ItemStack laptopStack = new ItemStack(laptopItem);
        if (level.removeBlock(pos, false) && !player.addItem(laptopStack)) {
            player.drop(laptopStack, false);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(OPEN)) {
            return BASE_SHAPE;
        }

        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
}
