package com.pr1tcha.riftborne.codex.block;

import com.pr1tcha.riftborne.codex.data.CodexData;
import com.pr1tcha.riftborne.codex.data.PocketCodexData;
import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import com.pr1tcha.riftborne.registry.ModContent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class CodexDockBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.codex_dock.idle");
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public CodexDockBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.CODEX_DOCK_BE_TYPE.get(), pos, state);
    }

    public boolean hasPocketCodex() {
        return items.getFirst().is(ModContent.POCKET_CODEX.get());
    }

    public ItemStack getPocketCodex() {
        return items.getFirst();
    }

    public void insert(ItemStack stack) {
        items.set(0, stack.copyWithCount(1));
        sync();
    }

    public ItemStack remove() {
        ItemStack result = items.getFirst();
        items.set(0, ItemStack.EMPTY);
        sync();
        return result;
    }

    public boolean hasAdjacentLaptop() {
        if (level == null) {
            return false;
        }
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(worldPosition.relative(direction)).is(ModContent.CODEX_LAPTOP.get())) {
                return true;
            }
        }
        return false;
    }

    public SyncResult synchronize(ServerPlayer player) {
        if (!hasPocketCodex()) {
            return SyncResult.NO_DEVICE;
        }
        if (!hasAdjacentLaptop()) {
            return SyncResult.NO_LAPTOP;
        }

        ItemStack pocket = getPocketCodex();
        List<String> queued = PocketCodexData.queuedEntries(pocket);
        List<String> damaged = PocketCodexData.damagedEntries(pocket);
        CodexData laptopData = RiftbornePlayerData.getCodex(player);
        int synchronizedCount = laptopData.synchronize(queued);
        damaged.forEach(laptopData::damage);
        if (synchronizedCount > 0 || !damaged.isEmpty()) {
            laptopData.addTranslatedNotification(
                    "codex.riftborne.feed.synchronized",
                    synchronizedCount,
                    damaged.size()
            );
            RiftbornePlayerData.saveCodex(player, laptopData);
            PocketCodexData.clearTransferQueue(pocket);
            sync();
        }
        return synchronizedCount == 0 && damaged.isEmpty() ? SyncResult.NOTHING_TO_SYNC : SyncResult.SUCCESS;
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
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    public enum SyncResult {
        SUCCESS,
        NOTHING_TO_SYNC,
        NO_DEVICE,
        NO_LAPTOP
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "status", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
