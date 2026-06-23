package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.codex.block.CodexDiagnosticCapsuleBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class CodexDiagnosticCapsuleRenderer extends GeoBlockRenderer<CodexDiagnosticCapsuleBlockEntity> {
    public CodexDiagnosticCapsuleRenderer() {
        super(new CodexDiagnosticCapsuleModel());
    }
}
