package com.pr1tcha.Rifts.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class RiftSplinterEntity extends PathfinderMob implements Enemy {
    private static final int DASH_COOLDOWN_TICKS = 80;
    private static final double DASH_MIN_DISTANCE_SQR = 4.0D;
    private static final double DASH_MAX_DISTANCE_SQR = 64.0D;

    private int dashCooldown = 40;

    public RiftSplinterEntity(EntityType<? extends RiftSplinterEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            tickDash();
        }
    }

    private void tickDash() {
        if (dashCooldown > 0) {
            dashCooldown--;
            return;
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            dashCooldown = 20;
            return;
        }

        double distanceSqr = distanceToSqr(target);
        if (distanceSqr < DASH_MIN_DISTANCE_SQR || distanceSqr > DASH_MAX_DISTANCE_SQR || !hasLineOfSight(target)) {
            return;
        }

        Vec3 direction = target.position().subtract(position()).normalize();
        setDeltaMovement(direction.x * 1.25D, 0.18D, direction.z * 1.25D);
        hasImpulse = true;
        dashCooldown = DASH_COOLDOWN_TICKS + random.nextInt(40);
    }
}
