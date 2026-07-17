package com.pr1tcha.riftborne.rna.combat;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.rna.combat.barrier.BarrierGestureMode;
import com.pr1tcha.riftborne.rna.combat.barrier.BarrierPhase;
import com.pr1tcha.riftborne.rna.combat.client.BarrierClientState;
import com.pr1tcha.riftborne.rna.combat.client.RnaCombatClient;
import com.pr1tcha.riftborne.rna.combat.client.VeilBarrierField;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityData;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityResult;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityUseContext;
import com.pr1tcha.riftborne.rna.combat.data.RnaLoadBand;
import com.pr1tcha.riftborne.rna.combat.registry.RnaAbilityRegistry;
import com.pr1tcha.riftborne.rna.combat.training.RnaTrainingManager;
import com.pr1tcha.riftborne.rna.combat.training.RnaTrainingPhase;
import com.pr1tcha.riftborne.rna.combat.training.TrainingPulseState;
import com.pr1tcha.riftborne.rna.combat.training.block.RnaTrainingAnchorBlockEntity;
import com.pr1tcha.riftborne.rna.combat.training.client.RnaTrainingAnchorClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import java.util.UUID;

public final class RnaCombatNetwork {
    private static final String NETWORK_VERSION = "6";

    private RnaCombatNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(ActivateSkillPayload.TYPE, ActivateSkillPayload.STREAM_CODEC, RnaCombatNetwork::handleActivateSkill);
        registrar.playToClient(CombatSyncPayload.TYPE, CombatSyncPayload.STREAM_CODEC, RnaCombatNetwork::handleCombatSync);
        registrar.playToClient(BarrierStatePayload.TYPE, BarrierStatePayload.STREAM_CODEC, RnaCombatNetwork::handleBarrierState);
        registrar.playToClient(TrainingStatePayload.TYPE, TrainingStatePayload.STREAM_CODEC, RnaCombatNetwork::handleTrainingState);
        registrar.playToClient(AnchorMenuPayload.TYPE, AnchorMenuPayload.STREAM_CODEC, RnaCombatNetwork::handleAnchorMenu);
        registrar.playToServer(AnchorMenuSelectPayload.TYPE, AnchorMenuSelectPayload.STREAM_CODEC, RnaCombatNetwork::handleAnchorMenuSelect);
    }

    public static void sendAnchorMenu(ServerPlayer player, BlockPos anchorPos, boolean hasActiveRna) {
        PacketDistributor.sendToPlayer(player, new AnchorMenuPayload(anchorPos.asLong(), hasActiveRna));
    }

    public static void sendSync(ServerPlayer player, ResourceLocation abilityId, RnaAbilityResult result) {
        RnaAbilityData data = RnaAbilityManager.getData(player);
        RnaLoadBand band = RnaAbilityManager.loadBand(data);
        int cooldown = abilityId == null ? 0 : (int) Math.min(Integer.MAX_VALUE, RnaAbilityManager.remainingCooldown(player, abilityId));
        PacketDistributor.sendToPlayer(player, new CombatSyncPayload(
                data.currentLoad(),
                RnaAbilityManager.overloadCapacity(player),
                data.activeStrength(RnaAbilityRegistry.BARRIER_ID.toString()),
                band.ordinal(),
                cooldown,
                abilityId == null ? "" : abilityId.toString(),
                result == null ? "" : result.name()
        ));
    }

    public static void sendBarrierState(
            ServerPlayer recipient,
            ServerPlayer owner,
            float integrity,
            BarrierPhase phase,
            BarrierGestureMode gesture,
            int phaseTicks
    ) {
        PacketDistributor.sendToPlayer(
                recipient,
                new BarrierStatePayload(
                        owner.getUUID(),
                        Math.max(0.0F, integrity),
                        phase.ordinal(),
                        gesture.ordinal(),
                        Math.max(0, phaseTicks)
                )
        );
    }

    public static void broadcastBarrierState(ServerPlayer owner, float integrity) {
        BarrierPhase phase = integrity > 0.0F ? BarrierPhase.ACTIVE : BarrierPhase.INACTIVE;
        broadcastBarrierState(owner, integrity, phase, RnaAbilityManager.barrierGestureMode(owner), 0);
    }

    public static void broadcastBarrierDeployment(ServerPlayer owner, int deploymentTicks) {
        broadcastBarrierState(
                owner,
                0.0F,
                BarrierPhase.DEPLOYING,
                RnaAbilityManager.barrierGestureMode(owner),
                deploymentTicks
        );
    }

    public static void broadcastTrainingBarrier(ServerPlayer owner, boolean visible) {
        broadcastBarrierState(
                owner,
                visible ? 6.0F : 0.0F,
                visible ? BarrierPhase.ACTIVE : BarrierPhase.INACTIVE,
                RnaAbilityManager.barrierGestureMode(owner),
                0
        );
    }

    public static void sendTrainingState(
            ServerPlayer player,
            BlockPos anchorPos,
            RnaTrainingPhase phase,
            int phaseSuccesses,
            int totalFailures,
            TrainingPulseState pulseState,
            int stateTicks
    ) {
        PacketDistributor.sendToPlayer(player, new TrainingStatePayload(
                true,
                anchorPos.asLong(),
                phase.ordinal(),
                Math.max(0, phaseSuccesses),
                phase.requiredSuccesses(),
                Math.max(0, totalFailures),
                pulseState.ordinal(),
                Math.max(0, stateTicks)
        ));
    }

    public static void sendTrainingStateInactive(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new TrainingStatePayload(
                false, 0L, 0, 0, 0, 0, TrainingPulseState.IDLE.ordinal(), 0
        ));
    }

    private static void broadcastBarrierState(
            ServerPlayer owner,
            float integrity,
            BarrierPhase phase,
            BarrierGestureMode gesture,
            int phaseTicks
    ) {
        if (owner.getServer() == null) {
            return;
        }
        for (ServerPlayer recipient : owner.getServer().getPlayerList().getPlayers()) {
            sendBarrierState(recipient, owner, integrity, phase, gesture, phaseTicks);
        }
    }

    private static void handleActivateSkill(ActivateSkillPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        ResourceLocation abilityId = ResourceLocation.tryParse(payload.abilityId());
        RnaAbilityResult result;
        if (abilityId == null || RnaAbilityRegistry.get(abilityId) == null) {
            result = RnaAbilityResult.FAIL_UNKNOWN_ABILITY;
            abilityId = null;
        } else if (RnaAbilityRegistry.BARRIER_ID.equals(abilityId)
                && RnaTrainingManager.handleBarrierInput(player)) {
            return;
        } else {
            result = RnaAbilityManager.activateBasicSkill(
                    player,
                    abilityId,
                    RnaAbilityUseContext.action(
                            player,
                            null,
                            player.blockPosition(),
                            "hotkey",
                            abilityId.getPath()
                    )
            );
        }
        sendSync(player, abilityId, result);
    }

    private static void handleCombatSync(CombatSyncPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RnaCombatClient.handleSync(payload);
        }
    }

    private static void handleBarrierState(BarrierStatePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            BarrierClientState.handleState(payload);
            if (ModList.get().isLoaded("veil")) {
                float integrity = BarrierPhase.fromOrdinal(payload.phaseOrdinal()) == BarrierPhase.ACTIVE
                        ? payload.integrity()
                        : 0.0F;
                VeilBarrierField.handleState(payload.playerId(), integrity);
            }
        }
    }

    private static void handleTrainingState(TrainingStatePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RnaCombatClient.handleTrainingSync(payload);
        }
    }

    private static void handleAnchorMenu(AnchorMenuPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RnaTrainingAnchorClient.openMenu(payload);
        }
    }

    private static void handleAnchorMenuSelect(AnchorMenuSelectPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos pos = BlockPos.of(payload.anchorPos());
        if (!(player.level().getBlockEntity(pos) instanceof RnaTrainingAnchorBlockEntity anchor)) {
            return;
        }
        switch (payload.option()) {
            case AnchorMenuSelectPayload.OPTION_BARRIER_TRAINING -> anchor.startTraining(player);
            case AnchorMenuSelectPayload.OPTION_FORMATION ->
                    player.displayClientMessage(Component.translatable("message.riftborne.training.formation_soon"), true);
            default -> {
            }
        }
    }

    public record ActivateSkillPayload(String abilityId) implements CustomPacketPayload {
        public static final Type<ActivateSkillPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_combat_activate")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, ActivateSkillPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> buffer.writeUtf(payload.abilityId()),
                buffer -> new ActivateSkillPayload(buffer.readUtf())
        );

        @Override
        public Type<ActivateSkillPayload> type() {
            return TYPE;
        }
    }

    public record CombatSyncPayload(
            float load,
            float overloadCapacity,
            float barrierIntegrity,
            int bandOrdinal,
            int cooldownTicks,
            String abilityId,
            String result
    ) implements CustomPacketPayload {
        public static final Type<CombatSyncPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_combat_sync")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, CombatSyncPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeFloat(payload.load());
                    buffer.writeFloat(payload.overloadCapacity());
                    buffer.writeFloat(payload.barrierIntegrity());
                    buffer.writeVarInt(payload.bandOrdinal());
                    buffer.writeVarInt(payload.cooldownTicks());
                    buffer.writeUtf(payload.abilityId());
                    buffer.writeUtf(payload.result());
                },
                buffer -> new CombatSyncPayload(
                        buffer.readFloat(),
                        buffer.readFloat(),
                        buffer.readFloat(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readUtf(),
                        buffer.readUtf()
                )
        );

        @Override
        public Type<CombatSyncPayload> type() {
            return TYPE;
        }
    }

    public record BarrierStatePayload(
            UUID playerId,
            float integrity,
            int phaseOrdinal,
            int gestureOrdinal,
            int phaseTicks
    ) implements CustomPacketPayload {
        public static final Type<BarrierStatePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "barrier_state")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, BarrierStatePayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUUID(payload.playerId());
                    buffer.writeFloat(payload.integrity());
                    buffer.writeVarInt(payload.phaseOrdinal());
                    buffer.writeVarInt(payload.gestureOrdinal());
                    buffer.writeVarInt(payload.phaseTicks());
                },
                buffer -> new BarrierStatePayload(
                        buffer.readUUID(),
                        buffer.readFloat(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt()
                )
        );

        @Override
        public Type<BarrierStatePayload> type() {
            return TYPE;
        }
    }

    public record TrainingStatePayload(
            boolean active,
            long anchorPos,
            int phaseOrdinal,
            int phaseSuccesses,
            int phaseRequired,
            int totalFailures,
            int pulseStateOrdinal,
            int stateTicks
    ) implements CustomPacketPayload {
        public static final Type<TrainingStatePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_training_state")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, TrainingStatePayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeBoolean(payload.active());
                    buffer.writeLong(payload.anchorPos());
                    buffer.writeVarInt(payload.phaseOrdinal());
                    buffer.writeVarInt(payload.phaseSuccesses());
                    buffer.writeVarInt(payload.phaseRequired());
                    buffer.writeVarInt(payload.totalFailures());
                    buffer.writeVarInt(payload.pulseStateOrdinal());
                    buffer.writeVarInt(payload.stateTicks());
                },
                buffer -> new TrainingStatePayload(
                        buffer.readBoolean(),
                        buffer.readLong(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt()
                )
        );

        @Override
        public Type<TrainingStatePayload> type() {
            return TYPE;
        }
    }

    public record AnchorMenuPayload(long anchorPos, boolean hasActiveRna) implements CustomPacketPayload {
        public static final Type<AnchorMenuPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_training_anchor_menu")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, AnchorMenuPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeLong(payload.anchorPos());
                    buffer.writeBoolean(payload.hasActiveRna());
                },
                buffer -> new AnchorMenuPayload(buffer.readLong(), buffer.readBoolean())
        );

        @Override
        public Type<AnchorMenuPayload> type() {
            return TYPE;
        }
    }

    public record AnchorMenuSelectPayload(long anchorPos, String option) implements CustomPacketPayload {
        public static final String OPTION_FORMATION = "formation";
        public static final String OPTION_BARRIER_TRAINING = "barrier_training";

        public static final Type<AnchorMenuSelectPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_training_anchor_menu_select")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, AnchorMenuSelectPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeLong(payload.anchorPos());
                    buffer.writeUtf(payload.option());
                },
                buffer -> new AnchorMenuSelectPayload(buffer.readLong(), buffer.readUtf())
        );

        @Override
        public Type<AnchorMenuSelectPayload> type() {
            return TYPE;
        }
    }
}
