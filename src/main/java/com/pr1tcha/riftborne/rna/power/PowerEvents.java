package com.pr1tcha.riftborne.rna.power;

import com.pr1tcha.riftborne.Riftborne;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Server-side hooks for the PS V2.5 power system. Facet crystallization triggers on a stress spike:
 * a hit that would drop the Synchron below 30% HP while RNA is active and a facet has not formed yet.
 */
@EventBusSubscriber(modid = Riftborne.MODID)
public final class PowerEvents {
    private static final float STRESS_HP_FRACTION = 0.3F;

    private PowerEvents() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        float postHealth = player.getHealth() - event.getAmount();
        if (postHealth > 0.0F && postHealth / player.getMaxHealth() < STRESS_HP_FRACTION) {
            PowerCrystallization.tryCrystallize(player);
        }
    }
}
