package com.pr1tcha.riftborne.rna.combat.training.client;

import com.pr1tcha.riftborne.rna.combat.training.block.RnaTrainingAnchorBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class RnaTrainingAnchorRenderer extends GeoBlockRenderer<RnaTrainingAnchorBlockEntity> {
    public RnaTrainingAnchorRenderer() {
        super(new RnaTrainingAnchorModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
