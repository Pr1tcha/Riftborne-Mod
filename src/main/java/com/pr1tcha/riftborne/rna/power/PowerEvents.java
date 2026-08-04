package com.pr1tcha.riftborne.rna.power;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.rna.power.data.RNAProfile;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Server-side hooks for the PS V2.5 power system: facet crystallization on a stress spike (a hit
 * that would drop the Synchron below 30% HP while RNA is active and no facet has formed yet), and
 * the slow passive recovery of meta-wear.
 *
 * <p>Recovery is deliberately partial and slow — meta-wear is the "pay forever" regulator, so it
 * only bleeds off while the architecture is left alone, and never fully below the scar it has left.
 */
@EventBusSubscriber(modid = Riftborne.MODID)
public final class PowerEvents {
    private static final float STRESS_HP_FRACTION = 0.3F;
    private static final int RECOVERY_INTERVAL_TICKS = 200;
    private static final float RECOVERY_AMOUNT = 1.0F;

    private PowerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % RECOVERY_INTERVAL_TICKS != 0) {
            return;
        }
        RNAProfile profile = PowerApi.get(player);
        if (profile.active() && profile.metaWear() > 0.0F) {
            PowerApi.reduceMetaWear(player, RECOVERY_AMOUNT);
        }
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
