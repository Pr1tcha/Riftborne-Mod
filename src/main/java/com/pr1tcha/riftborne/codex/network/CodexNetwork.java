package com.pr1tcha.riftborne.codex.network;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.codex.block.CodexDiagnosticCapsuleBlockEntity;
import com.pr1tcha.riftborne.codex.block.CodexLaptopBlockEntity;
import com.pr1tcha.riftborne.codex.client.CodexClient;
import com.pr1tcha.riftborne.codex.data.CodexData;
import com.pr1tcha.riftborne.codex.data.PocketCodexData;
import com.pr1tcha.riftborne.codex.data.PocketCodexMode;
import com.pr1tcha.riftborne.codex.data.entry.CodexInfobaseSnapshot;
import com.pr1tcha.riftborne.codex.data.entry.CodexEntryDefinition;
import com.pr1tcha.riftborne.codex.data.entry.CodexEntryRegistry;
import com.pr1tcha.riftborne.codex.item.PocketCodexItem;
import com.pr1tcha.riftborne.codex.PocketCodexScanner;
import com.pr1tcha.riftborne.player.RiftbornePlayerData;
import com.pr1tcha.riftborne.registry.ModContent;
import com.pr1tcha.riftborne.rna.power.PowerApi;
import com.pr1tcha.riftborne.rna.power.PowerRules;
import com.pr1tcha.riftborne.rna.power.Primitive;
import com.pr1tcha.riftborne.rna.power.data.PowerProgress;
import com.pr1tcha.riftborne.rna.power.data.RNAProfile;
import java.util.ArrayList;
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
import software.bernie.geckolib.animatable.GeoItem;

