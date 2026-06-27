package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.codex.block.CodexDiagnosticCapsuleBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class CodexDiagnosticCapsuleRenderer extends GeoBlockRenderer<CodexDiagnosticCapsuleBlockEntity> {
    public CodexDiagnosticCapsuleRenderer() {
        super(new CodexDiagnosticCapsuleModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
