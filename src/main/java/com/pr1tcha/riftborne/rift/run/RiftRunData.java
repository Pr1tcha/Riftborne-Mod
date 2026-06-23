package com.pr1tcha.riftborne.rift.run;

import com.pr1tcha.riftborne.rift.dimension.RiftTier;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class RiftRunData {
    private final UUID runId;
    private final RiftTier tier;
    private final ResourceKey<Level> sourceDimension;
    private final BlockPos sourcePos;
    private final ResourceKey<Level> riftDimension;
    private final BlockPos anchorPos;
    private final BlockPos returnPos;
    private final long createdGameTime;
    private final Set<UUID> players = new HashSet<>();
    private float pressure;
    private RiftRunState state;

    public RiftRunData(
            UUID runId,
            RiftTier tier,
            ResourceKey<Level> sourceDimension,
            BlockPos sourcePos,
            BlockPos anchorPos,
            long createdGameTime
    ) {
        this(runId, tier, sourceDimension, sourcePos, tier.dimension(), anchorPos, anchorPos.offset(4, 0, 0),
                createdGameTime, 0.0F, RiftRunState.OPEN);
    }

    private RiftRunData(
            UUID runId,
            RiftTier tier,
            ResourceKey<Level> sourceDimension,
            BlockPos sourcePos,
            ResourceKey<Level> riftDimension,
            BlockPos anchorPos,
            BlockPos returnPos,
            long createdGameTime,
            float pressure,
            RiftRunState state
    ) {
        this.runId = runId;
        this.tier = tier;
        this.sourceDimension = sourceDimension;
        this.sourcePos = sourcePos.immutable();
        this.riftDimension = riftDimension;
        this.anchorPos = anchorPos.immutable();
        this.returnPos = returnPos.immutable();
        this.createdGameTime = createdGameTime;
        this.pressure = pressure;
        this.state = state;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("RunId", runId);
        tag.putInt("Tier", tier.level());
        tag.putString("SourceDimension", sourceDimension.location().toString());
        tag.putLong("SourcePos", sourcePos.asLong());
        tag.putString("RiftDimension", riftDimension.location().toString());
        tag.putLong("AnchorPos", anchorPos.asLong());
        tag.putLong("ReturnPos", returnPos.asLong());
        tag.putLong("CreatedGameTime", createdGameTime);
        tag.putFloat("Pressure", pressure);
        tag.putString("State", state.name());
        ListTag playerList = new ListTag();
        for (UUID playerId : players) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("Id", playerId);
            playerList.add(playerTag);
        }
        tag.put("Players", playerList);
        return tag;
    }

    public static RiftRunData load(CompoundTag tag) {
        RiftTier tier = RiftTier.fromLevel(tag.getInt("Tier")).orElse(RiftTier.SURFACE_SHARD);
        ResourceKey<Level> sourceDimension = dimension(tag.getString("SourceDimension"), Level.OVERWORLD);
        ResourceKey<Level> riftDimension = dimension(tag.getString("RiftDimension"), tier.dimension());
        RiftRunState state;
        try {
            state = RiftRunState.valueOf(tag.getString("State"));
        } catch (IllegalArgumentException ignored) {
            state = RiftRunState.OPEN;
        }

        RiftRunData run = new RiftRunData(
                tag.getUUID("RunId"),
                tier,
                sourceDimension,
                BlockPos.of(tag.getLong("SourcePos")),
                riftDimension,
                BlockPos.of(tag.getLong("AnchorPos")),
                BlockPos.of(tag.getLong("ReturnPos")),
                tag.getLong("CreatedGameTime"),
                tag.getFloat("Pressure"),
                state
        );
        ListTag players = tag.getList("Players", Tag.TAG_COMPOUND);
        for (Tag entry : players) {
            CompoundTag playerTag = (CompoundTag) entry;
            if (playerTag.hasUUID("Id")) {
                run.players.add(playerTag.getUUID("Id"));
            }
        }
        return run;
    }

    private static ResourceKey<Level> dimension(String value, ResourceKey<Level> fallback) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        return id == null ? fallback : ResourceKey.create(Registries.DIMENSION, id);
    }

    public void addPlayer(UUID playerId) {
        players.add(playerId);
    }

    public UUID runId() {
        return runId;
    }

    public RiftTier tier() {
        return tier;
    }

    public ResourceKey<Level> sourceDimension() {
        return sourceDimension;
    }

    public BlockPos sourcePos() {
        return sourcePos;
    }

    public ResourceKey<Level> riftDimension() {
        return riftDimension;
    }

    public BlockPos anchorPos() {
        return anchorPos;
    }

    public BlockPos returnPos() {
        return returnPos;
    }

    public long createdGameTime() {
        return createdGameTime;
    }

    public float pressure() {
        return pressure;
    }

    public RiftRunState state() {
        return state;
    }

    public Set<UUID> players() {
        return Set.copyOf(players);
    }
}
