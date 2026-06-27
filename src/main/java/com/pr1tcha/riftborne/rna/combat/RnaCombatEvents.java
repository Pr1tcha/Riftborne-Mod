package com.pr1tcha.riftborne.rna.combat;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.rna.combat.registry.RnaAbilityRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Riftborne.MODID)
public final class RnaCombatEvents {
    private RnaCombatEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RnaAbilityManager.getData(player);
            RnaCombatNetwork.sendSync(player, null, null);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RnaAbilityManager.tick(player);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        Entity attacker = event.getSource().getEntity();
        if (RnaAbilityManager.isActive(player, RnaAbilityRegistry.RNA_DEFLECT_ID, gameTime)) {
            event.setAmount(event.getAmount() * 0.25F);
            event.setInvulnerabilityTicks(12);
            if (attacker instanceof LivingEntity livingAttacker) {
                Vec3 away = livingAttacker.position().subtract(player.position()).normalize();
                livingAttacker.setDeltaMovement(livingAttacker.getDeltaMovement().add(away.scale(0.45D).add(0.0D, 0.08D, 0.0D)));
                livingAttacker.hasImpulse = true;
                livingAttacker.hurtMarked = true;
            }
            RnaAbilityManager.addCombatLoad(player, -4.0F);
            RnaAbilityManager.clearActive(player, RnaAbilityRegistry.RNA_DEFLECT_ID);
            particles(player, 14, 0.55D, 0.25D, 0.55D, 0.05D);
            return;
        }

        if (RnaAbilityManager.isActive(player, RnaAbilityRegistry.RNA_GUARD_ID, gameTime) && isFrontal(player, attacker)) {
            event.setAmount(event.getAmount() * 0.65F);
            event.setInvulnerabilityTicks(8);
            particles(player, 8, 0.45D, 0.2D, 0.45D, 0.02D);
        }

        if (RnaAbilityManager.isActive(player, RnaAbilityRegistry.RNA_ANCHOR_HOLD_ID, gameTime)) {
            event.setAmount(event.getAmount() * 0.85F);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        if (!RnaAbilityManager.isActive(player, RnaAbilityRegistry.RNA_STRIKE_ID, gameTime)) {
            return;
        }

        target.hurt(player.damageSources().playerAttack(player), 3.0F);
        Vec3 push = target.position().subtract(player.position()).normalize().scale(0.25D);
        target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.08D, push.z));
        target.hasImpulse = true;
        target.hurtMarked = true;
        RnaAbilityManager.clearActive(player, RnaAbilityRegistry.RNA_STRIKE_ID);
        particles(target, 12, 0.35D, 0.25D, 0.35D, 0.04D);
    }

    private static boolean isFrontal(ServerPlayer player, Entity attacker) {
        if (attacker == null) {
            return true;
        }
        Vec3 toAttacker = attacker.position().subtract(player.position());
        if (toAttacker.lengthSqr() < 0.001D) {
            return true;
        }
        return player.getLookAngle().normalize().dot(toAttacker.normalize()) > 0.0D;
    }

    private static void particles(LivingEntity entity, int count, double dx, double dy, double dz, double speed) {
        if (entity.level().isClientSide()) {
            return;
        }
        ((net.minecraft.server.level.ServerLevel) entity.level()).sendParticles(
                ParticleTypes.END_ROD,
                entity.getX(),
                entity.getY() + entity.getBbHeight() * 0.55D,
                entity.getZ(),
                count,
                dx,
                dy,
                dz,
                speed
        );
    }
}
