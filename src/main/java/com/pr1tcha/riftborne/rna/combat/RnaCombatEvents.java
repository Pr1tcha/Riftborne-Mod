package com.pr1tcha.riftborne.rna.combat;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.rna.combat.registry.RnaAbilityRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Riftborne.MODID)
public final class RnaCombatEvents {
    private static final double BARRIER_HALF_ANGLE_COS = Math.cos(Math.toRadians(50.0D));

    private RnaCombatEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RnaAbilityManager.getData(player);
            RnaCombatNetwork.sendSync(player, null, null);
            for (ServerPlayer owner : player.getServer().getPlayerList().getPlayers()) {
                float integrity = RnaAbilityManager.activeStrength(owner, RnaAbilityRegistry.BARRIER_ID);
                com.pr1tcha.riftborne.rna.combat.barrier.BarrierPhase phase = RnaAbilityManager.barrierPhase(owner);
                if (phase != com.pr1tcha.riftborne.rna.combat.barrier.BarrierPhase.INACTIVE) {
                    RnaCombatNetwork.sendBarrierState(
                            player,
                            owner,
                            integrity,
                            phase,
                            RnaAbilityManager.barrierGestureMode(owner),
                            RnaAbilityManager.barrierPhaseTicks(owner)
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RnaAbilityManager.cancelBarrierDeployment(player);
            RnaCombatNetwork.broadcastBarrierState(player, 0.0F);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RnaAbilityManager.tick(player);
            if (player.isUsingItem() && RnaAbilityManager.isBarrierHandOccupied(player, player.getUsedItemHand())) {
                player.stopUsingItem();
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player
                && RnaAbilityManager.isBarrierHandOccupied(player, event.getHand())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player
                && RnaAbilityManager.isBarrierHandOccupied(player, event.getHand())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player
                && RnaAbilityManager.isBarrierHandOccupied(player, event.getHand())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player
                && RnaAbilityManager.isBarrierHandOccupied(player, event.getHand())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player
                && RnaAbilityManager.isBarrierHandOccupied(player, event.getHand())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        Entity attacker = event.getSource().getDirectEntity();
        if (attacker == null) {
            attacker = event.getSource().getEntity();
        }
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

        if (RnaAbilityManager.isActive(player, RnaAbilityRegistry.BARRIER_ID, gameTime) && isFrontal(player, attacker)) {
            RnaAbilityManager.BarrierImpact impact = RnaAbilityManager.absorbBarrierDamage(
                    player,
                    event.getAmount(),
                    attacker instanceof Projectile
            );
            event.setAmount(impact.overflowDamage());
            Vec3 impactPoint = barrierImpactPoint(player);
            stopProjectile(attacker, impactPoint);

            if (!(attacker instanceof Projectile)
                    && attacker instanceof LivingEntity livingAttacker
                    && impact.overflowDamage() <= 0.0F) {
                Vec3 away = livingAttacker.position().subtract(player.position());
                if (away.lengthSqr() > 0.001D) {
                    livingAttacker.setDeltaMovement(livingAttacker.getDeltaMovement().add(
                            away.normalize().scale(0.16D).add(0.0D, 0.04D, 0.0D)
                    ));
                    livingAttacker.hasImpulse = true;
                    livingAttacker.hurtMarked = true;
                }
            }

            if (impact.broken()) {
                player.displayClientMessage(
                        Component.translatable(impact.overloadBreak()
                                ? "message.riftborne.technique.barrier.collapsed"
                                : "message.riftborne.technique.barrier.broken"),
                        true
                );
            }

            if (impact.overflowDamage() <= 0.0F) {
                event.setCanceled(true);
                return;
            }

            event.setInvulnerabilityTicks(8);
            return;
        }

        if (RnaAbilityManager.isActive(player, RnaAbilityRegistry.RNA_ANCHOR_HOLD_ID, gameTime)) {
            event.setAmount(event.getAmount() * 0.85F);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (RnaAbilityManager.isBarrierHandOccupied(player, net.minecraft.world.InteractionHand.MAIN_HAND)) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity target)) {
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
            return false;
        }
        Vec3 look = player.getLookAngle();
        Vec3 facing = new Vec3(look.x, 0.0D, look.z);
        if (facing.lengthSqr() < 0.001D) {
            facing = Vec3.directionFromRotation(0.0F, player.getYRot());
        }
        facing = facing.normalize();

        if (attacker instanceof Projectile projectile) {
            Vec3 velocity = projectile.getDeltaMovement();
            Vec3 horizontalVelocity = new Vec3(velocity.x, 0.0D, velocity.z);
            if (horizontalVelocity.lengthSqr() > 0.001D) {
                return facing.dot(horizontalVelocity.normalize()) <= -BARRIER_HALF_ANGLE_COS;
            }
        }

        Vec3 offset = attacker.position().subtract(player.position());
        Vec3 toAttacker = new Vec3(offset.x, 0.0D, offset.z);
        if (toAttacker.lengthSqr() < 0.001D) {
            return true;
        }
        return facing.dot(toAttacker.normalize()) >= BARRIER_HALF_ANGLE_COS;
    }

    private static Vec3 barrierImpactPoint(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.001D) {
            horizontal = Vec3.directionFromRotation(0.0F, player.getYRot());
        }
        return player.position()
                .add(0.0D, player.getBbHeight() * 0.58D, 0.0D)
                .add(horizontal.normalize().scale(0.95D));
    }

    private static void stopProjectile(Entity attacker, Vec3 impactPoint) {
        if (!(attacker instanceof Projectile projectile)) {
            return;
        }
        projectile.setPos(impactPoint.x, impactPoint.y, impactPoint.z);
        if (projectile instanceof AbstractArrow) {
            projectile.setDeltaMovement(Vec3.ZERO);
            projectile.setNoGravity(false);
        } else {
            projectile.discard();
        }
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
