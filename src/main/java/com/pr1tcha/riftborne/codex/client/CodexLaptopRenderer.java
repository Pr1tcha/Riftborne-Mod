package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.codex.block.CodexLaptopBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class CodexLaptopRenderer extends GeoBlockRenderer<CodexLaptopBlockEntity> {
    public CodexLaptopRenderer() {
        super(new CodexLaptopModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
