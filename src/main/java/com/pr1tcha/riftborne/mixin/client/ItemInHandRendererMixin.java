package com.pr1tcha.riftborne.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pr1tcha.riftborne.rna.combat.client.BarrierClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private void riftborne$transformBarrierHand(
            AbstractClientPlayer player,
            float partialTick,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo callback
    ) {
        HumanoidArm arm = physicalArm(player, hand);
        BarrierClientState.PoseSample pose = BarrierClientState.sample(player.getUUID(), partialTick);
        if (pose.occupies(arm)) {
            applyBarrierTransform(poseStack, arm, pose);
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void riftborne$renderOccupiedEmptyOffhand(
            AbstractClientPlayer player,
            float partialTick,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo callback
    ) {
        if (hand != InteractionHand.OFF_HAND || !stack.isEmpty()) {
            return;
        }
        HumanoidArm arm = physicalArm(player, hand);
        BarrierClientState.PoseSample pose = BarrierClientState.sample(player.getUUID(), partialTick);
        if (!pose.occupies(arm) || player.isInvisible()) {
            return;
        }

        poseStack.pushPose();
        applyBarrierTransform(poseStack, arm, pose);
        applyVanillaEmptyArmTransform(poseStack, arm, equipProgress, swingProgress);
        if (Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            if (arm == HumanoidArm.RIGHT) {
                renderer.renderRightHand(poseStack, bufferSource, packedLight, player);
            } else {
                renderer.renderLeftHand(poseStack, bufferSource, packedLight, player);
            }
        }
        poseStack.popPose();
        callback.cancel();
    }

    private static HumanoidArm physicalArm(AbstractClientPlayer player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }

    private static void applyBarrierTransform(
            PoseStack poseStack,
            HumanoidArm arm,
            BarrierClientState.PoseSample pose
    ) {
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float forward = Math.max(0.0F, pose.drive()) * pose.weight();
        float pull = Math.max(0.0F, -pose.drive()) * pose.weight();
        poseStack.translate(
                side * -0.27F * pose.weight(),
                0.30F * forward - 0.08F * pull,
                -0.56F * forward + 0.20F * pull
        );
        poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F * forward + 9.0F * pull));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -11.0F * forward));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * (7.0F * forward - 13.0F * pull)));
    }

    private static void applyVanillaEmptyArmTransform(
            PoseStack poseStack,
            HumanoidArm arm,
            float equipProgress,
            float swingProgress
    ) {
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float swingRoot = Mth.sqrt(swingProgress);
        float sideSwing = -0.3F * Mth.sin(swingRoot * Mth.PI);
        float verticalSwing = 0.4F * Mth.sin(swingRoot * Mth.TWO_PI);
        float depthSwing = -0.4F * Mth.sin(swingProgress * Mth.PI);
        poseStack.translate(side * (sideSwing + 0.64000005F), verticalSwing - 0.6F + equipProgress * -0.6F, depthSwing - 0.71999997F);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
        float rollSwing = Mth.sin(swingProgress * swingProgress * Mth.PI);
        float yawSwing = Mth.sin(swingRoot * Mth.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * yawSwing * 70.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * rollSwing * -20.0F));
        poseStack.translate(side * -1.0F, 3.6F, 3.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
        poseStack.translate(side * 5.6F, 0.0F, 0.0F);
    }
}
