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
    private static final int RNA_RIVER_LEVEL = 62;

    private InterspaceFeatures() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        boolean rna = level.dimension().equals(InterspaceDimensions.RNA_INTERSPACE);
        boolean riftwalker = level.dimension().equals(InterspaceDimensions.RIFTWALKER_INTERSPACE);
        if (!rna && !riftwalker) {
            return;
        }

        ChunkAccess chunk = event.getChunk();
        if (!event.isNewChunk()) {
            if (rna) {
                replaceLegacyRnaCurrent(chunk);
            }
            return;
        }

        Palette palette = riftwalker
                ? new Palette(ModContent.RIFTWALKER_INTERSPACE_STONE.get(), ModContent.RIFTWALKER_INTERSPACE_SURFACE.get(), ModContent.RIFTWALKER_INTERSPACE_VEIN.get())
                : new Palette(ModContent.RNA_INTERSPACE_STONE.get(), ModContent.RNA_INTERSPACE_SURFACE.get(), ModContent.RNA_INTERSPACE_VEIN.get());
        ChunkPos chunkPos = chunk.getPos();
        long salt = riftwalker ? 0x71F7A1CE5EEDL : 0x6B1E5AACE5EEDL;
        RandomSource random = RandomSource.create(level.getSeed() ^ chunkPos.toLong() ^ salt);

        shapeTerrain(chunk, chunkPos, palette, riftwalker);
        if (random.nextFloat() < (riftwalker ? 0.24F : 0.14F)) {
            addFloatingShard(chunk, chunkPos, palette, random, riftwalker);
        }
    }

    private static void replaceLegacyRnaCurrent(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int x = chunkPos.getBlockX(localX);
                int z = chunkPos.getBlockZ(localZ);
                for (int y = 46; y <= RNA_RIVER_LEVEL; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (chunk.getBlockState(pos).is(Blocks.WATER)) {
                        chunk.setBlockState(pos, ModContent.RNA_INTERSPACE_CURRENT.get().defaultBlockState(), false);
                    }
                }
            }
        }
    }

    private static void shapeTerrain(ChunkAccess chunk, ChunkPos chunkPos, Palette palette, boolean riftwalker) {
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int x = chunkPos.getBlockX(localX);
                int z = chunkPos.getBlockZ(localZ);
                int top = terrainHeight(x, z, riftwalker);
                boolean rnaRiver = !riftwalker && isRnaRiver(x, z);
                boolean surfaceVein = riftwalker
                        ? isRiftwalkerVein(x, z)
                        : (rnaRiver || isRnaVein(x, z));
                chunk.setBlockState(new BlockPos(x, 0, z), Blocks.BEDROCK.defaultBlockState(), false);
                for (int y = 1; y <= top; y++) {
                    int depth = top - y;
                    var state = depth == 0
                            ? (surfaceVein ? palette.vein.defaultBlockState() : palette.surface.defaultBlockState())
                            : (riftwalker && surfaceVein && depth < 3
                                    ? palette.vein.defaultBlockState()
                                    : (depth < 3 ? palette.surface.defaultBlockState() : palette.stone.defaultBlockState()));
                    chunk.setBlockState(new BlockPos(x, y, z), state, false);
                }
                for (int y = top + 1; y <= 150; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), false);
                }
                if (rnaRiver) {
                    for (int y = top + 1; y <= RNA_RIVER_LEVEL; y++) {
                        chunk.setBlockState(
                                new BlockPos(x, y, z),
                                ModContent.RNA_INTERSPACE_CURRENT.get().defaultBlockState(),
                                false
                        );
                    }
                }
            }
        }
    }

    private static int terrainHeight(int x, int z, boolean riftwalker) {
        if (!riftwalker) {
            double broad = fractalNoise(x * 0.0045D, z * 0.0045D, 4, 1101) * 7.0D;
            double strata = Math.abs(fractalNoise(x * 0.012D, z * 0.012D, 3, 1201));
            double shelves = Math.floor((strata * 17.0D + broad) / 3.0D) * 3.0D;
            double riverCut = rnaRiverDistance(x, z);
            double channel = 1.0D - Mth.clamp((riverCut - 0.055D) / 0.12D, 0.0D, 1.0D);
            double height = 65.0D + shelves - channel * 9.0D;
            if (channel > 0.55D) {
                height = Math.min(height, RNA_RIVER_LEVEL - 2.0D - (channel - 0.55D) * 7.0D);
            }
            return Mth.clamp((int) Math.round(height), 48, 96);
        }

        double warpX = x + valueNoise(x * 0.0037D, z * 0.0037D, 17) * 72.0D;
        double warpZ = z + valueNoise(x * 0.0037D, z * 0.0037D, 53) * 72.0D;
        double distance = Math.sqrt(warpX * warpX + warpZ * warpZ);

        // A wide, broken basin keeps the reference's readable horizon while the warped
        // ridges rise into layered walls instead of ordinary Minecraft hills.
        double basin = Mth.clamp((distance - 96.0D) / 270.0D, 0.0D, 1.0D);
        basin = basin * basin * (3.0D - 2.0D * basin);

        double continental = fractalNoise(x * 0.0048D, z * 0.0048D, 4, 101);
        double strata = Math.abs(fractalNoise(x * 0.011D, z * 0.011D, 3, 211));
        double brokenRidges = Math.pow(strata, 1.65D);
        double shelves = Math.floor((basin * 20.0D + brokenRidges * 13.0D) / 3.0D) * 3.0D;
        double fine = fractalNoise(x * 0.035D, z * 0.035D, 2, 307) * 2.7D;

        return Mth.clamp((int) Math.round(61.0D + shelves + continental * 7.0D + fine), 48, 112);
    }

    private static boolean isRiftwalkerVein(int x, int z) {
        double warp = fractalNoise(x * 0.012D, z * 0.012D, 3, 701) * 8.0D;
        double branchA = Math.abs(Math.sin(x * 0.071D + z * 0.026D + warp));
        double branchB = Math.abs(Math.sin(x * -0.031D + z * 0.083D - warp * 0.74D));
        double branchC = Math.abs(Math.sin((x + z) * 0.045D + warp * 1.31D));
        double crack = Math.min(branchA, Math.min(branchB, branchC));
        double breakup = valueNoise(x * 0.027D, z * 0.027D, 809);
        return crack < 0.075D && breakup > -0.48D;
    }

    private static boolean isRnaRiver(int x, int z) {
        return rnaRiverDistance(x, z) < 0.105D;
    }

    private static double rnaRiverDistance(int x, int z) {
        double warp = fractalNoise(x * 0.006D, z * 0.006D, 3, 1301) * 13.0D;
        double main = Math.abs(Math.sin(x * 0.018D + z * 0.007D + warp));
        double tributary = Math.abs(Math.sin(x * -0.011D + z * 0.026D - warp * 0.63D));
        return Math.min(main, tributary * 1.22D);
    }

    private static boolean isRnaVein(int x, int z) {
        double warp = fractalNoise(x * 0.018D, z * 0.018D, 2, 1409) * 5.0D;
        double trace = Math.abs(Math.sin(x * 0.052D - z * 0.031D + warp));
        return trace < 0.055D && !isRnaRiver(x, z);
    }

    private static double fractalNoise(double x, double z, int octaves, int seed) {
        double value = 0.0D;
        double amplitude = 1.0D;
        double totalAmplitude = 0.0D;
        for (int octave = 0; octave < octaves; octave++) {
            value += valueNoise(x, z, seed + octave * 131) * amplitude;
            totalAmplitude += amplitude;
            x = x * 2.03D + 19.17D;
            z = z * 2.03D - 7.41D;
            amplitude *= 0.5D;
        }
        return value / totalAmplitude;
    }

    private static double valueNoise(double x, double z, int seed) {
        int x0 = Mth.floor(x);
        int z0 = Mth.floor(z);
        double tx = smooth(x - x0);
        double tz = smooth(z - z0);
        double a = Mth.lerp(tx, hash(x0, z0, seed), hash(x0 + 1, z0, seed));
        double b = Mth.lerp(tx, hash(x0, z0 + 1, seed), hash(x0 + 1, z0 + 1, seed));
        return Mth.lerp(tz, a, b);
    }

    private static double smooth(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double hash(int x, int z, int seed) {
        long value = x * 0x632BE59BD9B4E019L ^ z * 0x9E3779B97F4A7C15L ^ seed;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return ((value & 0x1FFFFFFFFFFFFFL) / (double) 0x1FFFFFFFFFFFFFL) * 2.0D - 1.0D;
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
