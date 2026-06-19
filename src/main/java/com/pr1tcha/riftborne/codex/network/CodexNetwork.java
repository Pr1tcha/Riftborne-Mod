package com.pr1tcha.riftborne.codex.network;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.codex.client.CodexClient;
import com.pr1tcha.riftborne.codex.data.CodexData;
import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import com.pr1tcha.riftborne.rna.RnaApi;
import com.pr1tcha.riftborne.rna.data.RnaData;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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

public final class CodexNetwork {
    private static final String NETWORK_VERSION = "1";
    private static final String SEPARATOR = "\u001F";

    private CodexNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToClient(SnapshotPayload.TYPE, SnapshotPayload.STREAM_CODEC, CodexNetwork::handleSnapshot);
        registrar.playToServer(TogglePowerPayload.TYPE, TogglePowerPayload.STREAM_CODEC, CodexNetwork::handleTogglePower);
    }

    public static void open(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, createSnapshot(player));
    }

    private static SnapshotPayload createSnapshot(ServerPlayer player) {
        CodexData codex = RiftbornePlayerData.getCodex(player);
        RnaData rna = RnaApi.get(player);
        return new SnapshotPayload(
                codex.devicePowered(),
                codex.battery(),
                join(codex.unlockedEntries().stream().toList()),
                join(codex.notifications()),
                join(codex.recentData()),
                rna.hasRNA(),
                rna.nodeDensity(),
                rna.connectivity(),
                rna.throughput(),
                rna.overloadResistance(),
                rna.metaWear(),
                rna.metaWearStage().name(),
                rna.formationPath().name()
        );
    }

    private static String join(List<String> values) {
        return String.join(SEPARATOR, values);
    }

    public static List<String> split(String value) {
        return value == null || value.isEmpty() ? List.of() : List.of(value.split(SEPARATOR, -1));
    }

    private static void handleSnapshot(SnapshotPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CodexClient.open(payload);
        }
    }

    private static void handleTogglePower(TogglePowerPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            CodexData data = RiftbornePlayerData.getCodex(player);
            data.setDevicePowered(payload.powered());
            RiftbornePlayerData.saveCodex(player, data);
            open(player);
        }
    }

    public record SnapshotPayload(
            boolean powered,
            int battery,
            String unlockedEntries,
            String notifications,
            String recentData,
            boolean hasRna,
            int nodeDensity,
            int connectivity,
            int throughput,
            int overloadResistance,
            int metaWear,
            String metaWearStage,
            String formationPath
    ) implements CustomPacketPayload {
        public static final Type<SnapshotPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "codex_snapshot")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, SnapshotPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeBoolean(payload.powered);
                    buffer.writeVarInt(payload.battery);
                    buffer.writeUtf(payload.unlockedEntries);
                    buffer.writeUtf(payload.notifications);
                    buffer.writeUtf(payload.recentData);
                    buffer.writeBoolean(payload.hasRna);
                    buffer.writeVarInt(payload.nodeDensity);
                    buffer.writeVarInt(payload.connectivity);
                    buffer.writeVarInt(payload.throughput);
                    buffer.writeVarInt(payload.overloadResistance);
                    buffer.writeVarInt(payload.metaWear);
                    buffer.writeUtf(payload.metaWearStage);
                    buffer.writeUtf(payload.formationPath);
                },
                buffer -> new SnapshotPayload(
                        buffer.readBoolean(),
                        buffer.readVarInt(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readBoolean(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readUtf(),
                        buffer.readUtf()
                )
        );

        @Override
        public Type<SnapshotPayload> type() {
            return TYPE;
        }
    }

    public record TogglePowerPayload(boolean powered) implements CustomPacketPayload {
        public static final Type<TogglePowerPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "codex_toggle_power")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, TogglePowerPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                TogglePowerPayload::powered,
                TogglePowerPayload::new
        );

        @Override
        public Type<TogglePowerPayload> type() {
            return TYPE;
        }
    }
}
