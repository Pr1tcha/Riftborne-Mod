package com.pr1tcha.riftborne.rna.combat;

import com.pr1tcha.riftborne.Riftborne;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Riftborne.MODID)
public final class RnaCombatEvents {
    private RnaCombatEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RnaAbilityManager.getData(player);
        }
    }
}
