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
import net.minecraft.world.phys.AABB;
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
    private static final VoxelShape WEST_PORT_0_SHAPE = Block.box(-1.8D, 0.95D, 5.55D, 0.8D, 1.7D, 6.45D);
    private static final VoxelShape WEST_PORT_1_SHAPE = Block.box(-1.8D, 0.95D, 6.85D, 0.8D, 1.7D, 7.75D);
    private static final VoxelShape EAST_PORT_0_SHAPE = Block.box(15.2D, 0.95D, 5.55D, 17.8D, 1.7D, 6.45D);
    private static final VoxelShape EAST_PORT_1_SHAPE = Block.box(15.2D, 0.95D, 6.85D, 17.8D, 1.7D, 7.75D);
    private static final VoxelShape NORTH_PORT_0_SHAPE = Block.box(5.55D, 0.95D, -1.8D, 6.45D, 1.7D, 0.8D);
    private static final VoxelShape NORTH_PORT_1_SHAPE = Block.box(6.85D, 0.95D, -1.8D, 7.75D, 1.7D, 0.8D);
    private static final VoxelShape SOUTH_PORT_0_SHAPE = Block.box(5.55D, 0.95D, 15.2D, 6.45D, 1.7D, 17.8D);
    private static final VoxelShape SOUTH_PORT_1_SHAPE = Block.box(6.85D, 0.95D, 15.2D, 7.75D, 1.7D, 17.8D);

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
        if (isPortZone(state, hitResult)) {
            if (level.getBlockEntity(pos) instanceof CodexLaptopBlockEntity laptop) {
                int slot = clickedFlashSlot(state, hitResult, laptop, true);
                if (slot >= 0 && !level.isClientSide && laptop.removeFlashDrive(slot)) {
                    ItemStack flashDrive = new ItemStack(com.pr1tcha.riftborne.registry.ModContent.CODEX_FLASH_DRIVE.get());
                    if (player.getMainHandItem().isEmpty()) {
                        player.setItemInHand(InteractionHand.MAIN_HAND, flashDrive);
                    } else if (!player.addItem(flashDrive)) {
                        player.drop(flashDrive, false);
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (player.isSecondaryUseActive()) {
            toggleLaptop(state, level, pos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (state.getValue(OPEN)
                && isInterfaceZone(state, hitResult)
                && !level.isClientSide
                && player instanceof ServerPlayer serverPlayer) {
            CodexNetwork.open(serverPlayer, pos);
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
        if (isPortZone(state, hitResult)) {
            if (stack.isEmpty()
                    && level.getBlockEntity(pos) instanceof CodexLaptopBlockEntity laptop
                    && laptop.hasFlashDrive()) {
                int slot = clickedFlashSlot(state, hitResult, laptop, true);
                if (slot >= 0 && !level.isClientSide && laptop.removeFlashDrive(slot)) {
                    ItemStack flashDrive = new ItemStack(
                            com.pr1tcha.riftborne.registry.ModContent.CODEX_FLASH_DRIVE.get()
                    );
                    player.setItemInHand(hand, flashDrive);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }

            if (stack.is(com.pr1tcha.riftborne.registry.ModContent.CODEX_FLASH_DRIVE.get())
                    && level.getBlockEntity(pos) instanceof CodexLaptopBlockEntity laptop
                    && laptop.insertedFlashDrives() < 2) {
                int slot = clickedFlashSlot(state, hitResult, laptop, false);
                if (slot < 0) {
                    slot = laptop.hasFlashDrive(0) ? 1 : 0;
                }
                if (!level.isClientSide && laptop.insertFlashDrive(slot)) {
                    stack.shrink(1);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!player.isSecondaryUseActive() && isInterfaceZone(state, hitResult)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (player.isSecondaryUseActive()) {
            toggleLaptop(state, level, pos);
        }
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

    private static boolean isPortZone(BlockState state, BlockHitResult hitResult) {
        Direction right = state.getValue(FACING).getCounterClockWise();
        if (hitResult.getDirection() == right) {
            return true;
        }

        double localX = hitResult.getLocation().x - hitResult.getBlockPos().getX() - 0.5D;
        double localZ = hitResult.getLocation().z - hitResult.getBlockPos().getZ() - 0.5D;
        double rightOffset = localX * right.getStepX() + localZ * right.getStepZ();
        double localY = hitResult.getLocation().y - hitResult.getBlockPos().getY();
        return hitResult.getDirection() == Direction.UP && localY <= 0.25D && rightOffset >= 0.32D;
    }

    private static int clickedFlashSlot(
            BlockState state,
            BlockHitResult hitResult,
            CodexLaptopBlockEntity laptop,
            boolean occupied
    ) {
        double localX = hitResult.getLocation().x - hitResult.getBlockPos().getX();
        double localY = hitResult.getLocation().y - hitResult.getBlockPos().getY();
        double localZ = hitResult.getLocation().z - hitResult.getBlockPos().getZ();
        final double epsilon = 0.035D;
        for (int slot = 0; slot < 2; slot++) {
            if (laptop.hasFlashDrive(slot) != occupied) {
                continue;
            }
            for (AABB box : portShape(state, slot).toAabbs()) {
                if (localX >= box.minX - epsilon && localX <= box.maxX + epsilon
                        && localY >= box.minY - epsilon && localY <= box.maxY + epsilon
                        && localZ >= box.minZ - epsilon && localZ <= box.maxZ + epsilon) {
                    return slot;
                }
            }
        }
        return -1;
    }

    private static VoxelShape portShape(BlockState state, int slot) {
        return switch (state.getValue(FACING).getCounterClockWise()) {
            case WEST -> slot == 0 ? WEST_PORT_0_SHAPE : WEST_PORT_1_SHAPE;
            case EAST -> slot == 0 ? EAST_PORT_0_SHAPE : EAST_PORT_1_SHAPE;
            case NORTH -> slot == 0 ? NORTH_PORT_0_SHAPE : NORTH_PORT_1_SHAPE;
            default -> slot == 0 ? SOUTH_PORT_0_SHAPE : SOUTH_PORT_1_SHAPE;
        };
    }

    private static boolean isInterfaceZone(BlockState state, BlockHitResult hitResult) {
        Direction hitSide = hitResult.getDirection();
        double localY = hitResult.getLocation().y - hitResult.getBlockPos().getY();
        boolean keyboardSurface = hitSide == Direction.UP && localY <= 0.25D;
        boolean monitorSurface = state.getValue(OPEN)
                && hitSide == state.getValue(FACING).getOpposite()
                && localY >= 0.18D;
        return keyboardSurface || monitorSurface;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!player.isSecondaryUseActive() || level.isClientSide) {
            return;
        }

        Item laptopItem = state.getBlock().asItem();
        ItemStack laptopStack = new ItemStack(laptopItem);
        if (level.getBlockEntity(pos) instanceof CodexLaptopBlockEntity laptop && laptop.hasFlashDrive()) {
            for (int slot = 0; slot < 2; slot++) {
                if (laptop.removeFlashDrive(slot)) {
                    ItemStack flashDrive = new ItemStack(
                            com.pr1tcha.riftborne.registry.ModContent.CODEX_FLASH_DRIVE.get()
                    );
                    if (!player.addItem(flashDrive)) {
                        player.drop(flashDrive, false);
                    }
                }
            }
        }
        if (level.removeBlock(pos, false) && !player.addItem(laptopStack)) {
            player.drop(laptopStack, false);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape laptopShape = !state.getValue(OPEN) ? BASE_SHAPE : switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };

        if (!(level.getBlockEntity(pos) instanceof CodexLaptopBlockEntity laptop) || !laptop.hasFlashDrive()) {
            return laptopShape;
        }

        VoxelShape result = laptopShape;
        for (int slot = 0; slot < 2; slot++) {
            if (laptop.hasFlashDrive(slot)) {
                result = Shapes.or(result, portShape(state, slot));
            }
        }
        return result;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
}
