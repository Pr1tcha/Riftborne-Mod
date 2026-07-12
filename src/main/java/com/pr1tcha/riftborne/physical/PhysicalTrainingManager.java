package com.pr1tcha.riftborne.physical;

import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import com.pr1tcha.riftborne.registry.ModContent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.pr1tcha.riftborne.physical.pushup.PushupTrainingManager;

public final class PhysicalTrainingManager {
    private static final float AMBIENT_PROGRESS_MULTIPLIER = 1.0F;
    private static final float ENDURANCE_SPRINT_PER_BLOCK = 100.0F / 500.0F;
    private static final float ENDURANCE_SWIM_PER_BLOCK = 100.0F / 300.0F;
    private static final float STRENGTH_CARRY_PER_BLOCK = 100.0F / 250.0F;
    private static final float MOTORICS_PER_JUMP = 100.0F / 12.0F;
    private static final float TREADMILL_PER_TICK = 100.0F / (45.0F * 20.0F);
    private static final float STABILITY_PER_TICK = 100.0F / (40.0F * 20.0F);
    private static final float STRENGTH_PER_REP = 100.0F / 15.0F;
    private static final double MAX_SAMPLE_DISTANCE = 2.5D;
    private static final double STATION_DISTANCE_SQR = 5.0D * 5.0D;
    private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();

    private PhysicalTrainingManager() {
    }

    public static void cancelStationSession(ServerPlayer player, boolean notify) {
        RuntimeState runtime = RUNTIME.get(player.getUUID());
        if (runtime == null || runtime.session == null) {
            return;
        }
        runtime.session = null;
        if (notify) {
            player.displayClientMessage(Component.translatable("message.riftborne.physical.session_stopped"), true);
        }
    }

    public static void onLogin(ServerPlayer player) {
        PhysicalTrainingData data = getData(player);
        data.beginDay(currentDay(player), false);
        save(player, data);
        RuntimeState runtime = new RuntimeState(player.position(), player.onGround(), player.isCrouching());
        RUNTIME.put(player.getUUID(), runtime);
        player.displayClientMessage(Component.translatable(
                "message.riftborne.physical.summary",
                format(data.overallForm()),
                format(overloadCapacity(data))
        ), false);
    }

    public static void onLogout(ServerPlayer player) {
        RUNTIME.remove(player.getUUID());
    }

    public static void tick(ServerPlayer player) {
        RuntimeState runtime = RUNTIME.computeIfAbsent(
                player.getUUID(),
                ignored -> new RuntimeState(player.position(), player.onGround(), player.isCrouching())
        );
        PhysicalTrainingData data = getData(player);
        boolean changed = false;

        long day = currentDay(player);
        if (data.activeDay() != day) {
            data.beginDay(day, true);
            player.displayClientMessage(Component.translatable("message.riftborne.physical.new_day"), false);
            changed = true;
        }

        changed |= trackJump(player, data, runtime);
        changed |= tickStationSession(player, data, runtime);

        if (player.tickCount % 5 == 0) {
            changed |= trackMovement(player, data, runtime);
            runtime.lastPosition = player.position();
        }
        runtime.wasOnGround = player.onGround();
        runtime.wasCrouching = player.isCrouching();

        if (changed) {
            save(player, data);
        }
    }

    public static PhysicalTrainingData getData(ServerPlayer player) {
        return RiftbornePlayerData.getPhysicalTraining(player);
    }

    public static float overallForm(ServerPlayer player) {
        return getData(player).overallForm();
    }

    public static float overloadCapacity(ServerPlayer player) {
        return overloadCapacity(getData(player));
    }

    public static float overloadCapacity(PhysicalTrainingData data) {
        return 75.0F + data.overallForm() * 0.5F;
    }

    public static float overloadCapacityMultiplier(ServerPlayer player) {
        return overloadCapacity(player) / 100.0F;
    }

    public static float recordExerciseProgress(
            ServerPlayer player,
            PhysicalStat stat,
            float amount,
            boolean notify
    ) {
        PhysicalTrainingData data = getData(player);
        data.beginDay(currentDay(player), false);
        if (addProgress(player, data, stat, amount, notify)) {
            save(player, data);
        }
        return data.dailyProgress(stat);
    }

