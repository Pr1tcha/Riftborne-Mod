package com.pr1tcha.riftborne.riftwalker.client;

import com.pr1tcha.riftborne.riftwalker.item.RiftwalkerArmorItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class RiftwalkerArmorRenderer extends GeoArmorRenderer<RiftwalkerArmorItem> {
    public RiftwalkerArmorRenderer() {
        super(new RiftwalkerArmorModel());
        addRenderLayer(new RiftwalkerGlowLayer(this));
    }
}
