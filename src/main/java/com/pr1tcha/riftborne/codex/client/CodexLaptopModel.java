package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.codex.block.CodexLaptopBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CodexLaptopModel extends GeoModel<CodexLaptopBlockEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "geo/codex_laptop.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "textures/block/codex_laptop.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "animations/codex_laptop.animation.json");

    @Override
    public ResourceLocation getModelResource(CodexLaptopBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CodexLaptopBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CodexLaptopBlockEntity animatable) {
        return ANIMATION;
    }
}
