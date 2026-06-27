package com.pr1tcha.riftborne.codex.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CodexDiagnosticCapsuleBlock extends BaseEntityBlock {
    public static final MapCodec<CodexDiagnosticCapsuleBlock> CODEC = simpleCodec(CodexDiagnosticCapsuleBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape OUTLINE_SHAPE = Shapes.or(
            Block.box(0.5D, 0.0D, 0.5D, 15.5D, 2.0D, 15.5D),
            Block.box(1.0D, 2.0D, 1.0D, 15.0D, 34.0D, 15.0D)
    );
    private static final VoxelShape BASE_COLLISION = Block.box(0.5D, 0.0D, 0.5D, 15.5D, 2.0D, 15.5D);
    private static final VoxelShape NORTH_COLLISION = Shapes.or(
            BASE_COLLISION,
            Block.box(0.5D, 2.0D, 0.5D, 2.0D, 32.0D, 15.5D),
            Block.box(14.0D, 2.0D, 0.5D, 15.5D, 32.0D, 15.5D),
            Block.box(0.5D, 2.0D, 14.0D, 15.5D, 32.0D, 15.5D)
    );
    private static final VoxelShape SOUTH_COLLISION = Shapes.or(
            BASE_COLLISION,
            Block.box(0.5D, 2.0D, 0.5D, 2.0D, 32.0D, 15.5D),
            Block.box(14.0D, 2.0D, 0.5D, 15.5D, 32.0D, 15.5D),
            Block.box(0.5D, 2.0D, 0.5D, 15.5D, 32.0D, 2.0D)
    );
    private static final VoxelShape WEST_COLLISION = Shapes.or(
            BASE_COLLISION,
            Block.box(0.5D, 2.0D, 0.5D, 15.5D, 32.0D, 2.0D),
            Block.box(0.5D, 2.0D, 14.0D, 15.5D, 32.0D, 15.5D),
            Block.box(14.0D, 2.0D, 0.5D, 15.5D, 32.0D, 15.5D)
    );
    private static final VoxelShape EAST_COLLISION = Shapes.or(
            BASE_COLLISION,
            Block.box(0.5D, 2.0D, 0.5D, 15.5D, 32.0D, 2.0D),
            Block.box(0.5D, 2.0D, 14.0D, 15.5D, 32.0D, 15.5D),
            Block.box(0.5D, 2.0D, 0.5D, 2.0D, 32.0D, 15.5D)
    );

    public CodexDiagnosticCapsuleBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CodexDiagnosticCapsuleBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return level.isClientSide
                ? null
                : createTickerHelper(
                        type,
                        com.pr1tcha.riftborne.registry.ModContent.CODEX_DIAGNOSTIC_CAPSULE_BE_TYPE.get(),
                        CodexDiagnosticCapsuleBlockEntity::serverTick
                );
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CodexDiagnosticCapsuleBlockEntity capsule) {
            if (capsule.isScanning()) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.riftborne.codex_diagnostic_capsule.busy"),
                        true
                );
            } else {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.riftborne.codex_diagnostic_capsule.enter"),
                        true
                );
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_COLLISION;
            case WEST -> WEST_COLLISION;
            case EAST -> EAST_COLLISION;
            default -> NORTH_COLLISION;
        };
    }
}
