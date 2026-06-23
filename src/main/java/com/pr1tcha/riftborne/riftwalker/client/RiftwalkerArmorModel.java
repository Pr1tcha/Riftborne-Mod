package com.pr1tcha.riftborne.riftwalker.client;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.riftwalker.item.RiftwalkerArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class RiftwalkerArmorModel extends GeoModel<RiftwalkerArmorItem> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "geo/armor/riftwalker_armor.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "textures/armor/riftwalker_armor.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "animations/armor/riftwalker_armor.animation.json");

    @Override
    public ResourceLocation getModelResource(RiftwalkerArmorItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(RiftwalkerArmorItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(RiftwalkerArmorItem animatable) {
        return ANIMATION;
    }
}
