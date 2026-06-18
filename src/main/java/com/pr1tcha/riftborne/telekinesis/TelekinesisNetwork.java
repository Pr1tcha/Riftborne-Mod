package com.pr1tcha.riftborne.telekinesis;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.telekinesis.client.TelekinesisClient;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class TelekinesisNetwork {
    private static final String NETWORK_VERSION = "1";

    private TelekinesisNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(HoldPayload.TYPE, HoldPayload.STREAM_CODEC, TelekinesisNetwork::handleHold);
        registrar.playToServer(DistancePayload.TYPE, DistancePayload.STREAM_CODEC, TelekinesisNetwork::handleDistance);
        registrar.playToServer(PushPayload.TYPE, PushPayload.STREAM_CODEC, TelekinesisNetwork::handlePush);
        registrar.playToClient(GrabStatePayload.TYPE, GrabStatePayload.STREAM_CODEC, TelekinesisNetwork::handleGrabState);
    }

    public static void sendGrabState(ServerPlayer player, boolean active) {
        PacketDistributor.sendToPlayer(player, new GrabStatePayload(active));
    }

    private static void handleHold(HoldPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            TelekinesisAbility.handleHoldInput(player, payload.holding());
        }
    }

    private static void handleDistance(DistancePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            TelekinesisAbility.adjustDistance(player, payload.scrollSteps());
        }
    }

    private static void handlePush(PushPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            TelekinesisAbility.pushHeldTarget(player);
        }
    }

    private static void handleGrabState(GrabStatePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            TelekinesisClient.setHasActiveGrab(payload.active());
        }
    }

    public record HoldPayload(boolean holding) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HoldPayload> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "telekinesis_hold")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, HoldPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                HoldPayload::holding,
                HoldPayload::new
        );

        @Override
        public CustomPacketPayload.Type<HoldPayload> type() {
            return TYPE;
        }
    }

    public record DistancePayload(float scrollSteps) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<DistancePayload> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "telekinesis_distance")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, DistancePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT,
                DistancePayload::scrollSteps,
                DistancePayload::new
        );

        @Override
        public CustomPacketPayload.Type<DistancePayload> type() {
            return TYPE;
        }
    }

    public record PushPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PushPayload> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "telekinesis_push")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, PushPayload> STREAM_CODEC = StreamCodec.unit(new PushPayload());

        @Override
        public CustomPacketPayload.Type<PushPayload> type() {
            return TYPE;
        }
    }

    public record GrabStatePayload(boolean active) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<GrabStatePayload> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "telekinesis_grab_state")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, GrabStatePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                GrabStatePayload::active,
                GrabStatePayload::new
        );

        @Override
        public CustomPacketPayload.Type<GrabStatePayload> type() {
            return TYPE;
        }
    }
}