public final class CodexNetwork {
    private static final String NETWORK_VERSION = "6";
    private static final String SEPARATOR = "\u001F";
    private static final String FIELD_SEPARATOR = "\u001D";

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
        RNAProfile rna = PowerApi.get(player);
        PowerProgress progress = PowerApi.getProgress(player);
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
                rna.active(),
                (int) Math.round(rna.nodeDensity()),
                rna.connectivity(),
                (int) Math.round(rna.throughput()),
                (int) Math.round(rna.overloadRes()),
                Math.round(rna.metaWear()),
                PowerRules.wearBand(rna.metaWear()),
                rna.formationPath().toUpperCase(java.util.Locale.ROOT),
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
                diagnostic == null ? "" : diagnostic.techniqueNotice(),
                join(encodePrimitiveLevels(rna)),
                join(encodeAxisPractice(progress)),
                progress.facet().map(f -> f.signature()).orElse(""),
                0.0F,
                0.0F,
                "",
                desktopLayout,
                CodexInfobaseSnapshot.encode(player)
        );
    }

    /** Primitive execution levels for the Codex readout: {@code id,level}, strongest first. */
    private static List<String> encodePrimitiveLevels(RNAProfile profile) {
        List<String> out = new ArrayList<>();
        for (Primitive primitive : Primitive.values()) {
            out.add(primitive.id() + "," + profile.level(primitive));
        }
        out.sort((a, b) -> Integer.compare(
                Integer.parseInt(b.split(",")[1]), Integer.parseInt(a.split(",")[1])));
        return out;
    }

    /** Accumulated Δ-axis practice: {@code axis,count}, most practised first. */
    private static List<String> encodeAxisPractice(PowerProgress progress) {
        List<String> out = new ArrayList<>();
        progress.practiceByAxis().forEach((axis, count) -> out.add(axis + "," + count));
        out.sort((a, b) -> Integer.compare(
                Integer.parseInt(b.split(",")[1]), Integer.parseInt(a.split(",")[1])));
        return out;
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
                PocketCodexData.mode(stack).ordinal(),
                join(PocketCodexData.shortEntries(stack)),
                join(PocketCodexData.queuedEntries(stack)),
                join(PocketCodexData.damagedEntries(stack)),
                encodePocketBuffer(stack),
                noticeKey,
                PocketCodexData.lastTarget(stack),
                entryTitle(PocketCodexData.lastTarget(stack)),
                PocketCodexData.lastResult(stack),
                PocketCodexData.observationCount(stack, PocketCodexData.lastTarget(stack)),
                PocketCodexData.MAX_OBSERVATIONS,
                entryThreat(PocketCodexData.lastTarget(stack)),
                PocketCodexData.lastPulseCount(stack),
                entryTitle(PocketCodexData.lastPulseNearest(stack)),
                PocketCodexData.lastPulseDistance(stack),
                PocketCodexData.BUFFER_CAPACITY,
                openScreen
        ));
    }

    private static String encodePocketBuffer(net.minecraft.world.item.ItemStack stack) {
        List<String> queued = PocketCodexData.queuedEntries(stack);
        List<String> damaged = PocketCodexData.damagedEntries(stack);
        List<String> rows = new ArrayList<>();
        for (String entryId : PocketCodexData.shortEntries(stack)) {
            String state = damaged.contains(entryId) ? "DAMAGED" : queued.contains(entryId) ? "QUEUED" : "STORED";
            rows.add(String.join(FIELD_SEPARATOR,
                    entryId,
                    entryTitle(entryId),
                    Integer.toString(PocketCodexData.observationCount(stack, entryId)),
                    state
            ));
        }
        return join(rows);
    }

    private static String entryTitle(String entryId) {
        if (entryId == null || entryId.isBlank()) {
            return "";
        }
        String qualified = entryId.contains(":") ? entryId : Riftborne.MODID + ":" + entryId;
        CodexEntryDefinition entry = CodexEntryRegistry.get(qualified);
        return entry == null ? entryId : entry.title();
    }

    private static int entryThreat(String entryId) {
        if (entryId == null || entryId.isBlank()) {
            return 0;
        }
        String qualified = entryId.contains(":") ? entryId : Riftborne.MODID + ":" + entryId;
        CodexEntryDefinition entry = CodexEntryRegistry.get(qualified);
        return entry == null ? 0 : entry.threatLevel();
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
        PocketCodexData.cycleMode(codex, payload.direction());
        sendPocketSnapshot(player, codex, false, "codex.riftborne.pocket.notice.mode");
    }

    private static void handlePocketAction(PocketActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        net.minecraft.world.item.ItemStack codex = heldPocketCodex(player);
        if (codex.isEmpty()) {
            return;
        }

        PocketCodexMode mode = PocketCodexData.mode(codex);
        String notice = switch (mode) {
            case SCANNER -> handlePocketScan(player, codex);
            case PULSE -> handlePocketPulse(player, codex);
            case BUFFER -> PocketCodexData.bufferSize(codex) == 0
                    ? "codex.riftborne.pocket.notice.buffer_empty"
                    : "codex.riftborne.pocket.notice.buffer_ready";
        };
        sendPocketSnapshot(player, codex, false, notice);
    }

    private static String handlePocketScan(ServerPlayer player, net.minecraft.world.item.ItemStack codex) {
        PocketCodexScanner.ScanReport report = PocketCodexScanner.scan(player, codex);
        triggerPocketAnimation(player, codex,
                report.result() == PocketCodexScanner.Result.NO_TARGET
                        || report.result() == PocketCodexScanner.Result.BUFFER_FULL
                        || report.result() == PocketCodexScanner.Result.INVALID_TARGET
                        ? "warning"
                        : "scan");
        return switch (report.result()) {
            case DISCOVERED -> "codex.riftborne.pocket.notice.discovered";
            case UPDATED -> "codex.riftborne.pocket.notice.updated";
            case DAMAGED -> "codex.riftborne.pocket.notice.damaged";
            case BUFFER_FULL -> "codex.riftborne.pocket.notice.buffer_full";
            case INVALID_TARGET -> "codex.riftborne.pocket.notice.invalid_target";
            case NO_TARGET -> "codex.riftborne.pocket.notice.no_target";
        };
    }

    private static String handlePocketPulse(ServerPlayer player, net.minecraft.world.item.ItemStack codex) {
        PocketCodexScanner.PulseReport report = PocketCodexScanner.pulse(player, codex);
        triggerPocketAnimation(player, codex, "pulse");
        return report.signalCount() > 0
                ? "codex.riftborne.pocket.notice.signals_found"
                : "codex.riftborne.pocket.notice.area_clear";
    }

    private static void triggerPocketAnimation(
            ServerPlayer player,
            net.minecraft.world.item.ItemStack codex,
            String animation
    ) {
        if (codex.getItem() instanceof PocketCodexItem pocketCodexItem) {
            long animationId = GeoItem.getOrAssignId(codex, player.serverLevel());
            pocketCodexItem.triggerAnim(player, animationId, "action", animation);
        }
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
            String techniqueProgress,
            String techniqueReadiness,
            String aspectResonance,
            float physicalOverallForm,
            float physicalOverloadCapacity,
            String physicalProfile,
            String desktopLayout,
            String infobaseData
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
                    buffer.writeUtf(payload.techniqueProgress);
                    buffer.writeUtf(payload.techniqueReadiness);
                    buffer.writeUtf(payload.aspectResonance);
                    buffer.writeFloat(payload.physicalOverallForm);
                    buffer.writeFloat(payload.physicalOverloadCapacity);
                    buffer.writeUtf(payload.physicalProfile);
                    buffer.writeUtf(payload.desktopLayout);
                    buffer.writeUtf(payload.infobaseData, 1_000_000);
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
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readFloat(),
                        buffer.readFloat(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readUtf(1_000_000)
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
            int mode,
            String shortEntries,
            String queuedEntries,
            String damagedEntries,
            String bufferRows,
            String noticeKey,
            String lastTargetId,
            String lastTargetTitle,
            String lastResult,
            int observations,
            int maximumObservations,
            int threat,
            int pulseSignals,
            String pulseNearestTitle,
            int pulseNearestDistance,
            int bufferCapacity,
            boolean openScreen
    ) implements CustomPacketPayload {
        public static final Type<PocketSnapshotPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "pocket_codex_snapshot")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, PocketSnapshotPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeVarInt(payload.mode);
                    buffer.writeUtf(payload.shortEntries);
                    buffer.writeUtf(payload.queuedEntries);
                    buffer.writeUtf(payload.damagedEntries);
                    buffer.writeUtf(payload.bufferRows);
                    buffer.writeUtf(payload.noticeKey);
                    buffer.writeUtf(payload.lastTargetId);
                    buffer.writeUtf(payload.lastTargetTitle);
                    buffer.writeUtf(payload.lastResult);
                    buffer.writeVarInt(payload.observations);
                    buffer.writeVarInt(payload.maximumObservations);
                    buffer.writeVarInt(payload.threat);
                    buffer.writeVarInt(payload.pulseSignals);
                    buffer.writeUtf(payload.pulseNearestTitle);
                    buffer.writeVarInt(payload.pulseNearestDistance);
                    buffer.writeVarInt(payload.bufferCapacity);
                    buffer.writeBoolean(payload.openScreen);
                },
                buffer -> new PocketSnapshotPayload(
                        buffer.readVarInt(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
                        buffer.readUtf(),
                        buffer.readVarInt(),
                        buffer.readVarInt(),
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