    public static void startStation(ServerPlayer player, PhysicalStat stat, BlockPos stationPos) {
        if (PushupTrainingManager.isTraining(player)) {
            player.displayClientMessage(Component.translatable("message.riftborne.pushup.already_training"), true);
            return;
        }
        PhysicalTrainingData data = getData(player);
        data.beginDay(currentDay(player), false);
        if (data.dailyProgress(stat) >= PhysicalTrainingData.COMPLETE_THRESHOLD) {
            player.displayClientMessage(Component.translatable(
                    "message.riftborne.physical.already_complete",
                    Component.translatable(stat.titleKey())
            ), true);
            return;
        }

        RuntimeState runtime = RUNTIME.computeIfAbsent(
                player.getUUID(),
                ignored -> new RuntimeState(player.position(), player.onGround(), player.isCrouching())
        );
        if (runtime.session != null
                && runtime.session.stat == stat
                && runtime.session.stationPos.equals(stationPos)) {
            runtime.session = null;
            player.displayClientMessage(Component.translatable("message.riftborne.physical.session_stopped"), true);
            return;
        }

        runtime.session = new StationSession(stat, stationPos.immutable(), player.serverLevel().getGameTime());
        player.displayClientMessage(Component.translatable(
                "message.riftborne.physical.session_started." + stat.id()
        ), true);
        player.serverLevel().playSound(null, stationPos, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 0.55F, 1.25F);
    }

