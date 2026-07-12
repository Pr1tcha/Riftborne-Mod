package com.pr1tcha.riftborne.mixin.client;

import com.pr1tcha.riftborne.client.animation.RiftborneBends;
import com.pr1tcha.riftborne.physical.pushup.client.PushupClient;
import com.pr1tcha.riftborne.rna.combat.client.BarrierClientState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    @Shadow public ModelPart head;
    @Shadow public ModelPart hat;
    @Shadow public ModelPart body;
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftLeg;
    @Shadow public ModelPart rightLeg;

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void riftborne$applyBarrierGesture(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callback
    ) {
        BarrierClientState.PoseSample pose = BarrierClientState.sample(entity.getUUID(), ageInTicks - entity.tickCount);
        applyArmPose(leftArm, HumanoidArm.LEFT, pose, ageInTicks);
        applyArmPose(rightArm, HumanoidArm.RIGHT, pose, ageInTicks);

        PushupClient.PoseSample pushup = PushupClient.pose(entity.getUUID(), ageInTicks - entity.tickCount);
        if (pushup.active()) {
            applyPushupPose(pushup.lowering());
        }
    }

    private void applyPushupPose(float lowering) {
        float armPitch = Mth.lerp(lowering, -0.82F, -0.48F);
        float armSpread = Mth.lerp(lowering, 0.18F, 0.30F);
        float armBend = Mth.lerp(lowering, -0.08F, -1.22F);

        body.xRot = Mth.lerp(lowering, 0.04F, -0.035F);
        body.yRot = 0.0F;
        body.zRot = 0.0F;

        head.xRot = 0.0F;
        head.yRot = 0.0F;
        head.zRot = 0.0F;
        hat.copyFrom(head);

        rightArm.xRot = armPitch;
        rightArm.yRot = 0.0F;
        rightArm.zRot = armSpread;
        leftArm.xRot = armPitch;
        leftArm.yRot = 0.0F;
        leftArm.zRot = -armSpread;
        RiftborneBends.apply(rightArm, armBend);
        RiftborneBends.apply(leftArm, armBend);

        rightLeg.xRot = 0.0F;
        rightLeg.yRot = 0.0F;
        rightLeg.zRot = 0.035F;
        leftLeg.xRot = 0.0F;
        leftLeg.yRot = 0.0F;
        leftLeg.zRot = -0.035F;

        if ((Object) this instanceof PlayerModel<?> playerModel) {
            RiftborneBends.apply(playerModel.rightSleeve, armBend);
            RiftborneBends.apply(playerModel.leftSleeve, armBend);
            RiftborneBends.apply(playerModel.rightPants, 0.0F);
            RiftborneBends.apply(playerModel.leftPants, 0.0F);
        }
    }

    private void applyArmPose(
            ModelPart arm,
            HumanoidArm side,
            BarrierClientState.PoseSample pose,
            float ageInTicks
    ) {
        if (!pose.occupies(side)) {
            return;
        }
        float sideSign = side == HumanoidArm.LEFT ? 1.0F : -1.0F;
        float pullX = 0.62F;
        float thrustX = -1.48F + Mth.clamp(head.xRot, -0.55F, 0.55F) * 0.45F;
        float targetX = Mth.lerp((pose.drive() + 1.0F) * 0.5F, pullX, thrustX);
        float targetY = head.yRot + sideSign * Mth.lerp((pose.drive() + 1.0F) * 0.5F, 0.38F, 0.13F);
        float targetZ = sideSign * Mth.lerp((pose.drive() + 1.0F) * 0.5F, -0.42F, 0.035F);
        if (pose.drive() > 0.95F) {
            targetX += Mth.sin(ageInTicks * 0.32F) * 0.012F;
        }
        arm.xRot = Mth.lerp(pose.weight(), arm.xRot, targetX);
        arm.yRot = Mth.lerp(pose.weight(), arm.yRot, targetY);
        arm.zRot = Mth.lerp(pose.weight(), arm.zRot, targetZ);
    }
}
