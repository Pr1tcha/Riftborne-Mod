package com.pr1tcha.riftborne.physical.block;

import com.mojang.serialization.MapCodec;
import com.pr1tcha.riftborne.physical.PhysicalStat;
import com.pr1tcha.riftborne.physical.PhysicalTrainingManager;
import com.pr1tcha.riftborne.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PhysicalTrainingStationBlock extends Block {
    public static final MapCodec<PhysicalTrainingStationBlock> CODEC = simpleCodec(PhysicalTrainingStationBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D);

    public PhysicalTrainingStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        PhysicalStat stat = statFor(state);
        if (stat == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PhysicalTrainingManager.startStation(serverPlayer, stat, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            PhysicalTrainingManager.cancelSessionsAt(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static PhysicalStat statFor(BlockState state) {
        if (state.is(ModContent.ENDURANCE_STATION.get())) {
            return PhysicalStat.ENDURANCE;
        }
        if (state.is(ModContent.STRENGTH_STATION.get())) {
            return PhysicalStat.STRENGTH;
        }
        if (state.is(ModContent.MOTORICS_STATION.get())) {
            return PhysicalStat.MOTORICS;
        }
        if (state.is(ModContent.STABILITY_STATION.get())) {
            return PhysicalStat.STABILITY;
        }
        return null;
    }

    public static Block blockFor(PhysicalStat stat) {
        return switch (stat) {
            case ENDURANCE -> ModContent.ENDURANCE_STATION.get();
            case STRENGTH -> ModContent.STRENGTH_STATION.get();
            case MOTORICS -> ModContent.MOTORICS_STATION.get();
            case STABILITY -> ModContent.STABILITY_STATION.get();
        };
    }
}
