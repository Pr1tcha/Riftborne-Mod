package com.pr1tcha.riftborne.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pr1tcha.riftborne.physical.pushup.client.PushupClient;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void riftborne$applyPushupOrientation(
            T entity,
            PoseStack poseStack,
            float bob,
            float bodyYaw,
            float partialTick,
            float scale,
            CallbackInfo callback
    ) {
        PushupClient.PoseSample pose = PushupClient.pose(entity.getUUID(), partialTick);
        if (!pose.active()) {
            return;
        }

        float lowering = pose.lowering();
        float height = 0.06F / Math.max(0.01F, scale);
        float pitch = Mth.lerp(lowering, -78.0F, -84.0F);

        // Keep the lower support point anchored to the mat. The push-up height comes
        // from the pitch change around that point, not from lifting the whole model.
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw - pose.bodyYaw()));
        poseStack.translate(0.0F, height, 0.72F / Math.max(0.01F, scale));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }
}
