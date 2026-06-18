package com.pr1tcha.riftborne.rift.data;

import com.pr1tcha.riftborne.rift.RiftType;
import com.pr1tcha.riftborne.Riftborne;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class RiftData {
    public static final ResourceLocation RIFT_TYPE = ResourceLocation.fromNamespaceAndPath("riftborne", "rift");
    public static final ResourceLocation CONTOUR_RIFT_TYPE = ResourceLocation.fromNamespaceAndPath("riftborne", "discard_contour_rift");
    public static final ResourceLocation PORTAL_RIFT_TYPE = ResourceLocation.fromNamespaceAndPath("riftborne", "rift_portal");
    public static final ResourceLocation ARCHIVED_RIFT_TYPE = ResourceLocation.fromNamespaceAndPath("riftborne", "rift_archived");

    public UUID id = UUID.randomUUID();
    public ResourceLocation riftType = RIFT_TYPE;
    public BlockPos centerPos = BlockPos.ZERO;
    public float radius = 5.0f;
    public RiftStage stage = RiftStage.DORMANT;
    public int ticksExisted = 0;
    public int stageTicks = 0;
    public int maxLifetimeTicks = 6000;
    public float instability = 0.0f;
    public boolean isCommandSpawned = false;
    public boolean isQuestRelated = false;
    public boolean useProceduralVisual = true;
    public int wavesCleared = 0;
    public int currentWaveMobsLeft = 0;
    public int spawnCooldown = 0;

    public CompoundTag save(CompoundTag tag) {
        tag.putUUID("Id", id);
        tag.putString("Type", riftType.toString());
        tag.putLong("Pos", centerPos.asLong());
        tag.putFloat("Radius", radius);
        tag.putString("Stage", stage.name());
        tag.putInt("TicksExisted", ticksExisted);
        tag.putInt("StageTicks", stageTicks);
        tag.putInt("MaxLifetime", maxLifetimeTicks);
        tag.putFloat("Instability", instability);
        tag.putBoolean("CommandSpawned", isCommandSpawned);
        tag.putBoolean("QuestRelated", isQuestRelated);
        tag.putBoolean("ProceduralVisual", useProceduralVisual);
        tag.putInt("WavesCleared", wavesCleared);
        tag.putInt("MobsLeft", currentWaveMobsLeft);
        tag.putInt("SpawnCooldown", spawnCooldown);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.hasUUID("Id")) {
            this.id = tag.getUUID("Id");
        }

        String rawType = tag.getString("Type");
        boolean legacyType = isLegacyRiftType(rawType);
        ResourceLocation loadedType = loadRiftType(rawType);
        this.centerPos = BlockPos.of(tag.getLong("Pos"));
        this.radius = tag.getFloat("Radius");
        this.stage = loadStage(tag.getString("Stage"));
        this.ticksExisted = tag.getInt("TicksExisted");
        this.stageTicks = tag.getInt("StageTicks");
        this.maxLifetimeTicks = tag.getInt("MaxLifetime");
        this.instability = tag.getFloat("Instability");
        this.isCommandSpawned = tag.getBoolean("CommandSpawned");
        this.isQuestRelated = tag.getBoolean("QuestRelated");
        this.useProceduralVisual = tag.contains("ProceduralVisual") ? tag.getBoolean("ProceduralVisual") : !ARCHIVED_RIFT_TYPE.equals(loadedType);
        this.riftType = legacyType && !this.useProceduralVisual ? ARCHIVED_RIFT_TYPE : loadedType;
        this.wavesCleared = tag.getInt("WavesCleared");
        this.currentWaveMobsLeft = tag.getInt("MobsLeft");
        this.spawnCooldown = tag.getInt("SpawnCooldown");
    }

    private static RiftStage loadStage(String rawStage) {
        if ("COLLAPSE".equals(rawStage)) {
            return RiftStage.COLLAPSING;
        }

        try {
            return RiftStage.valueOf(rawStage);
        } catch (IllegalArgumentException ignored) {
            return RiftStage.DORMANT;
        }
    }

    private static ResourceLocation loadRiftType(String rawType) {
        if (isLegacyRiftType(rawType)) {
            return RIFT_TYPE;
        }

        ResourceLocation parsedType = ResourceLocation.parse(rawType);
        return PORTAL_RIFT_TYPE.equals(parsedType) ? CONTOUR_RIFT_TYPE : parsedType;
    }

    private static boolean isLegacyRiftType(String rawType) {
        return rawType == null || rawType.isBlank() || "riftborne:small_spatial".equals(rawType);
    }

    public static boolean isContourRift(ResourceLocation riftType) {
        return CONTOUR_RIFT_TYPE.equals(riftType) || PORTAL_RIFT_TYPE.equals(riftType);
    }
}
