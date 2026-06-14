package com.pr1tcha.Rifts.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pr1tcha.Rifts.RiftData.RiftBlockEntity;
import com.pr1tcha.Rifts.RiftData.RiftData;
import com.pr1tcha.Rifts.RiftData.RiftStage;
import com.pr1tcha.Rifts.Config;
import com.pr1tcha.Rifts.RiftborneRift;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ProceduralRiftRenderer {
    private static final ResourceLocation SOLID_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RiftborneRift.MODID,
            "textures/effect/procedural_rift_solid.png"
    );
    private static final ResourceLocation BODY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RiftborneRift.MODID,
            "textures/effect/story_rift.png"
    );
    private static final ResourceLocation HAZE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RiftborneRift.MODID,
            "textures/effect/story_rift_haze.png"
    );
    private static final int SEGMENTS = 72;

    private ProceduralRiftRenderer() {
    }

    public static void render(RiftBlockEntity rift, float partialTick, PoseStack poseStack, MultiBufferSource buffer) {
        float age = getAge(rift, partialTick);
        long seed = rift.getData().id.getMostSignificantBits() ^ rift.getData().id.getLeastSignificantBits();
        RiftStage stage = rift.getData().stage;
        boolean portalRift = RiftData.PORTAL_RIFT_TYPE.equals(rift.getData().riftType);
        float openingProgress = getOpeningProgress(rift);
        float typeScale = portalRift ? 2.35F : 1.0F;
        float widthScale = portalRift ? 1.62F : 1.0F;
        float stageScale = getStageScale(stage, openingProgress, portalRift) * typeScale;
        float height = 2.95F * stageScale;
        float baseWidth = 0.5F * stageScale * widthScale;
        float alpha = getAlpha(stage);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.1F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(getYawToCamera(rift) + 180.0F));

        PoseStack.Pose pose = poseStack.last();
        VeilRiftDistortion.recordRenderedRift(rift, pose, height, baseWidth, alpha);

        VertexConsumer solidBody = buffer.getBuffer(RenderType.entityCutoutNoCull(SOLID_TEXTURE));
        renderSolidBody(pose, solidBody, seed, height, baseWidth, age, stage);

        VertexConsumer body = buffer.getBuffer(RenderType.entityTranslucentEmissive(BODY_TEXTURE));
        renderEnergySkin(pose, body, seed, height, baseWidth, alpha, age, stage);
        renderGlowShell(pose, body, seed, height, baseWidth, alpha, age, stage);
        renderOpeningPixelBurst(pose, body, seed, height, baseWidth, alpha, openingProgress, age, stage, portalRift);

        VertexConsumer haze = buffer.getBuffer(RenderType.entityTranslucentEmissive(HAZE_TEXTURE));
        renderRefractionShell(pose, haze, seed, height, baseWidth, alpha, age, stage);
        renderHaze(pose, haze, height, baseWidth, alpha, age, stage);

        VertexConsumer energy = buffer.getBuffer(RenderType.lightning());
        renderOpeningConvergence(pose, energy, seed, height, baseWidth, alpha, openingProgress, age, stage, portalRift);
        renderOpeningShockLines(pose, energy, seed, height, baseWidth, alpha, openingProgress, age, stage, portalRift);
        renderEdges(pose, energy, seed, height, baseWidth, alpha, age, stage);
        renderVeins(pose, energy, seed, height, baseWidth, alpha, age, stage);

        poseStack.popPose();
    }

    private static float getYawToCamera(RiftBlockEntity rift) {
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        BlockPos blockPos = rift.getBlockPos();
        double dx = cameraPos.x - (blockPos.getX() + 0.5D);
        double dz = cameraPos.z - (blockPos.getZ() + 0.5D);
        return (float) Math.toDegrees(Math.atan2(dx, dz));
    }

    private static void renderSolidBody(PoseStack.Pose pose, VertexConsumer consumer, long seed, float height, float baseWidth, float age, RiftStage stage) {
        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;
            RiftSlice a = slice(seed, t0, height, baseWidth, age, stage);
            RiftSlice b = slice(seed, t1, height, baseWidth, age, stage);

            texturedQuad(consumer, pose,
                    a.leftEdge(), a.y(), 0.01F, 0.0F, t0,
                    a.rightEdge(), a.y(), 0.01F, 1.0F, t0,
                    b.rightEdge(), b.y(), 0.01F, 1.0F, t1,
                    b.leftEdge(), b.y(), 0.01F, 0.0F, t1,
                    5, 0, 13, 255);

            texturedQuad(consumer, pose,
                    a.leftInner(), a.y(), 0.012F, 0.18F, t0,
                    a.rightInner(), a.y(), 0.012F, 0.82F, t0,
                    b.rightInner(), b.y(), 0.012F, 0.82F, t1,
                    b.leftInner(), b.y(), 0.012F, 0.18F, t1,
                    0, 0, 5, 255);
        }
    }

    private static void renderEnergySkin(PoseStack.Pose pose, VertexConsumer consumer, long seed, float height, float baseWidth, float alpha, float age, RiftStage stage) {
        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;
            RiftSlice a = slice(seed, t0, height, baseWidth, age, stage);
            RiftSlice b = slice(seed, t1, height, baseWidth, age, stage);

            texturedQuad(consumer, pose,
                    a.leftInner(), a.y(), 0.004F, 0.18F, t0,
                    a.rightInner(), a.y(), 0.004F, 0.82F, t0,
                    b.rightInner(), b.y(), 0.004F, 0.82F, t1,
                    b.leftInner(), b.y(), 0.004F, 0.18F, t1,
                    22, 4, 42, Mth.clamp((int) (alpha * (62.0F + a.darkness() * 22.0F)), 0, 96));

            texturedQuad(consumer, pose,
                    a.leftOuter(), a.y(), -0.006F, 0.0F, t0,
                    a.leftInner(), a.y(), -0.006F, 0.35F, t0,
                    b.leftInner(), b.y(), -0.006F, 0.35F, t1,
                    b.leftOuter(), b.y(), -0.006F, 0.0F, t1,
                    112, 24, 190, Mth.clamp((int) (alpha * (118.0F + a.edgeHeat() * 46.0F)), 0, 190));

            texturedQuad(consumer, pose,
                    a.rightInner(), a.y(), -0.006F, 0.65F, t0,
                    a.rightOuter(), a.y(), -0.006F, 1.0F, t0,
                    b.rightOuter(), b.y(), -0.006F, 1.0F, t1,
                    b.rightInner(), b.y(), -0.006F, 0.65F, t1,
                    126, 28, 210, Mth.clamp((int) (alpha * (118.0F + a.edgeHeat() * 46.0F)), 0, 190));
        }
    }

    private static void renderGlowShell(PoseStack.Pose pose, VertexConsumer consumer, long seed, float height, float baseWidth, float alpha, float age, RiftStage stage) {
        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;
            RiftSlice a = slice(seed, t0, height, baseWidth * 1.2F, age + 15.0F, stage);
            RiftSlice b = slice(seed, t1, height, baseWidth * 1.2F, age + 15.0F, stage);
            float flicker = 0.55F + Mth.sin(age * 0.07F + i * 0.41F) * 0.25F + a.edgeHeat() * 0.25F;
            int shellAlpha = Mth.clamp((int) (alpha * flicker * 88.0F), 14, 112);

            texturedQuad(consumer, pose,
                    a.leftOuter(), a.y(), -0.022F, 0.0F, t0,
                    a.rightOuter(), a.y(), -0.022F, 1.0F, t0,
                    b.rightOuter(), b.y(), -0.022F, 1.0F, t1,
                    b.leftOuter(), b.y(), -0.022F, 0.0F, t1,
                    134, 44, 255, shellAlpha);
        }
    }

    private static void renderRefractionShell(PoseStack.Pose pose, VertexConsumer consumer, long seed, float height, float baseWidth, float alpha, float age, RiftStage stage) {
        float stageStrength = switch (stage) {
            case OPENING -> 0.55F;
            case DORMANT -> 0.08F;
            case REACTING -> 0.22F;
            case CRACKING -> 0.48F;
            case ACTIVE -> 0.82F;
            case UNSTABLE -> 1.15F;
            case COLLAPSING -> 1.3F;
            case SCAR -> 0.28F;
        };
        float time = age * 0.01F;

        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;
            RiftSlice a = slice(seed, t0, height, baseWidth, age, stage);
            RiftSlice b = slice(seed, t1, height, baseWidth, age, stage);
            float envelope = Mth.sin(((t0 + t1) * 0.5F) * Mth.PI);
            float flutter = ProceduralNoise.fbm(seed + 719L, t0 * 12.0F, time, 3, 2.1F, 0.5F);
            float strength = Mth.clamp((0.4F + envelope * 0.75F + flutter * 0.18F) * stageStrength, 0.0F, 1.35F);

            renderRefractionSide(consumer, pose, a, b, -1.0F, t0, t1, strength, alpha, age, seed);
            renderRefractionSide(consumer, pose, a, b, 1.0F, t0, t1, strength, alpha, age, seed + 37L);
        }
    }

    private static void renderRefractionSide(VertexConsumer consumer, PoseStack.Pose pose, RiftSlice a, RiftSlice b, float side, float t0, float t1, float strength, float alpha, float age, long seed) {
        float edgeA = side < 0.0F ? a.leftOuter() : a.rightOuter();
        float edgeB = side < 0.0F ? b.leftOuter() : b.rightOuter();
        float midA = side < 0.0F ? a.leftEdge() : a.rightEdge();
        float midB = side < 0.0F ? b.leftEdge() : b.rightEdge();
        float wobbleA = ProceduralNoise.fbm(seed + 811L, t0 * 18.0F, age * 0.012F, 2, 2.0F, 0.5F);
        float wobbleB = ProceduralNoise.fbm(seed + 811L, t1 * 18.0F, age * 0.012F, 2, 2.0F, 0.5F);
        float outerA = edgeA + side * (0.14F + strength * 0.18F + wobbleA * 0.035F);
        float outerB = edgeB + side * (0.14F + strength * 0.18F + wobbleB * 0.035F);
        float farA = edgeA + side * (0.28F + strength * 0.3F + wobbleA * 0.055F);
        float farB = edgeB + side * (0.28F + strength * 0.3F + wobbleB * 0.055F);
        int innerAlpha = Mth.clamp((int) (alpha * strength * 72.0F), 0, 92);
        int outerAlpha = Mth.clamp((int) (alpha * strength * 34.0F), 0, 52);
        float shimmer = 0.5F + Mth.sin(age * 0.09F + t0 * 37.0F) * 0.5F;

        texturedQuad(consumer, pose,
                midA, a.y(), -0.035F, 0.24F, t0,
                outerA, a.y(), -0.035F, 0.72F, t0,
                outerB, b.y(), -0.035F, 0.72F, t1,
                midB, b.y(), -0.035F, 0.24F, t1,
                184, 126, 255, innerAlpha);

        texturedQuad(consumer, pose,
                edgeA, a.y(), -0.047F, 0.18F, t0,
                farA, a.y(), -0.047F, 0.92F, t0,
                farB, b.y(), -0.047F, 0.92F, t1,
                edgeB, b.y(), -0.047F, 0.18F, t1,
                183, 92, 255, outerAlpha);

        if (((int) (t0 * 1000.0F) + (int) (seed & 7L)) % 11 == 0) {
            float glintY0 = Mth.lerp(0.35F, a.y(), b.y());
            float glintY1 = glintY0 + 0.06F + shimmer * 0.06F;
            float glintX = Mth.lerp(0.55F, edgeA, outerA);
            texturedQuad(consumer, pose,
                    glintX - side * 0.012F, glintY0, -0.028F, 0.35F, 0.0F,
                    glintX + side * (0.055F + shimmer * 0.045F), glintY0 + 0.025F, -0.028F, 0.72F, 0.0F,
                    glintX + side * (0.047F + shimmer * 0.04F), glintY1, -0.028F, 0.72F, 1.0F,
                    glintX - side * 0.01F, glintY1 - 0.018F, -0.028F, 0.35F, 1.0F,
                    235, 210, 255, Mth.clamp((int) (alpha * strength * 105.0F), 0, 145));
        }
    }

    private static void renderEdges(PoseStack.Pose pose, VertexConsumer consumer, long seed, float height, float baseWidth, float alpha, float age, RiftStage stage) {
        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;
            RiftSlice a = slice(seed, t0, height, baseWidth, age, stage);
            RiftSlice b = slice(seed, t1, height, baseWidth, age, stage);
            int edgeAlpha = Mth.clamp((int) (alpha * (122.0F + a.edgeHeat() * 72.0F + Mth.sin(age * 0.17F + i) * 28.0F)), 35, 210);

            ribbon(consumer, pose, a.leftEdge(), a.y(), b.leftEdge(), b.y(), 0.017F, 185, 58, 255, edgeAlpha);
            ribbon(consumer, pose, a.rightEdge(), a.y(), b.rightEdge(), b.y(), 0.017F, 205, 72, 255, edgeAlpha);

            if (i % 9 == 0) {
                ribbon(consumer, pose, a.leftOuter(), a.y(), b.leftOuter(), b.y(), 0.008F, 238, 224, 255, edgeAlpha / 2);
                ribbon(consumer, pose, a.rightOuter(), a.y(), b.rightOuter(), b.y(), 0.008F, 238, 224, 255, edgeAlpha / 2);
            }
        }

        renderEndpointSeams(pose, consumer, seed, height, baseWidth, alpha, age, stage);
    }

    private static void renderVeins(PoseStack.Pose pose, VertexConsumer consumer, long seed, float height, float baseWidth, float alpha, float age, RiftStage stage) {
        int veinCount = switch (stage) {
            case DORMANT -> 1;
            case REACTING -> 3;
            case CRACKING, OPENING -> 6;
            case UNSTABLE, COLLAPSING -> 9;
            default -> 5;
        };
        for (int i = 0; i < veinCount; i++) {
            float start = hash(seed, 300 + i) * 0.72F + 0.08F;
            float length = 0.12F + hash(seed, 350 + i) * 0.22F;
            float side = hash(seed, 400 + i) < 0.5F ? -1.0F : 1.0F;
            float t0 = Mth.clamp(start + Mth.sin(age * 0.025F + i) * 0.018F, 0.02F, 0.96F);
            float t1 = Mth.clamp(t0 + length, 0.04F, 0.98F);
            RiftSlice a = slice(seed, t0, height, baseWidth, age, stage);
            RiftSlice b = slice(seed, t1, height, baseWidth, age, stage);
            float x0 = side < 0 ? a.leftInner() : a.rightInner();
            float x1 = side < 0 ? b.leftEdge() : b.rightEdge();
            int veinAlpha = Mth.clamp((int) (alpha * (105.0F + Mth.sin(age * 0.55F + i * 1.3F) * 70.0F)), 30, 210);

            ribbon(consumer, pose, x0, a.y(), x1, b.y(), 0.011F, 238, 218, 255, veinAlpha);
        }
    }

    private static void renderOpeningConvergence(PoseStack.Pose pose, VertexConsumer consumer, long seed, float height, float baseWidth, float alpha, float progress, float age, RiftStage stage, boolean portalRift) {
        if (stage != RiftStage.OPENING) {
            return;
        }

        float gather = smoothstep(Mth.clamp(progress / 0.62F, 0.0F, 1.0F));
        float fade = 1.0F - smoothstep(Mth.clamp((progress - 0.54F) / 0.14F, 0.0F, 1.0F));
        if (fade <= 0.01F) {
            return;
        }

        int crackCount = portalRift ? 34 : 22;
        float centerY = height * ((portalRift ? 0.5F : 0.42F) + Mth.sin(age * 0.06F) * 0.025F);
        float reach = portalRift ? 1.55F : 1.0F;
        float pullWidth = portalRift ? 0.58F : 0.28F;
        for (int i = 0; i < crackCount; i++) {
            float side = i % 2 == 0 ? -1.0F : 1.0F;
            float ySeed = hash(seed, 700 + i);
            float xSeed = hash(seed, 740 + i);
            float startX = side * baseWidth * reach * (2.2F + xSeed * 3.2F);
            float startY = height * (portalRift ? (0.02F + ySeed * 0.96F) : (0.08F + ySeed * 0.84F));
            float wobble = Mth.sin(age * (0.12F + xSeed * 0.08F) + i * 1.7F) * 0.035F;
            float endX = Mth.lerp(gather, startX, side * baseWidth * (0.22F + xSeed * pullWidth)) + wobble;
            float endY = Mth.lerp(gather, startY, centerY + (ySeed - 0.5F) * height * (portalRift ? 0.3F : 0.18F));
            float tailX = Mth.lerp(0.44F, startX, endX);
            float tailY = Mth.lerp(0.44F, startY, endY);
            int crackAlpha = Mth.clamp((int) (alpha * fade * (portalRift ? 125.0F : 95.0F + gather * 170.0F)), 0, 245);
            float thickness = (0.007F + gather * 0.016F + hash(seed, 780 + i) * 0.007F) * (portalRift ? 1.35F : 1.0F);
            int red = portalRift ? 92 : 156;
            int green = portalRift ? 92 : 48;
            int blue = 255;

            ribbon(consumer, pose, tailX, tailY, endX, endY, thickness, red, green, blue, crackAlpha);
            if (i % 3 == 0) {
                float branchX = Mth.lerp(0.62F, tailX, endX);
                float branchY = Mth.lerp(0.62F, tailY, endY);
                ribbon(consumer, pose, branchX, branchY, branchX - side * baseWidth * (portalRift ? 0.52F : 0.28F), branchY + height * (portalRift ? 0.082F : 0.055F), thickness * 0.64F, 236, 220, 255, crackAlpha / 2);
            }
        }

    }

    private static void renderOpeningPixelBurst(PoseStack.Pose pose, VertexConsumer consumer, long seed, float height, float baseWidth, float alpha, float progress, float age, RiftStage stage, boolean portalRift) {
        if (stage != RiftStage.OPENING) {
            return;
        }

        float burst = openingBurst(progress);
        if (burst <= 0.01F) {
            return;
        }

        float centerY = height * (portalRift ? 0.5F : 0.46F);
        int particleCount = portalRift ? 78 : 46;
        float spread = portalRift ? 1.72F : 1.0F;
        for (int i = 0; i < particleCount; i++) {
            float angle = hash(seed, 1500 + i) * Mth.TWO_PI;
            float lane = hash(seed, 1540 + i);
            float speed = 0.55F + hash(seed, 1580 + i) * 1.65F;
            float distance = baseWidth * spread * (0.45F + speed * (0.45F + burst * 1.95F));
            float vertical = (hash(seed, 1620 + i) - 0.5F) * height * (portalRift ? 0.34F + burst * 0.72F : 0.18F + burst * 0.52F);
            float x = Mth.cos(angle) * distance * (0.75F + lane * 0.55F);
            float y = centerY + Mth.sin(angle) * distance * 0.36F + vertical;
            float twinkle = 0.72F + Mth.sin(age * 0.58F + i * 1.37F) * 0.28F;
            float size = baseWidth * (0.035F + hash(seed, 1660 + i) * 0.075F) * (1.0F + burst * (portalRift ? 1.15F : 0.7F));
            int particleAlpha = Mth.clamp((int) (alpha * burst * twinkle * 245.0F), 0, 245);

            int red = portalRift ? 80 + (int) (hash(seed, 1740 + i) * 70.0F) : hash(seed, 1700 + i) > 0.78F ? 238 : 118 + (int) (hash(seed, 1740 + i) * 75.0F);
            int green = portalRift ? 80 + (int) (hash(seed, 1820 + i) * 95.0F) : hash(seed, 1780 + i) > 0.82F ? 226 : 18 + (int) (hash(seed, 1820 + i) * 38.0F);
            int blue = 210 + (int) (hash(seed, 1860 + i) * 45.0F);
            pixelQuad(consumer, pose, x, y, 0.082F, size, red, green, blue, particleAlpha);

            if (i % 5 == 0) {
                pixelQuad(consumer, pose, x, y, 0.086F, size * 0.42F, 255, 238, 255, Mth.clamp((int) (particleAlpha * 0.82F), 0, 230));
            }
        }
    }

    private static void renderOpeningShockLines(PoseStack.Pose pose, VertexConsumer consumer, long seed, float height, float baseWidth, float alpha, float progress, float age, RiftStage stage, boolean portalRift) {
        if (stage != RiftStage.OPENING) {
            return;
        }

        float burst = openingBurst(progress);
        if (burst <= 0.01F) {
            return;
        }

        float centerY = height * (portalRift ? 0.5F : 0.46F);
        int lineCount = portalRift ? 20 : 12;
        for (int i = 0; i < lineCount; i++) {
            float side = i % 2 == 0 ? -1.0F : 1.0F;
            float yJitter = (hash(seed, 1900 + i) - 0.5F) * height * (portalRift ? 0.42F : 0.22F);
            float x0 = side * baseWidth * (0.35F + hash(seed, 1940 + i) * 0.35F);
            float x1 = side * baseWidth * (1.35F + hash(seed, 1980 + i) * (portalRift ? 2.35F : 1.45F)) * (0.55F + burst);
            float y0 = centerY + yJitter * 0.35F;
            float y1 = centerY + yJitter + (hash(seed, 2020 + i) - 0.5F) * height * 0.14F;
            int shockAlpha = Mth.clamp((int) (alpha * burst * 190.0F), 0, 210);

            ribbon(consumer, pose, x0, y0, x1, y1, (0.009F + burst * 0.012F) * (portalRift ? 1.35F : 1.0F), 220, 182, 255, shockAlpha);
        }
    }

    private static void renderEndpointSeams(PoseStack.Pose pose, VertexConsumer consumer, long seed, float height, float baseWidth, float alpha, float age, RiftStage stage) {
        if (stage == RiftStage.SCAR) {
            return;
        }

        renderEndpointLine(pose, consumer, seed, height, baseWidth, alpha, age, stage, false);
        renderEndpointLine(pose, consumer, seed, height, baseWidth, alpha, age, stage, true);
    }

    private static void renderEndpointLine(PoseStack.Pose pose, VertexConsumer consumer, long seed, float height, float baseWidth, float alpha, float age, RiftStage stage, boolean top) {
        float t = top ? 0.985F : 0.015F;
        RiftSlice slice = slice(seed, t, height, baseWidth, age, stage);
        float left = slice.leftEdge();
        float right = slice.rightEdge();
        float y = slice.y();
        int seamAlpha = Mth.clamp((int) (alpha * 132.0F), 36, 158);

        ribbon(consumer, pose, left, y, right, y, 0.007F, 142, 34, 212, seamAlpha);
    }

    private static void renderHaze(PoseStack.Pose pose, VertexConsumer haze, float height, float baseWidth, float alpha, float age, RiftStage stage) {
        float stageAlpha = switch (stage) {
            case DORMANT -> 0.08F;
            case REACTING -> 0.18F;
            case CRACKING -> 0.34F;
            case OPENING -> 0.56F;
            case ACTIVE -> 0.42F;
            case UNSTABLE -> 0.64F;
            case COLLAPSING -> 0.72F;
            case SCAR -> 0.14F;
        };
        float breath = 1.0F + Mth.sin(age * 0.055F) * 0.055F;

        hazeQuad(haze, pose, baseWidth * 3.0F * breath, height * 1.08F, -0.052F, 145, 116, 220, Mth.clamp((int) (alpha * stageAlpha * 178.0F), 0, 175));
        hazeQuad(haze, pose, baseWidth * 2.18F, height * 1.16F, -0.06F, 214, 192, 255, Mth.clamp((int) (alpha * stageAlpha * 96.0F), 0, 112));
    }

    private static RiftSlice slice(long seed, float t, float height, float baseWidth, float age, RiftStage stage) {
        float y = t * height;
        float envelope = Mth.sin(t * Mth.PI);
        envelope = Mth.clamp(0.1F + envelope * 0.96F, 0.1F, 1.0F);
        float stageMotion = switch (stage) {
            case DORMANT -> 0.002F;
            case REACTING -> 0.006F;
            case CRACKING -> 0.011F;
            case UNSTABLE, COLLAPSING -> 0.016F;
            default -> 0.007F;
        };
        float time = age * 0.006F;
        ProceduralNoise.Warp centerWarp = ProceduralNoise.warp(seed, t * 2.0F, time, 0.9F);
        ProceduralNoise.Warp edgeWarp = ProceduralNoise.warp(seed + 43L, t * 5.5F, time * 1.6F, 0.65F);

        float center = ProceduralNoise.fbm(seed + 7L, centerWarp.x(), centerWarp.y(), 4, 2.05F, 0.52F) * 0.14F;
        center += ProceduralNoise.fbm(seed + 13L, t * 9.0F, time * 1.8F, 3, 2.0F, 0.48F) * 0.035F;
        center += Mth.sin(age * 0.023F + t * 11.0F) * stageMotion;

        float widthNoise = ProceduralNoise.fbm(seed + 17L, edgeWarp.x(), edgeWarp.y(), 4, 2.1F, 0.5F);
        float tearNoise = ProceduralNoise.ridged(seed + 31L, t * 13.0F, time * 2.0F, 3, 2.25F, 0.55F);
        float stageWidth = switch (stage) {
            case DORMANT -> 0.34F;
            case REACTING -> 0.52F;
            case CRACKING -> 0.74F;
            case OPENING -> 1.0F;
            default -> 1.0F;
        };
        float width = baseWidth * envelope * stageWidth * (0.78F + widthNoise * 0.18F + tearNoise * 0.12F);
        width = Math.max(baseWidth * 0.16F, width);

        float leftRag = 0.86F + ProceduralNoise.ridged(seed + 47L, t * 18.0F, time + 1.0F, 3, 2.15F, 0.5F) * 0.26F;
        float rightRag = 0.86F + ProceduralNoise.ridged(seed + 61L, t * 18.5F, time + 5.0F, 3, 2.15F, 0.5F) * 0.26F;
        float innerWidth = width * (0.32F + envelope * 0.1F);
        float edgeWidth = width * 0.68F;
        float outerWidth = width * (0.94F + tearNoise * 0.12F);
        float heat = Mth.clamp((tearNoise + 1.0F) * 0.5F, 0.0F, 1.0F);
        float darkness = Mth.clamp(1.0F - Math.abs(widthNoise) * 0.45F, 0.0F, 1.0F);

        return new RiftSlice(
                y,
                center - innerWidth * leftRag,
                center + innerWidth * rightRag,
                center - edgeWidth * leftRag,
                center + edgeWidth * rightRag,
                center - outerWidth * leftRag,
                center + outerWidth * rightRag,
                heat,
                darkness
        );
    }

    private static float wave(long seed, float t, float frequency, float amplitude) {
        return Mth.sin(t * frequency + hash(seed, (int) frequency * 13) * Mth.TWO_PI) * amplitude;
    }

    private static float hash(long seed, int salt) {
        long value = seed + salt * 0x9E3779B97F4A7C15L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    private static void ribbon(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float thickness, int red, int green, int blue, int alpha) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length <= 0.0001F) {
            return;
        }

        float nx = -dy / length * thickness;
        float ny = dx / length * thickness;
        quad(consumer, pose,
                x0 - nx, y0 - ny, 0.018F,
                x0 + nx, y0 + ny, 0.018F,
                x1 + nx, y1 + ny, 0.018F,
                x1 - nx, y1 - ny, 0.018F,
                red, green, blue, alpha);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             int red, int green, int blue, int alpha) {
        vertex(consumer, pose, x0, y0, z0, red, green, blue, alpha);
        vertex(consumer, pose, x1, y1, z1, red, green, blue, alpha);
        vertex(consumer, pose, x2, y2, z2, red, green, blue, alpha);
        vertex(consumer, pose, x3, y3, z3, red, green, blue, alpha);
    }

    private static void texturedQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                     float x0, float y0, float z0, float u0, float v0,
                                     float x1, float y1, float z1, float u1, float v1,
                                     float x2, float y2, float z2, float u2, float v2,
                                     float x3, float y3, float z3, float u3, float v3,
                                     int red, int green, int blue, int alpha) {
        texturedVertex(consumer, pose, x0, y0, z0, u0, v0, red, green, blue, alpha);
        texturedVertex(consumer, pose, x1, y1, z1, u1, v1, red, green, blue, alpha);
        texturedVertex(consumer, pose, x2, y2, z2, u2, v2, red, green, blue, alpha);
        texturedVertex(consumer, pose, x3, y3, z3, u3, v3, red, green, blue, alpha);
    }

    private static void hazeQuad(VertexConsumer consumer, PoseStack.Pose pose, float width, float height, float z, int red, int green, int blue, int alpha) {
        float half = width * 0.5F;
        texturedQuad(consumer, pose,
                -half, -height * 0.03F, z, 0.0F, 1.0F,
                half, -height * 0.03F, z, 1.0F, 1.0F,
                half * 0.72F, height, z, 1.0F, 0.0F,
                -half * 0.72F, height, z, 0.0F, 0.0F,
                red, green, blue, alpha);
    }

    private static void pixelQuad(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float size, int red, int green, int blue, int alpha) {
        float half = size * 0.5F;
        texturedQuad(consumer, pose,
                x - half, y - half, z, 0.0F, 1.0F,
                x + half, y - half, z, 1.0F, 1.0F,
                x + half, y + half, z, 1.0F, 0.0F,
                x - half, y + half, z, 0.0F, 0.0F,
                red, green, blue, alpha);
    }

    private static void renderTexturedRibbon(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float thickness, int red, int green, int blue, int alpha) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length <= 0.0001F) {
            return;
        }

        float nx = -dy / length * thickness;
        float ny = dx / length * thickness;
        texturedQuad(consumer, pose,
                x0 - nx, y0 - ny, 0.058F, 0.0F, 0.0F,
                x0 + nx, y0 + ny, 0.058F, 1.0F, 0.0F,
                x1 + nx, y1 + ny, 0.058F, 1.0F, 1.0F,
                x1 - nx, y1 - ny, 0.058F, 0.0F, 1.0F,
                red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, x, y, z).setColor(red, green, blue, alpha);
    }

    private static void texturedVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static float getAge(RiftBlockEntity rift, float partialTick) {
        if (rift.getLevel() == null) {
            return partialTick;
        }

        return rift.getLevel().getGameTime() + partialTick;
    }

    private static float getOpeningProgress(RiftBlockEntity rift) {
        if (rift.getData().stage != RiftStage.OPENING) {
            return 1.0F;
        }

        return Mth.clamp(rift.getData().stageTicks / (float) Config.riftOpeningDurationTicks.get(), 0.0F, 1.0F);
    }

    private static float getStageScale(RiftStage stage, float openingProgress, boolean portalRift) {
        if (portalRift) {
            return switch (stage) {
                case DORMANT -> 0.5F;
                case REACTING -> 0.66F;
                case CRACKING -> 0.82F;
                case OPENING -> portalOpeningScale(openingProgress);
                case ACTIVE -> 1.0F;
                case UNSTABLE -> 1.18F;
                case COLLAPSING -> 1.34F;
                case SCAR -> 0.36F;
                default -> 0.62F;
            };
        }

        return switch (stage) {
            case DORMANT -> 0.22F;
            case REACTING -> 0.36F;
            case CRACKING -> 0.58F;
            case OPENING -> openingScale(openingProgress);
            case ACTIVE -> 1.0F;
            case UNSTABLE -> 1.18F;
            case COLLAPSING -> 1.34F;
            case SCAR -> 0.36F;
        };
    }

    private static float getAlpha(RiftStage stage) {
        return switch (stage) {
            case DORMANT -> 0.42F;
            case REACTING -> 0.62F;
            case CRACKING -> 0.78F;
            case OPENING -> 0.92F;
            case ACTIVE -> 0.92F;
            case UNSTABLE -> 1.0F;
            case COLLAPSING -> 0.98F;
            case SCAR -> 0.34F;
        };
    }

    private static float openingScale(float progress) {
        return progress < 0.72F ? 0.58F : 1.0F;
    }

    private static float portalOpeningScale(float progress) {
        return progress < 0.72F ? 0.82F : 1.0F;
    }

    private static float openingBurst(float progress) {
        float burst = 1.0F - Math.abs(progress - 0.72F) / 0.1F;
        burst = Mth.clamp(burst, 0.0F, 1.0F);
        return burst * burst * (3.0F - 2.0F * burst);
    }

    private static float smoothstep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private record RiftSlice(float y, float leftInner, float rightInner, float leftEdge, float rightEdge, float leftOuter, float rightOuter, float edgeHeat, float darkness) {
    }
}
