package com.pr1tcha.riftborne.rna.combat.training;

import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import com.pr1tcha.riftborne.rna.RnaApi;
import com.pr1tcha.riftborne.rna.combat.RnaAbilityManager;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityData;
import com.pr1tcha.riftborne.rna.combat.progression.RnaAcquisitionMethod;
import com.pr1tcha.riftborne.rna.combat.progression.RnaTechniqueRegistry;
import com.pr1tcha.riftborne.rna.combat.progression.RnaTechniqueProgress;
import com.pr1tcha.riftborne.rna.combat.training.block.RnaTrainingAnchorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Server-side training state machine. A future arena or anomaly can report real trial results
 * through this boundary without owning progression data.
 */
public final class RnaTrainingManager {
    public static final float FAILURE_LOAD = 5.0F;

    private RnaTrainingManager() {
    }

    public static StartResult start(ServerPlayer player, ResourceLocation techniqueId) {
        return start(player, techniqueId, null, null);
    }

    public static StartResult startAt(ServerPlayer player, ResourceLocation techniqueId, BlockPos anchorPos) {
        return start(player, techniqueId, player.level().dimension().location(), anchorPos);
    }

    private static StartResult start(
            ServerPlayer player,
            ResourceLocation techniqueId,
            ResourceLocation anchorDimension,
            BlockPos anchorPos
    ) {
        if (!RnaApi.hasActiveRna(player)) {
            return StartResult.NO_ACTIVE_RNA;
        }
        if (RnaTechniqueRegistry.get(techniqueId) == null) {
            return StartResult.UNKNOWN_TECHNIQUE;
        }
        RnaAbilityData data = RnaAbilityManager.getData(player);
        if (data.isUnlocked(techniqueId.toString())) {
            return StartResult.ALREADY_STABILIZED;
        }
        if (data.trainingSession() != null) {
            RnaTrainingSession existing = data.trainingSession();
            if (existing.anchored() && !anchorStillExists(player, existing)) {
                data.clearTrainingSession();
                save(player, data);
            } else {
                return StartResult.ALREADY_TRAINING;
            }
        }

        RnaTechniqueProgress progress = data.techniqueProgress(techniqueId.toString());
        int pattern = progress == null ? 0 : progress.patternProgress();
        RnaTrainingPhase initialPhase = pattern >= RnaTechniqueProgress.PROVISIONAL_THRESHOLD
                ? RnaTrainingPhase.STABILIZATION
                : pattern >= RnaTechniqueProgress.DISCOVERY_THRESHOLD
                        ? RnaTrainingPhase.ALIGNMENT
                        : RnaTrainingPhase.FORMATION;
        data.setTrainingSession(new RnaTrainingSession(
                techniqueId,
                player.serverLevel().getGameTime(),
                initialPhase,
                anchorDimension,
                anchorPos
        ));
        save(player, data);
        return StartResult.STARTED;
    }

    public static boolean handleBarrierInput(ServerPlayer player) {
        RnaTrainingSession session = RnaAbilityManager.getData(player).trainingSession();
        if (session == null || !session.anchored()) {
            return false;
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, session.anchorDimension());
        if (player.getServer() == null || player.getServer().getLevel(dimension) == null) {
            return true;
        }
        if (!(player.getServer().getLevel(dimension).getBlockEntity(session.anchorPos())
                instanceof RnaTrainingAnchorBlockEntity anchor)) {
            stop(player);
            return true;
        }
        anchor.acceptBarrierInput(player);
        return true;
    }

    private static boolean anchorStillExists(ServerPlayer player, RnaTrainingSession session) {
        if (player.getServer() == null || !session.anchored()) {
            return false;
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, session.anchorDimension());
        return player.getServer().getLevel(dimension) != null
                && player.getServer().getLevel(dimension).getBlockEntity(session.anchorPos())
                instanceof RnaTrainingAnchorBlockEntity;
    }

    public static TrialResult recordSuccess(ServerPlayer player) {
        RnaAbilityData data = RnaAbilityManager.getData(player);
        RnaTrainingSession session = data.trainingSession();
        if (session == null) {
            return TrialResult.NO_SESSION;
        }

        RnaTrainingSession.SuccessResult result = session.recordSuccess(player.serverLevel().getGameTime());
        ResourceLocation techniqueId = session.techniqueId();
        if (result.sessionCompleted()) {
            data.clearTrainingSession();
        }
        save(player, data);
        RnaAbilityManager.addTechniquePatternProgress(
                player,
                techniqueId,
                RnaAcquisitionMethod.TRAINING,
                result.patternGain()
        );
        RnaAbilityData refreshed = RnaAbilityManager.getData(player);
        boolean stabilized = refreshed.isUnlocked(techniqueId.toString());
        if (stabilized && refreshed.trainingSession() != null) {
            refreshed.clearTrainingSession();
            save(player, refreshed);
        }
        if (stabilized) {
            return TrialResult.SESSION_COMPLETED;
        }
        if (result.sessionCompleted()) {
            return TrialResult.PATTERN_COMPLETED;
        }
        return result.phaseCompleted() ? TrialResult.PHASE_COMPLETED : TrialResult.SUCCESS;
    }

    public static TrialResult recordFailure(ServerPlayer player) {
        RnaAbilityData data = RnaAbilityManager.getData(player);
        RnaTrainingSession session = data.trainingSession();
        if (session == null) {
            return TrialResult.NO_SESSION;
        }
        session.recordFailure(player.serverLevel().getGameTime());
        save(player, data);
        RnaAbilityManager.addCombatLoad(player, FAILURE_LOAD);
        return TrialResult.FAILURE;
    }

    public static boolean stop(ServerPlayer player) {
        RnaAbilityData data = RnaAbilityManager.getData(player);
        if (data.trainingSession() == null) {
            return false;
        }
        data.clearTrainingSession();
        save(player, data);
        return true;
    }

    private static void save(ServerPlayer player, RnaAbilityData data) {
        RiftbornePlayerData.saveRnaCombat(player, data);
    }

    public enum StartResult {
        STARTED,
        STABILIZED,
        NO_ACTIVE_RNA,
        UNKNOWN_TECHNIQUE,
        ALREADY_STABILIZED,
        ALREADY_TRAINING
    }

    public enum TrialResult {
        SUCCESS,
        PHASE_COMPLETED,
        PATTERN_COMPLETED,
        SESSION_COMPLETED,
        FAILURE,
        NO_SESSION
    }
}
