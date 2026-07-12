package com.pr1tcha.riftborne.rna.combat.training.block;

import com.mojang.serialization.MapCodec;
import com.pr1tcha.riftborne.registry.ModContent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class RnaTrainingAnchorBlock extends BaseEntityBlock {
    public static final MapCodec<RnaTrainingAnchorBlock> CODEC = simpleCodec(RnaTrainingAnchorBlock::new);
    public static final BooleanProperty DEPLOYED = BooleanProperty.create("deployed");

    // Deployed the node is a vertical multiblock: PART 0 is the interactive base
    // (owns the block entity + animated render); PART 1..SEGMENTS are invisible
    // collision-only fillers stacked above so the hitbox is a real 4-block column.
    private static final int SEGMENTS = 3;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, SEGMENTS);

    // Deploy arena: an 11x11 footprint (radius 5, "~10x10") that must be clear at the two
    // training-height layers, plus SEGMENTS blocks of headroom above the anchor.
    private static final int ARENA_RADIUS = 5;
    private static final int ARENA_HEIGHT = 2;

    private static boolean tearingDown = false;

    public RnaTrainingAnchorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(DEPLOYED, false).setValue(PART, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DEPLOYED, PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(DEPLOYED, false).setValue(PART, 0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new RnaTrainingAnchorBlockEntity(pos, state) : null;
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
        int part = state.getValue(PART);
        BlockPos basePos = part == 0 ? pos : pos.below(part);
        BlockState baseState = part == 0 ? state : level.getBlockState(basePos);
        if (!baseState.is(this) || !(level.getBlockEntity(basePos) instanceof RnaTrainingAnchorBlockEntity anchor)) {
            return InteractionResult.PASS;
        }

        if (!baseState.getValue(DEPLOYED)) {
            if (player.isSecondaryUseActive()) {
                return InteractionResult.PASS;
            }
            return tryDeploy(baseState, (ServerLevel) level, basePos, serverPlayer, anchor);
        }

        if (player.isSecondaryUseActive()) {
            collapse(baseState, (ServerLevel) level, basePos, serverPlayer, anchor);
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
        level.setBlock(pos, state.setValue(DEPLOYED, true).setValue(PART, 0), Block.UPDATE_ALL);
        for (int i = 1; i <= SEGMENTS; i++) {
            level.setBlock(pos.above(i),
                    defaultBlockState().setValue(DEPLOYED, true).setValue(PART, i),
                    Block.UPDATE_ALL);
        }
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
        tearingDown = true;
        try {
            for (int i = 1; i <= SEGMENTS; i++) {
                BlockPos segment = pos.above(i);
                BlockState segState = level.getBlockState(segment);
                if (segState.is(this) && segState.getValue(PART) == i) {
                    level.removeBlock(segment, false);
                }
            }
        } finally {
            tearingDown = false;
        }
        level.setBlock(pos, state.setValue(DEPLOYED, false).setValue(PART, 0), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.7F, 1.1F);
        player.displayClientMessage(Component.translatable("message.riftborne.training.collapsed"), true);
    }

    private boolean hasDeploySpace(Level level, BlockPos pos) {
        for (int y = 1; y <= SEGMENTS; y++) {
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
        return state.getValue(PART) == 0 ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(ModContent.RNA_TRAINING_ANCHOR_ITEM.get()));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return level.isClientSide || state.getValue(PART) != 0
                ? null
                : createTickerHelper(
                        type,
                        ModContent.RNA_TRAINING_ANCHOR_BE_TYPE.get(),
                        RnaTrainingAnchorBlockEntity::serverTick
                );
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && !tearingDown) {
            tearingDown = true;
            try {
                int part = state.getValue(PART);
                BlockPos basePos = pos.below(part);
                if (level.getBlockEntity(basePos) instanceof RnaTrainingAnchorBlockEntity anchor) {
                    anchor.stopTraining(null, false);
                }
                for (int i = 0; i <= SEGMENTS; i++) {
                    BlockPos p = basePos.above(i);
                    if (p.equals(pos)) {
                        continue;
                    }
                    BlockState s = level.getBlockState(p);
                    if (s.is(this) && s.getValue(PART) == i) {
                        level.removeBlock(p, false);
                    }
                }
            } finally {
                tearingDown = false;
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
