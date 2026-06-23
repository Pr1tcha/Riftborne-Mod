package com.pr1tcha.riftborne.riftwalker.network;

import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.riftwalker.item.RiftwalkerArmorItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RiftwalkerNetwork {
    private RiftwalkerNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                ToggleMaskLightsPayload.TYPE,
                ToggleMaskLightsPayload.STREAM_CODEC,
                RiftwalkerNetwork::handleToggleMaskLights
        );
    }

    private static void handleToggleMaskLights(ToggleMaskLightsPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof RiftwalkerArmorItem)) {
            return;
        }
        boolean enabled = RiftwalkerArmorItem.toggleMaskLights(helmet);
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        enabled
                                ? "message.riftborne.riftwalker.mask_lights_on"
                                : "message.riftborne.riftwalker.mask_lights_off"
                ),
                true
        );
    }

    public record ToggleMaskLightsPayload() implements CustomPacketPayload {
        public static final Type<ToggleMaskLightsPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "riftwalker_toggle_mask_lights")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, ToggleMaskLightsPayload> STREAM_CODEC =
                StreamCodec.unit(new ToggleMaskLightsPayload());

        @Override
        public Type<ToggleMaskLightsPayload> type() {
            return TYPE;
        }
    }
}
