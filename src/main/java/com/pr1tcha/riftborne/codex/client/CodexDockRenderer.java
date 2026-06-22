package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.codex.block.CodexDockBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class CodexDockRenderer extends GeoBlockRenderer<CodexDockBlockEntity> {
    public CodexDockRenderer() {
        super(new CodexDockModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
