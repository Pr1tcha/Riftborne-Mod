package com.pr1tcha.riftborne.rift.portal;

import com.pr1tcha.riftborne.registry.ModContent;
import com.pr1tcha.riftborne.rift.dimension.RiftDimensions;
import com.pr1tcha.riftborne.rift.dimension.RiftTier;
import com.pr1tcha.riftborne.rift.run.RiftRunData;
import com.pr1tcha.riftborne.rift.run.RiftRunSavedData;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class RiftPortalBlockEntity extends BlockEntity {
    private static final String COOLDOWN_TAG = "RiftbornePortalCooldown";

    private RiftTier tier = RiftTier.SURFACE_SHARD;
    private UUID runId;

    public RiftPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.RIFT_PORTAL_BE_TYPE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RiftPortalBlockEntity portal) {
        if (!(level instanceof ServerLevel serverLevel) || serverLevel.getGameTime() % 5 != 0) {
            return;
        }

        AABB trigger = new AABB(pos).inflate(0.55D, 1.5D, 0.55D);
        List<ServerPlayer> players = serverLevel.getEntitiesOfClass(ServerPlayer.class, trigger);
        for (ServerPlayer player : players) {
            CompoundTag persistent = player.getPersistentData();
            long cooldownUntil = persistent.getLong(COOLDOWN_TAG);
            if (serverLevel.getGameTime() < cooldownUntil) {
                continue;
            }

            RiftRunSavedData storage = RiftRunSavedData.get(serverLevel.getServer());
            RiftRunData run = portal.resolveRun(serverLevel, storage);
            persistent.putLong(COOLDOWN_TAG, serverLevel.getGameTime() + 60L);
            RiftDimensions.enter(player, run);
        }
    }

    private RiftRunData resolveRun(ServerLevel sourceLevel, RiftRunSavedData storage) {
        if (runId != null) {
            RiftRunData existing = storage.getRun(runId).orElse(null);
            if (existing != null) {
                return existing;
            }
        }

        RiftRunData created = storage.createRun(sourceLevel, worldPosition, tier);
        runId = created.runId();
        sync();
        return created;
    }

    public void configure(RiftTier tier) {
        this.tier = tier;
        this.runId = null;
        sync();
    }

    public RiftTier tier() {
        return tier;
    }

    public UUID runId() {
        return runId;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Tier", tier.level());
        if (runId != null) {
            tag.putUUID("RunId", runId);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tier = RiftTier.fromLevel(tag.getInt("Tier")).orElse(RiftTier.SURFACE_SHARD);
        runId = tag.hasUUID("RunId") ? tag.getUUID("RunId") : null;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
