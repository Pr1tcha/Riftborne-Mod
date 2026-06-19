package com.pr1tcha.riftborne.rna;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.config.Config;
import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import com.pr1tcha.riftborne.rna.data.MetaWearStage;
import com.pr1tcha.riftborne.rna.data.RnaData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Riftborne.MODID)
public final class RnaEvents {
    private RnaEvents() {
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        RiftbornePlayerData.copy(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        RnaData data = RnaApi.get(player);
        if (!data.hasRNA() || data.metaWear() <= 0 || data.metaWearStage() == MetaWearStage.ARCHITECTURE_BREAK) {
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        int interval = decayInterval(data.metaWearStage());
        if (gameTime - data.lastMetaWearTick() >= interval) {
            RnaApi.reduceMetaWear(player, 1);
        }
    }

    private static int decayInterval(MetaWearStage stage) {
        return switch (stage) {
            case STABLE -> Config.metaWearStableDecayInterval.get();
            case STRAIN -> Config.metaWearStrainDecayInterval.get();
            case DISTORTION -> Config.metaWearDistortionDecayInterval.get();
            case REJECTION -> Config.metaWearRejectionDecayInterval.get();
            case ARCHITECTURE_BREAK -> Integer.MAX_VALUE;
        };
    }
}
