package com.pr1tcha.riftborne.rift.client;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.rift.entity.RiftSplinterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class RiftSplinterRenderer extends GeoEntityRenderer<RiftSplinterEntity> {
    public RiftSplinterRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rift_splinter")
        ));
        withScale(0.512F);
        this.shadowRadius = 0.35F;
    }
}
