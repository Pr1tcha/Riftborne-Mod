package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.codex.item.PocketCodexItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class PocketCodexItemModel extends GeoModel<PocketCodexItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "geo/pocket_codex.geo.json");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "animations/pocket_codex.animation.json");

    @Override
    public ResourceLocation getModelResource(PocketCodexItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PocketCodexItem animatable) {
        return PocketCodexDynamicDisplay.texture();
    }

    @Override
    public ResourceLocation getAnimationResource(PocketCodexItem animatable) {
        return ANIMATION;
    }
}
