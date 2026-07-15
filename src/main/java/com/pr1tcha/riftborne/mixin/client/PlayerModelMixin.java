package com.pr1tcha.riftborne.mixin.client;

import com.pr1tcha.riftborne.flight.client.FlightExternalAnimation;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void riftborne$suppressFlightCrouch(
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (entity instanceof AbstractClientPlayer player
                && FlightExternalAnimation.shouldSuppressCrouch(player, 1.0F)) {
            ((PlayerModel<?>) (Object) this).crouching = false;
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    private void riftborne$applyFlightExternalAnimation(
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callbackInfo
    ) {
        if (entity instanceof AbstractClientPlayer player) {
            FlightExternalAnimation.apply((PlayerModel<?>) (Object) this, player, ageInTicks);
        }
    }
}
