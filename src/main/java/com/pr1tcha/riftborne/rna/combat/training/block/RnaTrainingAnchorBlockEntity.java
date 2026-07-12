package com.pr1tcha.riftborne.rna.combat.training.block;

import com.pr1tcha.riftborne.registry.ModContent;
import com.pr1tcha.riftborne.rna.combat.RnaAbilityManager;
import com.pr1tcha.riftborne.rna.combat.RnaCombatNetwork;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityData;
import com.pr1tcha.riftborne.rna.combat.registry.RnaAbilityRegistry;
import com.pr1tcha.riftborne.rna.combat.training.RnaTrainingManager;
import com.pr1tcha.riftborne.rna.combat.training.RnaTrainingPhase;
import com.pr1tcha.riftborne.rna.combat.training.RnaTrainingSession;
import com.pr1tcha.riftborne.rna.combat.training.TrainingPulseState;
import com.pr1tcha.riftborne.rna.RnaApi;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class RnaTrainingAnchorBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.rna_training_anchor.idle");
    private static final RawAnimation ACTIVE = RawAnimation.begin().thenLoop("animation.rna_training_anchor.active");
    private static final RawAnimation DEPLOY = RawAnimation.begin()
            .thenPlayAndHold("animation.rna_training_anchor.deploy");
    private static final double MAX_DISTANCE_SQR = 6.5D * 6.5D;
    private static final int CALIBRATION_TICKS = 40;
    private static final int RECOVERY_TICKS = 28;
    private static final int PAUSE_TICKS = 80;
    private static final int MIN_INPUT_LEAD = 4;
    private static final int DEPLOY_ANIM_TICKS = 20;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private UUID participantId;
    private TrainingPulseState pulseState = TrainingPulseState.IDLE;
    private int stateTicks;
    private int trialIndex;
    private int missStreak;
    private Vec3 pulseSource = Vec3.ZERO;
    private int inputLead = -1;
    private boolean inputFacing;
    private int deployAnimTicks;

    public RnaTrainingAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.RNA_TRAINING_ANCHOR_BE_TYPE.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            RnaTrainingAnchorBlockEntity anchor
    ) {
        if (level instanceof ServerLevel serverLevel) {
            anchor.tickServer(serverLevel);
        }
    }

    public void onDeployed() {
        deployAnimTicks = DEPLOY_ANIM_TICKS;
        sync();
    }

    public void startTraining(ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!getBlockState().getValue(RnaTrainingAnchorBlock.DEPLOYED)) {
            player.displayClientMessage(Component.translatable("message.riftborne.training.not_deployed"), true);
            return;
        }
        if (participantId != null) {
            if (participantId.equals(player.getUUID())) {
                player.displayClientMessage(Component.translatable("message.riftborne.training.already_active"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.riftborne.training.busy"), true);
            }
            return;
        }
        if (player.position().distanceToSqr(Vec3.atCenterOf(worldPosition)) > MAX_DISTANCE_SQR) {
            player.displayClientMessage(Component.translatable("message.riftborne.training.too_far"), true);
            return;
        }

        RnaTrainingSession existing = session(player);
        if (existing != null
                && existing.anchored()
                && existing.anchorDimension().equals(player.level().dimension().location())
                && worldPosition.equals(existing.anchorPos())
                && RnaAbilityRegistry.BARRIER_ID.equals(existing.techniqueId())) {
            participantId = player.getUUID();
            pulseState = TrainingPulseState.CALIBRATING;
            stateTicks = CALIBRATION_TICKS;
            clearInput();
            player.displayClientMessage(Component.translatable("message.riftborne.training.resumed"), true);
            sync();
            sendTrainingState(player);
            return;
        }

        RnaTrainingManager.StartResult result = RnaTrainingManager.startAt(
                player,
                RnaAbilityRegistry.BARRIER_ID,
                worldPosition
        );
        if (result != RnaTrainingManager.StartResult.STARTED) {
            player.displayClientMessage(Component.translatable(
                    "message.riftborne.training.start." + result.name().toLowerCase(java.util.Locale.ROOT)
            ), true);
            return;
        }

        participantId = player.getUUID();
        pulseState = TrainingPulseState.CALIBRATING;
        stateTicks = CALIBRATION_TICKS;
        trialIndex = 0;
        missStreak = 0;
        clearInput();
        serverLevel.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7F, 1.35F);
        player.displayClientMessage(Component.translatable("message.riftborne.training.started"), true);
        sync();
        sendTrainingState(player);
    }

    public void stopTraining(ServerPlayer requester, boolean notify) {
        ServerPlayer participant = resolveParticipant();
        if (requester != null && participantId != null && !participantId.equals(requester.getUUID())) {
            requester.displayClientMessage(Component.translatable("message.riftborne.training.busy"), true);
            return;
        }
        if (participant != null) {
            RnaTrainingManager.stop(participant);
            RnaCombatNetwork.broadcastTrainingBarrier(participant, false);
            RnaCombatNetwork.sendTrainingStateInactive(participant);
            if (notify) {
                participant.displayClientMessage(Component.translatable("message.riftborne.training.stopped"), true);
            }
        }
        deactivate();
    }

    public void acceptBarrierInput(ServerPlayer player) {
        if (!player.getUUID().equals(participantId)) {
            return;
        }
        if (pulseState != TrainingPulseState.TELEGRAPH) {
            player.displayClientMessage(Component.translatable("message.riftborne.training.wait"), true);
            return;
        }
        RnaTrainingSession session = session(player);
        if (session == null) {
            stopTraining(player, false);
            return;
        }

        inputLead = stateTicks;
        inputFacing = isFacingSource(player, session.phase().facingThreshold());
        RnaCombatNetwork.broadcastTrainingBarrier(player, true);
        if (!inputFacing) {
            player.displayClientMessage(Component.translatable("message.riftborne.training.face_source"), true);
        }
    }

    public boolean isActive() {
        return participantId != null && pulseState != TrainingPulseState.IDLE;
    }

    public boolean hasParticipant(UUID playerId) {
        return playerId != null && playerId.equals(participantId);
    }

    private void tickServer(ServerLevel serverLevel) {
        if (deployAnimTicks > 0) {
            deployAnimTicks--;
            if (deployAnimTicks == 0) {
                sync();
            }
        }
        if (participantId == null) {
            return;
        }
        ServerPlayer player = resolveParticipant();
        if (player == null) {
            return;
        }
        if (player.level() != serverLevel
                || player.position().distanceToSqr(Vec3.atCenterOf(worldPosition)) > MAX_DISTANCE_SQR) {
            player.displayClientMessage(Component.translatable("message.riftborne.training.left_area"), true);
            stopTraining(player, false);
            return;
        }
        RnaTrainingSession session = session(player);
        if (session == null || !RnaAbilityRegistry.BARRIER_ID.equals(session.techniqueId())) {
            deactivate();
            return;
        }
        if (!RnaApi.hasActiveRna(player)) {
            player.displayClientMessage(Component.translatable("message.riftborne.training.rna_lost"), true);
            stopTraining(player, false);
            return;
        }

        if (serverLevel.getGameTime() % 20L == 0L) {
            sendTrainingState(player);
        }

        if (pulseState == TrainingPulseState.TELEGRAPH) {
            renderTelegraph(serverLevel, player);
            if (stateTicks == 10) {
                serverLevel.playSound(null, pulseSource.x, pulseSource.y, pulseSource.z,
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.75F, 1.75F);
            }
        }

        if (stateTicks > 0) {
            stateTicks--;
        }
        if (stateTicks > 0) {
            return;
        }

        switch (pulseState) {
            case CALIBRATING, RECOVERY, PAUSED -> schedulePulse(player, session.phase());
            case TELEGRAPH -> resolvePulse(serverLevel, player, session.phase());
            case IDLE -> {
            }
        }
    }

    private void schedulePulse(ServerPlayer player, RnaTrainingPhase phase) {
        pulseState = TrainingPulseState.TELEGRAPH;
        stateTicks = phase.telegraphTicks();
        trialIndex++;
        clearInput();

        double offsetDegrees = switch (phase) {
            case FORMATION -> 0.0D;
            case ALIGNMENT -> switch (trialIndex % 4) {
                case 1 -> -55.0D;
                case 2 -> 55.0D;
                case 3 -> -30.0D;
                default -> 30.0D;
            };
            case STABILIZATION -> switch (trialIndex % 5) {
                case 1 -> -78.0D;
                case 2 -> 62.0D;
                case 3 -> -38.0D;
                case 4 -> 80.0D;
                default -> 0.0D;
            };
        };
        Vec3 forward = Vec3.directionFromRotation(0.0F, player.getYRot());
        double angle = Math.atan2(forward.z, forward.x) + Math.toRadians(offsetDegrees);
        Vec3 target = player.position().add(0.0D, player.getBbHeight() * 0.58D, 0.0D);
        pulseSource = target.add(Math.cos(angle) * 3.4D, 0.2D, Math.sin(angle) * 3.4D);
        serverLevel().playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.BLOCKS, 0.45F, 1.55F);
        sync();
        sendTrainingState(player);
    }

    private void resolvePulse(ServerLevel level, ServerPlayer player, RnaTrainingPhase phase) {
        int maxLead = switch (phase) {
            case FORMATION -> 30;
            case ALIGNMENT -> 21;
            case STABILIZATION -> 14;
        };
        boolean success = inputFacing && inputLead >= MIN_INPUT_LEAD && inputLead <= maxLead;
        RnaCombatNetwork.broadcastTrainingBarrier(player, false);

        if (success) {
            missStreak = 0;
            RnaTrainingManager.TrialResult result = RnaTrainingManager.recordSuccess(player);
            successEffects(level, player);
            RnaTrainingSession updated = session(player);
            if (result == RnaTrainingManager.TrialResult.SESSION_COMPLETED) {
                player.displayClientMessage(Component.translatable("message.riftborne.training.completed"), true);
                RnaCombatNetwork.sendTrainingStateInactive(player);
                deactivate();
                return;
            }
            if (result == RnaTrainingManager.TrialResult.PATTERN_COMPLETED || updated == null) {
                player.displayClientMessage(Component.translatable("message.riftborne.training.pattern_completed"), true);
                RnaCombatNetwork.sendTrainingStateInactive(player);
                deactivate();
                return;
            }
            if (result == RnaTrainingManager.TrialResult.PHASE_COMPLETED) {
                player.displayClientMessage(Component.translatable(
                        "message.riftborne.training.phase_completed",
                        Component.translatable(updated.phase().translationKey())
                ), true);
            } else {
                player.displayClientMessage(Component.translatable(
                        "message.riftborne.training.success",
                        updated.phaseSuccesses(),
                        updated.phase().requiredSuccesses()
                ), true);
            }
            pulseState = TrainingPulseState.RECOVERY;
            stateTicks = RECOVERY_TICKS;
        } else {
            missStreak++;
            RnaTrainingManager.recordFailure(player);
            failureEffects(level, player);
            String failureKey = inputLead < 0
                    ? "message.riftborne.training.missed"
                    : !inputFacing
                            ? "message.riftborne.training.face_source"
                            : inputLead > maxLead
                                    ? "message.riftborne.training.too_early"
                                    : "message.riftborne.training.too_late";
            player.displayClientMessage(Component.translatable(failureKey), true);
            pulseState = missStreak >= 3 ? TrainingPulseState.PAUSED : TrainingPulseState.RECOVERY;
            stateTicks = missStreak >= 3 ? PAUSE_TICKS : RECOVERY_TICKS;
            if (missStreak >= 3) {
                missStreak = 0;
                player.displayClientMessage(Component.translatable("message.riftborne.training.overload_pause"), true);
            }
        }
        clearInput();
        sync();
        sendTrainingState(player);
    }

    private void renderTelegraph(ServerLevel level, ServerPlayer player) {
        Vec3 target = player.position().add(0.0D, player.getBbHeight() * 0.58D, 0.0D);
        Vec3 delta = target.subtract(pulseSource);
        int samples = 12;
        for (int i = 0; i <= samples; i++) {
            Vec3 point = pulseSource.add(delta.scale(i / (double) samples));
            if ((i + level.getGameTime()) % 3 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z,
                        1, 0.015D, 0.015D, 0.015D, 0.0D);
            }
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                pulseSource.x, pulseSource.y, pulseSource.z, 2,
                0.10D, 0.10D, 0.10D, 0.01D);
    }

    private void successEffects(ServerLevel level, ServerPlayer player) {
        Vec3 target = player.position().add(0.0D, player.getBbHeight() * 0.58D, 0.0D);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.x, target.y, target.z,
                22, 0.65D, 0.75D, 0.65D, 0.06D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 0.9F, 1.35F);
    }

    private void failureEffects(ServerLevel level, ServerPlayer player) {
        Vec3 target = player.position().add(0.0D, player.getBbHeight() * 0.58D, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE, target.x, target.y, target.z,
                12, 0.35D, 0.45D, 0.35D, 0.025D);
        level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.PLAYERS, 0.55F, 1.6F);
    }

    private boolean isFacingSource(ServerPlayer player, double threshold) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        Vec3 toSource = pulseSource.subtract(player.position());
        Vec3 horizontalSource = new Vec3(toSource.x, 0.0D, toSource.z);
        return horizontalLook.lengthSqr() > 0.001D
                && horizontalSource.lengthSqr() > 0.001D
                && horizontalLook.normalize().dot(horizontalSource.normalize()) >= threshold;
    }

    private RnaTrainingSession session(ServerPlayer player) {
        RnaAbilityData data = RnaAbilityManager.getData(player);
        return data.trainingSession();
    }

    private ServerPlayer resolveParticipant() {
        if (participantId == null || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(participantId);
    }

    private ServerLevel serverLevel() {
        return (ServerLevel) level;
    }

    private void clearInput() {
        inputLead = -1;
        inputFacing = false;
    }

    private void deactivate() {
        participantId = null;
        pulseState = TrainingPulseState.IDLE;
        stateTicks = 0;
        pulseSource = Vec3.ZERO;
        clearInput();
        sync();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.BLOCKS, 0.55F, 1.4F);
        }
    }

    private void sendTrainingState(ServerPlayer player) {
        RnaTrainingSession session = session(player);
        if (session != null) {
            RnaCombatNetwork.sendTrainingState(
                    player,
                    worldPosition,
                    session.phase(),
                    session.phaseSuccesses(),
                    session.totalFailures(),
                    pulseState,
                    stateTicks
            );
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (participantId != null) {
            tag.putUUID("Participant", participantId);
        }
        tag.putInt("PulseState", pulseState.ordinal());
        tag.putInt("StateTicks", stateTicks);
        tag.putInt("TrialIndex", trialIndex);
        tag.putInt("MissStreak", missStreak);
        tag.putDouble("PulseX", pulseSource.x);
        tag.putDouble("PulseY", pulseSource.y);
        tag.putDouble("PulseZ", pulseSource.z);
        tag.putInt("DeployAnimTicks", deployAnimTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        participantId = tag.hasUUID("Participant") ? tag.getUUID("Participant") : null;
        pulseState = TrainingPulseState.fromOrdinal(tag.getInt("PulseState"));
        stateTicks = Math.max(0, tag.getInt("StateTicks"));
        trialIndex = Math.max(0, tag.getInt("TrialIndex"));
        missStreak = Math.max(0, tag.getInt("MissStreak"));
        pulseSource = new Vec3(tag.getDouble("PulseX"), tag.getDouble("PulseY"), tag.getDouble("PulseZ"));
        deployAnimTicks = Math.max(0, tag.getInt("DeployAnimTicks"));
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "training_state",
                4,
                state -> {
                    if (deployAnimTicks > 0) {
                        return state.setAndContinue(DEPLOY);
                    }
                    return state.setAndContinue(isActive() ? ACTIVE : IDLE);
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
