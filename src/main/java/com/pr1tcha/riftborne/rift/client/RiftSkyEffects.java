package com.pr1tcha.riftborne.rift.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.interspace.InterspaceDimensions;
import com.pr1tcha.riftborne.rift.dimension.RiftTier;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.joml.Matrix4f;

public final class RiftSkyEffects extends DimensionSpecialEffects {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rift_sky");
    public static final ResourceLocation RIFTWALKER_ID = ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "riftwalker_sky");
    public static final ResourceLocation RNA_ID = ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_sky");
    private static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rift_sky");
    private static final float SKY_SIZE = 96.0F;
    private static final int LONGITUDE_SEGMENTS = 64;
    private static final int LATITUDE_SEGMENTS = 32;

    public RiftSkyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    public static void register(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ID, new RiftSkyEffects());
        event.register(RIFTWALKER_ID, new RiftSkyEffects());
        event.register(RNA_ID, new RiftSkyEffects());
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return color.scale(0.35D + brightness * 0.25D);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public boolean renderSky(
            ClientLevel level,
            int ticks,
            float partialTick,
            Matrix4f modelViewMatrix,
            Camera camera,
            Matrix4f projectionMatrix,
            boolean isFoggy,
            Runnable setupFog
    ) {
        RiftTier tier = RiftTier.fromDimension(level.dimension()).orElse(null);
        boolean riftwalker = level.dimension().equals(InterspaceDimensions.RIFTWALKER_INTERSPACE);
        boolean rna = level.dimension().equals(InterspaceDimensions.RNA_INTERSPACE);
        if (tier == null && !riftwalker && !rna) {
            return false;
        }

        SkyProfile profile = rna ? SkyProfile.rna() : (riftwalker ? SkyProfile.riftwalker() : SkyProfile.forTier(tier));
        float time = (ticks + partialTick) / 20.0F;
        ShaderProgram shader = VeilRenderSystem.setShader(SHADER);
        if (shader == null) {
            return true;
        }

        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();

        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(modelViewMatrix);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * profile.rotationSpeed()));
        poseStack.mulPose(Axis.XP.rotationDegrees(profile.pitch()));
        Matrix4f skyMatrix = poseStack.last().pose();

        shader.setDefaultUniforms(VertexFormat.Mode.QUADS, skyMatrix, projectionMatrix);
        shader.getUniformSafe("RiftTime").setFloat(time);
        shader.getUniformSafe("RiftTier").setInt(rna ? -1 : (riftwalker ? 0 : tier.level()));
        shader.getUniformSafe("PrimaryColor").setVector(profile.primaryRed(), profile.primaryGreen(), profile.primaryBlue());
        shader.getUniformSafe("SecondaryColor").setVector(profile.secondaryRed(), profile.secondaryGreen(), profile.secondaryBlue());
        shader.getUniformSafe("Intensity").setFloat(profile.intensity());
        shader.getUniformSafe("SkyRotation").setMatrix(new Matrix4f().rotation(camera.rotation()));

        renderSphere(skyMatrix, SKY_SIZE);

        ShaderProgram.unbind();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        return true;
    }

    private static void renderSphere(Matrix4f matrix, float size) {
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION
        );

        for (int latitude = 0; latitude < LATITUDE_SEGMENTS; latitude++) {
            double phi0 = -Math.PI / 2.0D + Math.PI * latitude / LATITUDE_SEGMENTS;
            double phi1 = -Math.PI / 2.0D + Math.PI * (latitude + 1) / LATITUDE_SEGMENTS;

            for (int longitude = 0; longitude < LONGITUDE_SEGMENTS; longitude++) {
                double theta0 = Math.PI * 2.0D * longitude / LONGITUDE_SEGMENTS;
                double theta1 = Math.PI * 2.0D * (longitude + 1) / LONGITUDE_SEGMENTS;

                sphereVertex(buffer, size, theta0, phi0);
                sphereVertex(buffer, size, theta0, phi1);
                sphereVertex(buffer, size, theta1, phi1);
                sphereVertex(buffer, size, theta1, phi0);
            }
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private static void sphereVertex(
            BufferBuilder buffer,
            float radius,
            double theta,
            double phi
    ) {
        float cosPhi = (float) Math.cos(phi);
        float x = radius * cosPhi * (float) Math.sin(theta);
        float y = radius * (float) Math.sin(phi);
        float z = radius * cosPhi * (float) Math.cos(theta);
        buffer.addVertex(x, y, z);
    }

    private record SkyProfile(
            float rotationSpeed,
            float pitch,
            float primaryRed,
            float primaryGreen,
            float primaryBlue,
            float secondaryRed,
            float secondaryGreen,
            float secondaryBlue,
            float intensity
    ) {
        private static SkyProfile forTier(RiftTier tier) {
            return switch (tier) {
                case SURFACE_SHARD -> new SkyProfile(
                        0.16F, 12.0F,
                        0.10F, 0.48F, 0.92F,
                        0.42F, 0.16F, 0.78F,
                        0.72F
                );
                case SHIFTED_LAYER -> new SkyProfile(
                        0.31F, -17.0F,
                        0.16F, 0.38F, 0.96F,
                        0.56F, 0.12F, 0.86F,
                        0.92F
                );
                case NODE_RIFT -> new SkyProfile(
                        0.48F, 23.0F,
                        0.08F, 0.64F, 1.00F,
                        0.76F, 0.12F, 0.98F,
                        1.18F
                );
                case DEEP_RIFT -> new SkyProfile(
                        0.10F, 7.0F,
                        0.08F, 0.12F, 0.28F,
                        0.34F, 0.08F, 0.44F,
                        0.82F
                );
                case LIMIT_SLICE -> new SkyProfile(
                        0.72F, -29.0F,
                        0.12F, 0.82F, 1.00F,
                        1.00F, 0.08F, 0.82F,
                        1.45F
                );
            };
        }

        private static SkyProfile riftwalker() {
            return new SkyProfile(
                    0.026F, -8.0F,
                    0.10F, 0.018F, 0.28F,
                    0.58F, 0.10F, 1.00F,
                    1.48F
            );
        }

        private static SkyProfile rna() {
            return new SkyProfile(
                    0.018F, 4.0F,
                    0.05F, 0.42F, 0.72F,
                    0.34F, 0.94F, 1.00F,
                    1.28F
            );
        }
    }
}
