package com.pr1tcha.Rifts;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

@EventBusSubscriber(modid = RiftborneRift.MODID)
public final class DiscardContourFeatures {
    private static final int CLEAR_HEIGHT = 150;

    private DiscardContourFeatures() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(RiftPortalTeleporter.DISCARD_CONTOUR)) {
            return;
        }

        ChunkAccess chunk = event.getChunk();
        ChunkPos chunkPos = chunk.getPos();
        RandomSource random = RandomSource.create(level.getSeed() ^ chunkPos.toLong() ^ 0x4D1B0A7A5EEDL);

        reshapeSurface(chunk, chunkPos);
        addSurfaceTraces(chunk, chunkPos, random);
        if (random.nextFloat() < 0.14F) {
            addFloatingFragment(chunk, chunkPos, random);
        }
        if (random.nextFloat() < 0.065F) {
            addHangingRuin(chunk, chunkPos, random);
        }
        if (random.nextFloat() < 0.11F) {
            addSkyChain(chunk, chunkPos, random);
        }
        if (random.nextFloat() < 0.075F) {
            addSkyRibbon(chunk, chunkPos, random);
        }
        if (random.nextFloat() < 0.18F) {
            addDistantShards(chunk, chunkPos, random);
        }
    }

    private static void reshapeSurface(ChunkAccess chunk, ChunkPos chunkPos) {
        int maxClearY = Math.min(chunk.getMaxBuildHeight() - 1, CLEAR_HEIGHT);
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int x = chunkPos.getBlockX(localX);
                int z = chunkPos.getBlockZ(localZ);
                TerrainColumn column = terrainColumn(x, z);

                chunk.setBlockState(new BlockPos(x, 0, z), Blocks.BEDROCK.defaultBlockState(), false);
                for (int y = 1; y <= column.topY; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), stateForDepth(column, column.topY - y), false);
                }
                for (int y = column.topY + 1; y <= maxClearY; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), false);
                }

                if (column.deepCrack) {
                    addCrackGlow(chunk, x, z, column.topY);
                }
            }
        }
    }

    private static TerrainColumn terrainColumn(int x, int z) {
        double broad = Math.sin(x * 0.014D) * 10.0D
                + Math.cos(z * 0.017D) * 8.0D
                + Math.sin((x + z) * 0.009D) * 6.0D
                + Math.sin((x - z) * 0.032D) * 3.5D;
        double basin = Math.max(0.0D, Math.sin(x * 0.006D + 1.7D) + Math.cos(z * 0.007D - 0.4D) - 1.18D) * 24.0D;

        double crackA = Math.abs(Math.sin(x * 0.034D + z * 0.006D));
        double crackB = Math.abs(Math.sin(x * 0.007D - z * 0.031D + 1.9D));
        double crackC = Math.abs(Math.sin((x + z) * 0.023D - 0.8D));
        double crack = Math.min(crackA, Math.min(crackB, crackC));
        double canyon = crack < 0.13D ? Mth.square((float) ((0.13D - crack) / 0.13D)) * 34.0D : 0.0D;
        boolean deepCrack = crack < 0.032D;

        int topY = Mth.clamp((int) Math.round(73.0D + broad - basin - canyon), 24, 104);
        if (deepCrack) {
            topY = Math.min(topY, 18 + (int) Math.round(Math.abs(Math.sin(x * 0.11D + z * 0.09D)) * 9.0D));
        }

        return new TerrainColumn(topY, deepCrack);
    }

    private static BlockState stateForDepth(TerrainColumn column, int depth) {
        if (depth == 0) {
            return column.deepCrack ? ModContent.CONTOUR_VEIN.get().defaultBlockState() : ModContent.CONTOUR_SURFACE.get().defaultBlockState();
        }
        if (depth < 4) {
            return ModContent.CONTOUR_SURFACE.get().defaultBlockState();
        }
        if (column.deepCrack && depth % 9 == 0) {
            return ModContent.CONTOUR_VEIN.get().defaultBlockState();
        }
        return ModContent.CONTOUR_STONE.get().defaultBlockState();
    }

    private static void addCrackGlow(ChunkAccess chunk, int x, int z, int topY) {
        for (int y = Math.max(2, topY - 7); y <= topY; y++) {
            if ((x + y + z) % 3 == 0) {
                chunk.setBlockState(new BlockPos(x, y, z), ModContent.CONTOUR_VEIN.get().defaultBlockState(), false);
            }
        }
    }

    private static void addSurfaceTraces(ChunkAccess chunk, ChunkPos chunkPos, RandomSource random) {
        int traces = 2 + random.nextInt(3);
        for (int trace = 0; trace < traces; trace++) {
            int x = chunkPos.getBlockX(1 + random.nextInt(14));
            int z = chunkPos.getBlockZ(1 + random.nextInt(14));
            int length = 6 + random.nextInt(13);
            double angle = random.nextDouble() * Math.PI * 2.0D;

            for (int step = 0; step < length; step++) {
                int px = x + (int) Math.round(Math.cos(angle) * step + random.nextInt(3) - 1);
                int pz = z + (int) Math.round(Math.sin(angle) * step + random.nextInt(3) - 1);
                if (px < chunkPos.getMinBlockX() || px > chunkPos.getMaxBlockX() || pz < chunkPos.getMinBlockZ() || pz > chunkPos.getMaxBlockZ()) {
                    continue;
                }

                TerrainColumn column = terrainColumn(px, pz);
                if (column.deepCrack || column.topY <= chunk.getMinBuildHeight() + 4) {
                    continue;
                }

                chunk.setBlockState(new BlockPos(px, column.topY, pz), ModContent.CONTOUR_TRACE.get().defaultBlockState(), false);
                if (random.nextFloat() < 0.18F) {
                    int sideX = px + (random.nextInt(3) - 1);
                    int sideZ = pz + (random.nextInt(3) - 1);
                    if (sideX >= chunkPos.getMinBlockX() && sideX <= chunkPos.getMaxBlockX() && sideZ >= chunkPos.getMinBlockZ() && sideZ <= chunkPos.getMaxBlockZ()) {
                        TerrainColumn sideColumn = terrainColumn(sideX, sideZ);
                        if (!sideColumn.deepCrack) {
                            chunk.setBlockState(new BlockPos(sideX, sideColumn.topY, sideZ), ModContent.CONTOUR_TRACE.get().defaultBlockState(), false);
                        }
                    }
                }
            }
        }
    }

    private static void addFloatingFragment(ChunkAccess chunk, ChunkPos chunkPos, RandomSource random) {
        int centerX = chunkPos.getBlockX(2 + random.nextInt(12));
        int centerZ = chunkPos.getBlockZ(2 + random.nextInt(12));
        int radiusX = 3 + random.nextInt(5);
        int radiusZ = 3 + random.nextInt(5);
        int localHigh = highestTerrain(centerX, centerZ, radiusX, radiusZ);
        int clearance = switch (random.nextInt(8)) {
            case 0 -> 0;
            case 1, 2 -> 3 + random.nextInt(5);
            case 3, 4, 5 -> 8 + random.nextInt(10);
            default -> 18 + random.nextInt(15);
        };
        int centerY = Math.min(CLEAR_HEIGHT - 16, localHigh + clearance + 4);
        int coreThickness = 2 + random.nextInt(4);
        boolean grounded = clearance <= 4 || random.nextFloat() < 0.22F;

        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                double nx = x / (double) radiusX;
                double nz = z / (double) radiusZ;
                double distance = nx * nx + nz * nz;
                double noise = Math.sin((centerX + x) * 0.57D + (centerZ + z) * 0.23D) * 0.13D
                        + Math.cos((centerX + x) * 0.31D - (centerZ + z) * 0.61D) * 0.12D;
                if (distance > 1.0D + noise) {
                    continue;
                }

                int worldX = centerX + x;
                int worldZ = centerZ + z;
                int terrainY = terrainColumn(worldX, worldZ).topY;
                int roughTop = centerY + (int) Math.round(Math.sin(worldX * 0.41D + worldZ * 0.19D) * 1.6D);
                int taper = (int) Math.round(distance * (coreThickness + 2));
                int bottom = roughTop - coreThickness - taper;
                if (grounded && distance < 0.42D) {
                    bottom = Math.min(bottom, terrainY + random.nextInt(2));
                } else {
                    bottom = Math.max(bottom, terrainY + 2);
                }

                for (int y = bottom; y <= roughTop; y++) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    if (isInsideChunk(chunkPos, pos)) {
                        chunk.setBlockState(pos, fragmentState(random, y, bottom, roughTop), false);
                    }
                }

                if (!grounded && random.nextFloat() < 0.12F + (1.0D - distance) * 0.2F) {
                    int spikeLength = 2 + random.nextInt(8);
                    for (int y = bottom - 1; y >= Math.max(terrainY + 1, bottom - spikeLength); y--) {
                        BlockPos pos = new BlockPos(worldX, y, worldZ);
                        if (isInsideChunk(chunkPos, pos)) {
                            chunk.setBlockState(pos, random.nextFloat() < 0.18F ? ModContent.CONTOUR_STONE_VEIN.get().defaultBlockState() : ModContent.CONTOUR_STONE.get().defaultBlockState(), false);
                        }
                    }
                } else if (grounded && distance < 0.22D) {
                    for (int y = terrainY + 1; y < bottom; y++) {
                        BlockPos pos = new BlockPos(worldX, y, worldZ);
                        if (isInsideChunk(chunkPos, pos)) {
                            chunk.setBlockState(pos, ModContent.CONTOUR_STONE.get().defaultBlockState(), false);
                        }
                    }
                }
            }
        }
    }

    private static void addHangingRuin(ChunkAccess chunk, ChunkPos chunkPos, RandomSource random) {
        int x = chunkPos.getBlockX(4 + random.nextInt(8));
        int z = chunkPos.getBlockZ(4 + random.nextInt(8));
        int terrainY = terrainColumn(x, z).topY;
        int topY = Math.min(CLEAR_HEIGHT - 8, terrainY + 18 + random.nextInt(34));
        int height = Math.min(topY - terrainY - 1, 10 + random.nextInt(28));

        for (int y = 0; y < height; y++) {
            BlockPos pos = new BlockPos(x + random.nextInt(3) - 1, topY - y, z + random.nextInt(3) - 1);
            if (isInsideChunk(chunkPos, pos)) {
                chunk.setBlockState(pos, y % 5 == 0 ? ModContent.CONTOUR_STONE_VEIN.get().defaultBlockState() : ModContent.CONTOUR_STONE.get().defaultBlockState(), false);
            }
        }
    }

    private static void addSkyChain(ChunkAccess chunk, ChunkPos chunkPos, RandomSource random) {
        int baseX = chunkPos.getBlockX(2 + random.nextInt(12));
        int baseZ = chunkPos.getBlockZ(2 + random.nextInt(12));
        int terrainY = terrainColumn(baseX, baseZ).topY;
        int baseY = Mth.clamp(terrainY + 44 + random.nextInt(36), 102, CLEAR_HEIGHT + 44);
        int pieces = 7 + random.nextInt(12);
        double driftX = random.nextDouble() * 1.4D - 0.7D;
        double driftZ = random.nextDouble() * 1.4D - 0.7D;

        for (int i = 0; i < pieces; i++) {
            int x = baseX + (int) Math.round(driftX * i + Math.sin(i * 0.9D) * 2.2D);
            int z = baseZ + (int) Math.round(driftZ * i + Math.cos(i * 0.72D) * 2.2D);
            int y = baseY + i * (2 + random.nextInt(3));
            int radius = i % 3 == 0 ? 2 : 1;
            placeShardBlob(chunk, chunkPos, x, y, z, radius, random, 0.22F);
        }
    }

    private static void addSkyRibbon(ChunkAccess chunk, ChunkPos chunkPos, RandomSource random) {
        int centerX = chunkPos.getBlockX(2 + random.nextInt(12));
        int centerZ = chunkPos.getBlockZ(2 + random.nextInt(12));
        int terrainY = terrainColumn(centerX, centerZ).topY;
        int centerY = Mth.clamp(terrainY + 34 + random.nextInt(30), 94, CLEAR_HEIGHT + 20);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        int length = 10 + random.nextInt(16);

        for (int i = -length; i <= length; i++) {
            double curve = Math.sin(i * 0.35D) * 4.0D;
            int x = centerX + (int) Math.round(Math.cos(angle) * i + Math.cos(angle + Math.PI / 2.0D) * curve);
            int z = centerZ + (int) Math.round(Math.sin(angle) * i + Math.sin(angle + Math.PI / 2.0D) * curve);
            int y = centerY + (int) Math.round(Math.cos(i * 0.28D) * 5.0D);
            if (random.nextFloat() < 0.72F) {
                placeShardBlob(chunk, chunkPos, x, y, z, random.nextFloat() < 0.2F ? 2 : 1, random, 0.16F);
            }
        }
    }

    private static void addDistantShards(ChunkAccess chunk, ChunkPos chunkPos, RandomSource random) {
        int count = 2 + random.nextInt(5);
        for (int i = 0; i < count; i++) {
            int x = chunkPos.getBlockX(random.nextInt(16));
            int z = chunkPos.getBlockZ(random.nextInt(16));
            int terrainY = terrainColumn(x, z).topY;
            int y = Mth.clamp(terrainY + 26 + random.nextInt(74), 92, CLEAR_HEIGHT + 58);
            placeShardBlob(chunk, chunkPos, x, y, z, random.nextFloat() < 0.28F ? 2 : 1, random, 0.12F);
        }
    }

    private static void placeShardBlob(ChunkAccess chunk, ChunkPos chunkPos, int centerX, int centerY, int centerZ, int radius, RandomSource random, float veinedChance) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y * 1.4D + z * z);
                    if (distance > radius + random.nextFloat() * 0.45F) {
                        continue;
                    }

                    BlockPos pos = new BlockPos(centerX + x, centerY + y, centerZ + z);
                    if (isInsideChunk(chunkPos, pos)) {
                        chunk.setBlockState(pos, random.nextFloat() < veinedChance ? ModContent.CONTOUR_STONE_VEIN.get().defaultBlockState() : ModContent.CONTOUR_STONE.get().defaultBlockState(), false);
                    }
                }
            }
        }
    }

    private static int highestTerrain(int centerX, int centerZ, int radiusX, int radiusZ) {
        int highest = Integer.MIN_VALUE;
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                highest = Math.max(highest, terrainColumn(centerX + x, centerZ + z).topY);
            }
        }
        return highest;
    }

    private static BlockState fragmentState(RandomSource random, int y, int bottom, int top) {
        if (y == top) {
            return random.nextFloat() < 0.28F ? ModContent.CONTOUR_TRACE.get().defaultBlockState() : ModContent.CONTOUR_SURFACE.get().defaultBlockState();
        }
        if (y == bottom && random.nextFloat() < 0.18F) {
            return ModContent.CONTOUR_STONE_VEIN.get().defaultBlockState();
        }
        return random.nextFloat() < 0.08F ? ModContent.CONTOUR_STONE_VEIN.get().defaultBlockState() : ModContent.CONTOUR_STONE.get().defaultBlockState();
    }

    private static boolean isInsideChunk(ChunkPos chunkPos, BlockPos pos) {
        return pos.getX() >= chunkPos.getMinBlockX()
                && pos.getX() <= chunkPos.getMaxBlockX()
                && pos.getZ() >= chunkPos.getMinBlockZ()
                && pos.getZ() <= chunkPos.getMaxBlockZ();
    }

    private record TerrainColumn(int topY, boolean deepCrack) {
    }
}
