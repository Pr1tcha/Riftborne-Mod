package com.pr1tcha.riftborne.interspace;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.registry.ModContent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public final class RnaFluidClient {
    private static final ResourceLocation STILL =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "block/interspace/rna_current_still");
    private static final ResourceLocation FLOW =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "block/interspace/rna_current_flow");
    private static final ResourceLocation OVERLAY =
            ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");
    private static final int TINT = 0xB81AF7FF;

    private RnaFluidClient() {
    }

    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public int getTintColor() {
                return TINT;
            }

            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOW;
            }

            @Override
            public ResourceLocation getRenderOverlayTexture(net.minecraft.client.Minecraft minecraft) {
                return OVERLAY;
            }
        }, ModContent.RNA_CURRENT_TYPE.get());
    }

    @SuppressWarnings("removal")
    public static void registerRenderLayers() {
        ItemBlockRenderTypes.setRenderLayer(ModContent.RNA_CURRENT_SOURCE.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModContent.RNA_CURRENT_FLOWING.get(), RenderType.translucent());
    }
}
