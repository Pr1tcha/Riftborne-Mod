package com.pr1tcha.riftborne.rna.combat.training.block;

import com.pr1tcha.riftborne.registry.ModContent;
import com.pr1tcha.riftborne.rna.RnaApi;
import com.pr1tcha.riftborne.rna.combat.RnaCombatNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Synchronization Node — currently a deployable shell only. The former Barrier training minigame was
 * removed pending a training rework; this block keeps the deploy/collapse multiblock behaviour, the
 * GeckoLib telescoping animations, and opens the selection menu, but does not train anything yet.
 */
public final class RnaTrainingAnchorBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation FOLDED = RawAnimation.begin().thenLoop("animation.rna_training_anchor.folded");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.rna_training_anchor.idle");
    private static final RawAnimation DEPLOY = RawAnimation.begin()
            .thenPlayAndHold("animation.rna_training_anchor.deploy");
    private static final RawAnimation FOLD = RawAnimation.begin()
            .thenPlayAndHold("animation.rna_training_anchor.fold");
    private static final int DEPLOY_ANIM_TICKS = 20;
    private static final int FOLD_ANIM_TICKS = 16;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int deployAnimTicks;
    private int foldAnimTicks;

    public RnaTrainingAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.RNA_TRAINING_ANCHOR_BE_TYPE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RnaTrainingAnchorBlockEntity anchor) {
        if (level instanceof ServerLevel) {
            anchor.tickServer();
        }
    }

    public void onDeployed() {
        deployAnimTicks = DEPLOY_ANIM_TICKS;
        foldAnimTicks = 0;
        sync();
    }

    public void onCollapsed() {
        foldAnimTicks = FOLD_ANIM_TICKS;
        deployAnimTicks = 0;
        sync();
    }

    /** Open the node's selection menu. Training options are placeholders until the training rework. */
    public void openMenu(ServerPlayer player) {
        if (!getBlockState().getValue(RnaTrainingAnchorBlock.DEPLOYED)) {
            player.displayClientMessage(Component.translatable("message.riftborne.training.not_deployed"), true);
            return;
        }
        RnaCombatNetwork.sendAnchorMenu(player, worldPosition, RnaApi.hasActiveRna(player));
    }

    private void tickServer() {
        if (deployAnimTicks > 0) {
            deployAnimTicks--;
            if (deployAnimTicks == 0) {
                sync();
            }
        }
        if (foldAnimTicks > 0) {
            foldAnimTicks--;
            if (foldAnimTicks == 0) {
                sync();
            }
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("DeployAnimTicks", deployAnimTicks);
        tag.putInt("FoldAnimTicks", foldAnimTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        deployAnimTicks = Math.max(0, tag.getInt("DeployAnimTicks"));
        foldAnimTicks = Math.max(0, tag.getInt("FoldAnimTicks"));
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "training_state", 4, state -> {
            if (foldAnimTicks > 0) {
                return state.setAndContinue(FOLD);
            }
            if (deployAnimTicks > 0) {
                return state.setAndContinue(DEPLOY);
            }
            if (!getBlockState().getValue(RnaTrainingAnchorBlock.DEPLOYED)) {
                return state.setAndContinue(FOLDED);
            }
            return state.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
