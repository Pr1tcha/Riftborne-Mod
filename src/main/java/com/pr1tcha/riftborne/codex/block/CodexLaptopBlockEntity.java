package com.pr1tcha.riftborne.codex.block;

import com.pr1tcha.riftborne.registry.ModContent;
import net.minecraft.core.BlockPos;
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

    private boolean isOpen() {
        return getBlockState().getValue(CodexLaptopBlock.OPEN);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
