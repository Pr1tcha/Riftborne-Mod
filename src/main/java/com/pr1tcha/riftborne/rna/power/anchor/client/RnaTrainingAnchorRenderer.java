package com.pr1tcha.riftborne.rna.power.anchor.client;

import com.pr1tcha.riftborne.rna.power.anchor.RnaTrainingAnchorBlockEntity;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class RnaTrainingAnchorRenderer extends GeoBlockRenderer<RnaTrainingAnchorBlockEntity> {
    public RnaTrainingAnchorRenderer() {
        super(new RnaTrainingAnchorModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public AABB getRenderBoundingBox(RnaTrainingAnchorBlockEntity animatable) {
        // Deployed the model rises ~4 blocks above the base cell. The default unit-cube
        // bounds frustum-cull the tall silhouette the moment the base cell leaves view, so
        // widen the render bounds to cover the full deployed column plus a margin.
        var pos = animatable.getBlockPos();
        return new AABB(
                pos.getX() - 1, pos.getY(), pos.getZ() - 1,
                pos.getX() + 2, pos.getY() + 5, pos.getZ() + 2
        );
    }
}
