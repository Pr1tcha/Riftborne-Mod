package com.pr1tcha.riftborne.rift.dimension;

import java.util.Optional;
import java.util.UUID;
import com.pr1tcha.riftborne.rift.run.RiftRunData;
import com.pr1tcha.riftborne.rift.run.RiftRunSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class RiftDimensions {
    public static final BlockPos ARRIVAL = new BlockPos(0, 82, 0);

    private static final String RETURN_DIMENSION = "RiftborneRiftReturnDimension";
    private static final String RETURN_X = "RiftborneRiftReturnX";
    private static final String RETURN_Y = "RiftborneRiftReturnY";
    private static final String RETURN_Z = "RiftborneRiftReturnZ";
    private static final String CURRENT_RUN = "RiftborneCurrentRun";
    private static final String PORTAL_COOLDOWN = "RiftbornePortalCooldown";
    private static final int RETURN_SEARCH_RADIUS = 8;
    private static final int RETURN_VERTICAL_RANGE = 12;

    private RiftDimensions() {
    }

    public static boolean enter(ServerPlayer player, RiftTier tier) {
        RiftRunData run = RiftRunSavedData.get(player.getServer())
                .createRun(player.serverLevel(), player.blockPosition(), tier);
        return enter(player, run);
    }

    public static boolean enter(ServerPlayer player, RiftRunData run) {
        ServerLevel target = player.getServer().getLevel(run.riftDimension());
        if (target == null) {
            return false;
        }

        if (!isRiftDimension(player.serverLevel().dimension())) {
            rememberReturn(player);
        }

        RiftRunSavedData storage = RiftRunSavedData.get(player.getServer());
        storage.playerJoined(run, player.getUUID());
        player.getPersistentData().putUUID(CURRENT_RUN, run.runId());
        buildArrivalPlatform(target, run.tier(), run.anchorPos());
        teleport(player, target, Vec3.atBottomCenterOf(run.anchorPos()));
        return true;
    }

    public static boolean exit(ServerPlayer player) {
        if (!isRiftDimension(player.serverLevel().dimension())) {
            return false;
        }

        CompoundTag data = player.getPersistentData();
        MinecraftServer server = player.getServer();
        RiftRunData run = currentRun(player).orElse(null);
        ServerLevel target = run == null ? resolveReturnLevel(server, data) : server.getLevel(run.sourceDimension());
        if (target == null) {
            target = server.overworld();
        }
        ServerLevel returnLevel = target;
        Vec3 requested = run == null
                ? readReturnPosition(returnLevel, data)
                : Vec3.atBottomCenterOf(run.sourcePos().offset(3, 1, 0));
        Vec3 safe = findSafeReturn(returnLevel, BlockPos.containing(requested))
                .map(Vec3::atBottomCenterOf)
                .orElseGet(() -> Vec3.atBottomCenterOf(returnLevel.getSharedSpawnPos().above()));

        data.putLong(PORTAL_COOLDOWN, returnLevel.getGameTime() + 100L);
        teleport(player, returnLevel, safe);
        clearReturn(player);
        return true;
    }

    public static boolean isRiftDimension(ResourceKey<Level> dimension) {
        return RiftTier.fromDimension(dimension).isPresent();
    }

    public static Optional<RiftTier> currentTier(ServerPlayer player) {
        return RiftTier.fromDimension(player.serverLevel().dimension());
    }

    public static void buildArrivalPlatform(ServerLevel level, RiftTier tier) {
        buildArrivalPlatform(level, tier, ARRIVAL);
    }

    public static void buildArrivalPlatform(ServerLevel level, RiftTier tier, BlockPos arrival) {
        BlockPos floor = arrival.below();
        var floorState = tier.platformBlock().defaultBlockState();
        var accentState = tier.level() >= 4
                ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                : Blocks.AMETHYST_BLOCK.defaultBlockState();

        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                if (x * x + z * z > 36) {
                    continue;
                }

                BlockPos floorPos = floor.offset(x, 0, z);
                boolean accent = x == 0 || z == 0 || Math.abs(x) == Math.abs(z);
                level.setBlock(floorPos, accent ? accentState : floorState, 3);
                for (int y = 1; y <= 5; y++) {
                    level.setBlock(floorPos.above(y), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void rememberReturn(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.putString(RETURN_DIMENSION, player.serverLevel().dimension().location().toString());
        data.putDouble(RETURN_X, player.getX());
        data.putDouble(RETURN_Y, player.getY());
        data.putDouble(RETURN_Z, player.getZ());
    }

    private static ServerLevel resolveReturnLevel(MinecraftServer server, CompoundTag data) {
        ResourceLocation id = ResourceLocation.tryParse(data.getString(RETURN_DIMENSION));
        if (id != null) {
            ServerLevel stored = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (stored != null && !isRiftDimension(stored.dimension())) {
                return stored;
            }
        }
        return server.overworld();
    }

    private static Vec3 readReturnPosition(ServerLevel target, CompoundTag data) {
        if (data.contains(RETURN_X) && data.contains(RETURN_Y) && data.contains(RETURN_Z)) {
            return new Vec3(data.getDouble(RETURN_X), data.getDouble(RETURN_Y), data.getDouble(RETURN_Z));
        }
        return Vec3.atBottomCenterOf(target.getSharedSpawnPos().above());
    }

    private static Optional<BlockPos> findSafeReturn(ServerLevel level, BlockPos origin) {
        for (int radius = 0; radius <= RETURN_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    for (int dy = 0; dy <= RETURN_VERTICAL_RANGE; dy++) {
                        BlockPos upward = origin.offset(dx, dy, dz);
                        if (isSafeStandingPosition(level, upward)) {
                            return Optional.of(upward);
                        }
                        if (dy > 0) {
                            BlockPos downward = origin.offset(dx, -dy, dz);
                            if (isSafeStandingPosition(level, downward)) {
                                return Optional.of(downward);
                            }
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isSafeStandingPosition(ServerLevel level, BlockPos feet) {
        if (feet.getY() <= level.getMinBuildHeight() || feet.getY() + 1 >= level.getMaxBuildHeight()) {
            return false;
        }
        if (!level.getWorldBorder().isWithinBounds(feet)) {
            return false;
        }

        BlockPos floor = feet.below();
        boolean sturdyFloor = level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
        boolean feetClear = level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && !level.getBlockState(feet).getFluidState().isSource();
        BlockPos head = feet.above();
        boolean headClear = level.getBlockState(head).getCollisionShape(level, head).isEmpty()
                && !level.getBlockState(head).getFluidState().isSource();
        return sturdyFloor && feetClear && headClear;
    }

    private static void clearReturn(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.remove(RETURN_DIMENSION);
        data.remove(RETURN_X);
        data.remove(RETURN_Y);
        data.remove(RETURN_Z);
        data.remove(CURRENT_RUN);
    }

    public static Optional<RiftRunData> currentRun(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.hasUUID(CURRENT_RUN)) {
            return Optional.empty();
        }
        UUID runId = data.getUUID(CURRENT_RUN);
        return RiftRunSavedData.get(player.getServer()).getRun(runId);
    }

    private static void teleport(ServerPlayer player, ServerLevel level, Vec3 position) {
        player.teleportTo(level, position.x, position.y, position.z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
    }
}
