package com.pr1tcha.riftborne.codex.network;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.codex.client.CodexClient;
import com.pr1tcha.riftborne.codex.data.CodexData;
import com.pr1tcha.riftborne.codex.data.PocketCodexData;
import com.pr1tcha.riftborne.codex.PocketCodexScanner;
import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import com.pr1tcha.riftborne.registry.ModContent;
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
        registrar.playToClient(PocketSnapshotPayload.TYPE, PocketSnapshotPayload.STREAM_CODEC, CodexNetwork::handlePocketSnapshot);
        registrar.playToServer(PocketCyclePayload.TYPE, PocketCyclePayload.STREAM_CODEC, CodexNetwork::handlePocketCycle);
        registrar.playToServer(PocketActionPayload.TYPE, PocketActionPayload.STREAM_CODEC, CodexNetwork::handlePocketAction);
        registrar.playToServer(RestoreDamagedPayload.TYPE, RestoreDamagedPayload.STREAM_CODEC, CodexNetwork::handleRestoreDamaged);
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
                join(codex.queuedEntries().stream().toList()),
                join(codex.damagedEntries().stream().toList()),
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

    public static void openPocket(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        sendPocketSnapshot(player, stack, true, "");
    }

    public static void sendPocketSnapshot(
            ServerPlayer player,
            net.minecraft.world.item.ItemStack stack,
            boolean openScreen,
            String noticeKey
    ) {
        PacketDistributor.sendToPlayer(player, new PocketSnapshotPayload(
                PocketCodexData.selectedScreen(stack),
                join(PocketCodexData.shortEntries(stack)),
                join(PocketCodexData.queuedEntries(stack)),
                join(PocketCodexData.damagedEntries(stack)),
                noticeKey,
                openScreen
        ));
    }

    private static void handlePocketSnapshot(PocketSnapshotPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CodexClient.openPocket(payload);
        }
    }

    private static void handlePocketCycle(PocketCyclePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        net.minecraft.world.item.ItemStack codex = heldPocketCodex(player);
        if (codex.isEmpty()) {
            return;
        }
        PocketCodexData.cycleScreen(codex, payload.direction());
        sendPocketSnapshot(player, codex, false, "codex.riftborne.pocket.notice.screen");
    }

    private static void handlePocketAction(PocketActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        net.minecraft.world.item.ItemStack codex = heldPocketCodex(player);
        if (codex.isEmpty()) {
            return;
        }

        String notice;
        if (PocketCodexData.selectedScreen(codex) != 1) {
            notice = "codex.riftborne.pocket.notice.select_scanner";
        } else {
            notice = switch (PocketCodexScanner.scan(player, codex)) {
                case DISCOVERED -> "codex.riftborne.pocket.notice.discovered";
                case KNOWN -> "codex.riftborne.pocket.notice.known";
                case NO_TARGET -> "codex.riftborne.pocket.notice.no_target";
            };
        }
        sendPocketSnapshot(player, codex, false, notice);
    }

    private static net.minecraft.world.item.ItemStack heldPocketCodex(ServerPlayer player) {
        if (player.getMainHandItem().is(ModContent.POCKET_CODEX.get())) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(ModContent.POCKET_CODEX.get())) {
            return player.getOffhandItem();
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
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
            String queuedEntries,
            String damagedEntries,
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
                    buffer.writeUtf(payload.queuedEntries);
                    buffer.writeUtf(payload.damagedEntries);
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

    private static void handleRestoreDamaged(RestoreDamagedPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        CodexData data = RiftbornePlayerData.getCodex(player);
        if (data.restoreDamaged(payload.entryId())) {
            data.addTranslatedNotification("codex.riftborne.feed.restored", payload.entryId());
            RiftbornePlayerData.saveCodex(player, data);
        }
        open(player);
    }

    public record PocketSnapshotPayload(
            int selectedScreen,
            String shortEntries,
            String queuedEntries,
            String damagedEntries,
            String noticeKey,
            boolean openScreen
    ) implements CustomPacketPayload {
        public static final Type<PocketSnapshotPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "pocket_codex_snapshot")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, PocketSnapshotPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeVarInt(payload.selectedScreen);
                    buffer.writeUtf(payload.shortEntries);
                    buffer.writeUtf(payload.queuedEntries);
                    buffer.writeUtf(payload.damagedEntries);
                    buffer.writeUtf(payload.noticeKey);
                    buffer.writeBoolean(payload.openScreen);
                },
                buffer -> new PocketSnapshotPayload(
                        buffer.readVarInt(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readBoolean()
                )
        );

        @Override
        public Type<PocketSnapshotPayload> type() {
            return TYPE;
        }
    }

    public record PocketCyclePayload(int direction) implements CustomPacketPayload {
        public static final Type<PocketCyclePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "pocket_codex_cycle")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, PocketCyclePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                PocketCyclePayload::direction,
                PocketCyclePayload::new
        );

        @Override
        public Type<PocketCyclePayload> type() {
            return TYPE;
        }
    }

    public record PocketActionPayload() implements CustomPacketPayload {
        public static final Type<PocketActionPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "pocket_codex_action")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, PocketActionPayload> STREAM_CODEC =
                StreamCodec.unit(new PocketActionPayload());

        @Override
        public Type<PocketActionPayload> type() {
            return TYPE;
        }
    }

    public record RestoreDamagedPayload(String entryId) implements CustomPacketPayload {
        public static final Type<RestoreDamagedPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "codex_restore_damaged")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, RestoreDamagedPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                RestoreDamagedPayload::entryId,
                RestoreDamagedPayload::new
        );

        @Override
        public Type<RestoreDamagedPayload> type() {
            return TYPE;
        }
    }
}
