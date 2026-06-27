package com.pr1tcha.riftborne.rna.combat;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.rna.combat.client.RnaCombatClient;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityData;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityResult;
import com.pr1tcha.riftborne.rna.combat.data.RnaAbilityUseContext;
import com.pr1tcha.riftborne.rna.combat.data.RnaLoadBand;
import com.pr1tcha.riftborne.rna.combat.registry.RnaAbilityRegistry;
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

public final class RnaCombatNetwork {
    private static final String NETWORK_VERSION = "1";

    private RnaCombatNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(ActivateSkillPayload.TYPE, ActivateSkillPayload.STREAM_CODEC, RnaCombatNetwork::handleActivateSkill);
        registrar.playToClient(CombatSyncPayload.TYPE, CombatSyncPayload.STREAM_CODEC, RnaCombatNetwork::handleCombatSync);
    }

    public static void sendSync(ServerPlayer player, ResourceLocation abilityId, RnaAbilityResult result) {
        RnaAbilityData data = RnaAbilityManager.getData(player);
        RnaLoadBand band = RnaAbilityManager.loadBand(data);
        int cooldown = abilityId == null ? 0 : (int) Math.min(Integer.MAX_VALUE, RnaAbilityManager.remainingCooldown(player, abilityId));
        PacketDistributor.sendToPlayer(player, new CombatSyncPayload(
                data.currentLoad(),
                band.ordinal(),
                cooldown,
                abilityId == null ? "" : abilityId.toString(),
                result == null ? "" : result.name()
        ));
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
                    buffer.writeVarInt(payload.bandOrdinal());
                    buffer.writeVarInt(payload.cooldownTicks());
                    buffer.writeUtf(payload.abilityId());
                    buffer.writeUtf(payload.result());
                },
                buffer -> new CombatSyncPayload(
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
}
