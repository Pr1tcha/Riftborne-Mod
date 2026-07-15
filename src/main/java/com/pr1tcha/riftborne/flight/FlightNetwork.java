package com.pr1tcha.riftborne.flight;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.flight.client.FlightClient;
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

public final class FlightNetwork {
    private static final String NETWORK_VERSION = "1";
    public static final byte VISUAL_INACTIVE = 0;
    public static final byte VISUAL_HOVER = 1;
    public static final byte VISUAL_LEVITATION = 2;
    public static final byte VISUAL_FLIGHT = 3;

    private FlightNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(TogglePayload.TYPE, TogglePayload.STREAM_CODEC, FlightNetwork::handleToggle);
        registrar.playToServer(InputPayload.TYPE, InputPayload.STREAM_CODEC, FlightNetwork::handleInput);
        registrar.playToClient(StatePayload.TYPE, StatePayload.STREAM_CODEC, FlightNetwork::handleState);
        registrar.playToClient(VisualStatePayload.TYPE, VisualStatePayload.STREAM_CODEC, FlightNetwork::handleVisualState);
    }

    public static void sendState(ServerPlayer player, boolean active) {
        PacketDistributor.sendToPlayer(player, new StatePayload(active));
    }

    public static void sendVisualState(ServerPlayer player, byte phase, float boost, double motionX, double motionY, double motionZ) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new VisualStatePayload(
                player.getId(),
                phase,
                boost,
                (float) motionX,
                (float) motionY,
                (float) motionZ
        ));
    }

    public static void sendVisualState(ServerPlayer player, boolean active, boolean boosting) {
        sendVisualState(player, active ? (boosting ? VISUAL_FLIGHT : VISUAL_HOVER) : VISUAL_INACTIVE, 0.0F, 0.0D, 0.0D, 0.0D);
    }

    private static void handleToggle(TogglePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            RiftFlightController.setActive(player, payload.active());
        }
    }

    private static void handleInput(InputPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            RiftFlightController.handleInput(player, payload.flags());
        }
    }

    private static void handleState(StatePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FlightClient.setActiveFromServer(payload.active());
        }
    }

    private static void handleVisualState(VisualStatePayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FlightClient.setVisualState(payload.entityId(), payload.phase(), payload.boost(), payload.motionX(), payload.motionY(), payload.motionZ());
        }
    }

    public record TogglePayload(boolean active) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<TogglePayload> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "flight_toggle")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, TogglePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                TogglePayload::active,
                TogglePayload::new
        );

        @Override
        public CustomPacketPayload.Type<TogglePayload> type() {
            return TYPE;
        }
    }

    public record InputPayload(int flags) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<InputPayload> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "flight_input")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, InputPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                InputPayload::flags,
                InputPayload::new
        );

        @Override
        public CustomPacketPayload.Type<InputPayload> type() {
            return TYPE;
        }
    }

    public record StatePayload(boolean active) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<StatePayload> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "flight_state")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                StatePayload::active,
                StatePayload::new
        );

        @Override
        public CustomPacketPayload.Type<StatePayload> type() {
            return TYPE;
        }
    }

    public record VisualStatePayload(int entityId, byte phase, float boost, float motionX, float motionY, float motionZ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<VisualStatePayload> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "flight_visual_state")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, VisualStatePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                VisualStatePayload::entityId,
                ByteBufCodecs.BYTE,
                VisualStatePayload::phase,
                ByteBufCodecs.FLOAT,
                VisualStatePayload::boost,
                ByteBufCodecs.FLOAT,
                VisualStatePayload::motionX,
                ByteBufCodecs.FLOAT,
                VisualStatePayload::motionY,
                ByteBufCodecs.FLOAT,
                VisualStatePayload::motionZ,
                VisualStatePayload::new
        );

        @Override
        public CustomPacketPayload.Type<VisualStatePayload> type() {
            return TYPE;
        }
    }
}
