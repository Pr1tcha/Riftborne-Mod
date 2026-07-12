package com.pr1tcha.riftborne.mixin.client;

import com.pr1tcha.riftborne.client.animation.RiftborneBends;
import com.pr1tcha.riftborne.physical.pushup.client.PushupClient;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies Riftborne's procedural push-up bends after Bendable Cuboids has
 * synchronized or reset player model bends from Player Animation Library.
 */
@Mixin(value = PlayerModel.class, priority = 900)
public abstract class PlayerModelPushupBendMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    private void riftborne$applyPushupBendsAfterPal(
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callback
    ) {
        PushupClient.PoseSample pushup = PushupClient.pose(entity.getUUID(), ageInTicks - entity.tickCount);
        if (!pushup.active()) {
            return;
        }

        float armBend = Mth.lerp(pushup.lowering(), -0.08F, -1.22F);
        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        RiftborneBends.apply(model.rightArm, armBend);
        RiftborneBends.apply(model.leftArm, armBend);
        RiftborneBends.apply(model.rightSleeve, armBend);
        RiftborneBends.apply(model.leftSleeve, armBend);
    }
}
