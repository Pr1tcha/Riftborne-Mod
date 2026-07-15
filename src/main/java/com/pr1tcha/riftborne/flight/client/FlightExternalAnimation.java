package com.pr1tcha.riftborne.flight.client;

import com.pr1tcha.riftborne.flight.FlightNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class FlightExternalAnimation {
    private FlightExternalAnimation() {
    }

    public static void apply(PlayerModel<?> model, AbstractClientPlayer player, float ageInTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        FlightClient.VisualSample sample = FlightClient.getVisualSample(player, minecraft.getTimer().getGameTimeDeltaPartialTick(false));
        if (!sample.active()) {
            return;
        }

        float activity = easeOutCubic(Mth.clamp(sample.activity(), 0.0F, 1.0F));
        float flightBlend = easeInOutCubic(Mth.clamp(sample.flightBlend(), 0.0F, 1.0F)) * activity;
        model.crouching = false;

        if (flightBlend > 0.01F) {
            float levitationAmount = activity * (1.0F - flightBlend)
                    * Mth.clamp(sample.boost() + (float) sample.motion().horizontalDistance() * 1.8F, 0.25F, 1.0F);
            if (levitationAmount > 0.01F) {
                applyLevitation(model, levitationAmount);
            }
            applyElytraModelPose(model, flightBlend);
            copyWearLayers(model);
            return;
        } else if (sample.phase() == FlightNetwork.VISUAL_LEVITATION) {
            float levitationAmount = activity * Mth.clamp(sample.boost() + (float) sample.motion().horizontalDistance() * 1.8F, 0.25F, 1.0F);
            applyLevitation(model, levitationAmount);
        } else {
            applyHover(model, player, ageInTicks, activity);
        }

        copyWearLayers(model);
    }

    private static void applyHover(PlayerModel<?> model, AbstractClientPlayer player, float ageInTicks, float amount) {
        float hover = Mth.sin(ageInTicks / 10.0F);

        rotateTo(model.rightArm, degrees(-2.5F), degrees(9.0F), degrees(8.0F - hover * 3.0F), amount);
        rotateTo(model.leftArm, degrees(4.0F), degrees(-9.0F), degrees(-8.0F + hover * 3.0F), amount);
        rotateTo(model.rightLeg, degrees(4.0F - hover * 5.0F), degrees(8.0F), degrees(3.0F), amount);
        rotateTo(model.leftLeg, degrees(-2.0F + hover * 5.0F), degrees(-8.0F), degrees(-3.0F), amount);

        float lookPitch = Mth.clamp(player.getXRot() / 90.0F, -1.0F, 1.0F);
        rotateTo(model.body, degrees(-4.0F * lookPitch), 0.0F, 0.0F, amount * 0.45F);
    }

    private static void applyLevitation(PlayerModel<?> model, float amount) {
        rotateTo(model.body, degrees(-15.0F), 0.0F, 0.0F, amount);
        rotateTo(model.head, degrees(-15.0F), model.head.yRot, 0.0F, amount * 0.75F);
        rotateTo(model.rightArm, degrees(0.0F), degrees(3.0F), degrees(12.0F), amount);
        rotateTo(model.leftArm, degrees(0.0F), degrees(-3.0F), degrees(-12.0F), amount);
        rotateTo(model.rightLeg, degrees(10.0F), degrees(2.5F), degrees(3.0F), amount);
        rotateTo(model.leftLeg, degrees(5.0F), degrees(-2.5F), degrees(-3.0F), amount);
    }

    private static void applyElytraModelPose(PlayerModel<?> model, float amount) {
        rotateTo(model.body, 0.0F, 0.0F, 0.0F, amount);
        rotateTo(model.head, degrees(-45.0F), model.head.yRot, 0.0F, amount);
        rotateTo(model.rightArm, degrees(8.0F), degrees(8.0F), degrees(6.0F), amount);
        rotateTo(model.leftArm, degrees(8.0F), degrees(-8.0F), degrees(-6.0F), amount);
        rotateTo(model.rightLeg, degrees(8.0F), degrees(2.0F), degrees(1.5F), amount);
        rotateTo(model.leftLeg, degrees(8.0F), degrees(-2.0F), degrees(-1.5F), amount);
    }

    public static void applyElytraRenderRotation(AbstractClientPlayer player, com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
        FlightClient.VisualSample sample = FlightClient.getVisualSample(player, partialTick);
        if (sample.activity() <= 0.01F || sample.flightBlend() <= 0.01F) {
            return;
        }

        float amount = easeOutCubic(Mth.clamp(sample.activity(), 0.0F, 1.0F))
                * easeInOutCubic(Mth.clamp(sample.flightBlend(), 0.0F, 1.0F));
        float viewPitch = player.getViewXRot(partialTick);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(amount * (-90.0F - viewPitch)));

        Vec3 look = player.getViewVector(partialTick);
        Vec3 motion = sample.motion();
        if (motion.horizontalDistanceSqr() < 1.0E-5D) {
            motion = player.getDeltaMovementLerped(partialTick);
        }

        double motionHorizontal = motion.horizontalDistanceSqr();
        double lookHorizontal = look.horizontalDistanceSqr();
        if (motionHorizontal > 1.0E-5D && lookHorizontal > 1.0E-5D) {
            double dot = (motion.x * look.x + motion.z * look.z) / Math.sqrt(motionHorizontal * lookHorizontal);
            double clampedDot = Mth.clamp(dot, -1.0D, 1.0D);
            double cross = motion.x * look.z - motion.z * look.x;
            float yaw = (float) (Math.signum(cross) * Math.acos(clampedDot));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotation(yaw * amount));
        }
    }

    public static boolean shouldSuppressCrouch(AbstractClientPlayer player, float partialTick) {
        return FlightClient.getVisualSample(player, partialTick).active();
    }

    private static void rotateTo(ModelPart part, float targetX, float targetY, float targetZ, float amount) {
        float clamped = Mth.clamp(amount, 0.0F, 1.0F);
        part.xRot = Mth.lerp(clamped, part.xRot, targetX);
        part.yRot = Mth.lerp(clamped, part.yRot, targetY);
        part.zRot = Mth.lerp(clamped, part.zRot, targetZ);
    }

    private static void copyWearLayers(PlayerModel<?> model) {
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
        model.jacket.copyFrom(model.body);
    }

    private static float degrees(float degrees) {
        return degrees * Mth.DEG_TO_RAD;
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse;
    }

    private static float easeInOutCubic(float value) {
        if (value < 0.5F) {
            return 4.0F * value * value * value;
        }
        float inverse = -2.0F * value + 2.0F;
        return 1.0F - inverse * inverse * inverse / 2.0F;
    }
}
