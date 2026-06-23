package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.codex.block.CodexDockBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class CodexDockRenderer extends GeoBlockRenderer<CodexDockBlockEntity> {
    public CodexDockRenderer() {
        super(new CodexDockModel());
    }
}
