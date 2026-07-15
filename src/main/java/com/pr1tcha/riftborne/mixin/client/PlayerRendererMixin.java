package com.pr1tcha.riftborne.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pr1tcha.riftborne.flight.client.FlightExternalAnimation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {
    @Inject(
            method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("RETURN")
    )
    private void riftborne$applyFlightRenderRotation(
            AbstractClientPlayer player,
            PoseStack poseStack,
            float bob,
            float bodyYaw,
            float partialTick,
            float scale,
            CallbackInfo callbackInfo
    ) {
        if (!player.isFallFlying()) {
            FlightExternalAnimation.applyElytraRenderRotation(player, poseStack, partialTick);
        }
    }
}
