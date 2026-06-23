package com.pr1tcha.riftborne.rift.run;

import com.pr1tcha.riftborne.rift.dimension.RiftTier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class RiftRunSavedData extends SavedData {
    private static final String FILE_NAME = "riftborne_rift_runs";
    private static final int REGION_SIZE = 2000;
    private static final int REGIONS_PER_ROW = 32;
    private static final SavedData.Factory<RiftRunSavedData> FACTORY =
            new SavedData.Factory<>(RiftRunSavedData::new, RiftRunSavedData::load);

    private final Map<UUID, RiftRunData> runs = new LinkedHashMap<>();
    private long nextRegionIndex;

    public static RiftRunSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_NAME);
    }

    public RiftRunData createRun(ServerLevel sourceLevel, BlockPos sourcePos, RiftTier tier) {
        long index = nextRegionIndex++;
        int regionX = (int) (index % REGIONS_PER_ROW);
        int regionZ = (int) (index / REGIONS_PER_ROW);
        BlockPos anchor = new BlockPos(regionX * REGION_SIZE, 82, regionZ * REGION_SIZE);
        RiftRunData run = new RiftRunData(
                UUID.randomUUID(),
                tier,
                sourceLevel.dimension(),
                sourcePos,
                anchor,
                sourceLevel.getGameTime()
        );
        runs.put(run.runId(), run);
        setDirty();
        return run;
    }

    public Optional<RiftRunData> getRun(UUID runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    public Collection<RiftRunData> runs() {
        return runs.values();
    }

    public void playerJoined(RiftRunData run, UUID playerId) {
        run.addPlayer(playerId);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("NextRegionIndex", nextRegionIndex);
        ListTag runList = new ListTag();
        for (RiftRunData run : runs.values()) {
            runList.add(run.save());
        }
        tag.put("Runs", runList);
        return tag;
    }

    private static RiftRunSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        RiftRunSavedData data = new RiftRunSavedData();
        data.nextRegionIndex = tag.getLong("NextRegionIndex");
        ListTag runList = tag.getList("Runs", Tag.TAG_COMPOUND);
        for (Tag entry : runList) {
            RiftRunData run = RiftRunData.load((CompoundTag) entry);
            data.runs.put(run.runId(), run);
        }
        return data;
    }
}
