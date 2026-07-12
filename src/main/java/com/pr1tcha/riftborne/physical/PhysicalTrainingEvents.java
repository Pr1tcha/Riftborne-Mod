package com.pr1tcha.riftborne.physical;

import com.pr1tcha.riftborne.Riftborne;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import com.pr1tcha.riftborne.physical.pushup.PushupTrainingManager;

@EventBusSubscriber(modid = Riftborne.MODID)
public final class PhysicalTrainingEvents {
    private PhysicalTrainingEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PhysicalTrainingManager.onLogin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PhysicalTrainingManager.onLogout(player);
            PushupTrainingManager.stop(player, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PhysicalTrainingManager.tick(player);
            PushupTrainingManager.tick(player);
        }
    }
}
