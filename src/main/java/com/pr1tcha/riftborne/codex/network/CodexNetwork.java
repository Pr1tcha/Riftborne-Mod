package com.pr1tcha.riftborne.codex.network;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.codex.block.CodexDiagnosticCapsuleBlockEntity;
import com.pr1tcha.riftborne.codex.block.CodexLaptopBlockEntity;
import com.pr1tcha.riftborne.codex.client.CodexClient;
import com.pr1tcha.riftborne.codex.data.CodexData;
import com.pr1tcha.riftborne.codex.data.PocketCodexData;
import com.pr1tcha.riftborne.codex.PocketCodexScanner;
import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import com.pr1tcha.riftborne.registry.ModContent;
import com.pr1tcha.riftborne.rna.RnaApi;
import com.pr1tcha.riftborne.rna.data.RnaData;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
        registrar.playToServer(TransferEntryPayload.TYPE, TransferEntryPayload.STREAM_CODEC, CodexNetwork::handleTransferEntry);
        registrar.playToServer(UpdateDesktopLayoutPayload.TYPE, UpdateDesktopLayoutPayload.STREAM_CODEC,
                CodexNetwork::handleUpdateDesktopLayout);
    }

    public static void open(ServerPlayer player) {
        open(player, BlockPos.ZERO);
    }

    public static void open(ServerPlayer player, BlockPos laptopPos) {
        PacketDistributor.sendToPlayer(player, createSnapshot(player, laptopPos));
    }

    private static SnapshotPayload createSnapshot(ServerPlayer player, BlockPos laptopPos) {
        CodexData codex = RiftbornePlayerData.getCodex(player);
        RnaData rna = RnaApi.get(player);
        boolean firstFlashInserted = false;
        boolean secondFlashInserted = false;
        String desktopLayout = "";
        if (player.level().getBlockEntity(laptopPos) instanceof CodexLaptopBlockEntity laptop) {
            firstFlashInserted = laptop.hasFlashDrive(0);
            secondFlashInserted = laptop.hasFlashDrive(1);
            desktopLayout = laptop.desktopLayout();
        }
        CodexDiagnosticCapsuleBlockEntity diagnostic = findDiagnosticCapsule(player, laptopPos);
        return new SnapshotPayload(
                laptopPos.asLong(),
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
                rna.formationPath().name(),
                firstFlashInserted,
                secondFlashInserted,
                "",
                "",
                diagnostic != null && diagnostic.hasDiagnosticData(),
                diagnostic == null ? 0L : diagnostic.getBlockPos().asLong(),
                diagnostic == null ? "" : diagnostic.subjectName(),
                diagnostic != null && diagnostic.hasRna(),
                diagnostic == null ? 0 : diagnostic.nodeDensity(),
                diagnostic == null ? 0 : diagnostic.connectivity(),
                diagnostic == null ? 0 : diagnostic.throughput(),
                diagnostic == null ? 0 : diagnostic.overloadResistance(),
                diagnostic == null ? 0 : diagnostic.metaWear(),
                diagnostic == null ? "STABLE" : diagnostic.metaWearStage(),
                diagnostic == null ? "UNKNOWN" : diagnostic.formationPath(),
                "",
                desktopLayout
        );
    }

    private static CodexDiagnosticCapsuleBlockEntity findDiagnosticCapsule(ServerPlayer player, BlockPos laptopPos) {
        if (laptopPos.equals(BlockPos.ZERO)) {
            return null;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (player.level().getBlockEntity(laptopPos.relative(direction))
                    instanceof CodexDiagnosticCapsuleBlockEntity capsule) {
                return capsule;
            }
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -3; x <= 3; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    cursor.set(laptopPos.getX() + x, laptopPos.getY() + y, laptopPos.getZ() + z);
                    if (player.level().getBlockEntity(cursor) instanceof CodexDiagnosticCapsuleBlockEntity capsule) {
                        return capsule;
                    }
                }
            }
        }
        return null;
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
            open(player, BlockPos.of(payload.laptopPos()));
        }
    }

    public record SnapshotPayload(
            long laptopPos,
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
            String formationPath,
            boolean firstFlashInserted,
            boolean secondFlashInserted,
            String firstFlashEntries,
            String secondFlashEntries,
            boolean diagnosticAvailable,
            long diagnosticCapsulePos,
            String diagnosticSubjectName,
            boolean diagnosticHasRna,
            int diagnosticNodeDensity,
            int diagnosticConnectivity,
            int diagnosticThroughput,
            int diagnosticOverloadResistance,
            int diagnosticMetaWear,
            String diagnosticMetaWearStage,
            String diagnosticFormationPath,
            String diagnosticNotice,
            String desktopLayout
    ) implements CustomPacketPayload {
        public static final Type<SnapshotPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "codex_snapshot")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, SnapshotPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeLong(payload.laptopPos);
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
                    buffer.writeBoolean(payload.firstFlashInserted);
                    buffer.writeBoolean(payload.secondFlashInserted);
                    buffer.writeUtf(payload.firstFlashEntries);
                    buffer.writeUtf(payload.secondFlashEntries);
                    buffer.writeBoolean(payload.diagnosticAvailable);
                    buffer.writeLong(payload.diagnosticCapsulePos);
                    buffer.writeUtf(payload.diagnosticSubjectName);
                    buffer.writeBoolean(payload.diagnosticHasRna);
                    buffer.writeVarInt(payload.diagnosticNodeDensity);
                    buffer.writeVarInt(payload.diagnosticConnectivity);
                    buffer.writeVarInt(payload.diagnosticThroughput);
                    buffer.writeVarInt(payload.diagnosticOverloadResistance);
                    buffer.writeVarInt(payload.diagnosticMetaWear);
                    buffer.writeUtf(payload.diagnosticMetaWearStage);
                    buffer.writeUtf(payload.diagnosticFormationPath);
                    buffer.writeUtf(payload.diagnosticNotice);
                    buffer.writeUtf(payload.desktopLayout);
                },
                buffer -> new SnapshotPayload(
                        buffer.readLong(),
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
                        buffer.readUtf(),
                        buffer.readBoolean(),
                        buffer.readBoolean(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readBoolean(),
                        buffer.readLong(),
                        buffer.readUtf(),
                        buffer.readBoolean(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readUtf()
                )
        );

        @Override
        public Type<SnapshotPayload> type() {
            return TYPE;
        }
    }

    public record TogglePowerPayload(boolean powered, long laptopPos) implements CustomPacketPayload {
        public static final Type<TogglePowerPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "codex_toggle_power")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, TogglePowerPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeBoolean(payload.powered);
                    buffer.writeLong(payload.laptopPos);
                },
                buffer -> new TogglePowerPayload(buffer.readBoolean(), buffer.readLong())
        );

        @Override
        public Type<TogglePowerPayload> type() {
            return TYPE;
        }
    }

    private static void handleUpdateDesktopLayout(UpdateDesktopLayoutPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos pos = BlockPos.of(payload.laptopPos());
        if (player.level().getBlockEntity(pos) instanceof CodexLaptopBlockEntity laptop) {
            laptop.setDesktopLayout(payload.layout());
        }
    }

    public record UpdateDesktopLayoutPayload(long laptopPos, String layout) implements CustomPacketPayload {
        public static final Type<UpdateDesktopLayoutPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "codex_desktop_layout")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateDesktopLayoutPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeLong(payload.laptopPos);
                    buffer.writeUtf(payload.layout);
                },
                buffer -> new UpdateDesktopLayoutPayload(buffer.readLong(), buffer.readUtf())
        );

        @Override
        public Type<UpdateDesktopLayoutPayload> type() {
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
        open(player, BlockPos.of(payload.laptopPos()));
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

    public record RestoreDamagedPayload(long laptopPos, String entryId) implements CustomPacketPayload {
        public static final Type<RestoreDamagedPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "codex_restore_damaged")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, RestoreDamagedPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeLong(payload.laptopPos);
                    buffer.writeUtf(payload.entryId);
                },
                buffer -> new RestoreDamagedPayload(buffer.readLong(), buffer.readUtf())
        );

        @Override
        public Type<RestoreDamagedPayload> type() {
            return TYPE;
        }
    }

    private static void handleTransferEntry(TransferEntryPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        CodexData data = RiftbornePlayerData.getCodex(player);
        if (data.synchronize(List.of(payload.entryId())) > 0) {
            data.addTranslatedNotification("codex.riftborne.feed.synchronized", payload.entryId());
            RiftbornePlayerData.saveCodex(player, data);
        }
        open(player, BlockPos.of(payload.laptopPos()));
    }

    public record TransferEntryPayload(long laptopPos, int driveSlot, String entryId) implements CustomPacketPayload {
        public static final Type<TransferEntryPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "codex_transfer_entry")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, TransferEntryPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeLong(payload.laptopPos);
                    buffer.writeVarInt(payload.driveSlot);
                    buffer.writeUtf(payload.entryId);
                },
                buffer -> new TransferEntryPayload(buffer.readLong(), buffer.readVarInt(), buffer.readUtf())
        );

        @Override
        public Type<TransferEntryPayload> type() {
            return TYPE;
        }
    }
}
