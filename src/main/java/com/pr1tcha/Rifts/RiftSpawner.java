package com.pr1tcha.Rifts;


import com.pr1tcha.Rifts.RiftData.RiftBlockEntity;
import com.pr1tcha.Rifts.RiftData.RiftData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;
import java.util.Random;

@EventBusSubscriber(modid = com.pr1tcha.Rifts.RiftborneRift.MODID)
public class RiftSpawner {
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != ServerLevel.OVERWORLD) {
            return;
        }

        // Читаем интервал проверки из конфига
        if (level.getGameTime() % Config.riftCheckInterval.get() != 0) {
            return;
        }

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        // Читаем шанс спавна из конфига
        if (RANDOM.nextDouble() > Config.riftSpawnChance.get()) {
            return;
        }

        ServerPlayer targetPlayer = players.get(RANDOM.nextInt(players.size()));
        BlockPos playerPos = targetPlayer.blockPosition();

        // Читаем радиусы из конфига
        int minR = Config.riftMinRadius.get();
        int maxR = Config.riftMaxRadius.get();

        if (maxR <= minR) maxR = minR + 10; // Защита от неправильной настройки конфига игроком

        int offsetX = (RANDOM.nextBoolean() ? 1 : -1) * (minR + RANDOM.nextInt(maxR - minR));
        int offsetZ = (RANDOM.nextBoolean() ? 1 : -1) * (minR + RANDOM.nextInt(maxR - minR));

        int spawnX = playerPos.getX() + offsetX;
        int spawnZ = playerPos.getZ() + offsetZ;

        if (!level.hasChunkAt(new BlockPos(spawnX, 64, spawnZ))) {
            return;
        }

        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, spawnX, spawnZ);
        int randomY = surfaceY + 3 + RANDOM.nextInt(5);
        BlockPos spawnPos = new BlockPos(spawnX, randomY, spawnZ);

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
            data.radius = 6.0f + RANDOM.nextFloat() * 4.0f;
            rift.setChanged();
        }

    }
}
