package com.pr1tcha.riftborne.mixin.client;

import com.pr1tcha.riftborne.physical.pushup.client.PushupClient;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At("TAIL"))
    private void riftborne$applyPushupCamera(
            BlockGetter level,
            Entity entity,
            boolean detached,
            boolean thirdPersonReverse,
            float partialTick,
            CallbackInfo callback
    ) {
        if (detached || !(entity instanceof LocalPlayer player)) {
            return;
        }
        PushupClient.PoseSample pose = PushupClient.pose(player.getUUID(), partialTick);
        if (!pose.active()) {
            return;
        }

        double x = Mth.lerp((double) partialTick, player.xo, player.getX());
        double y = Mth.lerp((double) partialTick, player.yo, player.getY());
        double z = Mth.lerp((double) partialTick, player.zo, player.getZ());
        double cameraHeight = Mth.lerp(pose.lowering(), 0.62D, 0.32D);
        setPosition(x, y + cameraHeight, z);
    }
}
