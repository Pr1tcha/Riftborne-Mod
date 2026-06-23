package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.codex.block.CodexDiagnosticCapsuleBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class CodexDiagnosticCapsuleModel extends GeoModel<CodexDiagnosticCapsuleBlockEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "geo/codex_diagnostic_capsule.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "textures/block/codex_diagnostic_capsule.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "animations/codex_diagnostic_capsule.animation.json");

    @Override
    public ResourceLocation getModelResource(CodexDiagnosticCapsuleBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CodexDiagnosticCapsuleBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CodexDiagnosticCapsuleBlockEntity animatable) {
        return ANIMATION;
    }
}
