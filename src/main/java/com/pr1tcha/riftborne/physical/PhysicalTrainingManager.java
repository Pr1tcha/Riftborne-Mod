package com.pr1tcha.riftborne.physical;

import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import com.pr1tcha.riftborne.registry.ModContent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class PhysicalTrainingManager {
    private static final float AMBIENT_PROGRESS_MULTIPLIER = 1.0F;
    private static final float ENDURANCE_SPRINT_PER_BLOCK = 100.0F / 500.0F;
    private static final float ENDURANCE_SWIM_PER_BLOCK = 100.0F / 300.0F;
    private static final float STRENGTH_CARRY_PER_BLOCK = 100.0F / 250.0F;
    private static final double MAX_SAMPLE_DISTANCE = 2.5D;
    private static final Map<UUID, RuntimeState> RUNTIME = new HashMap<>();

    private PhysicalTrainingManager() {
    }

    public static void onLogin(ServerPlayer player) {
        PhysicalTrainingData data = getData(player);
        data.beginDay(currentDay(player), false);
        save(player, data);
        RUNTIME.put(player.getUUID(), new RuntimeState(player.position()));
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
                ignored -> new RuntimeState(player.position())
        );
        PhysicalTrainingData data = getData(player);
        boolean changed = false;

        long day = currentDay(player);
        if (data.activeDay() != day) {
            data.beginDay(day, true);
            player.displayClientMessage(Component.translatable("message.riftborne.physical.new_day"), false);
            changed = true;
        }

        if (player.tickCount % 5 == 0) {
            changed |= trackMovement(player, data, runtime);
            runtime.lastPosition = player.position();
        }

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

        private RuntimeState(Vec3 lastPosition) {
            this.lastPosition = lastPosition;
        }
    }
}
