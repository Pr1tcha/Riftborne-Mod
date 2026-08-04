package com.pr1tcha.riftborne.rna.power.anchor.client;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.rna.power.anchor.RnaTrainingAnchorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class RnaTrainingAnchorModel extends GeoModel<RnaTrainingAnchorBlockEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "geo/rna_training_anchor.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "textures/block/rna_training_anchor.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "animations/rna_training_anchor.animation.json");

    @Override
    public ResourceLocation getModelResource(RnaTrainingAnchorBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(RnaTrainingAnchorBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(RnaTrainingAnchorBlockEntity animatable) {
        return ANIMATION;
    }
}
