package com.pr1tcha.riftborne.interspace;

import com.pr1tcha.riftborne.Riftborne;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class InterspaceDimensions {
    public static final ResourceKey<Level> RNA_INTERSPACE = dimension("rna_interspace");
    public static final ResourceKey<Level> RIFTWALKER_INTERSPACE = dimension("riftwalker_interspace");
    public static final BlockPos ARRIVAL = new BlockPos(0, 0, 0);

    private static final String RETURN_DIMENSION = "RiftborneInterspaceReturnDimension";
    private static final String RETURN_X = "RiftborneInterspaceReturnX";
    private static final String RETURN_Y = "RiftborneInterspaceReturnY";
    private static final String RETURN_Z = "RiftborneInterspaceReturnZ";

    private InterspaceDimensions() {
    }

    public static boolean enter(ServerPlayer player, ResourceKey<Level> destination) {
        ServerLevel target = player.getServer().getLevel(destination);
        if (target == null) {
            return false;
        }

        if (!isInterspace(player.serverLevel().dimension())) {
            rememberReturn(player);
        }

        target.getChunk(ARRIVAL);
        int surfaceY = target.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ARRIVAL.getX(),
                ARRIVAL.getZ()
        );
        teleport(player, target, ARRIVAL.getX() + 0.5D, surfaceY + 0.05D, ARRIVAL.getZ() + 0.5D);
        return true;
    }

    public static boolean returnToOrigin(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        ResourceLocation id = ResourceLocation.tryParse(data.getString(RETURN_DIMENSION));
        MinecraftServer server = player.getServer();
        ServerLevel target = id == null ? server.overworld() : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (target == null) {
            target = server.overworld();
        }

        double x = data.contains(RETURN_X) ? data.getDouble(RETURN_X) : target.getSharedSpawnPos().getX() + 0.5D;
        double y = data.contains(RETURN_Y) ? data.getDouble(RETURN_Y) : target.getSharedSpawnPos().getY() + 1.0D;
        double z = data.contains(RETURN_Z) ? data.getDouble(RETURN_Z) : target.getSharedSpawnPos().getZ() + 0.5D;
        teleport(player, target, x, y, z);
        return true;
    }

    public static boolean isInterspace(ResourceKey<Level> dimension) {
        return dimension.equals(RNA_INTERSPACE) || dimension.equals(RIFTWALKER_INTERSPACE);
    }

    private static void rememberReturn(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.putString(RETURN_DIMENSION, player.serverLevel().dimension().location().toString());
        data.putDouble(RETURN_X, player.getX());
        data.putDouble(RETURN_Y, player.getY());
        data.putDouble(RETURN_Z, player.getZ());
    }

    private static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z) {
        player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
    }

    private static ResourceKey<Level> dimension(String path) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, path));
    }
}
