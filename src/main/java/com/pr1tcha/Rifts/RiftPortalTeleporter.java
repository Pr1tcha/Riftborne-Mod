package com.pr1tcha.Rifts;

import com.pr1tcha.Rifts.RiftData.RiftBlockEntity;
import com.pr1tcha.Rifts.RiftData.RiftData;
import com.pr1tcha.Rifts.RiftData.RiftStage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

@EventBusSubscriber(modid = RiftborneRift.MODID)
public final class RiftPortalTeleporter {
    public static final ResourceKey<Level> DISCARD_CONTOUR = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("riftborne", "discard_contour")
    );

    private static final String RETURN_TAG = "RiftborneReturn";
    private static final String COOLDOWN_TAG = "RiftbornePortalCooldown";
    private static final BlockPos CONTOUR_SPAWN = new BlockPos(0, 81, 0);
    private static final BlockPos CONTOUR_EXIT_PORTAL = new BlockPos(4, 81, 0);

    private RiftPortalTeleporter() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        CompoundTag data = serverPlayer.getPersistentData();
        int cooldown = data.getInt(COOLDOWN_TAG);
        if (cooldown > 0) {
            data.putInt(COOLDOWN_TAG, cooldown - 1);
            return;
        }

        ServerLevel level = serverPlayer.serverLevel();
        Optional<BlockPos> portalPos = findNearbyPortal(level, serverPlayer.blockPosition());
        if (portalPos.isEmpty()) {
            return;
        }

        if (level.dimension().equals(DISCARD_CONTOUR)) {
            returnToStoredPosition(serverPlayer);
        } else {
            enterDiscardContour(serverPlayer, portalPos.get());
        }
    }

    public static void buildContourAnchor(ServerLevel level) {
        BlockPos floorCenter = CONTOUR_SPAWN.below();
        clearAnchorSpace(level, floorCenter);
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                double distance = Math.sqrt(x * x + z * z);
                BlockPos pos = floorCenter.offset(x, 0, z);
                if (distance <= 10.2D) {
                    level.setBlock(pos, anchorFloorState(distance, x, z), 3);
                }
            }
        }

        carveContourVeins(level, floorCenter);
        buildExitPortal(level, CONTOUR_EXIT_PORTAL);
    }

    private static net.minecraft.world.level.block.state.BlockState anchorFloorState(double distance, int x, int z) {
        if (distance > 9.1D) {
            return ModContent.CONTOUR_WEEPING_STONE.get().defaultBlockState();
        }
        if (distance < 2.6D || Math.abs(x) == Math.abs(z) && distance < 8.8D) {
            return ModContent.CONTOUR_STONE_VEIN.get().defaultBlockState();
        }
        if ((Math.abs(x * 31 + z * 17) % 11 == 0) || (distance > 6.2D && distance < 7.2D && (x + z) % 2 == 0)) {
            return ModContent.CONTOUR_TRACE.get().defaultBlockState();
        }
        return ModContent.CONTOUR_SURFACE.get().defaultBlockState();
    }

    private static void clearAnchorSpace(ServerLevel level, BlockPos floorCenter) {
        for (int x = -11; x <= 11; x++) {
            for (int z = -11; z <= 11; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance > 11.4D) {
                    continue;
                }

                for (int y = 1; y <= 13; y++) {
                    level.setBlock(floorCenter.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void carveContourVeins(ServerLevel level, BlockPos floorCenter) {
        for (int i = 0; i < 9; i++) {
            double angle = (Math.PI * 2.0D * i) / 9.0D + (i % 2) * 0.22D;
            int length = 5 + (i % 4) * 2;
            for (int step = 1; step <= length; step++) {
                double bend = Math.sin(step * 0.85D + i * 1.7D) * 0.75D;
                int x = (int) Math.round(Math.cos(angle) * step + Math.cos(angle + Math.PI / 2.0D) * bend);
                int z = (int) Math.round(Math.sin(angle) * step + Math.sin(angle + Math.PI / 2.0D) * bend);
                BlockPos veinPos = floorCenter.offset(x, 0, z);
                level.setBlock(veinPos, step <= 2 ? ModContent.CONTOUR_STONE_VEIN.get().defaultBlockState() : ModContent.CONTOUR_WEEPING_STONE.get().defaultBlockState(), 3);

                if (step % 3 == 0) {
                    BlockPos side = veinPos.offset((int) Math.signum(Math.cos(angle + Math.PI / 2.0D)), 0, (int) Math.signum(Math.sin(angle + Math.PI / 2.0D)));
                    level.setBlock(side, ModContent.CONTOUR_TRACE.get().defaultBlockState(), 3);
                }
            }
        }
    }

    private static void enterDiscardContour(ServerPlayer player, BlockPos sourcePortalPos) {
        MinecraftServer server = player.getServer();
        ServerLevel targetLevel = server.getLevel(DISCARD_CONTOUR);
        if (targetLevel == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Discard Contour dimension is not loaded."), true);
            return;
        }

        CompoundTag ret = new CompoundTag();
        ret.putString("Dimension", player.serverLevel().dimension().location().toString());
        ret.putDouble("X", sourcePortalPos.getX() + 0.5D);
        ret.putDouble("Y", sourcePortalPos.getY() + 0.2D);
        ret.putDouble("Z", sourcePortalPos.getZ() + 0.5D);
        ret.putFloat("Yaw", player.getYRot());
        ret.putFloat("Pitch", player.getXRot());
        player.getPersistentData().put(RETURN_TAG, ret);

        buildContourAnchor(targetLevel);
        player.getPersistentData().putInt(COOLDOWN_TAG, 60);
        player.teleportTo(targetLevel, CONTOUR_SPAWN.getX() + 0.5D, CONTOUR_SPAWN.getY(), CONTOUR_SPAWN.getZ() + 0.5D, player.getYRot(), player.getXRot());
        targetLevel.playSound(null, CONTOUR_SPAWN, ModContent.RIFT_OPENING.get(), SoundSource.BLOCKS, 0.9F, 0.72F);
    }

    private static void returnToStoredPosition(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(RETURN_TAG)) {
            returnToOverworldSpawn(player);
            return;
        }

        CompoundTag ret = data.getCompound(RETURN_TAG);
        ResourceLocation dimensionId = ResourceLocation.parse(ret.getString("Dimension"));
        ServerLevel targetLevel = player.getServer().getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId));
        if (targetLevel == null) {
            returnToOverworldSpawn(player);
            return;
        }

        data.remove(RETURN_TAG);
        data.putInt(COOLDOWN_TAG, 80);
        player.teleportTo(targetLevel, ret.getDouble("X"), ret.getDouble("Y"), ret.getDouble("Z"), ret.getFloat("Yaw"), ret.getFloat("Pitch"));
        targetLevel.playSound(null, BlockPos.containing(player.position()), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 0.85F);
    }

    private static void returnToOverworldSpawn(ServerPlayer player) {
        ServerLevel overworld = player.getServer().overworld();
        BlockPos pos = overworld.getSharedSpawnPos();
        player.getPersistentData().putInt(COOLDOWN_TAG, 80);
        player.teleportTo(overworld, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, player.getYRot(), player.getXRot());
    }

    private static Optional<BlockPos> findNearbyPortal(ServerLevel level, BlockPos playerPos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    cursor.set(playerPos.getX() + dx, playerPos.getY() + dy, playerPos.getZ() + dz);
                    if (!level.getBlockState(cursor).is(ModContent.RIFT_BLOCK.get())) {
                        continue;
                    }

                    BlockEntity blockEntity = level.getBlockEntity(cursor);
                    if (blockEntity instanceof RiftBlockEntity rift
                            && RiftData.PORTAL_RIFT_TYPE.equals(rift.getData().riftType)
                            && (rift.getData().stage == RiftStage.ACTIVE || rift.getData().stage == RiftStage.UNSTABLE)) {
                        return Optional.of(cursor.immutable());
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static void buildExitPortal(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ModContent.RIFT_BLOCK.get().defaultBlockState(), 3);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RiftBlockEntity rift) {
            RiftData data = rift.getData();
            data.riftType = RiftData.PORTAL_RIFT_TYPE;
            data.useProceduralVisual = true;
            data.isQuestRelated = true;
            data.maxLifetimeTicks = Integer.MAX_VALUE;
            data.radius = 7.0F;
            data.stage = RiftStage.DORMANT;
            data.stageTicks = 0;
            rift.sync();
        }
    }
}
