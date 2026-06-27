package com.pr1tcha.riftborne.codex.block;

import com.pr1tcha.riftborne.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class CodexLaptopBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation OPEN = RawAnimation.begin()
            .thenPlayAndHold("animation.codex_laptop.open");
    private static final RawAnimation CLOSE = RawAnimation.begin()
            .thenPlayAndHold("animation.codex_laptop.close");
    private static final RawAnimation OPENED = RawAnimation.begin()
            .thenPlayAndHold("animation.codex_laptop.opened");
    private static final RawAnimation CLOSED = RawAnimation.begin()
            .thenPlayAndHold("animation.codex_laptop.closed");
    private static final RawAnimation WORKING = RawAnimation.begin()
            .thenLoop("animation.codex_laptop.working");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final boolean[] flashDrives = new boolean[2];
    private String desktopLayout = "";

    public CodexLaptopBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModContent.CODEX_LAPTOP_BE_TYPE.get(), pos, blockState);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "lid", 0, state ->
                        state.setAndContinue(isOpen() ? OPENED : CLOSED))
                        .triggerableAnim("open", OPEN)
                        .triggerableAnim("close", CLOSE),
                new AnimationController<>(this, "screen", 0, state ->
                        isOpen() ? state.setAndContinue(WORKING) : PlayState.STOP)
        );
    }

    public void triggerLidAnimation(boolean opening) {
        triggerAnim("lid", opening ? "open" : "close");
    }

    public boolean hasFlashDrive() {
        return flashDrives[0] || flashDrives[1];
    }

    public boolean hasFlashDrive(int slot) {
        return slot >= 0 && slot < flashDrives.length && flashDrives[slot];
    }

    public int insertedFlashDrives() {
        int count = 0;
        for (boolean inserted : flashDrives) {
            if (inserted) {
                count++;
            }
        }
        return count;
    }

    public boolean insertFlashDrive(int slot) {
        if (slot < 0 || slot >= flashDrives.length || flashDrives[slot]) {
            return false;
        }
        flashDrives[slot] = true;
        sync();
        return true;
    }

    public boolean removeFlashDrive(int slot) {
        if (slot < 0 || slot >= flashDrives.length || !flashDrives[slot]) {
            return false;
        }
        flashDrives[slot] = false;
        sync();
        return true;
    }

    public String desktopLayout() {
        return desktopLayout;
    }

    public void setDesktopLayout(String desktopLayout) {
        this.desktopLayout = desktopLayout == null ? "" : desktopLayout;
        sync();
    }

    private boolean isOpen() {
        return getBlockState().getValue(CodexLaptopBlock.OPEN);
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
        tag.putBoolean("FlashDrive0", flashDrives[0]);
        tag.putBoolean("FlashDrive1", flashDrives[1]);
        tag.putString("DesktopLayout", desktopLayout);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        flashDrives[0] = tag.getBoolean("FlashDrive0");
        flashDrives[1] = tag.getBoolean("FlashDrive1");
        desktopLayout = tag.getString("DesktopLayout");
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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
