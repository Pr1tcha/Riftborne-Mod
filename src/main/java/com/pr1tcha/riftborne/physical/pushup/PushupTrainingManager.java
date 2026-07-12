package com.pr1tcha.riftborne.physical.pushup;

import com.pr1tcha.riftborne.physical.PhysicalStat;
import com.pr1tcha.riftborne.physical.PhysicalTrainingData;
import com.pr1tcha.riftborne.physical.PhysicalTrainingManager;
import com.pr1tcha.riftborne.physical.pushup.block.PushupMatBlock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class PushupTrainingManager {
    private static final int PREPARE_TICKS = 16;
    private static final int RAISE_TICKS = 10;
    private static final int HOLD_TICKS = 5;
    private static final int LOWER_TICKS = 14;
    private static final float REP_PROGRESS = 100.0F / 15.0F;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<StationKey, UUID> OCCUPIED_MATS = new HashMap<>();

    private PushupTrainingManager() {
    }

    public static void start(ServerPlayer player, BlockPos matPos, Direction facing) {
        PhysicalTrainingData data = PhysicalTrainingManager.getData(player);
        if (data.dailyProgress(PhysicalStat.STRENGTH) >= PhysicalTrainingData.COMPLETE_THRESHOLD) {
            player.displayClientMessage(Component.translatable(
                    "message.riftborne.physical.already_complete",
                    Component.translatable(PhysicalStat.STRENGTH.titleKey())
            ), true);
            return;
        }

        Session current = SESSIONS.get(player.getUUID());
        if (current != null) {
            if (current.matPos.equals(matPos)) {
                stop(player, true);
            } else {
                player.displayClientMessage(Component.translatable("message.riftborne.pushup.already_training"), true);
            }
            return;
        }

        PhysicalTrainingManager.cancelStationSession(player, false);

        StationKey key = new StationKey(player.level().dimension().location().toString(), matPos.immutable());
        UUID occupant = OCCUPIED_MATS.get(key);
        if (occupant != null && !occupant.equals(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.riftborne.pushup.mat_busy"), true);
            return;
        }

        Vec3 anchor = Vec3.atBottomCenterOf(matPos).add(
                facing.getStepX() * 0.5D,
                0.13D,
                facing.getStepZ() * 0.5D
        );
        float yaw = facing.toYRot();
        Session session = new Session(key, matPos.immutable(), anchor, facing, yaw);
        SESSIONS.put(player.getUUID(), session);
        OCCUPIED_MATS.put(key, player.getUUID());
        player.teleportTo(anchor.x, anchor.y, anchor.z);
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setDeltaMovement(Vec3.ZERO);
        player.displayClientMessage(Component.translatable("message.riftborne.pushup.started"), true);
        player.serverLevel().playSound(null, matPos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.7F, 1.1F);
        PhysicalTrainingNetwork.broadcastState(player, session);
    }

    public static void tick(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }

        if (!player.isAlive()) {
            stop(player, false);
            return;
        }

        if (!PushupMatBlock.isComplete(player.serverLevel(), session.matPos, session.facing)) {
            stop(player, false);
            return;
        }

        if (player.position().distanceToSqr(session.anchor) > 0.02D) {
            player.teleportTo(session.anchor.x, session.anchor.y, session.anchor.z);
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setSprinting(false);

        if (session.stateTicks > 0) {
            session.stateTicks--;
        }
        if (session.stateTicks > 0) {
            if (player.tickCount % 10 == 0) {
                PhysicalTrainingNetwork.broadcastState(player, session);
            }
            return;
        }

        switch (session.phase) {
            case PREPARE, REST, PAUSED -> {
                session.phase = PushupPhase.LOWER;
                session.stateTicks = 0;
            }
            case LOWER -> {
                return;
            }
            case RAISE -> completeRepetition(player, session);
            case HOLD -> {
                session.phase = PushupPhase.REST;
                session.stateTicks = LOWER_TICKS;
            }
            case IDLE -> stop(player, false);
        }
        if (!SESSIONS.containsKey(player.getUUID())) {
            return;
        }
        PhysicalTrainingNetwork.broadcastState(player, session);
    }

    public static void handleInput(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (session.phase != PushupPhase.LOWER) {
            return;
        }
        session.phase = PushupPhase.RAISE;
        session.stateTicks = RAISE_TICKS;
        player.serverLevel().playSound(null, session.matPos,
                SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 0.45F, 0.92F);
        PhysicalTrainingNetwork.broadcastState(player, session);
    }

    public static void stop(ServerPlayer player, boolean notify) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        OCCUPIED_MATS.remove(session.stationKey, player.getUUID());
        player.setDeltaMovement(Vec3.ZERO);
        if (notify) {
            player.displayClientMessage(Component.translatable("message.riftborne.pushup.stopped"), true);
        }
        PhysicalTrainingNetwork.broadcastInactive(player);
    }

    public static void stopAt(ServerLevel level, BlockPos matPos) {
        UUID playerId = OCCUPIED_MATS.remove(new StationKey(level.dimension().location().toString(), matPos));
        if (playerId == null) {
            return;
        }
        Session session = SESSIONS.remove(playerId);
        if (session != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.riftborne.pushup.interrupted"), true);
                PhysicalTrainingNetwork.broadcastInactive(player);
            }
        }
    }

    public static boolean isTraining(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    private static void completeRepetition(ServerPlayer player, Session session) {
        session.repetitions++;
        float progress = PhysicalTrainingManager.recordExerciseProgress(
                player,
                PhysicalStat.STRENGTH,
                REP_PROGRESS,
                true
        );
        player.serverLevel().playSound(null, session.matPos,
                SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.45F,
                1.0F + Math.min(0.5F, session.repetitions * 0.025F));
        if (progress >= PhysicalTrainingData.COMPLETE_THRESHOLD) {
            player.displayClientMessage(Component.translatable("message.riftborne.pushup.completed"), false);
            stop(player, false);
            return;
        }
        session.phase = PushupPhase.HOLD;
        session.stateTicks = HOLD_TICKS;
    }

    public static final class Session {
        private final StationKey stationKey;
        private final BlockPos matPos;
        private final Vec3 anchor;
        private final Direction facing;
        private final float yaw;
        private PushupPhase phase = PushupPhase.PREPARE;
        private int stateTicks = PREPARE_TICKS;
        private int repetitions;

        private Session(StationKey stationKey, BlockPos matPos, Vec3 anchor, Direction facing, float yaw) {
            this.stationKey = stationKey;
            this.matPos = matPos;
            this.anchor = anchor;
            this.facing = facing;
            this.yaw = yaw;
        }

        public PushupPhase phase() {
            return phase;
        }

        public int stateTicks() {
            return stateTicks;
        }

        public int repetitions() {
            return repetitions;
        }

        public int failures() {
            return 0;
        }

        public float yaw() {
            return yaw;
        }
    }

    private record StationKey(String dimension, BlockPos pos) {
    }
}
