package com.pr1tcha.riftborne.rift;

import com.pr1tcha.riftborne.Riftborne;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

public final class RiftSpawnLocator {
    private static final Random RANDOM = new Random();

    private RiftSpawnLocator() {
    }

    public static Optional<BlockPos> findValidRiftPosition(ServerLevel level, BlockPos playerPos, RiftSpawnProfile profile) {
        for (int attempt = 0; attempt < profile.attempts(); attempt++) {
            int radius = 4 + RANDOM.nextInt(Math.max(1, profile.searchRadius() - 3));
            double angle = RANDOM.nextDouble() * Mth.TWO_PI;
            int x = playerPos.getX() + Mth.floor(Math.cos(angle) * radius);
            int z = playerPos.getZ() + Mth.floor(Math.sin(angle) * radius);
            int y = playerPos.getY() + profile.minYOffset() + RANDOM.nextInt(profile.maxYOffset() - profile.minYOffset() + 1);

            BlockPos candidate = new BlockPos(x, y, z);
            Optional<BlockPos> clearSpace = findNearbyClearSpace(level, candidate, profile);
            if (clearSpace.isPresent()) {
                return clearSpace;
            }
        }

        return Optional.empty();
    }

    public static boolean isValidRiftPosition(Level level, BlockPos pos, RiftSpawnProfile profile) {
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -profile.halfWidth(); dx <= profile.halfWidth(); dx++) {
            for (int dy = 0; dy < profile.height(); dy++) {
                for (int dz = -profile.halfDepth(); dz <= profile.halfDepth(); dz++) {
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!level.getWorldBorder().isWithinBounds(cursor)) {
                        return false;
                    }

                    BlockState state = level.getBlockState(cursor);
                    if (!state.getFluidState().isEmpty() || !state.getCollisionShape(level, cursor).isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static Optional<BlockPos> findNearbyClearSpace(ServerLevel level, BlockPos origin, RiftSpawnProfile profile) {
        int minY = Math.max(level.getMinBuildHeight() + 2, origin.getY() - 8);
        int maxY = Math.min(level.getMaxBuildHeight() - profile.height() - 2, origin.getY() + 8);

        for (int y = origin.getY(); y <= maxY; y++) {
            BlockPos candidate = new BlockPos(origin.getX(), y, origin.getZ());
            if (isValidRiftPosition(level, candidate, profile)) {
                return Optional.of(candidate);
            }
        }

        for (int y = origin.getY() - 1; y >= minY; y--) {
            BlockPos candidate = new BlockPos(origin.getX(), y, origin.getZ());
            if (isValidRiftPosition(level, candidate, profile)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }
}
