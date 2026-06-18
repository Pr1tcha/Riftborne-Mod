package com.pr1tcha.riftborne.rift;

import com.pr1tcha.riftborne.config.Config;
import com.pr1tcha.riftborne.registry.ModContent;
import com.pr1tcha.riftborne.rift.block.RiftBlockEntity;
import com.pr1tcha.riftborne.rift.data.RiftData;
import com.pr1tcha.riftborne.rift.data.RiftStage;
import com.pr1tcha.riftborne.Riftborne;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Riftborne.MODID)
public class RiftSpawner {
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != ServerLevel.OVERWORLD) {
            return;
        }

        if (RiftWorldStage.getStage(level) < RiftWorldStage.STAGE_WEAK_RIFTS_ENABLED) {
            return;
        }

        if (level.getGameTime() % Config.riftCheckInterval.get() != 0) {
            return;
        }

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }

        if (RANDOM.nextDouble() > Config.riftSpawnChance.get()) {
            return;
        }

        ServerPlayer targetPlayer = players.get(RANDOM.nextInt(players.size()));
        RiftSpawnProfile profile = new RiftSpawnProfile(
                RiftType.NORMAL_RIFT,
                Math.max(Config.riftMaxRadius.get(), Config.riftMinRadius.get() + 8),
                RiftSpawnProfile.NORMAL.attempts(),
                RiftSpawnProfile.NORMAL.halfWidth(),
                RiftSpawnProfile.NORMAL.height(),
                RiftSpawnProfile.NORMAL.halfDepth(),
                RiftSpawnProfile.NORMAL.minYOffset(),
                RiftSpawnProfile.NORMAL.maxYOffset()
        );
        Optional<BlockPos> foundPos = RiftSpawnLocator.findValidRiftPosition(level, targetPlayer.blockPosition(), profile);
        if (foundPos.isEmpty()) {
            return;
        }

        BlockPos spawnPos = foundPos.get();

        level.setBlock(spawnPos, ModContent.RIFT_BLOCK.get().defaultBlockState(), 3);

        BlockEntity blockEntity = level.getBlockEntity(spawnPos);
        if (blockEntity instanceof RiftBlockEntity rift) {
            RiftData data = rift.getData();
            int baseLifetime = Config.riftDefaultLifetime.get();
            int percent = Config.riftLifetimeVariationPercent.get();
            int variation = (int) (baseLifetime * (percent / 100.0));
            int randomLifetime = baseLifetime;

            if (variation > 0) {
                randomLifetime = baseLifetime - variation + RANDOM.nextInt(variation * 2);
            }

            data.maxLifetimeTicks = randomLifetime;
            data.isCommandSpawned = false;
            data.useProceduralVisual = true;
            data.riftType = RiftData.RIFT_TYPE;
            data.stage = RiftStage.DORMANT;
            data.stageTicks = 0;
            data.radius = 6.0f + RANDOM.nextFloat() * 4.0f;
            rift.setChanged();
        }
    }
}