    public static void cancelSessionsAt(ServerLevel level, BlockPos stationPos) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            RuntimeState runtime = RUNTIME.get(player.getUUID());
            if (runtime != null && runtime.session != null && runtime.session.stationPos.equals(stationPos)) {
                runtime.session = null;
                player.displayClientMessage(Component.translatable("message.riftborne.physical.session_stopped"), true);
            }
        }
    }

    private static boolean trackMovement(ServerPlayer player, PhysicalTrainingData data, RuntimeState runtime) {
        if (AMBIENT_PROGRESS_MULTIPLIER <= 0.0F) {
            return false;
        }
        Vec3 current = player.position();
        Vec3 delta = current.subtract(runtime.lastPosition);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontal <= 0.01D || horizontal > MAX_SAMPLE_DISTANCE || player.isPassenger()) {
            return false;
        }

        boolean changed = false;
        if (player.isSwimming()) {
            changed |= addProgress(player, data, PhysicalStat.ENDURANCE,
                    (float) horizontal * ENDURANCE_SWIM_PER_BLOCK * AMBIENT_PROGRESS_MULTIPLIER, false);
        } else if (player.isSprinting() && player.onGround()) {
            changed |= addProgress(player, data, PhysicalStat.ENDURANCE,
                    (float) horizontal * ENDURANCE_SPRINT_PER_BLOCK * AMBIENT_PROGRESS_MULTIPLIER, false);
        }

        if (player.getMainHandItem().is(ModContent.TRAINING_WEIGHT.get())
                || player.getOffhandItem().is(ModContent.TRAINING_WEIGHT.get())) {
            changed |= addProgress(player, data, PhysicalStat.STRENGTH,
                    (float) horizontal * STRENGTH_CARRY_PER_BLOCK * AMBIENT_PROGRESS_MULTIPLIER, false);
        }
        return changed;
    }

    private static boolean trackJump(ServerPlayer player, PhysicalTrainingData data, RuntimeState runtime) {
        if (runtime.session == null || runtime.session.stat != PhysicalStat.MOTORICS) {
            runtime.jumpStart = null;
            return false;
        }
        boolean onGround = player.onGround();
        if (runtime.wasOnGround && !onGround && player.getDeltaMovement().y > 0.08D) {
            runtime.jumpStart = player.position();
            return false;
        }
        if (!runtime.wasOnGround && onGround && runtime.jumpStart != null) {
            Vec3 landing = player.position();
            Vec3 jumpDelta = landing.subtract(runtime.jumpStart);
            runtime.jumpStart = null;
            double horizontal = Math.sqrt(jumpDelta.x * jumpDelta.x + jumpDelta.z * jumpDelta.z);
            if (horizontal < 1.25D) {
                return false;
            }
            if (runtime.lastValidLanding != null && runtime.lastValidLanding.distanceToSqr(landing) < 1.0D) {
                return false;
            }
            runtime.lastValidLanding = landing;
            boolean changed = addProgress(player, data, PhysicalStat.MOTORICS, MOTORICS_PER_JUMP, true);
            player.serverLevel().sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    landing.x, landing.y + 0.15D, landing.z,
                    8, 0.25D, 0.05D, 0.25D, 0.02D
            );
            return changed;
        }
        return false;
    }

    private static boolean tickStationSession(
            ServerPlayer player,
            PhysicalTrainingData data,
            RuntimeState runtime
    ) {
        StationSession session = runtime.session;
        if (session == null) {
            return false;
        }
        if (!player.serverLevel().getBlockState(session.stationPos).getBlock().equals(
                com.pr1tcha.riftborne.physical.block.PhysicalTrainingStationBlock.blockFor(session.stat)
        ) || player.position().distanceToSqr(Vec3.atCenterOf(session.stationPos)) > STATION_DISTANCE_SQR) {
            runtime.session = null;
            player.displayClientMessage(Component.translatable("message.riftborne.physical.session_interrupted"), true);
            return false;
        }

        boolean changed = false;
        long gameTime = player.serverLevel().getGameTime();
        switch (session.stat) {
            case ENDURANCE -> {
                if (player.isSprinting() && player.onGround() && isFacingStation(player, session.stationPos, 0.55D)) {
                    changed = addProgress(player, data, PhysicalStat.ENDURANCE, TREADMILL_PER_TICK, false);
                    if (gameTime % 20L == 0L) {
                        player.serverLevel().sendParticles(ParticleTypes.CLOUD,
                                player.getX(), player.getY() + 0.1D, player.getZ(),
                                2, 0.18D, 0.02D, 0.18D, 0.01D);
                    }
                }
            }
            case STRENGTH -> {
                if (!runtime.wasCrouching && player.isCrouching()) {
                    long interval = gameTime - session.lastActionTick;
                    if (interval >= 16L && interval <= 50L) {
                        session.lastActionTick = gameTime;
                        session.repetitions++;
                        changed = addProgress(player, data, PhysicalStat.STRENGTH, STRENGTH_PER_REP, true);
                        player.serverLevel().playSound(null, session.stationPos,
                                SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.35F,
                                0.8F + Math.min(0.35F, session.repetitions * 0.015F));
                    } else if (interval > 50L) {
                        session.lastActionTick = gameTime;
                        player.displayClientMessage(Component.translatable("message.riftborne.physical.find_rhythm"), true);
                    }
                }
            }
            case MOTORICS -> {
                if (gameTime % 40L == 0L) {
                    player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            session.stationPos.getX() + 0.5D,
                            session.stationPos.getY() + 1.15D,
                            session.stationPos.getZ() + 0.5D,
                            5, 0.45D, 0.15D, 0.45D, 0.02D);
                }
            }
            case STABILITY -> {
                if (player.isCrouching() && player.onGround() && isFacingStation(player, session.stationPos, 0.92D)) {
                    changed = addProgress(player, data, PhysicalStat.STABILITY, STABILITY_PER_TICK, false);
                    if (gameTime % 40L == 0L) {
                        player.serverLevel().playSound(null, session.stationPos,
                                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.35F, 1.4F);
                    }
                }
            }
        }

        if (data.dailyProgress(session.stat) >= PhysicalTrainingData.COMPLETE_THRESHOLD) {
            runtime.session = null;
            player.displayClientMessage(Component.translatable(
                    "message.riftborne.physical.daily_complete",
                    Component.translatable(session.stat.titleKey())
            ), false);
            player.serverLevel().playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.45F, 1.55F);
        }
        return changed;
    }

    private static boolean addProgress(
            ServerPlayer player,
            PhysicalTrainingData data,
            PhysicalStat stat,
            float amount,
            boolean alwaysNotify
    ) {
        float before = data.dailyProgress(stat);
        float after = data.addProgress(stat, amount);
        if (after <= before) {
            return false;
        }
        int beforeStep = Mth.floor(before / 10.0F);
        int afterStep = Mth.floor(after / 10.0F);
        if (alwaysNotify || afterStep > beforeStep || (before < 60.0F && after >= 60.0F)) {
            player.displayClientMessage(Component.translatable(
                    "message.riftborne.physical.progress",
                    Component.translatable(stat.titleKey()),
                    Mth.floor(after),
                    format(data.form(stat))
            ), true);
        }
        return true;
    }

    private static boolean isFacingStation(ServerPlayer player, BlockPos stationPos, double threshold) {
        Vec3 look = player.getLookAngle();
        Vec3 toStation = Vec3.atCenterOf(stationPos).subtract(player.getEyePosition());
        return toStation.lengthSqr() > 0.001D && look.normalize().dot(toStation.normalize()) >= threshold;
    }

    private static long currentDay(ServerPlayer player) {
        return Math.floorDiv(player.getServer().overworld().getDayTime(), 24000L);
    }

    private static String format(float value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static void save(ServerPlayer player, PhysicalTrainingData data) {
        RiftbornePlayerData.savePhysicalTraining(player, data);
    }

    private static final class RuntimeState {
        private Vec3 lastPosition;
        private boolean wasOnGround;
        private boolean wasCrouching;
        private Vec3 jumpStart;
        private Vec3 lastValidLanding;
        private StationSession session;

        private RuntimeState(Vec3 lastPosition, boolean wasOnGround, boolean wasCrouching) {
            this.lastPosition = lastPosition;
            this.wasOnGround = wasOnGround;
            this.wasCrouching = wasCrouching;
        }
    }

    private static final class StationSession {
        private final PhysicalStat stat;
        private final BlockPos stationPos;
        private long lastActionTick;
        private int repetitions;

        private StationSession(PhysicalStat stat, BlockPos stationPos, long startTick) {
            this.stat = stat;
            this.stationPos = stationPos;
            this.lastActionTick = startTick - 20L;
        }
    }
}
