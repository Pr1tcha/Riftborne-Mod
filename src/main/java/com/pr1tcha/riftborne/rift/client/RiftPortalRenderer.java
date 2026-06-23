package com.pr1tcha.riftborne.rift.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.rift.dimension.RiftTier;
import com.pr1tcha.riftborne.rift.portal.RiftPortalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RiftPortalRenderer implements BlockEntityRenderer<RiftPortalBlockEntity> {
    private static final ResourceLocation BODY = ResourceLocation.fromNamespaceAndPath(
            Riftborne.MODID,
            "textures/effect/standard_rift/body.png"
    );

    public RiftPortalRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(RiftPortalBlockEntity portal) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public void render(
            RiftPortalBlockEntity portal,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        if (portal.getLevel() == null) {
            return;
        }

        RiftTier tier = portal.tier();
        float age = portal.getLevel().getGameTime() + partialTick;
        long seed = portal.getBlockPos().asLong() ^ (long) tier.level() * 0x9E3779B97F4A7C15L;
        float height = 3.35F + tier.level() * 0.22F;
        float baseWidth = 0.57F + tier.level() * 0.055F;
        PortalPalette palette = PortalPalette.forTier(tier);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.08F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawToCamera(portal) + 180.0F));

        renderTierBase(poseStack, buffer, tier, seed, height, baseWidth, age, palette);

        VertexConsumer body = buffer.getBuffer(RenderType.entityTranslucentEmissive(BODY));
        renderTierAccents(poseStack, body, tier, seed, height, baseWidth, age);
        poseStack.popPose();
    }

    private static void renderTierBase(
            PoseStack poseStack,
            MultiBufferSource buffer,
            RiftTier tier,
            long seed,
            float height,
            float width,
            float age,
            PortalPalette palette
    ) {
        switch (tier) {
            case SURFACE_SHARD -> renderBaseTear(
                    poseStack, buffer, seed, height, width, age, palette,
                    0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F
            );
            case SHIFTED_LAYER -> {
                renderBaseTear(
                        poseStack, buffer, seed + 41L, height * 0.98F, width * 0.92F, age, palette,
                        -0.31F, 0.02F, -0.018F, -10.0F, 0.92F, 1.0F
                );
                renderBaseTear(
                        poseStack, buffer, seed + 83L, height * 0.91F, width * 0.84F, age + 23.0F, palette,
                        0.39F, 0.12F, 0.024F, 13.0F, 0.86F, 1.0F
                );
            }
            case NODE_RIFT -> {
                renderBaseTear(
                        poseStack, buffer, seed, height, width * 0.82F, age, palette,
                        0.0F, 0.0F, 0.0F, 0.0F, 0.90F, 1.0F
                );
                renderBaseTear(
                        poseStack, buffer, seed + 131L, height * 0.82F, width * 0.66F, age + 41.0F, palette,
                        -0.31F, height * 0.07F, 0.025F, -52.0F, 0.82F, 0.86F
                );
                renderBaseTear(
                        poseStack, buffer, seed + 197L, height * 0.80F, width * 0.64F, age + 79.0F, palette,
                        0.34F, height * 0.09F, 0.045F, 49.0F, 0.80F, 0.84F
                );
            }
            case DEEP_RIFT -> {
                renderBaseTear(
                        poseStack, buffer, seed, height * 1.10F, width * 1.18F, age, palette,
                        0.0F, -0.18F, 0.0F, -4.0F, 1.18F, 1.04F
                );
                renderBaseTear(
                        poseStack, buffer, seed + 251L, height * 0.84F, width * 0.50F, age + 61.0F, palette,
                        -0.52F, -0.48F, 0.035F, -12.0F, 0.70F, 0.92F
                );
                renderBaseTear(
                        poseStack, buffer, seed + 283L, height * 0.80F, width * 0.46F, age + 103.0F, palette,
                        0.55F, -0.55F, 0.055F, 11.0F, 0.66F, 0.90F
                );
            }
            case LIMIT_SLICE -> {
                renderBaseTear(
                        poseStack, buffer, seed, height * 0.94F, width * 0.74F, age, palette,
                        0.0F, 0.0F, 0.0F, 0.0F, 0.82F, 1.0F
                );
                renderBaseTear(
                        poseStack, buffer, seed + 307L, height * 0.92F, width * 0.70F, age + 37.0F, palette,
                        -0.08F, 0.06F, 0.028F, -61.0F, 0.88F, 0.92F
                );
                renderBaseTear(
                        poseStack, buffer, seed + 389L, height * 0.90F, width * 0.68F, age + 83.0F, palette,
                        0.10F, 0.08F, 0.055F, 58.0F, 0.86F, 0.90F
                );
            }
        }
    }

    private static void renderBaseTear(
            PoseStack poseStack,
            MultiBufferSource buffer,
            long seed,
            float height,
            float width,
            float age,
            PortalPalette palette,
            float x,
            float y,
            float z,
            float rotation,
            float scaleX,
            float scaleY
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y + height * 0.5F, z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        poseStack.scale(scaleX, scaleY, 1.0F);
        poseStack.translate(0.0F, -height * 0.5F, 0.0F);
        ProceduralRiftRenderer.renderPortalBase(
                poseStack.last(),
                buffer,
                seed,
                height,
                width,
                age,
                palette.primaryRed(),
                palette.primaryGreen(),
                palette.primaryBlue(),
                palette.secondaryRed(),
                palette.secondaryGreen(),
                palette.secondaryBlue()
        );
        poseStack.popPose();
    }

    private static void renderTierAccents(
            PoseStack poseStack,
            VertexConsumer consumer,
            RiftTier tier,
            long seed,
            float height,
            float width,
            float age
    ) {
        PortalPalette palette = PortalPalette.forTier(tier);

        switch (tier) {
            case SURFACE_SHARD -> renderBranchVeins(
                    poseStack.last(), consumer, seed, height, width, age, 3, 0.72F, palette
            );
            case SHIFTED_LAYER -> {
                renderBranchVeins(poseStack.last(), consumer, seed, height, width, age, 6, 0.94F, palette);
            }
            case NODE_RIFT -> {
                renderBranchVeins(poseStack.last(), consumer, seed, height, width, age, 12, 1.28F, palette);
                renderNode(poseStack.last(), consumer, 0.0F, height * 0.50F,
                        0.16F + Mth.sin(age * 0.09F) * 0.014F, palette, 245);
                renderOrbitShards(poseStack, consumer, seed, height, width, age, 14, 1.02F, palette);
            }
            case DEEP_RIFT -> {
                renderBranchVeins(poseStack.last(), consumer, seed, height, width, age, 5, 0.72F, palette);
                renderDownwardThreads(poseStack.last(), consumer, seed, height, width, age, palette);
            }
            case LIMIT_SLICE -> {
                renderBranchVeins(poseStack.last(), consumer, seed, height, width, age, 18, 1.72F, palette);
                renderOrbitShards(poseStack, consumer, seed, height, width, age, 26, 1.42F, palette);
                renderNode(poseStack.last(), consumer, 0.0F, height * 0.49F,
                        0.13F + Mth.sin(age * 0.17F) * 0.024F, palette, 250);
            }
        }
    }

    private static void renderGhostTear(
            PoseStack poseStack,
            VertexConsumer consumer,
            long seed,
            float height,
            float width,
            float xOffset,
            float zOffset,
            PortalPalette palette,
            int alpha
    ) {
        poseStack.pushPose();
        poseStack.translate(xOffset, height * 0.02F, zOffset);
        PoseStack.Pose pose = poseStack.last();
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            TearPoint a = tearPoint(seed, t0, height, width, 0.0F);
            TearPoint b = tearPoint(seed, t1, height, width, 0.0F);
            ribbon(consumer, pose, a.left(), a.y(), b.left(), b.y(), 0.012F,
                    palette.secondaryRed(), palette.secondaryGreen(), palette.secondaryBlue(), alpha);
            ribbon(consumer, pose, a.right(), a.y(), b.right(), b.y(), 0.012F,
                    palette.primaryRed(), palette.primaryGreen(), palette.primaryBlue(), alpha);
        }
        poseStack.popPose();
    }

    private static void renderBranchVeins(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            long seed,
            float height,
            float width,
            float age,
            int count,
            float reach,
            PortalPalette palette
    ) {
        for (int i = 0; i < count; i++) {
            float startT = 0.10F + random(seed, 100 + i) * 0.80F;
            TearPoint start = tearPoint(seed, startT, height, width, age);
            float side = i % 2 == 0 ? -1.0F : 1.0F;
            float edge = side < 0.0F ? start.left() : start.right();
            float length = width * reach * (0.42F + random(seed, 200 + i) * 0.88F);
            float endX = edge + side * length;
            float endY = start.y() + (random(seed, 300 + i) - 0.5F) * height * 0.20F;
            float flicker = 0.60F + Mth.sin(age * 0.12F + i * 1.71F) * 0.40F;
            int alpha = Mth.clamp((int) ((75 + count * 5) * flicker), 20, 205);

            float midX = Mth.lerp(0.53F, edge, endX)
                    + Mth.sin(age * 0.035F + i * 2.3F) * width * 0.08F;
            float midY = Mth.lerp(0.47F, start.y(), endY)
                    + (random(seed, 400 + i) - 0.5F) * height * 0.08F;
            ribbon(consumer, pose, edge, start.y(), midX, midY, 0.010F,
                    palette.primaryRed(), palette.primaryGreen(), palette.primaryBlue(), alpha);
            ribbon(consumer, pose, midX, midY, endX, endY, 0.007F,
                    palette.secondaryRed(), palette.secondaryGreen(), palette.secondaryBlue(), alpha * 3 / 4);
        }
    }

    private static void renderDownwardThreads(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            long seed,
            float height,
            float width,
            float age,
            PortalPalette palette
    ) {
        for (int i = -4; i <= 4; i++) {
            float x = i * width * 0.18F;
            float sway = Mth.sin(age * 0.022F + i * 1.4F) * 0.07F;
            ribbon(consumer, pose,
                    x, height * (0.18F + random(seed, 500 + i) * 0.20F),
                    x + sway, -0.35F - Math.abs(i) * 0.07F,
                    0.010F + (4 - Math.abs(i)) * 0.002F,
                    palette.primaryRed(), palette.primaryGreen(), palette.primaryBlue(),
                    68 + (4 - Math.abs(i)) * 12);
        }
    }

    private static void renderOrbitShards(
            PoseStack poseStack,
            VertexConsumer consumer,
            long seed,
            float height,
            float width,
            float age,
            int count,
            float spread,
            PortalPalette palette
    ) {
        for (int i = 0; i < count; i++) {
            float direction = i % 2 == 0 ? 1.0F : -1.0F;
            float angle = random(seed, 600 + i) * Mth.TWO_PI
                    + age * (0.008F + random(seed, 700 + i) * 0.012F) * direction;
            float radius = width * spread * (1.0F + random(seed, 800 + i) * 0.62F);
            float x = Mth.cos(angle) * radius;
            float y = height * 0.50F + Mth.sin(angle) * height * (0.24F + random(seed, 900 + i) * 0.18F);
            float size = 0.018F + random(seed, 1000 + i) * 0.035F;

            poseStack.pushPose();
            poseStack.translate(x, y, 0.07F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * direction + i * 31.0F));
            diamond(poseStack.last(), consumer, size, size * 1.8F,
                    palette.secondaryRed(), palette.secondaryGreen(), palette.secondaryBlue(),
                    95 + (i % 4) * 24);
            poseStack.popPose();
        }
    }

    private static void renderNode(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x,
            float y,
            float radius,
            PortalPalette palette,
            int alpha
    ) {
        diamond(pose, consumer, radius, radius,
                palette.secondaryRed(), palette.secondaryGreen(), palette.secondaryBlue(), alpha);
        diamond(pose, consumer, radius * 0.43F, radius * 0.43F, 220, 248, 255, alpha);
    }

    private static TearPoint tearPoint(long seed, float t, float height, float width, float age) {
        float envelope = Mth.clamp(0.10F + Mth.sin(t * Mth.PI) * 0.96F, 0.10F, 1.0F);
        float time = age * 0.006F;
        ProceduralNoise.Warp centerWarp = ProceduralNoise.warp(seed, t * 2.0F, time, 0.9F);
        ProceduralNoise.Warp edgeWarp = ProceduralNoise.warp(seed + 43L, t * 5.5F, time * 1.6F, 0.65F);
        float center = ProceduralNoise.fbm(seed + 7L, centerWarp.x(), centerWarp.y(), 4, 2.05F, 0.52F) * 0.14F;
        center += ProceduralNoise.fbm(seed + 13L, t * 9.0F, time * 1.8F, 3, 2.0F, 0.48F) * 0.035F;
        float widthNoise = ProceduralNoise.fbm(seed + 17L, edgeWarp.x(), edgeWarp.y(), 4, 2.1F, 0.5F);
        float tearNoise = ProceduralNoise.ridged(seed + 31L, t * 13.0F, time * 2.0F, 3, 2.25F, 0.55F);
        float halfWidth = width * envelope * (0.78F + widthNoise * 0.18F + tearNoise * 0.12F);
        halfWidth = Math.max(width * 0.16F, halfWidth);
        float leftRag = 0.86F + ProceduralNoise.ridged(seed + 47L, t * 18.0F, time + 1.0F, 3, 2.15F, 0.5F) * 0.26F;
        float rightRag = 0.86F + ProceduralNoise.ridged(seed + 61L, t * 18.5F, time + 5.0F, 3, 2.15F, 0.5F) * 0.26F;
        return new TearPoint(t * height, center - halfWidth * leftRag, center + halfWidth * rightRag);
    }

    private static void ribbon(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x0,
            float y0,
            float x1,
            float y1,
            float thickness,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length <= 0.0001F) {
            return;
        }
        float nx = -dy / length * thickness;
        float ny = dx / length * thickness;
        quad(consumer, pose,
                x0 - nx, y0 - ny, 0.055F, 0.0F, 0.0F,
                x0 + nx, y0 + ny, 0.055F, 1.0F, 0.0F,
                x1 + nx, y1 + ny, 0.055F, 1.0F, 1.0F,
                x1 - nx, y1 - ny, 0.055F, 0.0F, 1.0F,
                red, green, blue, alpha);
    }

    private static void diamond(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float halfWidth,
            float halfHeight,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        quad(consumer, pose,
                0.0F, halfHeight, 0.08F, 0.5F, 0.0F,
                halfWidth, 0.0F, 0.08F, 1.0F, 0.5F,
                0.0F, -halfHeight, 0.08F, 0.5F, 1.0F,
                -halfWidth, 0.0F, 0.08F, 0.0F, 0.5F,
                red, green, blue, alpha);
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            int red, int green, int blue, int alpha
    ) {
        vertex(consumer, pose, x0, y0, z0, u0, v0, red, green, blue, alpha);
        vertex(consumer, pose, x1, y1, z1, u1, v1, red, green, blue, alpha);
        vertex(consumer, pose, x2, y2, z2, u2, v2, red, green, blue, alpha);
        vertex(consumer, pose, x3, y3, z3, u3, v3, red, green, blue, alpha);
    }

    private static void vertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static float yawToCamera(RiftPortalBlockEntity portal) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double dx = camera.x - (portal.getBlockPos().getX() + 0.5D);
        double dz = camera.z - (portal.getBlockPos().getZ() + 0.5D);
        return (float) Math.toDegrees(Math.atan2(dx, dz));
    }

    private static float random(long seed, int salt) {
        long value = seed + (long) salt * 0x9E3779B97F4A7C15L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value & 0xFFFFFFL) / (float) 0xFFFFFFL;
    }

    private record TearPoint(float y, float left, float right) {
    }

    private record PortalPalette(
            int primaryRed,
            int primaryGreen,
            int primaryBlue,
            int secondaryRed,
            int secondaryGreen,
            int secondaryBlue
    ) {
        private static PortalPalette forTier(RiftTier tier) {
            return switch (tier) {
                case SURFACE_SHARD -> new PortalPalette(18, 57, 105, 47, 105, 166);
                case SHIFTED_LAYER -> new PortalPalette(83, 27, 145, 178, 58, 224);
                case NODE_RIFT -> new PortalPalette(18, 67, 112, 134, 103, 222);
                case DEEP_RIFT -> new PortalPalette(31, 12, 52, 76, 31, 110);
                case LIMIT_SLICE -> new PortalPalette(20, 168, 199, 234, 65, 199);
            };
        }
    }
}
