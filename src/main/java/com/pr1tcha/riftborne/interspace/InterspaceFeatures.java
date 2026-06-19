package com.pr1tcha.riftborne.interspace;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

@EventBusSubscriber(modid = Riftborne.MODID)
public final class InterspaceFeatures {
    private InterspaceFeatures() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        boolean rna = level.dimension().equals(InterspaceDimensions.RNA_INTERSPACE);
        boolean riftwalker = level.dimension().equals(InterspaceDimensions.RIFTWALKER_INTERSPACE);
        if (!rna && !riftwalker) {
            return;
        }

        Palette palette = riftwalker
                ? new Palette(ModContent.RIFTWALKER_INTERSPACE_STONE.get(), ModContent.RIFTWALKER_INTERSPACE_SURFACE.get(), ModContent.RIFTWALKER_INTERSPACE_VEIN.get())
                : new Palette(ModContent.RNA_INTERSPACE_STONE.get(), ModContent.RNA_INTERSPACE_SURFACE.get(), ModContent.RNA_INTERSPACE_VEIN.get());
        ChunkAccess chunk = event.getChunk();
        ChunkPos chunkPos = chunk.getPos();
        long salt = riftwalker ? 0x71F7A1CE5EEDL : 0x6B1E5AACE5EEDL;
        RandomSource random = RandomSource.create(level.getSeed() ^ chunkPos.toLong() ^ salt);

        shapeTerrain(chunk, chunkPos, palette, riftwalker);
        if (random.nextFloat() < (riftwalker ? 0.24F : 0.14F)) {
            addFloatingShard(chunk, chunkPos, palette, random, riftwalker);
        }
    }

    private static void shapeTerrain(ChunkAccess chunk, ChunkPos chunkPos, Palette palette, boolean riftwalker) {
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int x = chunkPos.getBlockX(localX);
                int z = chunkPos.getBlockZ(localZ);
                int top = terrainHeight(x, z, riftwalker);
                chunk.setBlockState(new BlockPos(x, 0, z), Blocks.BEDROCK.defaultBlockState(), false);
                for (int y = 1; y <= top; y++) {
                    int depth = top - y;
                    var state = depth == 0
                            ? ((Math.abs(x * 17 + z * 29) % 13 == 0) ? palette.vein.defaultBlockState() : palette.surface.defaultBlockState())
                            : (depth < 3 ? palette.surface.defaultBlockState() : palette.stone.defaultBlockState());
                    chunk.setBlockState(new BlockPos(x, y, z), state, false);
                }
                for (int y = top + 1; y <= 150; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), false);
                }
            }
        }
    }

    private static int terrainHeight(int x, int z, boolean riftwalker) {
        double scale = riftwalker ? 0.012D : 0.009D;
        double broad = Math.sin(x * scale) * (riftwalker ? 7.0D : 4.0D)
                + Math.cos(z * scale * 1.21D) * (riftwalker ? 6.0D : 3.5D)
                + Math.sin((x + z) * scale * 0.63D) * 3.0D;
        return Mth.clamp((int) Math.round(72.0D + broad), 56, 90);
    }

    private static void addFloatingShard(ChunkAccess chunk, ChunkPos chunkPos, Palette palette, RandomSource random, boolean riftwalker) {
        int cx = chunkPos.getBlockX(3 + random.nextInt(10));
        int cz = chunkPos.getBlockZ(3 + random.nextInt(10));
        int cy = terrainHeight(cx, cz, riftwalker) + 18 + random.nextInt(riftwalker ? 28 : 18);
        int radius = 2 + random.nextInt(riftwalker ? 4 : 3);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double horizontal = Math.sqrt(x * x + z * z);
                if (horizontal > radius + random.nextFloat() * 0.35F) {
                    continue;
                }
                int thickness = Math.max(1, radius - (int) horizontal);
                for (int y = -thickness; y <= 1; y++) {
                    BlockPos pos = new BlockPos(cx + x, cy + y, cz + z);
                    if (pos.getX() >= chunkPos.getMinBlockX() && pos.getX() <= chunkPos.getMaxBlockX()
                            && pos.getZ() >= chunkPos.getMinBlockZ() && pos.getZ() <= chunkPos.getMaxBlockZ()) {
                        chunk.setBlockState(pos, y == 1 ? palette.surface.defaultBlockState()
                                : (random.nextFloat() < 0.12F ? palette.vein.defaultBlockState() : palette.stone.defaultBlockState()), false);
                    }
                }
            }
        }
    }

    private record Palette(Block stone, Block surface, Block vein) {
    }
}
