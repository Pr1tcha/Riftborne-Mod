package com.pr1tcha.riftborne.physical.pushup;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.physical.pushup.client.PushupClient;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PhysicalTrainingNetwork {
    private static final String NETWORK_VERSION = "3";

    private PhysicalTrainingNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(PushupInputPayload.TYPE, PushupInputPayload.STREAM_CODEC,
                PhysicalTrainingNetwork::handleInput);
        registrar.playToClient(PushupStatePayload.TYPE, PushupStatePayload.STREAM_CODEC,
                PhysicalTrainingNetwork::handleState);
    }

    public static void broadcastState(ServerPlayer owner, PushupTrainingManager.Session session) {
        if (owner.getServer() == null) {
            return;
        }
        PushupStatePayload payload = new PushupStatePayload(
                owner.getUUID(),
                true,
                session.phase().ordinal(),
                session.stateTicks(),
                session.repetitions(),
                session.failures(),
                session.yaw()
        );
        for (ServerPlayer recipient : owner.getServer().getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(recipient, payload);
        }
    }

    public static void broadcastInactive(ServerPlayer owner) {
        if (owner.getServer() == null) {
            return;
        }
        PushupStatePayload payload = new PushupStatePayload(
                owner.getUUID(), false, PushupPhase.IDLE.ordinal(), 0, 0, 0, 0.0F
        );
        for (ServerPlayer recipient : owner.getServer().getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(recipient, payload);
        }
    }

    private static void handleInput(PushupInputPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            if (payload.stop()) {
                PushupTrainingManager.stop(player, true);
            } else {
                PushupTrainingManager.handleInput(player);
            }
        }
    }

    private static void handleState(PushupStatePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            PushupClient.handleState(payload);
        }
    }

    public record PushupInputPayload(int sequence, boolean stop) implements CustomPacketPayload {
        public static final Type<PushupInputPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "pushup_input")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, PushupInputPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeVarInt(payload.sequence());
                    buffer.writeBoolean(payload.stop());
                },
                buffer -> new PushupInputPayload(buffer.readVarInt(), buffer.readBoolean())
        );

        @Override
        public Type<PushupInputPayload> type() {
            return TYPE;
        }
    }

    public record PushupStatePayload(
            UUID playerId,
            boolean active,
            int phaseOrdinal,
            int stateTicks,
            int repetitions,
            int failures,
            float bodyYaw
    ) implements CustomPacketPayload {
        public static final Type<PushupStatePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "pushup_state")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, PushupStatePayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUUID(payload.playerId());
                    buffer.writeBoolean(payload.active());
                    buffer.writeVarInt(payload.phaseOrdinal());
                    buffer.writeVarInt(payload.stateTicks());
                    buffer.writeVarInt(payload.repetitions());
                    buffer.writeVarInt(payload.failures());
                    buffer.writeFloat(payload.bodyYaw());
                },
                buffer -> new PushupStatePayload(
                        buffer.readUUID(),
                        buffer.readBoolean(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readFloat()
                )
        );

        @Override
        public Type<PushupStatePayload> type() {
            return TYPE;
        }
    }
}
