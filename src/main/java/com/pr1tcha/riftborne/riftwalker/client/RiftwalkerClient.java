package com.pr1tcha.riftborne.riftwalker.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pr1tcha.riftborne.Riftborne;
import com.pr1tcha.riftborne.registry.ModContent;
import com.pr1tcha.riftborne.riftwalker.network.RiftwalkerNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class RiftwalkerClient {
    private static final ResourceLocation FIRST_PERSON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Riftborne.MODID, "textures/armor/riftwalker_first_person.png");
    private static final KeyMapping TOGGLE_MASK_LIGHTS = new KeyMapping(
            "key.riftborne.riftwalker.mask_lights",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.riftborne"
    );

    private RiftwalkerClient() {
    }

    @EventBusSubscriber(modid = Riftborne.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_MASK_LIGHTS);
        }
    }

    @EventBusSubscriber(modid = Riftborne.MODID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        public static void clientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.screen != null || minecraft.getConnection() == null) {
                return;
            }
            while (TOGGLE_MASK_LIGHTS.consumeClick()) {
                if (minecraft.player.getItemBySlot(EquipmentSlot.HEAD).is(ModContent.RIFTWALKER_HOOD.get())) {
                    PacketDistributor.sendToServer(new RiftwalkerNetwork.ToggleMaskLightsPayload());
                }
            }
        }

        @SubscribeEvent
        @SuppressWarnings({"rawtypes", "unchecked"})
        public static void renderArm(RenderArmEvent event) {
            if (!event.getPlayer().getItemBySlot(EquipmentSlot.CHEST).is(ModContent.RIFTWALKER_COAT.get())) {
                return;
            }
            if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(event.getPlayer())
                    instanceof PlayerRenderer renderer)) {
                return;
            }

            PlayerModel model = renderer.getModel();
            VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(FIRST_PERSON_TEXTURE));
            boolean right = event.getArm() == HumanoidArm.RIGHT;
            if (right) {
                model.rightArm.render(event.getPoseStack(), consumer, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
                model.rightSleeve.render(event.getPoseStack(), consumer, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
            } else {
                model.leftArm.render(event.getPoseStack(), consumer, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
                model.leftSleeve.render(event.getPoseStack(), consumer, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
            }
            event.setCanceled(true);
        }
    }
}
