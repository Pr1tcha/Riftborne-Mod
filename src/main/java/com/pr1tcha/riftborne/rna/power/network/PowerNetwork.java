package com.pr1tcha.riftborne.rna.power.network;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.rna.power.PowerCast;
import com.pr1tcha.riftborne.rna.power.Primitive;
import com.pr1tcha.riftborne.rna.power.client.PowerClientState;
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

/**
 * Networking for the PS V2.5 power system. The profile itself rides the synced Data Attachment, so
 * only two messages are needed: a cast request from the client and the resulting outcome back for
 * HUD feedback. Casting is decided entirely server-side — the client only asks.
 */
public final class PowerNetwork {
    private static final String VERSION = "1";

    private PowerNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(CastRequestPayload.TYPE, CastRequestPayload.STREAM_CODEC, PowerNetwork::handleCastRequest);
        registrar.playToClient(CastFeedbackPayload.TYPE, CastFeedbackPayload.STREAM_CODEC, PowerNetwork::handleCastFeedback);
        registrar.playToClient(AnchorMenuPayload.TYPE, AnchorMenuPayload.STREAM_CODEC, PowerNetwork::handleAnchorMenu);
        registrar.playToServer(AnchorMenuSelectPayload.TYPE, AnchorMenuSelectPayload.STREAM_CODEC,
                PowerNetwork::handleAnchorMenuSelect);
    }

    public static void sendAnchorMenu(ServerPlayer player, net.minecraft.core.BlockPos anchorPos, boolean hasActiveRna) {
        PacketDistributor.sendToPlayer(player, new AnchorMenuPayload(anchorPos.asLong(), hasActiveRna));
    }

    private static void handleAnchorMenu(AnchorMenuPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.pr1tcha.riftborne.rna.power.anchor.client.RnaTrainingAnchorClient.openMenu(payload);
        }
    }

    private static void handleAnchorMenuSelect(AnchorMenuSelectPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        // The node is a shell until the training rework lands; both branches only report status.
        String key = AnchorMenuSelectPayload.OPTION_FORMATION.equals(payload.option())
                ? "message.riftborne.training.formation_soon"
                : "message.riftborne.training.training_soon";
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(key), true);
    }

    private static void handleCastRequest(CastRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        Primitive primitive = Primitive.fromId(payload.primitiveId());
        if (primitive == null) {
            return;
        }
        PowerCast.Outcome outcome = PowerCast.cast(player, primitive);
        PacketDistributor.sendToPlayer(player, new CastFeedbackPayload(
                primitive.id(),
                outcome.result().name(),
                outcome.load(),
                outcome.window(),
                outcome.overload()
        ));
    }

    private static void handleCastFeedback(CastFeedbackPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            PowerClientState.handleFeedback(payload);
        }
    }

    public record CastRequestPayload(String primitiveId) implements CustomPacketPayload {
        public static final Type<CastRequestPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "power_cast_request")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, CastRequestPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, CastRequestPayload::primitiveId,
                        CastRequestPayload::new
                );

        @Override
        public Type<CastRequestPayload> type() {
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
        public static final String OPTION_TRAINING = "training";

        public static final Type<AnchorMenuSelectPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "rna_training_anchor_menu_select")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, AnchorMenuSelectPayload> STREAM_CODEC =
                StreamCodec.of(
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

    public record CastFeedbackPayload(
            String primitiveId,
            String result,
            float load,
            float window,
            boolean overload
    ) implements CustomPacketPayload {
        public static final Type<CastFeedbackPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "power_cast_feedback")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, CastFeedbackPayload> STREAM_CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.primitiveId());
                    buffer.writeUtf(payload.result());
                    buffer.writeFloat(payload.load());
                    buffer.writeFloat(payload.window());
                    buffer.writeBoolean(payload.overload());
                },
                buffer -> new CastFeedbackPayload(
                        buffer.readUtf(),
                        buffer.readUtf(),
                        buffer.readFloat(),
                        buffer.readFloat(),
                        buffer.readBoolean()
                )
        );

        @Override
        public Type<CastFeedbackPayload> type() {
            return TYPE;
        }
    }
}
