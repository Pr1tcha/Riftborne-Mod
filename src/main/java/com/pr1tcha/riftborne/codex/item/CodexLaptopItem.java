package com.pr1tcha.riftborne.codex.item;

import com.pr1tcha.riftborne.codex.client.CodexLaptopItemRenderer;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class CodexLaptopItem extends BlockItem implements GeoItem {
    private static final RawAnimation DISPLAY = RawAnimation.begin()
            .thenPlayAndHold("animation.codex_laptop.closed");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public CodexLaptopItem(Block block, Properties properties) {
        super(block, properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private CodexLaptopItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new CodexLaptopItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "display", 0,
                state -> state.setAndContinue(DISPLAY)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
