package com.kwinta.Rifts.RiftData;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

public class RiftData {
    public UUID id = UUID.randomUUID();
    public ResourceLocation riftType = ResourceLocation.fromNamespaceAndPath("eventmod", "small_spatial");
    public BlockPos centerPos = BlockPos.ZERO;
    public float radius = 5.0f;
    public RiftStage stage = RiftStage.OPENING;
    public int ticksExisted = 0;
    public int maxLifetimeTicks = 6000; // 5 минут по умолчанию
    public float instability = 0.0f;
    public boolean isCommandSpawned = false;
    public boolean isQuestRelated = false;
    public int wavesCleared = 0;       // Сколько волн зачищено
    public int currentWaveMobsLeft = 0; // Сколько мобов осталось убить в текущей волне
    public int spawnCooldown = 0;       // Задержка между спавном мобов

    public CompoundTag save(CompoundTag tag) {
        tag.putUUID("Id", id);
        tag.putString("Type", riftType.toString());
        tag.putLong("Pos", centerPos.asLong());
        tag.putFloat("Radius", radius);
        tag.putString("Stage", stage.name());
        tag.putInt("TicksExisted", ticksExisted);
        tag.putInt("MaxLifetime", maxLifetimeTicks);
        tag.putFloat("Instability", instability);
        tag.putBoolean("CommandSpawned", isCommandSpawned);
        tag.putBoolean("QuestRelated", isQuestRelated);
        tag.putInt("WavesCleared", wavesCleared);
        tag.putInt("MobsLeft", currentWaveMobsLeft);
        tag.putInt("SpawnCooldown", spawnCooldown);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.hasUUID("Id")) this.id = tag.getUUID("Id");
        this.riftType = ResourceLocation.parse(tag.getString("Type"));
        this.centerPos = BlockPos.of(tag.getLong("Pos"));
        this.radius = tag.getFloat("Radius");
        this.stage = RiftStage.valueOf(tag.getString("Stage"));
        this.ticksExisted = tag.getInt("TicksExisted");
        this.maxLifetimeTicks = tag.getInt("MaxLifetime");
        this.instability = tag.getFloat("Instability");
        this.isCommandSpawned = tag.getBoolean("CommandSpawned");
        this.isQuestRelated = tag.getBoolean("QuestRelated");
        this.wavesCleared = tag.getInt("WavesCleared");
        this.currentWaveMobsLeft = tag.getInt("MobsLeft");
        this.spawnCooldown = tag.getInt("SpawnCooldown");
    }
}
