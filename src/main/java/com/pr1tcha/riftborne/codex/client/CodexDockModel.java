package com.pr1tcha.riftborne.codex.client;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.codex.block.CodexDockBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public final class CodexDockModel extends GeoModel<CodexDockBlockEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "geo/codex_dock.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "textures/block/codex_dock.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "animations/codex_dock.animation.json");

    @Override
    public ResourceLocation getModelResource(CodexDockBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CodexDockBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CodexDockBlockEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(
            CodexDockBlockEntity animatable,
            long instanceId,
            AnimationState<CodexDockBlockEntity> animationState
    ) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        getBone("inserted_device").ifPresent(bone -> bone.setHidden(!animatable.hasPocketCodex()));
    }
}
