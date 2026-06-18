package com.pr1tcha.Rifts.entity;

import com.pr1tcha.Rifts.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TelekineticBlockEntity extends Entity {
    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE = SynchedEntityData.defineId(TelekineticBlockEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Float> DATA_HARDNESS = SynchedEntityData.defineId(TelekineticBlockEntity.class, EntityDataSerializers.FLOAT);
    private static final double IMPACT_SPEED_THRESHOLD = 0.35D;
    private static final int MAX_LIFETIME_TICKS = 1200;

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private Entity cachedOwner;
    private final Set<Integer> hitEntities = new HashSet<>();
    private int lifetimeTicks;
    private boolean released;
    private boolean launched;

    public TelekineticBlockEntity(EntityType<? extends TelekineticBlockEntity> entityType, Level level) {
        super(entityType, level);
        blocksBuilding = true;
    }

    public TelekineticBlockEntity(Level level, double x, double y, double z, BlockState blockState, float hardness) {
        this(ModContent.TELEKINETIC_BLOCK.get(), level);
        setBlockState(blockState);
        setHardness(hardness);
        moveTo(x, y, z, 0.0F, 0.0F);
        xo = x;
        yo = y;
        zo = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BLOCK_STATE, Blocks.STONE.defaultBlockState());
        builder.define(DATA_HARDNESS, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (getBlockState().isAir()) {
            discard();
            return;
        }

        lifetimeTicks++;
        if (!level().isClientSide && lifetimeTicks > MAX_LIFETIME_TICKS) {
            dropAndDestroyAsBlock();
            return;
        }

        if (!isNoGravity()) {
            setDeltaMovement(getDeltaMovement().add(0.0D, -getGravity(), 0.0D));
        }

        Vec3 motionBeforeMove = getDeltaMovement();
        move(MoverType.SELF, motionBeforeMove);

        if (!level().isClientSide) {
            hitLivingEntities(motionBeforeMove);
            if (isRemoved()) {
                return;
            }
            if (isResolvingAfterTelekinesis() && touchedGround(motionBeforeMove)) {
                resolveOnGround();
                if (isRemoved()) {
                    return;
                }
            }
        }

        Vec3 dampedMotion = getDeltaMovement().scale(onGround() ? 0.62D : 0.98D);
        if (horizontalCollision) {
            dampedMotion = new Vec3(0.0D, dampedMotion.y, 0.0D);
        }
        if (verticalCollision) {
            dampedMotion = new Vec3(dampedMotion.x, 0.0D, dampedMotion.z);
        }

        setDeltaMovement(dampedMotion);
    }

    private void hitLivingEntities(Vec3 impactMotion) {
        if (isNoGravity() || !isResolvingAfterTelekinesis() || impactMotion.lengthSqr() < IMPACT_SPEED_THRESHOLD * IMPACT_SPEED_THRESHOLD) {
            return;
        }

        AABB hitBox = getBoundingBox().inflate(0.18D);
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, hitBox, this::canHit)) {
            if (!hitEntities.add(target.getId())) {
                continue;
            }

            float damage = getImpactDamage();
            DamageSource damageSource = damageSources().thrown(this, getOwner());
            if (damage > 0.0F) {
                target.hurt(damageSource, damage);
            } else {
                target.hurt(damageSource, 0.0F);
            }

            Vec3 direction = impactMotion.normalize();
            target.knockback(0.4D, -direction.x, -direction.z);
            hasImpulse = true;
            dropAndDestroyAsBlock();
            return;
        }
    }

    private boolean isResolvingAfterTelekinesis() {
        return released || launched;
    }

    private boolean touchedGround(Vec3 motionBeforeMove) {
        return onGround() || verticalCollision && motionBeforeMove.y < 0.0D;
    }

    private void resolveOnGround() {
        if (!tryPlaceReleasedBlock()) {
            dropAndDestroyAsBlock();
        }
    }

    private boolean tryPlaceReleasedBlock() {
        BlockState carriedState = getBlockState();
        BlockPos landingPos = blockPosition();
        if (tryPlaceAt(landingPos, carriedState) || tryPlaceAt(landingPos.above(), carriedState)) {
            discard();
            return true;
        }
        return false;
    }

    private boolean tryPlaceAt(BlockPos pos, BlockState carriedState) {
        BlockState existingState = level().getBlockState(pos);
        if (!canReplaceLandingBlock(existingState) || !carriedState.canSurvive(level(), pos)) {
            return false;
        }

        return level().setBlock(pos, carriedState, 3);
    }

    private static boolean canReplaceLandingBlock(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }

    private void dropAndDestroyAsBlock() {
        if (level() instanceof ServerLevel serverLevel) {
            for (ItemStack drop : Block.getDrops(getBlockState(), serverLevel, blockPosition(), null)) {
                if (!drop.isEmpty()) {
                    Block.popResource(serverLevel, blockPosition(), drop);
                }
            }
            serverLevel.levelEvent(2001, blockPosition(), Block.getId(getBlockState()));
        }
        discard();
    }

    private boolean canHit(LivingEntity target) {
        if (!target.isAlive() || target.isSpectator()) {
            return false;
        }

        Entity owner = getOwner();
        return owner == null || target != owner;
    }

    public void markHeld(ServerPlayer owner) {
        ownerUuid = owner.getUUID();
        cachedOwner = owner;
        released = false;
        launched = false;
        hitEntities.clear();
    }

    public void markReleased(ServerPlayer owner) {
        ownerUuid = owner.getUUID();
        cachedOwner = owner;
        released = true;
        launched = false;
        hitEntities.clear();
    }

    public void markLaunched(ServerPlayer owner) {
        ownerUuid = owner.getUUID();
        cachedOwner = owner;
        released = false;
        launched = true;
        hitEntities.clear();
    }

    @Nullable
    public Entity getOwner() {
        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        }
        if (ownerUuid != null && level() instanceof ServerLevel serverLevel) {
            cachedOwner = serverLevel.getEntity(ownerUuid);
            return cachedOwner;
        }

        return null;
    }

    public BlockState getBlockState() {
        return entityData.get(DATA_BLOCK_STATE);
    }

    public void setBlockState(BlockState blockState) {
        entityData.set(DATA_BLOCK_STATE, blockState);
    }

    public float getHardness() {
        return entityData.get(DATA_HARDNESS);
    }

    public void setHardness(float hardness) {
        entityData.set(DATA_HARDNESS, hardness);
    }

    private float getImpactDamage() {
        float hardness = getHardness();
        if (hardness < 1.0F) {
            return 0.0F;
        }

        return Math.max(1.0F, hardness * 0.5F);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04D;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("BlockState", NbtUtils.writeBlockState(getBlockState()));
        tag.putFloat("Hardness", getHardness());
        tag.putInt("Lifetime", lifetimeTicks);
        tag.putBoolean("Released", released);
        tag.putBoolean("Launched", launched);
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setBlockState(NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), tag.getCompound("BlockState")));
        setHardness(tag.getFloat("Hardness"));
        lifetimeTicks = tag.getInt("Lifetime");
        released = tag.getBoolean("Released");
        launched = tag.getBoolean("Launched");
        if (tag.hasUUID("Owner")) {
            ownerUuid = tag.getUUID("Owner");
            cachedOwner = null;
        }
    }
}
