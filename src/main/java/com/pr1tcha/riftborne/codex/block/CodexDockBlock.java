package com.pr1tcha.riftborne.codex.block;

import com.mojang.serialization.MapCodec;
import com.pr1tcha.riftborne.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

public final class CodexDockBlock extends BaseEntityBlock {
    public static final MapCodec<CodexDockBlock> CODEC = simpleCodec(CodexDockBlock::new);
    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 4.0D, 13.0D, 5.0D, 12.0D);

    public CodexDockBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CodexDockBlockEntity(pos, state);
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
        if (!stack.is(ModContent.POCKET_CODEX.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof CodexDockBlockEntity dock) || dock.hasPocketCodex()) {
            return ItemInteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            dock.insert(stack);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                showSyncResult(serverPlayer, dock.synchronize(serverPlayer));
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof CodexDockBlockEntity dock)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (player.isSecondaryUseActive() && dock.hasPocketCodex()) {
                ItemStack device = dock.remove();
                if (!player.addItem(device)) {
                    player.drop(device, false);
                }
                serverPlayer.displayClientMessage(Component.translatable("message.riftborne.codex_dock.removed"), true);
            } else {
                showSyncResult(serverPlayer, dock.synchronize(serverPlayer));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void showSyncResult(ServerPlayer player, CodexDockBlockEntity.SyncResult result) {
        String key = switch (result) {
            case SUCCESS -> "message.riftborne.codex_dock.success";
            case NOTHING_TO_SYNC -> "message.riftborne.codex_dock.nothing";
            case NO_DEVICE -> "message.riftborne.codex_dock.no_device";
            case NO_LAPTOP -> "message.riftborne.codex_dock.no_laptop";
        };
        player.displayClientMessage(Component.translatable(key), true);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CodexDockBlockEntity dock) {
            ItemStack device = dock.remove();
            if (!device.isEmpty()) {
                popResource(level, pos, device);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
