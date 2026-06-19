package com.pr1tcha.riftborne.rift.contour;

import com.pr1tcha.riftborne.registry.ModContent;
import com.pr1tcha.riftborne.rift.block.RiftBlockEntity;
import com.pr1tcha.riftborne.rift.data.RiftData;
import com.pr1tcha.riftborne.rift.data.RiftStage;
import com.pr1tcha.riftborne.rift.RiftType;
import com.pr1tcha.riftborne.Riftborne;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Riftborne.MODID)
public final class RiftContourTeleporter {
    public static final ResourceKey<Level> DISCARD_CONTOUR = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("riftborne", "discard_contour")
    );

    private static final String COOLDOWN_TAG = "RiftborneContourCooldown";
    private static final BlockPos CONTOUR_SPAWN = new BlockPos(0, 81, 0);
    private static final int VOID_ENTRY_DEPTH = 16;

    private RiftContourTeleporter() {
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
        if (shouldEnterFromOverworldVoid(serverPlayer, level)) {
            enterDiscardContour(serverPlayer);
            return;
        }

        Optional<BlockPos> contourRiftPos = findNearbyContourRift(level, serverPlayer.blockPosition());
        if (contourRiftPos.isEmpty()) {
            return;
        }

        if (!level.dimension().equals(DISCARD_CONTOUR)) {
            enterDiscardContour(serverPlayer);
        }
    }

    private static boolean shouldEnterFromOverworldVoid(ServerPlayer player, ServerLevel level) {
        return level.dimension().equals(ServerLevel.OVERWORLD)
                && !player.isSpectator()
                && player.getDeltaMovement().y < 0.0D
                && player.getY() < level.getMinBuildHeight() - VOID_ENTRY_DEPTH;
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

    private static void enterDiscardContour(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        ServerLevel targetLevel = server.getLevel(DISCARD_CONTOUR);
        if (targetLevel == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Discard Contour dimension is not loaded."), true);
            return;
        }

        buildContourAnchor(targetLevel);
        player.getPersistentData().putInt(COOLDOWN_TAG, 60);
        player.teleportTo(targetLevel, CONTOUR_SPAWN.getX() + 0.5D, CONTOUR_SPAWN.getY(), CONTOUR_SPAWN.getZ() + 0.5D, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        targetLevel.playSound(null, CONTOUR_SPAWN, ModContent.RIFT_OPENING.get(), SoundSource.BLOCKS, 0.9F, 0.72F);
    }

    public static void emergencyEscape(ServerPlayer player) {
        ServerLevel overworld = player.getServer().overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        player.getPersistentData().putInt(COOLDOWN_TAG, 100);
        player.teleportTo(overworld, spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());
        overworld.playSound(null, spawn, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 0.85F);
    }

    public static BlockPos contourSpawn() {
        return CONTOUR_SPAWN;
    }

    public static double contourSpawnX() {
        return CONTOUR_SPAWN.getX() + 0.5D;
    }

    public static double contourSpawnY() {
        return CONTOUR_SPAWN.getY();
    }

    public static double contourSpawnZ() {
        return CONTOUR_SPAWN.getZ() + 0.5D;
    }

    private static Optional<BlockPos> findNearbyContourRift(ServerLevel level, BlockPos playerPos) {
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
                            && RiftData.isContourRift(rift.getData().riftType)
                            && (rift.getData().stage == RiftStage.ACTIVE || rift.getData().stage == RiftStage.UNSTABLE)) {
                        return Optional.of(cursor.immutable());
                    }
                }
            }
        }

        return Optional.empty();
    }
}
